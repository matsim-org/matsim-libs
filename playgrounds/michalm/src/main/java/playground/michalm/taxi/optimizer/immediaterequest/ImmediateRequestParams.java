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

package playground.michalm.taxi.optimizer.immediaterequest;

public class ImmediateRequestParams
{
    public final boolean destinationKnown;
    public final boolean minimizePickupTripTime;
    public final double pickupDuration;
    public final double dropoffDuration;


    public ImmediateRequestParams(boolean destinationKnown, boolean minimizePickupTripTime,
            double pickupDuration, double dropoffDuration)
    {
        this.destinationKnown = destinationKnown;
        this.minimizePickupTripTime = minimizePickupTripTime;
        this.pickupDuration = pickupDuration;
        this.dropoffDuration = dropoffDuration;
    }
}