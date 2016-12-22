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
package org.carewebframework.shell.layout;

import org.carewebframework.web.component.Pane;

/**
 * A child of the UIElementSplitterView.
 */
public class UIElementSplitterPane extends UIElementCWFBase {
    
    static {
        registerAllowedParentClass(UIElementSplitterPane.class, UIElementSplitterView.class);
        registerAllowedChildClass(UIElementSplitterPane.class, UIElementBase.class);
    }
    
    private final Pane pane = new Pane();
    
    private int size;
    
    private boolean relative;
    
    public UIElementSplitterPane() {
        super();
        setRelative(true);
        setOuterComponent(pane);
    }
    
    public void setSize(int size) {
        this.size = size;
        updateSize();
    }
    
    public int getSize() {
        return size;
    }
    
    @Override
    public String getInstanceName() {
        return "Pane #" + (getParent().indexOfChild(this) + 1);
    }
    
    public void setRelative(boolean relative) {
        this.relative = relative;
        updateSize();
    }
    
    public boolean isRelative() {
        return relative;
    }
    
    public String getCaption() {
        return pane.getTitle();
    }
    
    public void setCaption(String caption) {
        pane.setTitle(caption);
    }
    
    private void updateSize() {
        if (relative) {
            pane.addStyle("flex", Integer.toString(size));
        } else {
            pane.setHeight(size + "px");
        }
    }
}
