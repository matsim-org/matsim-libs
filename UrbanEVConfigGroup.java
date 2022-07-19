/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
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

package org.matsim.urbanEV;

import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.HashSet;
import java.util.Set;


//TODO add decriptions
public class UrbanEVConfigGroup extends ReflectiveConfigGroup {

    //TODO should we rename the entire package from UrbanEV to EVChargingPreplanning or something similar?
    static final String GROUP_NAME = "urbanEV" ;



    public UrbanEVConfigGroup() {
        super(GROUP_NAME);

    }

    private static final String MAXIMUM_CHARGING_PROCEDURES = "maximumChargingProceduresPerAgent";
    /**
     * determines how often an agent can charge per day at max. During the pre-planning process, the agent has to go through it's entire plan n + 1 times where n is the
     * number of charging procedures he ends up with.
     */
    private int maximumChargingProceduresPerAgent = 3;

    private static final String CRITICAL_RELATIVE_SOC = "criticalRelativeSOC";
    /**
     * agents intend to charge their vehicle prior to the relative SOC falling under the given value
     */
    private double criticalRelativeSOC = 0.2;

    private static final String MIN_WHILE_CHARGING_ACT_DURATION_s = "minWhileChargingActivityDuration_s";
    /**
     * determines the minimum duration for activities to be determined suitable for charging the vehicle during the performance of the activity. In seconds.
     */
    private double minWhileChargingActivityDuration_s = 20 * 60;

    private static final String WHILE_CHARGING_ACT_TYPES = "whileChargingActivityTypes";
    /**
     * the activity types during which agents can charge their vehicle
     */
    private Set<String> whileChargingActivityTypes = new HashSet<>();

    private double maxDistanceBetweenActAndCharger_m = 5000;
    /**
     * determines the maximum distance between act while charging and charger
     */
    private static final String MAXIMUM_DISTANCE_TO_CHARGER ="maxDistanceToCharger";

    //TODO: the setting of this to true currently leads to inconsistency between the planning and the qsim, due to the non-reflection of plugin trip at the start of the day. needs further investigation!
    private static boolean pluginBeforeStartingThePlan = false;

    /**
     * determines the plug in act before the start of the plan to simulate charging between last act of the plan and first
     */

    private static final String PLUGIN_BEFORE_STARTING_THE_PLAN = "pluginBeforeTheStartingThePlan";

    //-------------------------------------------------------------------------------------------

    //	@StringGetter(MAXIMUM_CHARGING_PROCEDURES)
    public int getMaximumChargingProceduresPerAgent() {
        return maximumChargingProceduresPerAgent;
    }

    //	@StringSetter(MAXIMUM_CHARGING_PROCEDURES)
    public void setMaximumChargingProceduresPerAgent(int maximumChargingProceduresPerAgent) {
        this.maximumChargingProceduresPerAgent = maximumChargingProceduresPerAgent;
    }

    //	@StringGetter(CRITICAL_RELATIVE_SOC)
    public double getCriticalRelativeSOC() {
        return criticalRelativeSOC;
    }

    //	@StringSetter(CRITICAL_RELATIVE_SOC)
    public void setCriticalRelativeSOC(double criticalRelativeSOC) {
        this.criticalRelativeSOC = criticalRelativeSOC;
    }

    //	@StringGetter(MIN_WHILE_CHARGING_ACT_DURATION_s)
    public double getMinWhileChargingActivityDuration_s() {
        return minWhileChargingActivityDuration_s;
    }

    //	@StringSetter(MIN_WHILE_CHARGING_ACT_DURATION_s)
    public void setMinWhileChargingActivityDuration_s(double minWhileChargingActivityDuration_s) {
        this.minWhileChargingActivityDuration_s = minWhileChargingActivityDuration_s;
    }

    //	@StringGetter(WHILE_CHARGING_ACT_TYPES)
    public Set<String> getWhileChargingActivityTypes() {
        return whileChargingActivityTypes;
    }

    //	@StringSetter(WHILE_CHARGING_ACT_TYPES)
    public void setWhileChargingActivityTypes(Set<String> whileChargingActivityTypes) {
        this.whileChargingActivityTypes = whileChargingActivityTypes;
    }

    // @StringGetter(MAXIMUM_DISTANCE_TO_CHARGER)
    public double getMaxDistanceBetweenActAndCharger_m(){
        return maxDistanceBetweenActAndCharger_m;
    }

    //  @StringSetter(MAXIMUM_DISTANCE_TO_CHARGER)
    public void setMaxDistanceBetweenActAndCharger_m(double maxDistanceBetweenActAndCharger_m){
        this.maxDistanceBetweenActAndCharger_m = maxDistanceBetweenActAndCharger_m;
    }


    //@StringGetter(PLUGIN_BEFORE_STARTING_THE_PLAN)
    public boolean getPluginBeforeStartingThePlan(){
        return pluginBeforeStartingThePlan;
    }

    //@StringSetter(PLUGIN_BEFORE_STARTING_THE_PLAN)
    public void setPluginBeforeStartingThePlan(boolean pluginBeforeStartingThePlan){
     this.pluginBeforeStartingThePlan = pluginBeforeStartingThePlan;
    }

}
