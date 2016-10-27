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

import java.net.URL;
import java.util.Map;

import org.matsim.core.config.*;


public class TaxiConfigGroup
    extends ReflectiveConfigGroup
{
    public static final String GROUP_NAME = "taxi";


    public static TaxiConfigGroup get(Config config)
    {
        return (TaxiConfigGroup)config.getModule(GROUP_NAME);
    }


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

    
    @Override
    public Map<String, String> getComments()
    {
        Map<String, String> map = super.getComments();
        map.put(DESTINATION_KNOWN, "If false, the drop-off location remains unknown to the"
                + " optimizer and scheduler until the end of pickup");
        map.put(VEHICLE_DIVERSION, "If true, vehicles can be diverted during empty trips."
                + " Requires online tracking.");
        map.put(PICKUP_DURATION, "Typically, 120 seconds");
        map.put(DROPOFF_DURATION, "Typically, 60 seconds");
        map.put(A_STAR_EUCLIDEAN_OVERDO_FACTOR,
                "Used in AStarEuclidean for shortest path search for occupied drives."
                        + " Values above 1.0 (typically, 1.5 to 3.0) speed up the search,"
                        + " but at the cost of obtaining longer paths");
        map.put(ONLINE_VEHICLE_TRACKER,
                "If true, vehicles are (GPS-like) monitored while moving."
                        + " This helps in getting more accurate estimates on the time of arrival."
                        + " Online tracking is necessary for vehicle diversion.");
        map.put(TAXIS_FILE, "An XML file specifying the taxi fleet."
                + " The file format according to dvrp_vehicles_v1.dtd");
        map.put(TIME_PROFILES, "If true, time profiles of vehicle statuses (i.e. current task type)"
                + " and the number of unplanned requests");
        map.put(DETAILED_STATS,
                "If true, detailed hourly taxi stats are dumped after each" + " iteration.");
        map.put(OPTIMIZER_PARAMETER_SET,//currently, comments for parameter sets are not printed
                "Specifies the type and parameters of the TaxiOptimizer."
                        + " See: TaxiOptimizerParams and classes implementing it,"
                        + " e.g. AbstractTaxiOptimizerParams.");
        return map;
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
    
    public URL getTaxisFileUrl(URL context) {
		return ConfigGroup.getInputFileURL(context, this.taxisFile);
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


    public void setOptimizerConfigGroup(ConfigGroup optimizerCfg)
    {
        clearParameterSetsForType(OPTIMIZER_PARAMETER_SET);
        addParameterSet(optimizerCfg);
    }
}
