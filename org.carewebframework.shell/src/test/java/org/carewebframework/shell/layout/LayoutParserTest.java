/**
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
 * If a copy of the MPL was not distributed with this file, You can obtain one at 
 * http://mozilla.org/MPL/2.0/.
 * 
 * This Source Code Form is also subject to the terms of the Health-Related Additional
 * Disclaimer of Warranty and Limitation of Liability available at
 * http://www.carewebframework.org/licensing/disclaimer.
 */
package org.carewebframework.shell.layout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.carewebframework.shell.CareWebShell;
import org.carewebframework.shell.plugins.PluginContainer;
import org.carewebframework.shell.plugins.PluginDefinition;
import org.carewebframework.shell.plugins.TestPlugin1;
import org.carewebframework.shell.property.PropertyInfo;
import org.carewebframework.ui.test.CommonTest;

import org.zkoss.util.resource.Labels;

import org.junit.Test;

public class LayoutParserTest extends CommonTest {
    
    private final UILayout layout = new UILayout();
    
    @Test
    public void ParserTest() throws Exception {
        String xml = CommonTest.getTextFromResource("LayoutParserTest1.xml");
        layout.loadFromText(xml);
        assertFalse(layout.isEmpty());
        testNode(1, "_menubar");
        testNode(0, "_toolbar");
        testNode(0, "splitterview");
        testNode(1, "splitterpane");
        testProperty("size", "90");
        testProperty("relative", "true");
        testNode(1, "tabview");
        testProperty("orientation", "horizontal");
        testNode(1, "tabpane");
        testNode(1, "treeview");
        testProperty("open", "true");
        testNode(1, "treepane");
        testProperty("path", "Pane 1");
        testNode(0, "treepane");
        testProperty("path", "Pane 2");
        PluginDefinition def = PluginDefinition.getDefinition("treeview");
        assertNotNull(def);
        assertEquals(def.getDescription(), Labels.getLabel("cwf.shell.plugin.treeview.description"));
        UIElementBase ele = def.createElement(null, null);
        assertTrue(ele instanceof UIElementTreeView);
        CareWebShell shell = new CareWebShell();
        shell.afterCompose();
        UIElementDesktop root = shell.getUIDesktop();
        UIElementBase element = layout.deserialize(root);
        assertTrue(element instanceof UIElementMenubar);
        assertTrue(element.hasAncestor(root));
        PluginContainer container1 = shell.getLoadedPlugin("testplugin1");
        assertNotNull(container1);
        TestPlugin1 plugin1 = (TestPlugin1) container1.getFirstChild();
        assertNotNull(plugin1);
        testPlugin(plugin1, 1, 1, 0, 0);
        root.activate(false);
        testPlugin(plugin1, 1, 1, 1, 0);
        root.activate(true);
        testPlugin(plugin1, 1, 2, 1, 0);
        testProperty(container1, "prop1", "value1");
        testProperty(container1, "prop2", 123);
        testProperty(container1, "prop3", true);
        root.removeChildren();
        testPlugin(plugin1, 1, 2, 1, 1);
        assertEquals(container1, PluginContainer.getContainer(plugin1));
    }
    
    private void testProperty(PluginContainer container, String propertyName, Object expectedValue) throws Exception {
        PluginDefinition def = container.getPluginDefinition();
        PropertyInfo propInfo = null;
        
        for (PropertyInfo pi : def.getProperties()) {
            if (pi.getId().equals(propertyName)) {
                propInfo = pi;
                break;
            }
        }
        
        assertNotNull("Property not found: " + propertyName, propInfo);
        assertEquals(expectedValue, container.getPropertyValue(propInfo));
    }
    
    private void testPlugin(TestPlugin1 plugin1, int loadCount, int activateCount, int inactivateCount, int unloadCount) {
        mockEnvironment.flushEvents();
        assertEquals(loadCount, plugin1.getLoadCount());
        assertEquals(activateCount, plugin1.getActivateCount());
        assertEquals(inactivateCount, plugin1.getInactivateCount());
        assertEquals(unloadCount, plugin1.getUnloadCount());
    }
    
    private void testNode(int dir, String name) {
        switch (dir) {
            case 1:
                layout.moveDown();
                break;
            
            case -1:
                layout.moveUp();
                break;
            
            case 0:
                layout.moveNext();
                break;
        }
        
        assertEquals(layout.getObjectName(), name);
    }
    
    private void testProperty(String key, String value) {
        assertEquals(layout.getProperty(key), value);
    }
}