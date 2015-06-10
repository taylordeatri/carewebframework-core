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

import java.util.Calendar;
import java.util.Date;

import org.carewebframework.common.DateUtil;

import org.zkoss.zul.Datebox;
import org.zkoss.zul.Timebox;

/**
 * Utility functions related to datebox and timebox components.
 */
public class ZKDateUtil {
    
    /**
     * Returns a date/time from the UI. This is combined from two UI input elements, one for date
     * and one for time.
     * 
     * @param datebox The date box.
     * @param timebox The time box.
     * @return The combined date/time.
     */
    public static Date getTime(Datebox datebox, Timebox timebox) {
        if (timebox.getValue() == null) {
            return DateUtil.stripTime(datebox.getValue());
        }
        
        Calendar date = Calendar.getInstance();
        Calendar time = Calendar.getInstance();
        date.setTime(datebox.getValue());
        time.setTime(timebox.getValue());
        time.set(date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH));
        return time.getTime();
    }
    
    /**
     * Sets the UI to reflect the specified time.
     * 
     * @param datebox The date box.
     * @param timebox The time box.
     * @param value Time value to set.
     */
    public static void setTime(Datebox datebox, Timebox timebox, Date value) {
        value = value == null ? new Date() : value;
        datebox.setValue(DateUtil.stripTime(value));
        timebox.setValue(value);
    }
    
    /**
     * Enforce static class.
     */
    private ZKDateUtil() {
    }
}
