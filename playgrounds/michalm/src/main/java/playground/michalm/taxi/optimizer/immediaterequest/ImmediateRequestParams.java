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
    public final Boolean destinationKnown;
    public final Boolean minimizePickupTripTime;
    public final Double pickupDuration;
    public final Double dropoffDuration;


    public ImmediateRequestParams(Boolean destinationKnown, Boolean minimizePickupTripTime,
            Double pickupDuration, Double dropoffDuration)
    {
        this.destinationKnown = destinationKnown;
        this.minimizePickupTripTime = minimizePickupTripTime;
        this.pickupDuration = pickupDuration;
        this.dropoffDuration = dropoffDuration;
    }
}