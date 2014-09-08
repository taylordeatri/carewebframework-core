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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.carewebframework.api.event.EventManager;
import org.carewebframework.api.event.IEventManager;
import org.carewebframework.shell.AboutDialog;
import org.carewebframework.shell.plugins.PluginDefinition;
import org.carewebframework.shell.plugins.PluginRegistry;
import org.carewebframework.shell.property.PropertyInfo;
import org.carewebframework.ui.zk.PromptDialog;
import org.carewebframework.ui.zk.ZKUtil;

/**
 * This is the base class for all UI elements supported by the CareWeb framework.
 */
public abstract class UIElementBase {
    
    protected static final Log log = LogFactory.getLog(UIElementBase.class);
    
    /**
     * Private inner class to support registering allowed parent - child relationships for UI
     * elements.
     */
    private static class RelatedClassMap {
        
        private final Map<Class<? extends UIElementBase>, List<Class<? extends UIElementBase>>> map = new HashMap<Class<? extends UIElementBase>, List<Class<? extends UIElementBase>>>();
        
        /**
         * Returns the list of related classes for the specified class. If no list yet exists, one
         * is created.
         * 
         * @param clazz Class whose related class list is sought.
         * @return The list of classes related to the specified class.
         */
        private List<Class<? extends UIElementBase>> getRelatedClasses(Class<? extends UIElementBase> clazz) {
            List<Class<? extends UIElementBase>> list = map.get(clazz);
            
            if (list == null) {
                list = new ArrayList<Class<? extends UIElementBase>>();
                map.put(clazz, list);
            }
            
            return list;
        }
        
        /**
         * Adds clazz2 as a related class to clazz1.
         * 
         * @param clazz1 The primary class.
         * @param clazz2 Class to be registered.
         */
        public void addRelated(Class<? extends UIElementBase> clazz1, Class<? extends UIElementBase> clazz2) {
            getRelatedClasses(clazz1).add(clazz2);
        }
        
        /**
         * Returns true if the specified class has any related classes.
         * 
         * @param clazz The primary class.
         * @return True if the specified class has any related classes.
         */
        public boolean hasRelated(Class<? extends UIElementBase> clazz) {
            List<Class<? extends UIElementBase>> list = map.get(clazz);
            return list != null && list.size() > 0;
        }
        
        /**
         * Returns true if class clazz2 or a superclass of clazz2 is related to clazz1.
         * 
         * @param clazz1 The primary class.
         * @param clazz2 The class to test.
         * @return True if class clazz2 or a superclass of clazz2 is related to clazz1.
         */
        public boolean isRelated(Class<? extends UIElementBase> clazz1, Class<? extends UIElementBase> clazz2) {
            List<Class<? extends UIElementBase>> list = map.get(clazz1);
            
            if (list != null) {
                for (Class<?> clazz : list) {
                    if (clazz.isAssignableFrom(clazz2)) {
                        return true;
                    }
                }
            }
            
            return false;
        }
        
    };
    
    private static final RelatedClassMap allowedParentClasses = new RelatedClassMap();
    
    private static final RelatedClassMap allowedChildClasses = new RelatedClassMap();
    
    protected int maxChildren = 1;
    
    protected boolean autoHide = true;
    
    protected boolean autoEnable = true;
    
    private final List<UIElementBase> children = new ArrayList<UIElementBase>();
    
    private UIElementBase parent;
    
    private boolean locked;
    
    private boolean enabled = true;
    
    private boolean activated;
    
    private boolean visible = true;
    
    private PluginDefinition definition;
    
    private boolean designMode;
    
    private Object innerComponent;
    
    private Object outerComponent;
    
    private String rejectReason;
    
    private String hint;
    
    private String color;
    
    private IEventManager eventManager;
    
    /**
     * A UIElementBase subclass should call this in its static initializer block to register any
     * subclasses that may act as a parent.
     * 
     * @param clazz Class whose valid parent classes are to be registered.
     * @param parentClass Class that may act as a parent to clazz.
     */
    protected static synchronized void registerAllowedParentClass(Class<? extends UIElementBase> clazz,
                                                                  Class<? extends UIElementBase> parentClass) {
        allowedParentClasses.addRelated(clazz, parentClass);
    }
    
