/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.commercialTrafficApplications.jointDemand;/*
 * created by jbischoff, 08.05.2019
 */

import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.Map;

public class JointDemandConfigGroup extends ReflectiveConfigGroup {

    @Positive
    private double firstLegTraveltimeBufferFactor = 2.0;
    public static final String FIRSTLEGBUFFER = "firstLegBufferFactor";
    private static final String FIRSTLEGBUFFERDESC = "Buffer travel time factor for the first leg of a freight tour.";

    public static final String MAXJOBSCORE = "maxJobScore";

    @Positive
    private double zeroUtilityDelay = 1800;
    public static final String ZEROUTILDELAY = "zeroUtilityDelay";
    private static final String ZEROUTILDELAYDESC = "Delay (in seconds) that marks the threshold for zero utility";
    public static final String MINJOBSCORE = "minJobScore";
    private static final String MAXJOBSCOREDESC = "Score for performing job in time.";
    private static final String MINJOBSCOREDESC = "Minimum score for delayed commercial jobs. " +
            "Note that if the customer is not served at all, that creates a score of zero. " +
            "So if this value is set to negative, that means an intensively delayed job is worse than no job.";
    @Positive
    private double maxJobScore = 6;
    private double minJobScore = -6;

	public static final String CHANGEOPERATORINTERVAL = "changeOperatorInterval";
	public static final String CHANGEOPERATORINTERVALDESC = ChangeCommercialJobOperator.SELECTOR_NAME
			+ " is actively used only every n-th iteration. Between this interval, assigned operator "
			+ "per job is kept constant and jsprit tourplanning gets bypassed.";
	@PositiveOrZero
	private int changeOperatorInterval = 0;

    public static final String GROUP_NAME = "commercialTraffic";

    public JointDemandConfigGroup() {
        super(GROUP_NAME);
    }

    public static JointDemandConfigGroup get(Config config) {
        return (JointDemandConfigGroup) config.getModules().get(GROUP_NAME);
    }

	/**
	 * @return -- {@value #CHANGEOPERATORINTERVAL}
	 */
	public int getChangeCommercialJobOperatorInterval() {
		return changeOperatorInterval;
	}

	/**
	 * @param changeOperatorInterval-- {@value #CHANGEOPERATORINTERVAL}
	 */
	public void setChangeCommercialJobOperatorInterval(int changeOperatorInterval) {
		this.changeOperatorInterval = changeOperatorInterval;
	}

    /**
     * @return firstLegTraveltimeBufferFactor --{@value #FIRSTLEGBUFFERDESC}
     */
//    @StringGetter(FIRSTLEGBUFFER)
    double getFirstLegTraveltimeBufferFactor() {
        return firstLegTraveltimeBufferFactor;
    }

    /**
     * @param firstLegTraveltimeBufferFactor --{@value #FIRSTLEGBUFFERDESC}
     */
//    @StringSetter(FIRSTLEGBUFFER)
    public void setFirstLegTraveltimeBufferFactor(double firstLegTraveltimeBufferFactor) {
        this.firstLegTraveltimeBufferFactor = firstLegTraveltimeBufferFactor;
    }

    /**
     * @return zeroUtilityDelay --{@value #ZEROUTILDELAYDESC}
     */
//    @StringGetter(ZEROUTILDELAY)
    public double getZeroUtilityDelay() {
        return zeroUtilityDelay;
    }

    /**
     * @param zeroUtilityDelay --{@value #ZEROUTILDELAYDESC}
     */
//    @StringSetter(ZEROUTILDELAY)
    public void setZeroUtilityDelay(double zeroUtilityDelay) {
        this.zeroUtilityDelay = zeroUtilityDelay;
    }

    /**
     * @return maxJobScore --{@value #MAXJOBSCOREDESC}
     */
//    @StringGetter(MAXJOBSCORE)
    public double getMaxJobScore() {
        return maxJobScore;
    }

    /**
     * @param maxJobScore --{@value #MAXJOBSCOREDESC}
     */
//    @StringSetter(MAXJOBSCORE)
    public void setMaxJobScore(double maxJobScore) {
        this.maxJobScore = maxJobScore;
    }

    /**
     * @return minJobScore --{@value #MINJOBSCOREDESC}
     */
//    @StringGetter(MINJOBSCORE)
    public double getMinJobScore() {
        return minJobScore;
    }

    /**
     * @param minJobScore --{@value #MINJOBSCOREDESC}
     */
//    @StringSetter(MINJOBSCORE)
    public void setMinJobScore(double minJobScore) {
        this.minJobScore = minJobScore;
    }

    @Override
    public Map<String, String> getComments() {
        Map<String, String> map = super.getComments();
        map.put(FIRSTLEGBUFFER, FIRSTLEGBUFFERDESC);
        map.put(MAXJOBSCORE, MAXJOBSCOREDESC);
        map.put(MINJOBSCORE, MINJOBSCOREDESC);
        map.put(ZEROUTILDELAY, ZEROUTILDELAYDESC);
        return map;
    }

    @Override
    protected void checkConsistency(Config config) {
        super.checkConsistency(config);
        if (getMaxJobScore() < getMinJobScore()) {
            throw new RuntimeException("Minimum Score for commercial jobs is higher than maximum score");
        }
    }
}
