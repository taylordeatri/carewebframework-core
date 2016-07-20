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

import org.carewebframework.shell.layout.UIElementBase;
import org.carewebframework.shell.property.PropertyInfo;

import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.SimpleDoubleSpinnerConstraint;

/**
 * Editor for double values.
 */
public class PropertyEditorDouble extends PropertyEditorBase {
    
    private final Doublebox doublebox;
    
    public PropertyEditorDouble() {
        super(new Doublebox());
        doublebox = (Doublebox) component;
    }
    
    @Override
    protected void init(UIElementBase target, PropertyInfo propInfo, PropertyGrid propGrid) {
        super.init(target, propInfo, propGrid);
        doublebox.addForward(Events.ON_CHANGING, propGrid, Events.ON_CHANGE);
        Double min = propInfo.getConfigValueDouble("min", null);
        Double max = propInfo.getConfigValueDouble("max", null);
        
        if (min != null || max != null) {
            SimpleDoubleSpinnerConstraint constraint = new SimpleDoubleSpinnerConstraint();
            constraint.setMin(min);
            constraint.setMax(max);
            doublebox.setConstraint(constraint);
        }
    }
    
    @Override
    protected String getValue() {
        return doublebox.getText();
    }
    
    @Override
    protected void setValue(Object value) {
        doublebox.setText(value == null ? null : value.toString());
        updateValue();
    }
}