    /**
     * A UIElementBase subclass should call this in its static initializer block to register any
     * subclasses that may be a child.
     * 
     * @param clazz Class whose valid child classes are to be registered.
     * @param childClass Class that may be a child of clazz.
     */
    protected static synchronized void registerAllowedChildClass(Class<? extends UIElementBase> clazz,
                                                                 Class<? extends UIElementBase> childClass) {
        allowedChildClasses.addRelated(clazz, childClass);
    }
    
    /**
     * Returns true if childClass can be a child of the parentClass.
     * 
     * @param parentClass Parent class
     * @param childClass Child class
     * @return True if childClass can be a child of the parentClass.
     */
    public static boolean canAcceptChild(Class<? extends UIElementBase> parentClass,
                                         Class<? extends UIElementBase> childClass) {
        return allowedChildClasses.isRelated(parentClass, childClass);
    }
    
    /**
     * Returns true if parentClass can be a parent of childClass.
     * 
     * @param childClass The child class.
     * @param parentClass The parent class.
     * @return True if parentClass can be a parent of childClass.
     */
    public static boolean canAcceptParent(Class<? extends UIElementBase> childClass,
                                          Class<? extends UIElementBase> parentClass) {
        return allowedParentClasses.isRelated(childClass, parentClass);
    }
    
    public static void raise(String text, Throwable t) throws UIException {
        raise(text + "\n" + ZKUtil.formatExceptionForDisplay(t));
    }
    
    public static void raise(String text) throws UIException {
        throw new UIException(text);
    }
    
    /**
     * Returns design mode status.
     * 
     * @return True if design mode is active.
     */
    public boolean isDesignMode() {
        return designMode;
    }
    
    /**
     * Sets design mode status for this component and all its children.
     * 
     * @param designMode The design mode flag.
     */
    public void setDesignMode(boolean designMode) {
        this.designMode = designMode;
        
        for (UIElementBase child : children) {
            child.setDesignMode(designMode);
        }
        
        updateState();
    }
    
    /**
     * Adds the specified child element. The validity of the operation is first tested and an
     * exception thrown if the element is not a valid child for this parent.
     * 
     * @param child Element to add as a child.
     */
    public void addChild(UIElementBase child) {
        addChild(child, true);
    }
    
    /**
     * Adds the specified child element. The validity of the operation is first tested and an
     * exception thrown if the element is not a valid child for this parent.
     * 
     * @param child Element to add as a child.
     * @param doEvent Fires the add child events if true.
     */
    protected void addChild(UIElementBase child, boolean doEvent) {
        if (!child.canAcceptParent(this)) {
            raise(child.rejectReason);
        }
        
        if (!canAcceptChild(child)) {
            raise(rejectReason);
        }
        
        if (doEvent) {
            beforeAddChild(child);
        }
        
        if (child.getParent() != null) {
            child.getParent().removeChild(child, false);
        }
        
        children.add(child);
        child.updateParent(this);
        
        if (doEvent) {
            afterAddChild(child);
        }
    }
    
    /**
     * Called after a child is logically added to the parent.
     * 
     * @param child The child element added.
     */
    protected void afterAddChild(UIElementBase child) {
    }
    
    /**
     * Called before a child is logically added to the parent.
     * 
     * @param child The new child element.
     */
    protected void beforeAddChild(UIElementBase child) {
    }
    
    /**
     * Removes the specified element as a child of this parent.
     * 
     * @param child Child element to remove.
     * @param destroy If true the child is explicitly destroyed.
     */
    public void removeChild(UIElementBase child, boolean destroy) {
        if (!children.contains(child)) {
            return;
        }
        
        boolean isLocked = child.isLocked() || child.getDefinition().isInternal();
        
        if (destroy) {
            child.removeChildren();
            
            if (!isLocked) {
                child.destroy();
            }
        }
        
        if (!isLocked) {
            beforeRemoveChild(child);
            children.remove(child);
            child.updateParent(null);
            afterRemoveChild(child);
        }
    }
    
