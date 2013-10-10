/**
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
 * If a copy of the MPL was not distributed with this file, You can obtain one at 
 * http://mozilla.org/MPL/2.0/.
 * 
 * This Source Code Form is also subject to the terms of the Health-Related Additional
 * Disclaimer of Warranty and Limitation of Liability available at
 * http://www.carewebframework.org/licensing/disclaimer.
 */
package org.carewebframework.shell.plugins;

import java.util.Properties;

import org.carewebframework.shell.BaseXmlParser;
import org.carewebframework.shell.property.PropertyInfo;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.ParserContext;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Spring xml configuration file parser extension. Supports the definition of plugins within the
 * configuration file in a much more abbreviated fashion than would be required without the
 * extension.
 */
public class PluginXmlParser extends BaseXmlParser {
    
    private enum ResourceType {
        unknown, button, help, menu, property, css, bean, command
    };
    
    @Override
    protected Class<?> getBeanClass(Element element) {
        return PluginDefinition.class;
    }
    
    @Override
    protected void doParse(Element element, BeanDefinitionBuilder builder) {
        doParse(element, null, builder);
    }
    
    @Override
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        builder.setInitMethodName("init");
        builder.setDestroyMethodName("destroy");
        builder.addDependsOn("pluginRegistry");
        builder.addDependsOn("manifestIterator");
        builder.addPropertyReference("pluginRegistry", "pluginRegistry");
        builder.addPropertyValue("path", getResourcePath(parserContext));
        addProperties(element, builder);
        
        Element resourceTag = findTag("resource", element);
        
        if (resourceTag != null) {
            ManagedList<AbstractBeanDefinition> resourceList = new ManagedList<AbstractBeanDefinition>();
            NodeList resources = getTagChildren("resource", element);
            
            for (int i = 0; i < resources.getLength(); i++) {
                parseResources((Element) resources.item(i), builder, resourceList);
            }
            
            builder.addPropertyValue("resources", resourceList);
        }
        
        Element securityTag = findTag("security", element);
        
        if (securityTag != null) {
            addProperties(securityTag, builder);
            ManagedList<AbstractBeanDefinition> privilegeList = new ManagedList<AbstractBeanDefinition>();
            NodeList privileges = getTagChildren("security", element);
            
            for (int i = 0; i < privileges.getLength(); i++) {
                parsePrivileges((Element) privileges.item(i), builder, privilegeList);
            }
            
            builder.addPropertyValue("privileges", privilegeList);
        }
        
        Element serializationTag = findTag("serialization", element);
        
