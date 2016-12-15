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
package org.carewebframework.ui.icon;

import java.util.List;

import org.carewebframework.web.component.Combobox;
import org.carewebframework.web.component.Comboitem;
import org.carewebframework.web.component.Div;
import org.carewebframework.web.component.ImagePicker;
import org.carewebframework.web.component.ImagePicker.ImagePickeritem;
import org.carewebframework.web.event.ChangeEvent;

/**
 * Extends the icon picker by adding the ability to pick an icon library from which to choose.
 */
public class IconPicker extends Div {
    
    private final IconLibraryRegistry iconRegistry = IconLibraryRegistry.getInstance();
    
    private IIconLibrary iconLibrary;
    
    private final Combobox cboLibrary = new Combobox();
    
    private final ImagePicker imagePicker = new ImagePicker();
    
    private boolean selectorVisible;
    
    private String dimensions = "16x16";
    
    public IconPicker() {
        addChild(cboLibrary);
        addChild(imagePicker);
        addStyle("overflow", "visible");
        imagePicker.addEventForward(ChangeEvent.class, this, null);
        imagePicker.setShowText(true);
        
        cboLibrary.addEventListener(ChangeEvent.class, (event) -> {
            iconLibrary = (IIconLibrary) cboLibrary.getSelectedItem().getData();
            libraryChanged();
        });
        
        for (IIconLibrary lib : iconRegistry) {
            Comboitem item = new Comboitem(lib.getId());
            item.setData(lib);
            cboLibrary.addChild(item);
        }
        
        setSelectorVisible(true);
    }
    
    public IIconLibrary getIconLibrary() {
        return iconLibrary;
    }
    
    public void setIconLibrary(String iconLibrary) {
        setIconLibrary(iconRegistry.get(iconLibrary));
    }
    
    public void setIconLibrary(IIconLibrary iconLibrary) {
        this.iconLibrary = iconLibrary;
        Comboitem item = (Comboitem) cboLibrary.getChildByData(iconLibrary);
        
        if (item != null) {
            cboLibrary.setSelectedItem(item);
            libraryChanged();
        }
    }
    
    public String getDimensions() {
        return dimensions;
    }
    
    public void setDimensions(String dimensions) {
        this.dimensions = dimensions;
    }
    
    public boolean isSelectorVisible() {
        return selectorVisible;
    }
    
    public void setSelectorVisible(boolean visible) {
        selectorVisible = visible;
        cboLibrary.setVisible(visible && cboLibrary.getChildCount() > 1);
    }
    
    public String getValue() {
        return imagePicker.getValue();
    }
    
    public void setValue(String value) {
        imagePicker.setValue(value);
    }
    
    public ImagePicker getImagePicker() {
        return imagePicker;
    }
    
    private void libraryChanged() {
        imagePicker.clear();
        imagePicker.addChild(new ImagePickeritem());
        
        for (String lib : iconLibrary.getMatching("*", dimensions)) {
            imagePicker.addChild(new ImagePickeritem(lib));
        }
    }
    
    public void addIconByUrl(String url) {
        ImagePickeritem item = new ImagePickeritem(url);
        imagePicker.addChild(item);
    }
    
    public void addIconsByUrl(List<String> urls) {
        for (String url : urls) {
            addIconByUrl(url);
        }
    }
    
}