    /**
     * Called after a child is logically removed from the parent.
     * 
     * @param child The child UI element.
     */
    protected void afterRemoveChild(UIElementBase child) {
    }
    
    /**
     * Called before a child is logically removed from the parent.
     * 
     * @param child The child UI element.
     */
    protected void beforeRemoveChild(UIElementBase child) {
    }
    
    /**
     * Changes the assigned parent, firing parent changed events if appropriate.
     * 
     * @param newParent The new parent.
     */
    private void updateParent(UIElementBase newParent) {
        UIElementBase oldParent = this.parent;
        
        if (oldParent != newParent) {
            beforeParentChanged(newParent);
            
            if (parent != null && !getDefinition().isInternal()) {
                unbind();
            }
            
            this.parent = newParent;
            
            if (oldParent != null) {
                oldParent.updateState();
            }
            
            if (newParent != null) {
                if (parent != null && !getDefinition().isInternal()) {
                    bind();
                }
                
                afterParentChanged(oldParent);
                newParent.updateState();
                setDesignMode(newParent.isDesignMode());
            }
        }
    }
    
    /**
     * Called after the parent has been changed.
     * 
     * @param oldParent The value of the parent property prior to the change.
     */
    protected void afterParentChanged(UIElementBase oldParent) {
    }
    
    /**
     * Called before the parent has been changed.
     * 
     * @param newParent The value of the parent property prior to the change.
     */
    protected void beforeParentChanged(UIElementBase newParent) {
    }
    
    /**
     * Removes this element from its parent and optionally destroys it.
     * 
     * @param destroy If true, the element is also destroyed.
     */
    public void remove(boolean destroy) {
        if (parent != null) {
            parent.removeChild(this, destroy);
        }
    }
    
    /**
     * Remove all children associated with this element.
     */
    public void removeChildren() {
        for (int i = children.size() - 1; i >= 0; i--) {
            removeChild(children.get(i), true);
        }
    }
    
    /**
     * Override to implement special cleanup when an object is destroyed.
     */
    public void destroy() {
        unbind();
    }
    
    /**
     * Override to bind wrapped components to the UI.
     */
    protected void bind() {
        
    }
    
    /**
     * Override to unbind wrapped components from the UI.
     */
    protected void unbind() {
        
    }
    
    /**
     * Returns the innermost wrapped UI component. For UI elements that may host child elements,
     * this would be the wrapped UI component that can host the child components. For UI elements
     * that wrap a single UI component, getInnerComponent and getOuterComponent should return the
     * same value.
     * 
     * @return The inner component.
     */
    public Object getInnerComponent() {
        return innerComponent == null ? outerComponent : innerComponent;
    }
    
    /**
     * Sets the innermost wrapped UI component.
     * 
     * @param value The innermost wrapped UI component.
     */
    protected void setInnerComponent(Object value) {
        innerComponent = value;
    }
    
    /**
     * Returns the outermost wrapped UI component. This represents the wrapped UI component that
     * will be the direct child of the UI component wrapped by the parent element. For UI elements
     * that wrap a single UI component, getInnerComponent and getOuterComponent should return the
     * same value.
     * 
     * @return The outer component.
     */
    public Object getOuterComponent() {
        return outerComponent == null ? innerComponent : outerComponent;
    }
    
    /**
     * Sets the outermost wrapped UI component.
     * 
     * @param value The outermost wrapped UI component.
     */
    protected void setOuterComponent(Object value) {
        outerComponent = value;
    }
    
    /**
     * Element has been moved to a different position under its parent. Descendant classes should
     * make any necessary adjustments to wrapped components.
     * 
     * @param index New position for element.
     */
    protected void afterMoveTo(int index) {
        
    }
    
    /**
     * Return the definition used to create this instance.
     * 
     * @return The plugin definition.
     */
    public final PluginDefinition getDefinition() {
        if (definition == null) {
            setDefinition(getClass());
        }
        
        return definition;
    }
    
