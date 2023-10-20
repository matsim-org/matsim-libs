/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package org.matsim.core.mobsim.hermes;

import jakarta.validation.constraints.Positive;
import org.apache.logging.log4j.LogManager;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.misc.Time;

import java.util.Map;
import java.util.Set;

public final class HermesConfigGroup extends ReflectiveConfigGroup {
    public static final String NAME = "hermes";
    private static final String END_TIME = "endTime";

    // Maximum number of links (limited to 24 bits in the plan)
    public static final int MAX_LINK_ID = 16777216;
    // Maximum number of stops (limited to 20bits in the plan)
    public static final int MAX_STOP_ROUTE_ID = 65536;
    // Maximum vehicle velocity (limited to 8 bits in the plan: integers 0-99 reserved for speeds 0.0 - 9.9
    // and 100-255 reserved for speeds 10 - 165)
    public static final int MAX_VEHICLE_VELOCITY = 165;
    // Maximum vehicle PCE types (limited to 4 bits)
    public static final int MAX_VEHICLE_PCETYPES = 15;
    // Maximum number of events per agent (limited to 16 bits in the plan)
    public static final int MAX_EVENTS_AGENT = 65536;

    private static final String DETPT = "useDeterministicPt";

    // Number of simulation steps
    public static int SIM_STEPS = 30 * 60 * 60;
    // Number of ticks that are added to every agent advancing links.
    public static final int LINK_ADVANCE_DELAY = 1;
    private static final String FLOW_CAPACITY_FACTOR = "flowCapacityFactor";
    private static final String STORAGE_CAPACITY_FACTOR = "storageCapacityFactor";

    private static final String STUCKTIMEPARAM = "stuckTime";
    private static final String STUCKTIMEPARAMDESC = "time in seconds.  Time after which the frontmost vehicle on a link is called `stuck' if it does not move."
            + " Set to Integer.MAX_VALUE to disable this behavior";

    private static final String MAINMODESPARAM = "mainMode";
    private static final String MAINMODESPARAMDESC = "[comma-separated list] Modes that are handled in the mobsim along links. By default: car";
    private Set<String> mainModes = Set.of(TransportMode.car);

    private static final String DETPTDESC = "treats PT as deterministic. PT vehicles will run with a steady speed. Should be used with separate network layers for PT and other network modes.";
    private boolean deterministicPt = false;
    public static final boolean DEBUG_REALMS = false;
    public static final boolean DEBUG_EVENTS = false;
    public static final boolean CONCURRENT_EVENT_PROCESSING = true;

    @Positive
    private double storageCapacityFactor = 1.0;

    @Positive
    private double flowCapacityFactor = 1.0;

    @Positive
    private int stuckTime = 10;

    public Set<String> getMainModes() {
        return mainModes;
    }

    public void setMainModes(Set<String> mainModes) {
        this.mainModes = mainModes;
    }

    @StringSetter(MAINMODESPARAM)
    public void setMainModes(String mainModes) {
        this.mainModes = CollectionUtils.stringToSet(mainModes);
    }

    @StringGetter(MAINMODESPARAM)
    public String getMainModesAsString() {
        return CollectionUtils.setToString(mainModes);
    }

    public HermesConfigGroup() {
        super(NAME);
    }

    public int getEndTime() {
        return SIM_STEPS;
    }

    @StringGetter(DETPT)
    public boolean isDeterministicPt() {
        return deterministicPt;
    }

    @StringSetter(DETPT)
    public void setDeterministicPt(boolean deterministicPt) {
        this.deterministicPt = deterministicPt;
    }

    @StringGetter(STUCKTIMEPARAM)
    public int getStuckTime() {
        return stuckTime;
    }

    @StringSetter(STUCKTIMEPARAM)
    public void setStuckTime(int stuckTime) {
        this.stuckTime = stuckTime;
    }

    @StringSetter(FLOW_CAPACITY_FACTOR)
    public void setFlowCapacityFactor(double flowCapacityFactor) {
        this.flowCapacityFactor = flowCapacityFactor;
    }

    @StringGetter(FLOW_CAPACITY_FACTOR)
    public double getFlowCapacityFactor() {
        return this.flowCapacityFactor;
    }

    @StringSetter(STORAGE_CAPACITY_FACTOR)
    public void setStorageCapacityFactor(double storageCapacityFactor) {
        this.storageCapacityFactor = storageCapacityFactor;
    }

    @StringGetter(STORAGE_CAPACITY_FACTOR)
    public double getStorageCapacityFactor() {
        return storageCapacityFactor;
    }

    @StringSetter(END_TIME)
    public static void setEndTime(String endTime) {
        SIM_STEPS = (int) Time.parseTime(endTime);
    }

    @StringGetter(END_TIME)
    public String getEndTimeAsString() {
        return Time.writeTime(SIM_STEPS);
    }

    @Override
    public Map<String, String> getComments() {
        Map<String, String> comments = super.getComments();
        comments.put(END_TIME, "Simulation End Time");
        comments.put(STUCKTIMEPARAM, STUCKTIMEPARAMDESC);
        comments.put(DETPT, DETPTDESC);
        return comments;
    }



    @Override
    protected void checkConsistency(Config config) {
        super.checkConsistency(config);
        if (config.eventsManager().getOneThreadPerHandler()!=true && config.controller().getMobsim().equals("hermes")){
            LogManager.getLogger(getClass()).warn("Hermes should be run with one thread per handler.");
        }
    }
}
