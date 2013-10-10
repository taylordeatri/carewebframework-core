/**
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
 * If a copy of the MPL was not distributed with this file, You can obtain one at 
 * http://mozilla.org/MPL/2.0/.
 * 
 * This Source Code Form is also subject to the terms of the Health-Related Additional
 * Disclaimer of Warranty and Limitation of Liability available at
 * http://www.carewebframework.org/licensing/disclaimer.
 */
package org.carewebframework.api.event;

import org.apache.commons.lang.StringUtils;

/**
 * Handles ping requests.
 */
public class PingEventHandler implements IGenericEvent<PingRequest> {
    
    private static final String EVENT_PING_REQUEST = "PING.REQUEST";
    
    private static final String EVENT_PING_RESPONSE = "PING.RESPONSE";
    
    private final IEventManager eventManager;
    
    private final IPublisherInfo publisherInfo;
    
    /**
     * Create the event handler.
     * 
     * @param eventManager The event manager.
     * @param publisherInfo Information about this publisher.
     */
    public PingEventHandler(IEventManager eventManager, IPublisherInfo publisherInfo) {
        super();
        this.eventManager = eventManager;
        this.publisherInfo = publisherInfo;
    }
    
    /**
     * Initialize after setting all requisite properties.
     */
    public void init() {
        eventManager.subscribe(EVENT_PING_REQUEST, this);
    }
    
    /**
     * Cleanup this instance.
     */
    public void destroy() {
        eventManager.unsubscribe(EVENT_PING_REQUEST, this);
    }
    
    @Override
    public void eventCallback(String eventName, PingRequest pingRequest) {
        if (StringUtils.isEmpty(pingRequest.appName) || pingRequest.appName.equals(publisherInfo.getAppName())) {
            eventManager.fireRemoteEvent(EVENT_PING_RESPONSE, publisherInfo.formatPublisherInfo(), pingRequest.requestor);
        }
    }
    
}