    /**
     * Sets the plugin definition for this element.
     * 
     * @param definition The plugin definition.
     */
    public void setDefinition(PluginDefinition definition) {
        if (this.definition != null) {
            if (this.definition == definition) {
                return;
            }
            
            throw new UIException("Cannot modify plugin definition.");
        }
        
        this.definition = definition;
        
        // Assign any default property values.
        if (definition != null) {
            for (PropertyInfo propInfo : definition.getProperties()) {
                String dflt = propInfo.getDefault();
                
                if (dflt != null) {
                    try {
                        propInfo.setPropertyValue(this, dflt);
                    } catch (Exception e) {
                        log.error("Error setting default value for property " + propInfo.getName(), e);
                    }
                }
            }
        }
    }
    
    /**
     * Sets the plugin definition based on the specified class. Typically this would be the same
     * class as the element itself, but in certain cases (as in the UIElementProxy class) it is not.
     * 
     * @param clazz The UI element class.
     */
    public void setDefinition(Class<? extends UIElementBase> clazz) {
        setDefinition(PluginRegistry.getInstance().get(clazz));
    }
    
    /**
     * Displays an about dialog for the UI element.
     */
    public void about() {
        AboutDialog.execute(this);
    }
    
    /**
     * Displays a property editor for the component. The default behavior is to display a message
     * that no property editor is defined. Subclasses must override this to implement a property
     * editor appropriate for the component.
     */
    public void editProperties() {
        PromptDialog.confirm("@cwf.shell.designer.propedit.none.message", "@cwf.shell.designer.propedit.none.caption");
    }
    
    /**
     * Activates or inactivates a UI element. In general, this method should not be overridden to
     * introduce new behavior. Rather, if a UI element must change its visual state in response to a
     * change in activation state, it should override the updateVisibility method. If a UI element
     * requires special activation logic for its children (e.g., if it allows only one child to be
     * active at a time), it should override the activateChildren method.
     * 
     * @param activate The activate status.
     */
    public void activate(boolean activate) {
        activateChildren(activate);
        activated = activate;
        updateVisibility();
        getEventManager().fireLocalEvent(
            activate ? LayoutConstants.EVENT_ELEMENT_ACTIVATE : LayoutConstants.EVENT_ELEMENT_INACTIVATE, this);
    }
    
    /**
     * Default behavior is to pass activation/inactivation event to children. Override to restrict
     * propagation of the event.
     * 
     * @param activate The activate status.
     */
    protected void activateChildren(boolean activate) {
        for (UIElementBase child : children) {
            child.activate(activate);
        }
    }
    
    /**
     * Returns the activation status of the element.
     * 
     * @return The activation status.
     */
    public final boolean isActivated() {
        return activated;
    }
    
    /**
     * Returns instance of the event manager.
     * 
     * @return Event manager instance.
     */
    protected IEventManager getEventManager() {
        if (eventManager == null) {
            eventManager = EventManager.getInstance();
        }
        
        return eventManager;
    }
    
    /**
     * Gets the UI element child at the specified index.
     * 
     * @param index Index of the child to retrieve.
     * @return The child at the specified index.
     */
    public UIElementBase getChild(int index) {
        return children.get(index);
    }
    
    /**
     * Locates and returns a child that is an instance of the specified class. If none is found,
     * returns null.
     * 
     * @param clazz Class of the child being sought.
     * @param last If specified, the search begins after this child. If null, the search begins with
     *            the first child.
     * @return The requested child or null if none found.
     */
    @SuppressWarnings("unchecked")
    public <T extends UIElementBase> T getChild(Class<T> clazz, UIElementBase last) {
        int i = last == null ? -1 : children.indexOf(last);
        
        for (i++; i < children.size(); i++) {
            if (clazz.isInstance(children.get(i))) {
                return (T) children.get(i);
            }
        }
        
        return null;
    }
    
    /**
     * Returns an iterable of this component's children.
     * 
     * @return Iterable of this component's children.
     */
    public Iterable<UIElementBase> getChildren() {
        return children;
    }
    
    /**
     * Returns an iterable of this component's serializable children. By default, this calls
     * getChildren() but may be overridden to accommodate specialized serialization needs.
     * 
     * @return Iterable of this component's serializable children.
     */
    public Iterable<UIElementBase> getSerializableChildren() {
        return getChildren();
    }
    
