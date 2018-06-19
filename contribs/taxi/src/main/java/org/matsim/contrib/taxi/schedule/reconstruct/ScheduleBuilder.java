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

package org.matsim.contrib.taxi.schedule.reconstruct;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.data.FleetImpl;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.data.VehicleImpl;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.schedule.TaxiDropoffTask;
import org.matsim.contrib.taxi.schedule.TaxiEmptyDriveTask;
import org.matsim.contrib.taxi.schedule.TaxiOccupiedDriveTask;
import org.matsim.contrib.taxi.schedule.TaxiPickupTask;
import org.matsim.contrib.taxi.schedule.TaxiStayTask;
import org.matsim.contrib.taxi.schedule.reconstruct.StayRecorder.Stay;
import org.matsim.contrib.taxi.vrpagent.TaxiActionCreator;

public class ScheduleBuilder {
	private VehicleImpl vehicle;
	private TaxiRequest currentRequest = null;

	ScheduleBuilder(FleetImpl fleet, Id<Person> personId, Link link, double t0) {
		vehicle = new VehicleImpl(Id.create(personId, Vehicle.class), link, 1, t0, Double.NaN);
		fleet.addVehicle(vehicle);
	}

	void addDrive(VrpPathWithTravelData vrpPath) {
		if (currentRequest != null) {
			vehicle.getSchedule().addTask(new TaxiOccupiedDriveTask(vrpPath, currentRequest));
		} else {
			vehicle.getSchedule().addTask(new TaxiEmptyDriveTask(vrpPath));
		}
	}

	void addStay(Stay stay) {
		switch (stay.activityType) {
			case TaxiActionCreator.STAY_ACTIVITY_TYPE:
			case "Stay":// old naming (TODO to be removed soon)
				vehicle.getSchedule().addTask(new TaxiStayTask(stay.startTime, stay.endTime, stay.link));
				return;

			case TaxiActionCreator.PICKUP_ACTIVITY_TYPE:
			case "PassengerPickup":// old naming (TODO to be removed soon)
				vehicle.getSchedule().addTask(new TaxiPickupTask(stay.startTime, stay.endTime, currentRequest));
				return;

			case TaxiActionCreator.DROPOFF_ACTIVITY_TYPE:
			case "PassengerDropoff":// old naming (TODO to be removed soon)
				// TODO setting 'toLink' should be moved to RequestRecorder once the events are re-ordered
				((TaxiRequestWithModifiableToLink)currentRequest).toLink = stay.link;
				vehicle.getSchedule().addTask(new TaxiDropoffTask(stay.startTime, stay.endTime, currentRequest));

				currentRequest = null;
				return;
		}
	}

	void addRequest(TaxiRequest request) {
		if (currentRequest != null) {
			throw new IllegalStateException("Currently only one passenger per vehicle");
		}
		currentRequest = request;
	}

	void endSchedule(double endTime) {
		if (currentRequest != null) {
			throw new IllegalStateException();
		}

		vehicle.setServiceEndTime(endTime);
		vehicle = null;// just to make sure no modifications will be made
	}

	boolean isScheduleBuilt() {
		return vehicle == null;
	}
}
