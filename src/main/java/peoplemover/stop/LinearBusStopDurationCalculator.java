/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package peoplemover.stop;

import org.matsim.contrib.drt.schedule.DrtStopTask;

/**
 * @author Michal Maciejewski (michalm)
 */
public class LinearBusStopDurationCalculator implements BusStopDurationCalculator {
    private final double pickupTimePerPassenger;
    private final double dropoffTimePerPassenger;
    private final double fixedStopTime;

    /**
     * @param pickupTimePerPassenger  - pick up time per Passenger for Boarding a vehicle
     * @param dropoffTimePerPassenger - drop off time per Passenger for Boarding a vehicle
     * @param fixedStopTime           - constant time per stop
     */
    public LinearBusStopDurationCalculator(double pickupTimePerPassenger, double dropoffTimePerPassenger,
                                           double fixedStopTime) {
        this.pickupTimePerPassenger = pickupTimePerPassenger;
        this.dropoffTimePerPassenger = dropoffTimePerPassenger;
        this.fixedStopTime = fixedStopTime;
    }

    @Override
    public double calcDuration(DrtStopTask task) {
        double pickupTime = pickupTimePerPassenger * task.getPickupRequests().size();
        double dropoffTime = dropoffTimePerPassenger * task.getDropoffRequests().size();
        return pickupTime + dropoffTime + fixedStopTime;
    }
}
