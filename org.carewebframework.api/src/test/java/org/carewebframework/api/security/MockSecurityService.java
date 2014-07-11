/**
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
 * If a copy of the MPL was not distributed with this file, You can obtain one at 
 * http://mozilla.org/MPL/2.0/.
 * 
 * This Source Code Form is also subject to the terms of the Health-Related Additional
 * Disclaimer of Warranty and Limitation of Liability available at
 * http://www.carewebframework.org/licensing/disclaimer.
 */
package org.carewebframework.api.security;

/**
 * The contents of this file are subject to the Regenstrief Public License
 * Version 1.0 (the "License"); you may not use this file except in compliance with the License.
 * Please contact Regenstrief Institute if you would like to obtain a copy of the license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) Regenstrief Institute.  All Rights Reserved.
 */
import org.carewebframework.api.domain.IDomainObject;

/**
 * Mock security service for testing.
 */
public class MockSecurityService implements ISecurityService {
    
    private final IDomainObject mockUser;
    
    public MockSecurityService(IDomainObject mockUser) {
        this.mockUser = mockUser;
    }
    
    @Override
    public boolean logout(boolean force, String target, String message) {
        return true;
    }
    
    @Override
    public boolean validatePassword(String password) {
        return true;
    }
    
    @Override
    public String changePassword(String oldPassword, String newPassword) {
        return null;
    }
    
    @Override
    public void changePassword() {
    }
    
    @Override
    public boolean canChangePassword() {
        return false;
    }
    
    @Override
    public String generateRandomPassword() {
        return null;
    }
    
    @Override
    public void setAuthorityAlias(String authority, String alias) {
    }
    
    @Override
    public boolean isAuthenticated() {
        return true;
    }
    
    @Override
    public IDomainObject getAuthenticatedUser() {
        return mockUser;
    }
    
    @Override
    public boolean hasDebugRole() {
        return true;
    }
    
    @Override
    public boolean isGranted(String grantedAuthority) {
        return true;
    }
    
    @Override
    public boolean isGranted(String grantedAuthorities, boolean checkAllRoles) {
        return true;
    }
    
    @Override
    public String loginDisabled() {
        return null;
    }
    
}
