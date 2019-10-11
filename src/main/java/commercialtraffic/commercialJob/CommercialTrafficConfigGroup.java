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

package commercialtraffic.commercialJob;/*
 * created by jbischoff, 08.05.2019
 */

import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;

import javax.validation.constraints.Positive;
import java.util.Map;

public class CommercialTrafficConfigGroup extends ReflectiveConfigGroup {

    @Positive
    private double firstLegTraveltimeBufferFactor = 2.0;
    public static final String FIRSTLEGBUFFER = "firstLegBufferFactor";
    private static final String FIRSTLEGBUFFERDESC = "Buffer travel time factor for the first leg of a freight tour.";

    private boolean runTourPlanning = true;
    public  static final String RUNJSPRIT = "runTourPlanning";
    public static final String MAXJOBSCORE = "maxJobScore";

    @Positive
    private double zeroUtilityDelay = 1800;
    public static final String ZEROUTILDELAY = "zeroUtilityDelay";
    private static final String ZEROUTILDELAYDESC = "Delay (in seconds) that marks the threshold for zero utility";
    public static final String MINJOBSCORE = "minJobScore";
    private static final String RUNJSPRITDESC = "Defines whether JSprit is run. " +
            "If this is set to false, ChangeJobOperator strategy must not be switched on and all carriers need to have at least one plan containing at least one tour.";
    private static final String MAXJOBSCOREDESC = "Score for performing job in time.";
    private static final String MINJOBSCOREDESC = "Minimum score for delayed commercial jobs.";
    @Positive
    private double maxJobScore = 6;
    private double minJobScore = -6;


    public static final String GROUP_NAME = "commercialTraffic";

    public CommercialTrafficConfigGroup() {
        super(GROUP_NAME);
    }

    public static CommercialTrafficConfigGroup get(Config config) {
        return (CommercialTrafficConfigGroup) config.getModules().get(GROUP_NAME);
    }


    /**
     * @return -- {@value #FIRSTLEGBUFFERDESC}
     */
//    @StringGetter(FIRSTLEGBUFFER)
    double getFirstLegTraveltimeBufferFactor() {
        return firstLegTraveltimeBufferFactor;
    }

    /**
     * @param -- {@value #FIRSTLEGBUFFERDESC}
     */
//    @StringSetter(FIRSTLEGBUFFER)
    public void setFirstLegTraveltimeBufferFactor(double firstLegTraveltimeBufferFactor) {
        this.firstLegTraveltimeBufferFactor = firstLegTraveltimeBufferFactor;
    }

//    @StringSetter(RUNJSPRIT)
    public void setRunTourPlanning(boolean runTourPlanning){ this.runTourPlanning = runTourPlanning; }

//    @StringGetter(RUNJSPRIT)
    public boolean getRunTourPlanning(){ return runTourPlanning; }

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
        map.put(RUNJSPRIT,RUNJSPRITDESC);
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
        } //TODO test
        if(!getRunTourPlanning() && config.strategy().getStrategySettings().stream()
                .anyMatch(strategySettings -> strategySettings.getStrategyName().equals(ChangeCommercialJobOperator.SELECTOR_NAME))) {
            throw new RuntimeException("if tour planning is switched off, the replanning  strategy " + ChangeCommercialJobOperator.SELECTOR_NAME
                                        + " is forbidden. Either let the carriers do tour planning before each  iteration by setting " + RUNJSPRIT + "=true "
                    + "or exclude the replanning strategy " + ChangeCommercialJobOperator.SELECTOR_NAME);
        }
    }
}
