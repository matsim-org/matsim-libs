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

package org.matsim.contrib.taxi.scheduler;

import org.matsim.contrib.taxi.run.TaxiConfigGroup;


public class TaxiSchedulerParams
{
    public final boolean destinationKnown;
    public final boolean vehicleDiversion;
    public final double pickupDuration;
    public final double dropoffDuration;
    public final double AStarEuclideanOverdoFactor;


    public TaxiSchedulerParams(TaxiConfigGroup taxiCfg)
    {
        this.destinationKnown = taxiCfg.isDestinationKnown();
        this.vehicleDiversion = taxiCfg.isVehicleDiversion();
        this.pickupDuration = taxiCfg.getPickupDuration();
        this.dropoffDuration = taxiCfg.getDropoffDuration();
        this.AStarEuclideanOverdoFactor = taxiCfg.getAStarEuclideanOverdoFactor();
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