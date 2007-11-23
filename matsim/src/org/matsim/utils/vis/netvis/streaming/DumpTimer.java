/* *********************************************************************** *
 * project: org.matsim.*
 * DumpTimer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.utils.vis.netvis.streaming;

import org.apache.log4j.Logger;

/**
 * 
 * @author gunnar
 * 
 */
public class DumpTimer {

    // -------------------- MEMBER VARIABLES --------------------

    private final int timeStepLength_s;

    private boolean open = false;

    private int firstDumpTime_s;

    private int lastDumpTime_s;

    // -------------------- CONSTRUCTION --------------------

    public DumpTimer(int timeStepLength_s) {
        this.timeStepLength_s = timeStepLength_s;
    }

    // -------------------- GETTERS --------------------

    public boolean getOpen() {
        return open;
    }

    public int getFirstDumpTime_s() {
        return firstDumpTime_s;
    }

    public int getLastDumpTime_s() {
        return lastDumpTime_s;
    }

    // -------------------- TIMING FUNCTIONS --------------------

    public boolean due(int time_s) {
        return (time_s >= 0) && (time_s % timeStepLength_s == 0)
                && (!open || time_s - lastDumpTime_s == timeStepLength_s);
    }

    public void notifyDump(int time_s) {
        if (!due(time_s))
            Logger.getLogger(DumpTimer.class).warn("due(int) does not allow for a dump!");
        if (!open) {
            open = true;
            firstDumpTime_s = time_s;
        }
        lastDumpTime_s = time_s;
    }

    public void close() {
        open = false;
    }

}
