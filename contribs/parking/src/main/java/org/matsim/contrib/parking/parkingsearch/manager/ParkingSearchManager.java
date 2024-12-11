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

package org.matsim.contrib.parking.parkingsearch.manager;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.vehicles.Vehicle;

/**
 * @author jbischoff
 */
public interface ParkingSearchManager extends IterationEndsListener, MobsimBeforeSimStepListener, MobsimInitializedListener {

	boolean reserveSpaceIfVehicleCanParkHere(Id<Vehicle> vehicleId, Id<Link> linkId);

	Id<Link> getVehicleParkingLocation(Id<Vehicle> vehicleId);

	boolean parkVehicleHere(Id<Vehicle> vehicleId, Id<Link> linkId, double time);

	boolean unParkVehicleHere(Id<Vehicle> vehicleId, Id<Link> linkId, double time);

	void reset(int iteration);


}