    /**
     * Returns the number of children.
     * 
     * @return Number of children.
     */
    public int getChildCount() {
        return children.size();
    }
    
    /**
     * Returns the first child, or null if there are no children.
     * 
     * @return First child, or null if none.
     */
    public UIElementBase getFirstChild() {
        return getChildCount() == 0 ? null : getChild(0);
    }
    
    /**
     * Returns the first visible child.
     * 
     * @return First visible child, or null if none;
     */
    public UIElementBase getFirstVisibleChild() {
        return getVisibleChild(true);
    }
    
    /**
     * Returns the last child, or null if there are no children.
     * 
     * @return Last child, or null if none.
     */
    public UIElementBase getLastChild() {
        return getChildCount() == 0 ? null : getChild(getChildCount() - 1);
    }
    
    /**
     * Returns the last visible child.
     * 
     * @return Last visible child, or null if none;
     */
    public UIElementBase getLastVisibleChild() {
        return getVisibleChild(false);
    }
    
    /**
     * Returns first or last visible child.
     * 
     * @param first If true, find first visible child; if false, last visible child.
     * @return Visible child, or null if none found.
     */
    private UIElementBase getVisibleChild(boolean first) {
        int start = first ? 0 : getChildCount() - 1;
        int end = first ? getChildCount() - 1 : 0;
        int inc = first ? 1 : -1;
        
        for (int i = start; i <= end; i += inc) {
            if (getChild(i).isVisible()) {
                return getChild(i);
            }
        }
        
        return null;
    }
    
    /**
     * Returns this element's next sibling.
     * 
     * @param visibleOnly If true, skip any non-visible siblings.
     * @return The next sibling, or null if none.
     */
    public UIElementBase getNextSibling(boolean visibleOnly) {
        List<UIElementBase> sibs = parent == null ? null : parent.children;
        
        if (sibs != null) {
            for (int i = sibs.indexOf(this) + 1; i < sibs.size(); i++) {
                if (!visibleOnly || sibs.get(i).isVisible()) {
                    return sibs.get(i);
                }
            }
        }
        
        return null;
    }
    
    /**
     * Returns the index of the specified child. If the specified component is not a child, -1 is
     * returned.
     * 
     * @param child The child component whose index is sought.
     * @return The child's index or -1 if not found.
     */
    public int indexOfChild(UIElementBase child) {
        return children.indexOf(child);
    }
    
    /**
     * Returns true if the specified element is a child of this element.
     * 
     * @param element The UI element to test.
     * @return True if the element is a child.
     */
    public boolean hasChild(UIElementBase element) {
        return indexOfChild(element) > -1;
    }
    
    /**
     * Recurses the component subtree for a child belonging to the specified class.
     * 
     * @param clazz Class of child being sought.
     * @return A child of the specified class, or null if not found.
     */
    @SuppressWarnings("unchecked")
    public <T extends UIElementBase> T findChildElement(Class<T> clazz) {
        for (UIElementBase child : getChildren()) {
            if (clazz.isInstance(child)) {
                return (T) child;
            }
        }
        
        for (UIElementBase child : getChildren()) {
            T child2 = child.findChildElement(clazz);
            
            if (child2 != null) {
                return child2;
            }
        }
        
        return null;
    }
    
    /**
     * Returns true if specified element is an ancestor of this element.
     * 
     * @param element A UI element.
     * @return True if the specified element is an ancestor of this element.
     */
    public boolean hasAncestor(UIElementBase element) {
        UIElementBase child = this;
        
        while (child != null) {
            if (element.hasChild(child)) {
                return true;
            }
            child = child.getParent();
        }
        
        return false;
    }
    
    /**
     * Returns this element's index in its parent's list of children. If this element has no parent,
     * returns -1.
     * 
     * @return This element's index.
     */
    public int getIndex() {
        return parent == null ? -1 : parent.indexOfChild(this);
    }
    
