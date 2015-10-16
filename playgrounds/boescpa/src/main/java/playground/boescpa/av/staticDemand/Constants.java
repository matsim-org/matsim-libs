/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package playground.boescpa.av.staticDemand;

/**
 * WHAT IS IT FOR?
 * WHAT DOES IT?
 *
 * @author boescpa
 */
public class Constants {

    // General Sim-Consts:
    public static final int SIMULATION_INTERVAL = 1; // [s]
    public static final int TOTAL_SIMULATION_TIME = 108000; // [s] (= 30h)
    public static final int MAX_SIMTIME_NEW_REQUESTS = 108000; // [s] (= 30h) CAREFUL: If this differs from TotalSimulationTime, the stats on the idle time of AVs may return strange results.
    public static final int STATS_INTERVAL = 300; // [s] (= 5min) <-> Every 5min, basic stats on the simulation are collected...
    public static final String[] MODES_REPLACED_BY_AV = {"car"};

    // AV-Behavior:
    public static final int LEVEL_OF_SERVICE = 300; // [s] (= 5min)
    public static final double BEELINE_FACTOR_STREET = 1.433920204872979; // Calculated with DetermineConstants from the original demand.
    public static final double AV_SPEED = 11.28; // [m/s] (= 40.6km/h)
    // Mikrozensus 2010 - Grossregion ZH: Unterwegszeit Auto (Fahrer und Mitfahrer) pro Tag:	21.95 + 8.00 = 29.95 [Min]
    // Mikrozensus 2010 - Grossregion ZH: Tagesdistanz Auto (Fahrer und Mitfahrer) pro Tag:	14.52 + 5.75 = 20.27 [km]
    //	=> Durchschnittliche Geschwindigkeit Auto - Grossregion ZH: 20'270m / 1797s = 11.28 m/s
    public static final int BOARDING_TIME = 0; // [s] (= 1min)
    public static final int UNBOARDING_TIME = 0; // [s] (= 1min)

    // AV-Redistribution:
    public static final int REDISTRIBUTIONINTERVAL = 1800; // [s] (= 15min)
    public static final int DURATION_OF_REDISTRIBUTION = 600; // [s] (= 10min) <-> How long cars will redistribute which were selected for redistribution in the redistribution process.
    public static final int REPLANNINGINTERVAL_REDISTRIBUTIONPROCESS = 60; // [s] (= 1min) <-> Every how many seconds the redistributing cars replan their route while redistributing.
    public static final double SHARE_OF_FREE_AV_TO_REDISTRIBUTE = 0.1; // Part of all free AVs which are selected for redistribution.
    public static final double HOW_MANY_TIMES_REDISTRIBUTIONINTERVAL_WAITING_FOR_REDISTRIBUTION = 2.00; // How many times the redistribution interval an AV needs to be in waiting mode already, before it is considered for redistribution.
    public static final double REDIST_CACHE_INTERVAL = 600; // [s] Redist cache interval decides over what time interval a floating average of all requests is taken to calculate the resulting force.
    public static final int MIN_CACHE_SIZE_FOR_REDIST = 10; // Minimal cache size required to start redistribution process. If less than this number of requests in cache, than to few for sensible redistribution.

    /**
     * Calculates the maximum search radius given a desired level of service.
     *
     * @return Maximum search radius [m]
     */
    public static double getSearchRadiusLevelOfService() {
        return ((AV_SPEED * LEVEL_OF_SERVICE) / BEELINE_FACTOR_STREET);
    }

    /**
     * Calculates the maximum search radius given a radius size (in seconds).
     *
     * @param radiusSize [s]
     * @return Maximum search radius [m]
     */
    public static double getSearchRadius(int radiusSize) {
        return ((AV_SPEED * radiusSize) / BEELINE_FACTOR_STREET);
    }

    public static double getRedistributingMoveRadius() {
        return ((AV_SPEED * SIMULATION_INTERVAL) / BEELINE_FACTOR_STREET);
    }
}
