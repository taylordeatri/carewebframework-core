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
package org.carewebframework.shell.designer;

import org.carewebframework.common.StrUtil;
import org.carewebframework.ui.zk.PopupDialog;
import org.carewebframework.ui.zk.ZKUtil;

import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.metainfo.PageDefinition;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.ConventionWires;
import org.zkoss.zul.Button;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

/**
 * Allows viewing and editing of clipboard contents.
 */
public class ClipboardViewer extends Window {
    
    private static final long serialVersionUID = 1L;
    
    private Clipboard clipboard;
    
    private Object data;
    
    private Textbox txtData;
    
    private Button btnSave;
    
    private Button btnRestore;
    
    private boolean modified;
    
    private final String MSG_EMPTY = StrUtil.getLabel("cwf.shell.clipboard.viewer.message.empty");
    
    /**
     * Show viewer.
     * 
     * @param clipboard Clipboard whose contents is to be accessed.
     * @throws Exception Unspecified exception.
     */
    public static void execute(Clipboard clipboard) throws Exception {
        PageDefinition def = ZKUtil.loadCachedPageDefinition(DesignConstants.RESOURCE_PREFIX + "ClipboardViewer.zul");
        ClipboardViewer viewer = (ClipboardViewer) PopupDialog.popup(def, null, true, true, false);
        viewer.clipboard = clipboard;
        viewer.data = clipboard.getData();
        ConventionWires.wireVariables(viewer, viewer);
        viewer.restore();
        ConventionWires.addForwards(viewer, viewer);
        Events.addEventListeners(viewer, viewer);
        viewer.doModal();
    }
    
    /**
     * Commit changes in viewer to clipboard.
     * 
     * @return True if successful.
     */
    private boolean commit() {
        if (modified) {
            String text = txtData.getText();
            try {
                clipboard.copy(data instanceof String ? text : data instanceof IClipboardAware ? ((IClipboardAware<?>) data)
                        .fromClipboard(text) : null);
            } catch (Exception e) {
                Clients.wrongValue(txtData, ZKUtil.formatExceptionForDisplay(e));
                txtData.focus();
                return false;
            }
            modified = false;
            updateControls();
        }
        
        return true;
    }
    
    /**
     * Restore changes from clipboard.
     */
    private void restore() {
        String text = data == null ? MSG_EMPTY : data instanceof IClipboardAware ? ((IClipboardAware<?>) data).toClipboard()
                : data.toString();
        txtData.setText(text);
        txtData.setReadonly(!(data instanceof String || data instanceof IClipboardAware));
        modified = false;
        updateControls();
    }
    
    /**
     * Update control states.
     */
    private void updateControls() {
        btnSave.setDisabled(!modified);
        btnRestore.setDisabled(!modified);
        Clients.clearWrongValue(txtData);
    }
    
    /**
     * Detected data edits.
     */
    public void onChanging$txtData() {
        modified = true;
        updateControls();
    }
    
    /**
     * Clicking OK button commits changes and closes viewer.
     * 
     * @throws Exception Unspecified exception.
     */
    public void onClick$btnOK() throws Exception {
        if (commit()) {
            detach();
        }
    }
    
    /**
     * Clicking cancel button discards changes and closes viewer.
     */
    public void onClick$btnCancel() {
        detach();
    }
    
    /**
     * Clicking save button commits changes.
     * 
     * @throws Exception Unspecified exception.
     */
    public void onClick$btnSave() throws Exception {
        commit();
    }
    
    /**
     * Clicking restore button restores original data.
     */
    public void onClick$btnRestore() {
        restore();
    }
}
