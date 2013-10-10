/**
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
 * If a copy of the MPL was not distributed with this file, You can obtain one at 
 * http://mozilla.org/MPL/2.0/.
 * 
 * This Source Code Form is also subject to the terms of the Health-Related Additional
 * Disclaimer of Warranty and Limitation of Liability available at
 * http://www.carewebframework.org/licensing/disclaimer.
 */
package org.carewebframework.ui;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.carewebframework.api.FrameworkRuntimeException;
import org.carewebframework.common.DateUtil;
import org.carewebframework.common.DateUtil.ITimeZoneAccessor;
import org.carewebframework.ui.LifecycleEventListener.ILifecycleCallback;
import org.carewebframework.ui.spring.AppContextFinder;
import org.carewebframework.ui.spring.FrameworkAppContext;

import org.zkoss.zk.au.AuRequest;
import org.zkoss.zk.au.AuService;
import org.zkoss.zk.au.out.AuClientInfo;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Execution;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.event.ClientInfoEvent;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.UiLifeCycle;

/**
 * Application singleton used to track active sessions and desktops.
 */
public class Application {
    
    private static final Log log = LogFactory.getLog(Application.class);
    
    private static final Application instance = new Application();
    
    /**
     * Tracks information about a session.
     */
    public class SessionInfo {
        
        private final List<Desktop> desktops = new ArrayList<Desktop>();
        
        private final Session session;
        
        /**
         * Creates a session info instance for this session.
         * 
         * @param session
         */
        private SessionInfo(final Session session) {
            this.session = session;
        }
        
        /**
         * Adds a desktop to this session information.
         * 
         * @param desktop
         * @return The number of active desktops including this one.
         */
        private synchronized int addDesktop(final Desktop desktop) {
            this.desktops.add(desktop);
            final DesktopInfo desktopInfo = new DesktopInfo(desktop);
            desktop.setAttribute(DesktopInfo.class.getName(), desktopInfo);
            desktop.addListener(desktopInfo);
            desktop.addListener(uiLifeCycle);
            return this.desktops.size();
        }
        
        /**
         * Removes a desktop from this session information.
         * 
         * @param desktop
         * @return The number of active desktops remaining.
         */
        private synchronized int removeDesktop(final Desktop desktop) {
            if (this.desktops.remove(desktop)) {
                desktop.removeListener(Application.getDesktopInfo(desktop));
            } else {
                log.warn(String.format("Desktop[%s] not found in managed list, already removed?", desktop));
            }
            return this.desktops.size();
        }
        
        /**
         * Returns the native session associated with this session information.
         * 
         * @return HttpSession
         */
        public HttpSession getNativeSession() {
            return this.session == null ? null : (HttpSession) this.session.getNativeSession();
        }
        
        /**
         * Returns the native session associated with this session information.
         * 
         * @return Session
         */
        public Session getSession() {
            return this.session;
        }
        
        /**
         * Returns a list of active desktops associated with this session. We return a copy of the
         * desktop list, to avoid concurrency issues.
         * 
         * @return List of Desktops
         */
        public synchronized List<Desktop> getDesktops() {
            return new ArrayList<Desktop>(this.desktops);
        }
        
        /**
         * String representation of <code>Application</code>
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            final StringBuffer buffer = new StringBuffer();
            if (this.session != null) {
                buffer.append("\nSessionInfo");
                final HttpSession httpSession = getNativeSession();
                if (httpSession != null) {//precaution, shouldn't ever be null
                    buffer.append("\n\tSession ID: ").append(httpSession.getId());
                    buffer.append("\n\tCreationTime: ").append(String.valueOf(new Date(httpSession.getCreationTime())));
                    buffer.append("\n\tLastAccessedTime: ").append(
                        String.valueOf(new Date(httpSession.getLastAccessedTime())));
                }
                final String deviceType = this.session.getDeviceType();
                final String localAddress = this.session.getLocalAddr();
                final String localName = this.session.getLocalName();
                final int maxInactiveInterval = this.session.getMaxInactiveInterval();
                final String remoteAddress = this.session.getRemoteAddr();
                final String remoteHost = this.session.getRemoteHost();
                final String serverName = this.session.getServerName();
                
                final Map<?, ?> attributes = this.session.getAttributes();
                if (attributes != null) {
                    for (final Object key : attributes.keySet()) {
                        buffer.append("\n\tSession Attribute Key=").append(key);
                        final Object attribute = attributes.get(key);
                        buffer.append(", Value=").append(attribute);
                        buffer.append(", Session Attribute Class=").append(attribute == null ? null : attribute.getClass());
                    }
                }
                
                buffer.append("\n\tSession(").append(localName).append("):MaxInactiveInterval=").append(maxInactiveInterval)
                        .append(", DeviceType=").append(deviceType).append(", LocalAddresss=").append(localAddress)
                        .append(", RemotedAddress=").append(remoteAddress).append(", RemoteHost=").append(remoteHost)
                        .append(", ServerName=").append(serverName);
                
                for (final Desktop desktop : getDesktops()) {
                    final DesktopInfo desktopInfo = Application.getDesktopInfo(desktop);
                    buffer.append(desktopInfo);
                }
            }
            return buffer.toString();
        }
        
    }
    
    /**
     * Tracks information about a desktop that is only available during an active execution.
     */
    public class DesktopInfo implements AuService {
        
