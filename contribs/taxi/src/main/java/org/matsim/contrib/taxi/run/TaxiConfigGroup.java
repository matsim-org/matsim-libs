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

package org.matsim.contrib.taxi.run;

import org.matsim.core.config.*;


public class TaxiConfigGroup
    extends ReflectiveConfigGroup
{
    public static final String GROUP_NAME = "taxi";

    public static final String DESTINATION_KNOWN = "destinationKnown";
    public static final String VEHICLE_DIVERSION = "vehicleDiversion";
    public static final String PICKUP_DURATION = "pickupDuration";
    public static final String DROPOFF_DURATION = "dropoffDuration";
    public static final String A_STAR_EUCLIDEAN_OVERDO_FACTOR = "AStarEuclideanOverdoFactor";
    public static final String ONLINE_VEHICLE_TRACKER = "onlineVehicleTracker";

    //input
    public static final String TAXIS_FILE = "taxisFile";

    //output
    public static final String TIME_PROFILES = "timeProfiles";
    public static final String DETAILED_STATS = "detailedStats";

    public static final String OPTIMIZER_PARAMETER_SET = "optimizer";

    private boolean destinationKnown = false;
    private boolean vehicleDiversion = false;
    private double pickupDuration = Double.NaN;//seconds
    private double dropoffDuration = Double.NaN;//seconds
    private double AStarEuclideanOverdoFactor = 1.;
    private boolean onlineVehicleTracker = false;

    private String taxisFile = null;

    private boolean timeProfiles = false;
    private boolean detailedStats = false;


    public TaxiConfigGroup()
    {
        super(GROUP_NAME);
    }


    @StringGetter(DESTINATION_KNOWN)
    public boolean isDestinationKnown()
    {
        return destinationKnown;
    }


    @StringSetter(DESTINATION_KNOWN)
    public void setDestinationKnown(boolean destinationKnown)
    {
        this.destinationKnown = destinationKnown;
    }


    @StringGetter(VEHICLE_DIVERSION)
    public boolean isVehicleDiversion()
    {
        return vehicleDiversion;
    }


    @StringSetter(VEHICLE_DIVERSION)
    public void setVehicleDiversion(boolean vehicleDiversion)
    {
        this.vehicleDiversion = vehicleDiversion;
    }


    @StringGetter(PICKUP_DURATION)
    public double getPickupDuration()
    {
        return pickupDuration;
    }


    @StringSetter(PICKUP_DURATION)
    public void setPickupDuration(double pickupDuration)
    {
        this.pickupDuration = pickupDuration;
    }


    @StringGetter(DROPOFF_DURATION)
    public double getDropoffDuration()
    {
        return dropoffDuration;
    }


    @StringSetter(DROPOFF_DURATION)
    public void setDropoffDuration(double dropoffDuration)
    {
        this.dropoffDuration = dropoffDuration;
    }


    @StringGetter(A_STAR_EUCLIDEAN_OVERDO_FACTOR)
    public double getAStarEuclideanOverdoFactor()
    {
        return AStarEuclideanOverdoFactor;
    }


    @StringSetter(A_STAR_EUCLIDEAN_OVERDO_FACTOR)
    public void setAStarEuclideanOverdoFactor(double aStarEuclideanOverdoFactor)
    {
        AStarEuclideanOverdoFactor = aStarEuclideanOverdoFactor;
    }


    @StringGetter(ONLINE_VEHICLE_TRACKER)
    public boolean isOnlineVehicleTracker()
    {
        return onlineVehicleTracker;
    }


    @StringSetter(ONLINE_VEHICLE_TRACKER)
    public void setOnlineVehicleTracker(boolean onlineVehicleTracker)
    {
        this.onlineVehicleTracker = onlineVehicleTracker;
    }


    @StringGetter(TAXIS_FILE)
    public String getTaxisFile()
    {
        return taxisFile;
    }


    @StringSetter(TAXIS_FILE)
    public void setTaxisFile(String taxisFile)
    {
        this.taxisFile = taxisFile;
    }


    @StringGetter(TIME_PROFILES)
    public boolean getTimeProfiles()
    {
        return timeProfiles;
    }


    @StringSetter(TIME_PROFILES)
    public void setTimeProfiles(boolean timeProfiles)
    {
        this.timeProfiles = timeProfiles;
    }


    @StringGetter(DETAILED_STATS)
    public boolean getDetailedStats()
    {
        return detailedStats;
    }


    @StringSetter(DETAILED_STATS)
    public void setDetailedStats(boolean detailedStats)
    {
        this.detailedStats = detailedStats;
    }


    public ConfigGroup getOptimizerConfigGroup()
    {
        return getParameterSets(OPTIMIZER_PARAMETER_SET).iterator().next();
    }
}
