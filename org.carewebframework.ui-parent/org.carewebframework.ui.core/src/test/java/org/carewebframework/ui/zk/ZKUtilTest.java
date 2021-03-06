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
package org.carewebframework.ui.zk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.zkoss.zul.A;
import org.zkoss.zul.Div;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.impl.XulElement;

import org.junit.Test;

public class ZKUtilTest {
    
    @Test
    public void updateStyleTest() {
        updateStyleTest("background:red;font-weight:bold;width:20%", "background:red;font-weight:normal;width:20%;",
            "font-weight", "bold", "normal");
        updateStyleTest("background:red;font-weight:bold;width:20%", "background:red;font-weight:bold;", "width", "20%",
            null);
        updateStyleTest("background:red;font-weight:bold;width:20%;",
            "background:red;font-weight:bold;width:20%;height:30%;", "height", null, "30%");
        updateStyleTest(null, "font-weight:bold;", "font-weight", null, "bold");
        updateStyleTest(null, null, "font-weight", null, null);
        updateStyleTest("font-weight:bold", null, "font-weight", "bold", null);
    }
    
    private void updateStyleTest(String oldStyle, String newStyle, String styleName, String oldValue, String newValue) {
        Div cmpt = new Div();
        cmpt.setStyle(oldStyle);
        String result = ZKUtil.updateStyle(cmpt, styleName, newValue);
        assertEquals(newStyle, cmpt.getStyle());
        assertEquals(oldValue, result);
    }
    
    @Test
    public void updateSclassTest() {
        updateSclassTest("cwf-orig1 cwf-orig2", "cwf-new1", "cwf-new2");
        updateSclassTest(null, "cwf-new1", "cwf-new2");
    }
    
    private void updateSclassTest(String initialClass, String class1, String class2) {
        Div cmpt = new Div();
        String pfx = initialClass == null || initialClass.isEmpty() ? "" : initialClass + " ";
        cmpt.setSclass(initialClass);
        String old1 = ZKUtil.updateSclass(cmpt, class1, false);
        assertEquals(pfx + class1, cmpt.getSclass());
        assertEquals(initialClass, old1);
        old1 = cmpt.getSclass();
        String old2 = ZKUtil.updateSclass(cmpt, class2, false);
        assertEquals(pfx + class1 + " " + class2, cmpt.getSclass());
        assertEquals(old1, old2);
        cmpt.setSclass(initialClass);
        old1 = ZKUtil.toggleSclass(cmpt, class1, class2, true);
        assertEquals(initialClass, old1);
        assertEquals(pfx + class1, cmpt.getSclass());
        ZKUtil.toggleSclass(cmpt, class1, class2, false);
        assertEquals(pfx + class2, cmpt.getSclass());
    }
    
    private static final String ATTR_TEST = "ATTR_TEST";
    
    private static final String ATTR_DUMMY = "ATTR_DUMMY";
    
    private static final String ATTR_NULL = "ATTR_NULL";
    
