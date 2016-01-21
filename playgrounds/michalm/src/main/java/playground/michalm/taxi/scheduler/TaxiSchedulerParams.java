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

package playground.michalm.taxi.scheduler;

import org.apache.commons.configuration.Configuration;


public class TaxiSchedulerParams
{
    public static final String DESTINATION_KNOWN = "destinationKnown";
    public static final String VEHICLE_DIVERSION = "vehicleDiversion";
    public static final String PICKUP_DURATION = "pickupDuration";
    public static final String DROPOFF_DURATION = "dropoffDuration";
    public static final String A_STAR_EUCLIDEAN_OVERDO_FACTOR = "AStarEuclideanOverdoFactor";

    public final boolean destinationKnown;
    public final boolean vehicleDiversion;
    public final double pickupDuration;
    public final double dropoffDuration;
    public final double AStarEuclideanOverdoFactor;


    public TaxiSchedulerParams(Configuration config)
    {
        this.destinationKnown = config.getBoolean(DESTINATION_KNOWN);
        this.vehicleDiversion = config.getBoolean(VEHICLE_DIVERSION);
        this.pickupDuration = config.getDouble(PICKUP_DURATION);
        this.dropoffDuration = config.getDouble(DROPOFF_DURATION);
        this.AStarEuclideanOverdoFactor = config.getDouble(A_STAR_EUCLIDEAN_OVERDO_FACTOR, 1.);
    }


    public TaxiSchedulerParams(boolean destinationKnown, boolean vehicleDiversion,
            double pickupDuration, double dropoffDuration, double AStarEuclideanOverdoFactor)
    {
        this.destinationKnown = destinationKnown;
        this.vehicleDiversion = vehicleDiversion;
        this.pickupDuration = pickupDuration;
        this.dropoffDuration = dropoffDuration;
        this.AStarEuclideanOverdoFactor = AStarEuclideanOverdoFactor;
    }
}