        /**
         * Error Code for illegal state exception
         */
        private static final String EXC_ILLEGAL_STATE = "@cwf.error.ui.illegal.state";
        
        private final String id;
        
        private final String userAgent;
        
        private final String remoteAddress;
        
        private final String remoteHost;
        
        private final String remoteUser;
        
        private boolean infoRequested;
        
        private final boolean isExplorer;
        
        private final boolean isGecko;
        
        private final Desktop desktop;
        
        private ClientInfoEvent clientInformation;
        
        /**
         * Copies information from the desktop's execution so that it is available to monitoring
         * applications. Also, captures information from the onClientInfo event when sent to the
         * desktop.
         * <p>
         * Note: Do not keep a reference to the desktop here.
         * 
         * @param desktop
         */
        private DesktopInfo(final Desktop desktop) {
            this.desktop = desktop;
            final Execution exec = desktop.getExecution();
            if (exec == null) {
                throw new FrameworkRuntimeException(EXC_ILLEGAL_STATE, null, DesktopInfo.this.toString());
            }
            this.userAgent = exec.getUserAgent();
            this.remoteAddress = exec.getRemoteAddr();
            this.remoteHost = exec.getRemoteHost();
            this.remoteUser = exec.getRemoteUser();
            this.isExplorer = exec.getBrowser("ie") != null;
            this.isGecko = exec.getBrowser("gecko") != null;
            this.id = desktop.getId();
        }
        
        /**
         * @return the id
         */
        public String getId() {
            return this.id;
        }
        
        /**
         * @return the isExplorer
         */
        public boolean isExplorer() {
            return this.isExplorer;
        }
        
        /**
         * @return the userAgent
         */
        public String getUserAgent() {
            return this.userAgent;
        }
        
        /**
         * @return the remoteAddress
         */
        public String getRemoteAddress() {
            return this.remoteAddress;
        }
        
        /**
         * @return the remoteHost
         */
        public String getRemoteHost() {
            return this.remoteHost;
        }
        
        /**
         * @return the remoteUser
         */
        public String getRemoteUser() {
            return this.remoteUser;
        }
        
        /**
         * @return the infoRequested
         */
        public boolean isInfoRequested() {
            return this.infoRequested;
        }
        
        /**
         * @return the clientInformation
         */
        public ClientInfoEvent getClientInformation() {
            return this.clientInformation;
        }
        
        /**
         * @return the isGecko
         */
        public boolean isGecko() {
            return this.isGecko;
        }
        
        /**
         * @return the desktop
         */
        public Desktop getDesktop() {
            return this.desktop;
        }
        
        /**
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            final StringBuffer buffer = new StringBuffer();
            final ClientInfoEvent clientInfo = getClientInformation();
            final String screenDimensions = clientInfo == null ? "" : (clientInfo.getScreenWidth() + "x" + clientInfo
                    .getScreenHeight());
            buffer.append("\n\t\tDesktopInfo");
            buffer.append("\n\t\t\tDesktop: ").append(getDesktop());//includes Id
            buffer.append("\n\t\t\tisAlive: ").append(getDesktop() == null || !getDesktop().isAlive() ? false : true);
            buffer.append("\n\t\t\tisServerPushEnabled: ").append(
                getDesktop() == null || !getDesktop().isServerPushEnabled() ? false : true);
            buffer.append("\n\t\t\tUserAgent: ").append(getUserAgent());
            buffer.append("\n\t\t\tUserAgent (According to ZK) ").append(
                isGecko() ? "is Gecko based (i.e. Firefox)" : (isExplorer() ? "is IE based" : " may not be IE or Firefox"));
            buffer.append("\n\t\t\tUserAgent screen dimensions: ").append(screenDimensions);
            return buffer.toString();
        }
        
        /**
         * Listens to client requests. Serves two functions:
         * <ol>
         * <li>Makes a request to the client for information about the client's operating
         * environment.</li>
         * <li>Processes the client response, storing the returned information in clientInformation.
         * </li>
         * </ol>
         * Once the requested information is processed, stops listening to further client requests.
         * <p>
         */
        @Override
        public boolean service(final AuRequest request, final boolean everError) {
            if (Events.ON_CLIENT_INFO.equals(request.getCommand())) {
                this.clientInformation = ClientInfoEvent.getClientInfoEvent(request);
                request.getDesktop().removeListener(this);
            } else if (!this.infoRequested) {
                this.infoRequested = true;
                request.getDesktop().getExecution().addAuResponse(new AuClientInfo(request.getDesktop()));
            }
            return false;
        }
    }
    