    /**
     * Sets this element's index to the specified value. This effectively changes the position of
     * the element relative to its siblings.
     * 
     * @param index The index.
     */
    public void setIndex(int index) {
        UIElementBase parent = getParent();
        
        if (parent == null) {
            raise("Element has no parent.");
        }
        
        int currentIndex = parent.children.indexOf(this);
        
        if (currentIndex < 0 || currentIndex == index) {
            return;
        }
        
        if (index >= parent.children.size() || index < 0) {
            raise("Index out of range.");
        }
        
        parent.children.remove(currentIndex);
        parent.children.add(index, this);
        afterMoveTo(index);
    }
    
    /**
     * Brings this element to the front of the user interface.
     */
    public void bringToFront() {
        if (parent != null) {
            parent.bringToFront();
        } else {
            activate(true);
        }
    }
    
    /**
     * Returns the display name of this element. By default, the definition name is returned, but
     * subclasses may override this to return some other name suitable for display in the design UI.
     * 
     * @return The display name.
     */
    public String getDisplayName() {
        return getDefinition().getName();
    }
    
    /**
     * Returns the instance name of this element. By default, this is the same as the display name,
     * but subclasses may override this to provide additional information that would distinguish
     * multiple instances of the same UI element.
     * 
     * @return The instance name.
     */
    public String getInstanceName() {
        return getDisplayName();
    }
    
    /**
     * Returns the class of the property editor associated with this UI element. Null means no
     * property editor exists.
     * 
     * @return The class of the associated property editor.
     */
    public Class<? extends Object> getPropEditClass() {
        return null;
    }
    
    /**
     * Returns true if the element is locked. When an element is locked, it may not be manipulated
     * within the designer.
     * 
     * @return True if the element is locked.
     */
    public boolean isLocked() {
        return locked;
    }
    
    /**
     * Sets the locked status of the element. When an element is locked, it may not be manipulated
     * within the designer.
     * 
     * @param locked The locked status.
     */
    public void setLocked(boolean locked) {
        this.locked = locked;
    }
    
    /**
     * Returns the parent of this element. May be null.
     * 
     * @return This element's parent.
     */
    public UIElementBase getParent() {
        return parent;
    }
    
    /**
     * Sets the parent of this element, subject to the parent/child constraints applicable to each.
     * 
     * @param parent The new parent.
     */
    public final void setParent(UIElementBase parent) {
        UIElementBase oldParent = this.parent;
        
        if (oldParent == parent) {
            return;
        }
        
        if (oldParent != null) {
            oldParent.removeChild(this, false);
        }
        
        if (parent != null) {
            parent.addChild(this);
        }
    }
    