        if (serializationTag != null) {
            addProperties(serializationTag, builder);
            ManagedList<AbstractBeanDefinition> propertyList = new ManagedList<AbstractBeanDefinition>();
            NodeList properties = getTagChildren("serialization", element);
            
            for (int i = 0; i < properties.getLength(); i++) {
                parseProperties((Element) properties.item(i), builder, propertyList);
            }
            
            builder.addPropertyValue("properties", propertyList);
        }
    }
    
    /**
     * Parse the resource list.
     * 
     * @param element Root resource tag.
     * @param builder Bean definition builder.
     * @param resourceList List of resources to build.
     */
    private void parseResources(Element element, BeanDefinitionBuilder builder,
                                ManagedList<AbstractBeanDefinition> resourceList) {
        NodeList resources = element.getChildNodes();
        
        for (int i = 0; i < resources.getLength(); i++) {
            Node node = resources.item(i);
            
            if (!(node instanceof Element)) {
                continue;
            }
            
            Element resource = (Element) resources.item(i);
            Class<? extends PluginResource> resourceClass = null;
            
            switch (getResourceType(getNodeName(resource))) {
                case button:
                    resourceClass = PluginResource.ButtonResource.class;
                    break;
                
                case help:
                    resourceClass = PluginResource.HelpResource.class;
                    break;
                
                case menu:
                    resourceClass = PluginResource.MenuResource.class;
                    break;
                
                case property:
                    resourceClass = PluginResource.PropertyResource.class;
                    break;
                
                case css:
                    resourceClass = PluginResource.CSSResource.class;
                    break;
                
                case bean:
                    resourceClass = PluginResource.BeanResource.class;
                    break;
                
                case command:
                    resourceClass = PluginResource.CommandResource.class;
                    break;
            }
            
            if (resourceClass != null) {
                BeanDefinitionBuilder resourceBuilder = BeanDefinitionBuilder.genericBeanDefinition(resourceClass);
                addProperties(resource, resourceBuilder);
                resourceList.add(resourceBuilder.getBeanDefinition());
            }
        }
    }
    
    /**
     * Parse the privilege list.
     * 
     * @param element Root privilege tag.
     * @param builder Bean definition builder.
     * @param privilegeList List of privileges to return.
     */
    private void parsePrivileges(Element element, BeanDefinitionBuilder builder,
                                 ManagedList<AbstractBeanDefinition> privilegeList) {
        NodeList privileges = element.getChildNodes();
        
        for (int i = 0; i < privileges.getLength(); i++) {
            Node node = privileges.item(i);
            
            if (!(node instanceof Element)) {
                continue;
            }
            
            Element privilege = (Element) node;
            BeanDefinitionBuilder privilegeBuilder = BeanDefinitionBuilder
                    .genericBeanDefinition(PluginDefinition.Privilege.class);
            addProperties(privilege, privilegeBuilder);
            privilegeList.add(privilegeBuilder.getBeanDefinition());
        }
    }
    
    /**
     * Parse the property list.
     * 
     * @param element Root property tag.
     * @param builder Bean definition builder.
     * @param propertyList List of properties to return.
     */
    private void parseProperties(Element element, BeanDefinitionBuilder builder,
                                 ManagedList<AbstractBeanDefinition> propertyList) {
        NodeList properties = element.getChildNodes();
        
        for (int i = 0; i < properties.getLength(); i++) {
            Node node = properties.item(i);
            
            if (!(node instanceof Element)) {
                continue;
            }
            
            Element property = (Element) node;
            BeanDefinitionBuilder propertyBuilder = BeanDefinitionBuilder.genericBeanDefinition(PropertyInfo.class);
            addProperties(property, propertyBuilder);
            parseConfig(property, propertyBuilder);
            propertyList.add(propertyBuilder.getBeanDefinition());
        }
    }
    
    /**
     * Parses out configuration settings for current property descriptor.
     * 
     * @param property Root element
     * @param propertyBuilder Bean definition builder.
     */
    private void parseConfig(Element property, BeanDefinitionBuilder propertyBuilder) {
        Element config = (Element) getTagChildren("config", property).item(0);
        
        if (config != null) {
            Properties properties = new Properties();
            NodeList entries = getTagChildren("entry", config);
            
            for (int i = 0; i < entries.getLength(); i++) {
                Element entry = (Element) entries.item(i);
                String key = entry.getAttribute("key");
                String value = entry.getTextContent();
                properties.put(key, value);
            }
            
            propertyBuilder.addPropertyValue("config", properties);
        }
    }
    
    /**
     * Returns the ResourceType enum value corresponding to the resource tag.
     * 
     * @param resourceTag Tag name of resource
     * @return ResourceType enum. Returns an enum of unknown if tag is not a supported resource
     *         type.
     */
    private ResourceType getResourceType(String resourceTag) {
        if (resourceTag == null || !resourceTag.endsWith("-resource")) {
            return ResourceType.unknown;
        }
        
        try {
            return ResourceType.valueOf(resourceTag.substring(0, resourceTag.length() - 9));
        } catch (Exception e) {
            return ResourceType.unknown;
        }
    }
    
    /**
     * Parses a plugin definition from an xml string.
     * 
     * @param xml XML containing plugin definition.
     * @return A plugin definition instance.
     * @throws Exception
     */
    public static PluginDefinition fromXml(String xml) throws Exception {
        return (PluginDefinition) new PluginXmlParser().fromXml(xml, "plugin");
    }
    
}