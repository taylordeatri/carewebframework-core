/**
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
 * If a copy of the MPL was not distributed with this file, You can obtain one at 
 * http://mozilla.org/MPL/2.0/.
 * 
 * This Source Code Form is also subject to the terms of the Health-Related Additional
 * Disclaimer of Warranty and Limitation of Liability available at
 * http://www.carewebframework.org/licensing/disclaimer.
 */
package org.carewebframework.ui.jmstesting;

import org.apache.commons.lang.StringUtils;

import org.carewebframework.common.StrUtil;
import org.carewebframework.jms.GlobalEventDispatcher;
import org.carewebframework.jms.MessagingSupport;
import org.carewebframework.shell.plugins.PluginController;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Label;
import org.zkoss.zul.Textbox;

/**
 * Controller class for ActiveMQ Tester.
 */
public class MainController extends PluginController {
    
    private static final long serialVersionUID = 1L;
    
    //members
    
    private Label lblMessage;
    
    //Messaging Support
    private Textbox txtMessageData;
    
    private Textbox txtTopicRecipients;
    
    private Textbox txtDestinationName;
    
    private Textbox txtClientId;
    
    private MessagingSupport messagingSupport;
    
    private GlobalEventDispatcher globalEventDispatcher;
    
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        txtClientId.setValue(globalEventDispatcher.getClientId());
    }
    
    public void onClick$btnProduceLocalMessage(final Event event) {
        //TODO clear local vars, constraints
        final String destName = StringUtils.trimToNull(this.txtDestinationName.getValue());
        final String messageData = StringUtils.trimToNull(this.txtMessageData.getValue());
        this.messagingSupport.produceLocalMessage(destName, messageData);
        showMessage("@cwf.jmstesting.msg.local.complete");
    }
    
    public void onClick$btnProduceTopicMessage(final Event event) {
        final String destName = StringUtils.trimToNull(this.txtDestinationName.getValue());
        final String messageData = StringUtils.trimToNull(this.txtMessageData.getValue());
        final String recipients = StringUtils.trimToNull(this.txtTopicRecipients.getValue());
        this.messagingSupport.produceTopicMessage(destName, messageData, recipients);
        showMessage("@cwf.jmstesting.msg.topic.complete");
    }
    
    public void onClick$btnProduceQueueMessage(final Event event) {
        final String destName = StringUtils.trimToNull(this.txtDestinationName.getValue());
        final String messageData = StringUtils.trimToNull(this.txtMessageData.getValue());
        this.messagingSupport.produceQueueMessage(destName, messageData);
        showMessage("@cwf.jmstesting.msg.queue.complete");
    }
    
    /**
     * Displays message to client
     * 
     * @param message Message to display to client.
     * @param params Message parameters.
     */
    private void showMessage(final String message, Object... params) {
        if (message == null) {
            lblMessage.setVisible(false);
        } else {
            lblMessage.setVisible(true);
            lblMessage.setValue(StrUtil.formatMessage(message, params));
        }
    }
    
    /**
     * @param messagingSupport The messaging support service.
     */
    public void setMessagingSupport(final MessagingSupport messagingSupport) {
        this.messagingSupport = messagingSupport;
    }
    
    /**
     * @param globalEventDispatcher The global event dispatcher.
     */
    public void setGlobalEventDispatcher(final GlobalEventDispatcher globalEventDispatcher) {
        this.globalEventDispatcher = globalEventDispatcher;
    }
    
}