    /**
     * Sets the enabled state of the component. This base implementation only sets the internal flag
     * and notifies the parent of the state change. Each UI element is responsible for overriding
     * this method and reflecting the enabled state in their wrapped UI components.
     * 
     * @param enabled The enabled state.
     */
    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            updateParentState();
        }
    }
    
    /**
     * Returns the enabled state of the UI element.
     * 
     * @return True if the UI element is enabled.
     */
    public final boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Sets the visibility state of the component. This base implementation only sets the internal
     * flag and notifies the parent of the state change. Each UI element is responsible for
     * overriding this method and reflecting the visibility state in their wrapped UI components.
     * 
     * @param visible The visibility state.
     */
    public final void setVisible(boolean visible) {
        if (this.visible != visible) {
            this.visible = visible;
            updateVisibility();
            updateParentState();
        }
    }
    
    /**
     * Calls updateVisibility with current settings.
     */
    protected final void updateVisibility() {
        updateVisibility(visible, activated);
    }
    
    /**
     * Override to set the visibility of wrapped components. Invoked when visibility or activation
     * states change.
     * 
     * @param visible The current visibility state.
     * @param activated The current activation state.
     */
    protected void updateVisibility(boolean visible, boolean activated) {
    }
    
    /**
     * Returns the visible state of the UI element.
     * 
     * @return True if the UI element is visible.
     */
    public final boolean isVisible() {
        return visible;
    }
    
    /**
     * Returns the color (as an HTML-formatted RGB string) for this element.
     * 
     * @return An HTML-formatted color specification (e.g., #0F134E). May be null.
     */
    public final String getColor() {
        return color;
    }
    
    /**
     * Provides a default implementation for setting the color of a ZK-based UI element. This is
     * provided to allow components to easily expose a color property in the property editor. It may
     * not be appropriate for all subclasses. To change which ZK elements are affected, override the
     * applyColor() method.
     * 
     * @param value A correctly formatted HTML color specification.
     */
    public final void setColor(String value) {
        color = value;
        applyColor();
    }
    
    /**
     * Provides a default implementation for setting the color of a UI element. Sets the color of
     * the inner and outer components. Override to modify this default behavior.
     */
    protected void applyColor() {
        applyColor(getOuterComponent());
        
        if (getInnerComponent() != getOuterComponent()) {
            applyColor(getInnerComponent());
        }
    }
    
    /**
     * Applies the current color setting to the wrapped component. Subclasses that support this
     * capability should override this to supply the proper implementation.
     * 
     * @param cmpt Component to receive the color setting.
     */
    protected void applyColor(Object cmpt) {
    }
    
    /**
     * Returns the tool tip text.
     * 
     * @return The tool tip text.
     */
    public final String getHint() {
        return hint;
    }
    
    /**
     * Sets the tool tip text.
     * 
     * @param value The tool tip text.
     */
    public final void setHint(String value) {
        this.hint = value;
        applyHint();
    }
    
    /**
     * Provides a default implementation for setting the hint text of a UI element. Sets the hint
     * text of the inner and outer components. Override to modify this default behavior.
     */
    protected void applyHint() {
        applyHint(getOuterComponent());
        
        if (getInnerComponent() != getOuterComponent()) {
            applyHint(getInnerComponent());
        }
    }
    
    /**
     * Applies the current hint text to the wrapped component. Subclasses that support this
     * capability should override this to supply the proper implementation.
     * 
     * @param cmpt Component to receive the hint text.
     */
    protected void applyHint(Object cmpt) {
    }
    
    /**
     * Calls updateState on the parent if one exists.
     */
    protected final void updateParentState() {
        if (parent != null) {
            parent.updateState();
        }
    }
    
    /**
     * Update a UI element based on the state of its children. The default implementation acts on
     * container elements only and has the following behavior:
     * <ul>
     * <li>If all children are disabled, the parent is also disabled (set autoEnable to false to
     * turn off).</li>
     * <li>If all children are hidden or there are no children and design mode is not active, the
     * parent is also hidden (set autoHide to false to turn off).
     * </ul>
     */
    protected void updateState() {
        if (!isContainer()) {
            return;
        }
        
        boolean anyEnabled = !autoEnable || getChildCount() == 0;
        boolean anyVisible = !autoHide || designMode;
        
        for (UIElementBase child : children) {
            if (anyEnabled && anyVisible) {
                break;
            }
            
            anyEnabled |= child.isEnabled();
            anyVisible |= child.isVisible();
        }
        
        setEnabled(anyEnabled);
        setVisible(anyVisible);
    }
    
    /**
     * Returns true if this UI element can contain other UI elements.
     * 
     * @return True if this UI element can contain other UI elements.
     */
    public boolean isContainer() {
        return allowedChildClasses.hasRelated(getClass());
    }
    
    /**
     * Returns true if this element may accept a child. Updates the reject reason with the result.
     * 
     * @return True if this element may accept a child. Updates the reject reason with the result.
     */
    public boolean canAcceptChild() {
        if (!isContainer()) {
            rejectReason = getDisplayName() + " does not accept any child components.";
        } else if (getChildCount() >= maxChildren) {
            rejectReason = getDisplayName() + " may accept at most " + maxChildren + " child component(s).";
        } else {
            rejectReason = null;
        }
        
        return rejectReason == null;
    }
    
    /**
     * Returns true if this element may accept a child of the specified class. Updates the reject
     * reason with the result.
     * 
     * @param clazz Child class to test.
     * @return True if this element may accept a child of the specified class.
     */
    public boolean canAcceptChild(Class<? extends UIElementBase> clazz) {
        if (!canAcceptChild()) {
            return false;
        }
        
        if (!canAcceptChild(getClass(), clazz)) {
            rejectReason = getDisplayName() + " does not accept " + clazz.getSimpleName() + " as a child.";
        } else {
            rejectReason = null;
        }
        
        return rejectReason == null;
    }
    
    /**
     * Returns true if this element may accept the specified child. Updates the reject reason with
     * the result.
     * 
     * @param child Child instance to test.
     * @return True if this element may accept the specified child.
     */
    public boolean canAcceptChild(UIElementBase child) {
        if (!canAcceptChild()) {
            return false;
        }
        
        if (!canAcceptChild(getClass(), child.getClass())) {
            rejectReason = getDisplayName() + " does not accept " + child.getDisplayName() + " as a child.";
        } else {
            rejectReason = null;
        }
        
        return rejectReason == null;
    }
    
    /**
     * Returns true if this element may accept a parent. Updates the reject reason with the result.
     * 
     * @return True if this element may accept a parent.
     */
    public boolean canAcceptParent() {
        rejectReason = !allowedParentClasses.hasRelated(getClass()) ? getDisplayName()
                + " does not accept any parent component." : null;
        return rejectReason == null;
    }
    
    /**
     * Returns true if this element may accept a parent of the specified class. Updates the reject
     * reason with the result.
     * 
     * @param clazz Parent class to test.
     * @return True if this element may accept a parent of the specified class.
     */
    public boolean canAcceptParent(Class<? extends UIElementBase> clazz) {
        if (!canAcceptParent(getClass(), clazz)) {
            rejectReason = getDisplayName() + " does not accept " + clazz.getSimpleName() + " as a parent.";
        } else {
            rejectReason = null;
        }
        
        return rejectReason == null;
    }
    
    /**
     * Returns true if this element may accept the specified element as a parent. Updates the reject
     * reason with the result.
     * 
     * @param parent Parent instance to test.
     * @return True if this element may accept the specified element as a parent.
     */
    public boolean canAcceptParent(UIElementBase parent) {
        if (!canAcceptParent()) {
            return false;
        }
        
        if (!canAcceptParent(getClass(), parent.getClass())) {
            rejectReason = getDisplayName() + " does not accept " + parent.getDisplayName() + " as a parent.";
        } else {
            rejectReason = null;
        }
        
        return rejectReason == null;
    }
    
    /**
     * Sets the reject reason to the specified value.
     * 
     * @param rejectReason Reason for rejection.
     */
    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }
    
    /**
     * Returns the reject reason. This is updated by the canAcceptParent and canAcceptChild calls.
     * 
     * @return The reject reason.
     */
    public String getRejectReason() {
        return rejectReason;
    }
    
    /**
     * Returns the UI element at the root of the component tree.
     * 
     * @return Root UI element.
     */
    public UIElementBase getRoot() {
        UIElementBase root = this;
        
        while (root.getParent() != null) {
            root = root.getParent();
        }
        
        return root;
    }
    
    /**
     * Returns the first ancestor corresponding to the specified class.
     * 
     * @param clazz Class of ancestor sought.
     * @return An ancestor of the specified class or null if not found.
     */
    @SuppressWarnings("unchecked")
    public <T extends UIElementBase> T getAncestor(Class<T> clazz) {
        UIElementBase parent = getParent();
        
        while (parent != null && !clazz.isInstance(parent)) {
            parent = parent.getParent();
        }
        
        return (T) parent;
    }
    
    /**
     * Subclasses may override this to implement any additional operations that are necessary before
     * this element is initialized (i.e., before property values and parent element are set).
     * 
     * @param deserializing If true, initialization is occurring as a result of deserialization.
     * @throws Exception Unspecified exception.
     */
    public void beforeInitialize(boolean deserializing) throws Exception {
        
    }
    
    /**
     * Subclasses may override this to implement any additional operations that are necessary after
     * this element is initialized (i.e., after property values and parent element are set).
     * 
     * @param deserializing If true, initialization is occurring as a result of deserialization.
     * @throws Exception Unspecified exception.
     */
    public void afterInitialize(boolean deserializing) throws Exception {
        
    }
    
}
