/*
 * #%L
 * carewebframework
 * %%
 * Copyright (C) 2008 - 2016 Regenstrief Institute, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * This Source Code Form is also subject to the terms of the Health-Related
 * Additional Disclaimer of Warranty and Limitation of Liability available at
 *
 *      http://www.carewebframework.org/licensing/disclaimer.
 *
 * #L%
 */
package org.carewebframework.plugin.eventtesting;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.carewebframework.api.event.EventManager;
import org.carewebframework.api.event.EventUtil;
import org.carewebframework.api.event.IEventManager;
import org.carewebframework.api.event.IGenericEvent;
import org.carewebframework.api.messaging.Recipient;
import org.carewebframework.api.messaging.Recipient.RecipientType;
import org.carewebframework.common.JSONUtil;
import org.carewebframework.common.StrUtil;
import org.carewebframework.shell.plugins.PluginController;
import org.carewebframework.ui.zk.ZKUtil;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.SelectEvent;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Textbox;

/**
 * Plug-in to test remote events.
 */
public class MainController extends PluginController implements IGenericEvent<Object> {
    
    private static final long serialVersionUID = 1L;
    
    private Textbox tboxEventName;
    
    private Textbox tboxEventRecipients;
    
    private Textbox tboxEventData;
    
    private Textbox tboxEventResults;
    
    private Textbox tboxNewEvent;
    
    private Listbox lboxEventList;
    
    private Checkbox chkAutoGenerate;
    
    private Checkbox chkScrollLock;
    
    private Label lblInfo;
    
    private final IEventManager eventManager = EventManager.getInstance();
    
    private int messageCount;
    
    public void onClick$btnSend() {
        messageCount++;
        
        if (chkAutoGenerate.isChecked()) {
            tboxEventData.setText("Sending test event #" + messageCount);
        }
        
        eventManager.fireRemoteEvent(tboxEventName.getText(), tboxEventData.getText(), parseRecipients(tboxEventRecipients.getText()));
        info("Fired", tboxEventName.getText());
    }
    
    public void onClick$btnReset() {
        tboxEventName.setText("");
        tboxEventRecipients.setText("");
        tboxEventData.setText("");
    }
    
    public void onClick$btnPing() {
        EventUtil.ping("PING.RESPONSE", null);
    }
    
    public void onClick$btnClear() {
        tboxEventResults.setText("");
    }
    
    public void onClick$btnNewEvent() {
        String eventName = tboxNewEvent.getText().trim();
        
        if (!StringUtils.isEmpty(eventName) && !containsEvent(eventName)) {
            Listitem item = new Listitem(eventName);
            lboxEventList.appendChild(item);
        }
        
        tboxNewEvent.setText("");
    }
    
    private Recipient[] parseRecipients(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        
        List<Recipient> recipients = new ArrayList<>();
        
        for (String recip : text.split("\\,")) {
            String[] pcs = recip.split("\\:", 2);
            
            if (pcs.length == 2) {
                RecipientType type = RecipientType.valueOf(pcs[0].trim().toUpperCase());
                recipients.add(new Recipient(type, pcs[1]));
            }
        }
        
        return recipients.isEmpty() ? null : (Recipient[]) recipients.toArray();
    }
    
    private boolean containsEvent(String eventName) {
        for (Object object : lboxEventList.getItems()) {
            if (((Listitem) object).getLabel().equals(eventName)) {
                return true;
            }
        }
        
        return false;
    }
    
    public void onSelect$lboxEventList(Event event) {
        SelectEvent<?, ?> sel = (SelectEvent<?, ?>) ZKUtil.getEventOrigin(event);
        Listitem item = (Listitem) sel.getReference();
        String eventName = item.getLabel();
        
        if (item.isSelected()) {
            eventManager.subscribe(eventName, this);
            info("Subscribed to", eventName);
        } else {
            eventManager.unsubscribe(eventName, this);
            info("Unsubscribed from", eventName);
        }
    }
    
    private void info(String action, String eventName) {
        lblInfo.setValue(action + " '" + eventName + " ' event.");
    }
    
    @Override
    public void eventCallback(String eventName, Object eventData) {
        String s = tboxEventResults.getText();
        
        if (!(eventData instanceof String)) {
            try {
                eventData = JSONUtil.serialize(eventData, true);
            } catch (Exception e) {}
        }
        
        s += "\n\n" + eventName + ":\n" + eventData;
        tboxEventResults.setText(s);
        info("Received", eventName);
        
        if (!chkScrollLock.isChecked()) {
            String js = StrUtil.formatMessage("jq('#%1$s').scrollTop(jq('#%1$s')[0].scrollHeight);", tboxEventResults.getUuid());
            Clients.evalJavaScript(js);
        }
    }
    
}
