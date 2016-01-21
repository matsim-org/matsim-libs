/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityIntervalTracker.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.wrashid.PSF2.chargingSchemes;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;

import playground.wrashid.PSF.ParametersPSF;
import playground.wrashid.PSF2.ParametersPSF2;
import playground.wrashid.PSF2.vehicle.vehicleFleet.Vehicle;

/**
 * 
 * There is a race condition between two handlers: LinkEnergyConsumptionTracker
 * and ActivityIntervalTracker. Therefore use just one thread when using
 * parallelEventHandling.
 * 
 * content: assume, we have a activity chain like:
 * a1-walk-a2-car-a3-walk-a4-walk-a5-car-a6-walk-a1
 * 
 * then, we need to report the following activity time intervals: 1.) start-a3
 * to end-a5 2.) start-a6 to end-a2
 * 
 * TODO: with introduction of parking activities, that parking activity should
 * be ignored for checking of activity type, but the activity start event should
 * be passed on, as it is used for the location information.
 * 
 * @author wrashid
 */
public class ActivityIntervalTracker_NonParallelizableHandler implements ActivityStartEventHandler, PersonDepartureEventHandler,
		LinkEnterEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {

	// personId, time
	HashMap<Id, Double> timeOfFirstCarDeparture;

	// personid, transportation mode
	HashMap<Id, String> mostRecentLegMode;

	// personId, activity type
	HashMap<Id, ActivityStartEvent> firstActivityAfterParkingCar;

	// personId, using car
	HashMap<Id, Boolean> stillBeforeFristEnterLinkEvent;

	// personId, time
	HashMap<Id, Double> timeOfMostRecentDeparture;
	
	Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler() ;

	@Override
	public void reset(int iteration) {
		delegate.reset( iteration );

		timeOfFirstCarDeparture = new HashMap<Id, Double>();

		mostRecentLegMode = new HashMap<Id, String>();

		firstActivityAfterParkingCar = new HashMap<Id, ActivityStartEvent>();

		ParametersPSF2.chargingTimes = new HashMap<>();

		stillBeforeFristEnterLinkEvent = new HashMap<Id, Boolean>();

		timeOfMostRecentDeparture = new HashMap<Id, Double>();
		
	}

	public void handleLastParkingActivityOfDay() {
		for (ActivityStartEvent activityStartEvent : firstActivityAfterParkingCar.values()) {
			Id personId = activityStartEvent.getPersonId();

			if (isChargingPossible(activityStartEvent)) {
				double departureTime = timeOfFirstCarDeparture.get(personId);
				chargeVehicle(personId, departureTime);
			}
		}

	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		Id personId = event.getPersonId();

		

		if (mostRecentTransportationModeWasCar(personId)) {
			firstActivityAfterParkingCar.put(event.getPersonId(), event);
		}
	}

	private boolean mostRecentTransportationModeWasCar(Id personId) {
		if (mostRecentLegMode.containsKey(personId)) {
			return mostRecentLegMode.get(personId).equals(TransportMode.car);
		}
		return false;
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		Id personId = event.getPersonId();

		

		timeOfMostRecentDeparture.put(personId, event.getTime());

		initializeLegModeDetection(event);
	}

	private void initializeLegModeDetection(PersonDepartureEvent event) {
		stillBeforeFristEnterLinkEvent.put(event.getPersonId(), true);

		mostRecentLegMode.put(event.getPersonId(), "unknown");
	}

	private void chargeVehicle(Id personId, double departureTime) {
		ActivityStartEvent activityStartEvent = firstActivityAfterParkingCar.get(personId);
		Vehicle vehicle = ParametersPSF2.vehicles.getValue(personId);
		Double actStartTime = activityStartEvent.getTime();

		ParametersPSF2.energyStateMaintainer.chargeVehicle(vehicle, actStartTime, departureTime, ParametersPSF
				.getFacilityChargingPowerMapper().getChargingPower(activityStartEvent.getFacilityId()), activityStartEvent
				.getLinkId(), activityStartEvent.getFacilityId());
	}

	private boolean isChargingPossible(ActivityStartEvent activityStartEvent) {
		return ParametersPSF2.isChargingPossibleAtActivityLocation(activityStartEvent.getActType());
	}

	private boolean isFirstDepartureOfDayWithCar(Id personId) {
		return !timeOfFirstCarDeparture.containsKey(personId);
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Id personId = delegate.getDriverOfVehicle( event.getVehicleId() ) ;

		

		if (stillBeforeFristEnterLinkEvent.get(personId)) {

			ActivityStartEvent activityStartEvent = firstActivityAfterParkingCar.get(personId);

			double departureTime = timeOfMostRecentDeparture.get(personId);

			if (isFirstDepartureOfDayWithCar(personId)) {
				timeOfFirstCarDeparture.put(personId, departureTime);
			} else if (isChargingPossible(activityStartEvent)) {
				chargeVehicle(personId, departureTime);
			}
			stillBeforeFristEnterLinkEvent.put(personId, false);

			mostRecentLegMode.put(personId, "car");
		}
	}
	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		this.delegate.handleEvent(event);
	}
	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		this.delegate.handleEvent(event);
	}

	

}