    private final ITimeZoneAccessor localTimeZone = new ITimeZoneAccessor() {
        
        @Override
        public TimeZone getTimeZone() {
            TimeZone tz = Application.getTimeZone(FrameworkWebSupport.getDesktop());
            return tz == null ? TimeZone.getDefault() : tz;
        }
        
        @Override
        public void setTimeZone(TimeZone timezone) {
            throw new UnsupportedOperationException();
        }
        
    };
    
    /**
     * Attaches the default variable resolver to the desktop's page.
     */
    private final UiLifeCycle uiLifeCycle = new UiLifeCycle() {
        
        @Override
        public void afterPageAttached(Page page, Desktop desktop) {
            desktop.removeListener(this);
            page.addVariableResolver(new FrameworkVariableResolver());
        }
        
        @Override
        public void afterPageDetached(Page page, Desktop prevdesktop) {
        }
        
        @Override
        public void afterComponentAttached(Component comp, Page page) {
        }
        
        @Override
        public void afterComponentDetached(Component comp, Page prevpage) {
        }
        
        @Override
        public void afterComponentMoved(Component parent, Component child, Component prevparent) {
        }
        
    };
    
    private final ILifecycleCallback<Desktop> desktopLifeCycle = new ILifecycleCallback<Desktop>() {
        
        /**
         * The desktop is registered and the associated session's active desktop count is
         * incremented by one.
         * 
         * @param desktop Desktop to register.
         */
        @Override
        public void onInit(Desktop desktop) {
            register(desktop, true);
        }
        
        /**
         * The desktop is unregistered, the associated session's active desktop count is decremented
         * by one and, if the count has reached zero, the session is invalidated.
         * 
         * @param desktop Desktop to unregister.
         */
        @Override
        public void onCleanup(Desktop desktop) {
            register(desktop, false);
        }
    };
    
    private final ILifecycleCallback<Session> sessionLifeCycle = new ILifecycleCallback<Session>() {
        
        /**
         * Registers a session upon creation.
         * 
         * @param session Session to register.
         */
        @Override
        public void onInit(Session session) {
            addSession(session);
        }
        
        /**
         * Unregisters a session upon destruction.
         * 
         * @param session Session to unregister.
         */
        @Override
        public void onCleanup(Session session) {
            removeSession(session);
        }
    };
    
    private final Map<String, SessionInfo> activeSessions = new ConcurrentHashMap<String, SessionInfo>();
    
    /**
     * Returns the singleton instance of the Application object.
     * 
     * @return Application instance
     */
    public static Application getInstance() {
        return instance;
    }
    
    /**
     * Returns the desktop info instance associated with the specified desktop.
     * 
     * @param desktop Desktop whose information is requested.
     * @return The DesktopInfo instance for the specified desktop.
     */
    public static DesktopInfo getDesktopInfo(final Desktop desktop) {
        return (DesktopInfo) desktop.getAttribute(DesktopInfo.class.getName());
    }
    
    /**
     * Return the time zone set for the specified desktop.
     * 
     * @param desktop
     * @return The time zone.
     */
    public static TimeZone getTimeZone(final Desktop desktop) {
        DesktopInfo dti = desktop == null ? null : getDesktopInfo(desktop);
        return dti == null || dti.clientInformation == null ? null : dti.clientInformation.getTimeZone();
    }
    
    /**
     * Set time zone resolver and add lifecycle callbacks.
     */
    private Application() {
        super();
        DateUtil.localTimeZone = localTimeZone;
        LifecycleEventDispatcher.addDesktopCallback(desktopLifeCycle);
        LifecycleEventDispatcher.addSessionCallback(sessionLifeCycle);
    }
    