    @Test
    public void attributeTests() {
        Div cmpt = new Div();
        cmpt.setAttribute(ATTR_DUMMY, new Object());
        cmpt.setAttribute(ATTR_TEST, 1234);
        assertEquals(1234, ZKUtil.getAttributeInt(cmpt, ATTR_TEST, 0));
        assertEquals(4321, ZKUtil.getAttributeInt(cmpt, ATTR_DUMMY, 4321));
        assertEquals(5678, ZKUtil.getAttributeInt(cmpt, ATTR_NULL, 5678));
        cmpt.setAttribute(ATTR_TEST, true);
        assertTrue(ZKUtil.getAttributeBoolean(cmpt, ATTR_TEST));
        cmpt.setAttribute(ATTR_TEST, "TRUE");
        assertTrue(ZKUtil.getAttributeBoolean(cmpt, ATTR_TEST));
        cmpt.setAttribute(ATTR_TEST, "ANYTHING BUT TRUE");
        assertTrue(!ZKUtil.getAttributeBoolean(cmpt, ATTR_TEST));
        assertTrue(!ZKUtil.getAttributeBoolean(cmpt, ATTR_DUMMY));
        assertTrue(!ZKUtil.getAttributeBoolean(cmpt, ATTR_NULL));
        cmpt.setAttribute(ATTR_TEST, ATTR_TEST);
        assertEquals(ATTR_TEST, ZKUtil.getAttributeString(cmpt, ATTR_TEST));
        assertTrue("".equals(ZKUtil.getAttributeString(cmpt, ATTR_DUMMY)));
        assertTrue("".equals(ZKUtil.getAttributeString(cmpt, ATTR_NULL)));
        List<Boolean> list = new ArrayList<>();
        cmpt.setAttribute(ATTR_TEST, list);
        assertSame(list, ZKUtil.getAttributeList(cmpt, ATTR_TEST));
        assertNull(ZKUtil.getAttributeList(cmpt, ATTR_DUMMY));
        assertNull(ZKUtil.getAttributeList(cmpt, ATTR_NULL));
        cmpt.setAttribute(ATTR_TEST, cmpt);
        assertSame(cmpt, ZKUtil.getAttributeComponent(cmpt, ATTR_TEST));
        assertNull(ZKUtil.getAttributeComponent(cmpt, ATTR_DUMMY));
        assertSame(cmpt, ZKUtil.getAttributeXulElement(cmpt, ATTR_TEST));
        assertNull(ZKUtil.getAttributeXulElement(cmpt, ATTR_DUMMY));
        assertNull(ZKUtil.getAttributeXulElement(cmpt, ATTR_NULL));
    }
    
    public interface ArgumentMapTest {
        
        void doAssertions();
        
        void setTest2Variable(String value);
    }
    
    @Test
    public void wireArgumentMapTest() {
        Map<Object, Object> map = new HashMap<>();
        map.put("test1Variable", 123);
        map.put("test2Variable", "testing");
        ArgumentMapTest controller = new ArgumentMapTest() {
            
            public int test1Variable;
            
            public String test2;
            
            @Override
            public void setTest2Variable(String value) {
                test2 = value;
            }
            
            @Override
            public void doAssertions() {
                assertEquals(123, test1Variable);
                assertEquals("testing", test2);
            }
        };
        
        ZKUtil.wireController(map, controller);
        controller.doAssertions();
    }
    
    @Test
    public void getTextComponentTest() {
        XulElement cmp;
        cmp = ZKUtil.getTextComponent("general text");
        assertTrue(cmp instanceof Label);
        cmp = ZKUtil.getTextComponent("<html>html text</html>");
        assertTrue(cmp instanceof Html);
        cmp = ZKUtil.getTextComponent("https://url");
        assertTrue(cmp instanceof A);
        cmp = ZKUtil.getTextComponent("http://url");
        assertTrue(cmp instanceof A);
    }
    
    @Test
    public void getResourcePathTest() {
        assertEquals("~./org/carewebframework/ui/zk/", ZKUtil.getResourcePath(ZKUtilTest.class));
        assertEquals("~./org/carewebframework/ui/", ZKUtil.getResourcePath(ZKUtilTest.class, 1));
        assertEquals("~./org/carewebframework/ui/zk/", ZKUtil.getResourcePath(ZKUtilTest.class.getPackage()));
        assertEquals("~./org/carewebframework/", ZKUtil.getResourcePath(ZKUtilTest.class.getPackage(), 2));
        assertEquals("~./org/carewebframework/ui/zk/", ZKUtil.getResourcePath("org.carewebframework.ui.zk"));
        assertEquals("~./org/carewebframework/ui/zk/", ZKUtil.getResourcePath("org.carewebframework.ui.zk", -2));
        assertEquals("~./org/carewebframework/ui/", ZKUtil.getResourcePath("org.carewebframework.ui.zk", 1));
    }
}
