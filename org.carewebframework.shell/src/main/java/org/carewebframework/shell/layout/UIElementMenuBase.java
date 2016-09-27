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

import org.carewebframework.ui.zk.MenuUtil;
import org.carewebframework.web.component.BaseComponent;
import org.carewebframework.web.component.Menu;

/**
 * Base implementation of a menu bar.
 */
public class UIElementMenuBase extends UIElementCWFBase {
    
    private final Menu menubar;
    
    public UIElementMenuBase(Menu menubar) {
        this(menubar, menubar);
    }
    
    public UIElementMenuBase(Menu menubar, BaseComponent root) {
        super();
        this.menubar = menubar;
        menubar.addClass("cwf-menubar");
        setOuterComponent(menubar);
        setInnerComponent(root);
        maxChildren = Integer.MAX_VALUE;
        setMaskMode(null);
    }
    
    /**
     * Returns a reference to the menu bar.
     * 
     * @return The menu bar.
     */
    public Menu getMenubar() {
        return menubar;
    }
    
    @Override
    protected void afterAddChild(UIElementBase child) {
        super.afterAddChild(child);
        updateMenubar();
    }
    
    @Override
    protected void afterRemoveChild(UIElementBase child) {
        super.afterRemoveChild(child);
        updateMenubar();
    }
    
    protected void updateMenubar() {
        MenuUtil.updateStyles(menubar);
    }
}
