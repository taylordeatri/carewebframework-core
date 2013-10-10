/**
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
 * If a copy of the MPL was not distributed with this file, You can obtain one at 
 * http://mozilla.org/MPL/2.0/.
 * 
 * This Source Code Form is also subject to the terms of the Health-Related Additional
 * Disclaimer of Warranty and Limitation of Liability available at
 * http://www.carewebframework.org/licensing/disclaimer.
 */
package org.carewebframework.ui.zk;

import org.zkoss.zk.ui.HtmlBasedComponent;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.MoveEvent;

/**
 * Used to prevent a modal window from being moved outside the view port.
 */
public class MoveEventListener implements EventListener<MoveEvent> {
    
    @Override
    public void onEvent(MoveEvent moveEvent) throws Exception {
        // Prevent movement outside of browser margins where we can't move it back.
        HtmlBasedComponent target = (HtmlBasedComponent) moveEvent.getTarget();
        
        if (target.getTop().startsWith("-")) {
            target.setTop("0px");
        }
        
        if (target.getLeft().startsWith("-")) {
            target.setLeft("0px");
        }
    }
}