    /**
     * Returns a list of SessionInfo objects for active sessions. This is a copy of the underlying
     * list to avoid concurrency issues.
     * 
     * @return A list of SessionInfo objects.
     */
    public List<SessionInfo> getActiveSessions() {
        return new ArrayList<SessionInfo>(this.activeSessions.values());
    }
    
    /**
     * Adds a session to the list of active sessions.
     * 
     * @param session Session to add.
     */
    private synchronized void addSession(final Session session) {
        final String id = ((HttpSession) session.getNativeSession()).getId();
        
        if (!this.activeSessions.containsKey(id)) {
            final SessionInfo sessionInfo = new SessionInfo(session);
            this.activeSessions.put(id, sessionInfo);
            log.debug(sessionInfo);
        }
    }
    
    /**
     * Removes a session from the list of active sessions.
     * 
     * @param session Session to remove.
     */
    private synchronized void removeSession(final Session session) {
        final String sessionId = ((HttpSession) session.getNativeSession()).getId();
        this.activeSessions.remove(sessionId);
    }
    
    /**
     * Register or unregister a desktop. This keeps count of all active desktops for each session.
     * When a session's active desktop count reaches 0, the session is invalidated.
     * 
     * @param desktop Desktop to register/unregister.
     * @param doRegister If true, the desktop is registered and the associated session's active
     *            desktop count is incremented by one. If false, the desktop is unregistered and the
     *            associated session's active desktop count is decremented by one and, if the count
     *            has reached zero, the session is invalidated.
     */
    public void register(final Desktop desktop, final boolean doRegister) {
        final Session session = desktop.getSession();
        
        if (doRegister) {
            addDesktop(desktop);
            final HttpServletRequest request = (HttpServletRequest) desktop.getExecution().getNativeRequest();
            
            if (isManaged(request.getRequestURI())) {
                AppContextFinder.createAppContext(desktop);
                session.setAttribute(Constants.MANAGED + desktop.getId(), desktop.getId());
            }
        } else {
            if (FrameworkAppContext.getAppContext(desktop) != null) {
                AppContextFinder.destroyAppContext(desktop);
            }
            
            if (removeDesktop(desktop) == 0) {
                if (log.isDebugEnabled()) {
                    HttpSession sess = (HttpSession) session.getNativeSession();
                    log.debug("Explicitly invalidating Session #" + sess.getId());
                }
                
                session.invalidate();
            }
        }
    }
    
    /**
     * Returns true if we are managing the lifecycle of the desktop loaded from the specified uri.
     * 
     * @param url Url indicating the request path for the desktop.
     * @return
     */
    private boolean isManaged(final String url) {
        return (url != null) && !url.contains("/zkau/");
    }
    
    /**
     * Adds a desktop to the list of active desktops (under the SessionInfo object for the
     * associated session).
     * 
     * @param desktop Desktop to add.
     * @return The number of active desktops. Returns -1 if the desktop's session is not known.
     */
    private int addDesktop(final Desktop desktop) {
        final SessionInfo sessionInfo = getSessionInfo(desktop);
        final int retVal = sessionInfo == null ? -1 : sessionInfo.addDesktop(desktop);
        log.debug(sessionInfo);
        return retVal;
    }
    
    /**
     * Removes a desktop from the list of active desktops.
     * 
     * @param desktop Desktop to remove.
     * @return The number of active desktops. Returns -1 if the desktop's session is not known.
     */
    private int removeDesktop(final Desktop desktop) {
        final SessionInfo sessionInfo = getSessionInfo(desktop);
        int retVal = sessionInfo == null ? -1 : sessionInfo.removeDesktop(desktop);
        log.debug(sessionInfo);
        return retVal;
    }
    
    /**
     * Returns the count of active desktops registered to the specified session.
     * 
     * @param session Session whose desktop count is sought.
     * @return The number of active desktops for this session, or 0 if the session is not known.
     */
    public int getDesktopCount(final HttpSession session) {
        final SessionInfo sessionInfo = this.activeSessions.get(session.getId());
        return sessionInfo == null ? 0 : sessionInfo.desktops.size();
    }
    
    /**
     * Returns the SessionInfo object appropriate for the specified desktop (based on the session
     * associated with the desktop).
     * 
     * @param desktop Desktop whose associated SessionInfo is sought.
     * @return A SessionInfo instance, or null if one was not found.
     */
    public SessionInfo getSessionInfo(final Desktop desktop) {
        final HttpSession session = (HttpSession) desktop.getSession().getNativeSession();
        return session == null ? null : this.activeSessions.get(session.getId());
    }
}