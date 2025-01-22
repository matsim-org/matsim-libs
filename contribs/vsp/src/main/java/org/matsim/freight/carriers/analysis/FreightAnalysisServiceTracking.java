/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.freight.carriers.analysis;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.events.CarrierServiceEndEvent;
import org.matsim.freight.carriers.events.CarrierServiceStartEvent;

import java.util.LinkedHashMap;

/**
 *  @deprecated We have new event types now, allowing us to use a more straight forward analysis without guessing.
 *  I will let this here for some time so we can have a look, what else should be moved over, but in the end, We will remove this here.
 *  (kmt apr'23)
 *
 * @author Jakob Harnisch (MATSim advanced class 2020/21)
 */

@Deprecated(since = "apr23", forRemoval = true)
class FreightAnalysisServiceTracking {

	private final LinkedHashMap<Id<Carrier>, ServiceTracker.CarrierServiceTracker> carrierServiceTrackers = new LinkedHashMap<>();

	public LinkedHashMap<Id<Carrier>, ServiceTracker.CarrierServiceTracker> getCarrierServiceTrackers(){return carrierServiceTrackers;}

	public void addTracker(CarrierService service, Id<Carrier> id) {
		ServiceTracker st = new ServiceTracker(service);
		if(carrierServiceTrackers.containsKey(id)){
			carrierServiceTrackers.get(id).serviceTrackers.put(service.getId(), st);
		} else {
			carrierServiceTrackers.put(id, new ServiceTracker.CarrierServiceTracker(id, service));
		}
	}

	// handle activityStartEvents to track the start of a service activity in case LSP Events are not thrown
	public void trackServiceActivityStart(ActivityStartEvent activityStartEvent) {
		for (ServiceTracker.CarrierServiceTracker cst: carrierServiceTrackers.values()) {
			for (ServiceTracker service : cst.serviceTrackers.values()) {
				if (service.service.getServiceLinkId().equals(activityStartEvent.getLinkId())) {
					if (service.driverId == null) {
						// if there is no driver, but there is a service which is to be performed at the moment at this place, we guess this could be the event for it.
						// (Does not work well obviously as soon as there are multiple services at a location that have generous time windows, like e.g. at stores).
						if (service.service.getServiceStaringTimeWindow().getStart() <= activityStartEvent.getTime() && activityStartEvent.getTime() <= service.service.getServiceStaringTimeWindow().getEnd()) {
							service.driverIdGuess = activityStartEvent.getPersonId();
							service.arrivalTimeGuess = activityStartEvent.getTime();
						}
						// if (unlikely) the driver is known for the service and this event is thrown at the location of that service, we assume this event is meant for this particular service.
						// (doesn't work well either, because the driver is likely not known without LSP events, and then we don't have to make guesses anyway.)
					} else if (service.driverId.toString().equals(activityStartEvent.getPersonId().toString())) {
						service.startTime = activityStartEvent.getTime();
					}
				}
			}
		}
	}

	public void setExpectedArrival(Id<Carrier> carrierId, Id<CarrierService> serviceId, double expectedArrival) {
	if (carrierServiceTrackers.containsKey(carrierId)){
		if (carrierServiceTrackers.get(carrierId).serviceTrackers.containsKey(serviceId)){
			carrierServiceTrackers.get(carrierId).serviceTrackers.get(serviceId).expectedArrival=expectedArrival;
		}
	}
	}

	public void setCalculatedArrival(Id<Carrier> carrierId, Id<CarrierService> serviceId, Double calculatedArrival) {
		if (carrierServiceTrackers.containsKey(carrierId)){
			if (carrierServiceTrackers.get(carrierId).serviceTrackers.containsKey(serviceId)){
				carrierServiceTrackers.get(carrierId).serviceTrackers.get(serviceId).calculatedArrival=calculatedArrival;
			}
		}
	}

	// UNTESTED handling of LSP Service events that provided reliable info about driver and timestamps.
	public void handleStartEvent(CarrierServiceStartEvent event) {
		if (carrierServiceTrackers.containsKey(event.getCarrierId())){
			if (carrierServiceTrackers.get(event.getCarrierId()).serviceTrackers.containsKey(event.getServiceId())){
				ServiceTracker service = carrierServiceTrackers.get(event.getCarrierId()).serviceTrackers.get(event.getServiceId());
//				service.driverId = event.getDriverId();
				service.startTime = event.getTime();
			}
		}
	}
	public void handleEndEvent(CarrierServiceEndEvent event) {
		if (carrierServiceTrackers.containsKey(event.getCarrierId())){
			if (carrierServiceTrackers.get(event.getCarrierId()).serviceTrackers.containsKey(event.getServiceId())){
				ServiceTracker service = carrierServiceTrackers.get(event.getCarrierId()).serviceTrackers.get(event.getServiceId());
				service.endTime = event.getTime();
			}
		}
	}

	// Method that estimates Arrival Times based on the expected leg departure and travel times to measure punctuality.
	public void estimateArrivalTimes(Carriers carriers) {
		for (Carrier carrier: carriers.getCarriers().values()){
			for (ScheduledTour tour : carrier.getSelectedPlan().getScheduledTours()) {
				double calculatedArrivalTime = 0.0;
				for (Tour.TourElement tourElement : tour.getTour().getTourElements()) {
					if (tourElement instanceof Tour.Leg) {
						calculatedArrivalTime = ((Tour.Leg) tourElement).getExpectedDepartureTime() + ((Tour.Leg) tourElement).getExpectedTransportTime();
					}
					if (tourElement instanceof Tour.ServiceActivity serviceAct) {
						Id<CarrierService> serviceId = serviceAct.getService().getId();
						setExpectedArrival(carrier.getId(),serviceId, serviceAct.getExpectedArrival());
						if (calculatedArrivalTime > 0.0) {
							setCalculatedArrival(carrier.getId(),serviceId, calculatedArrivalTime);
							calculatedArrivalTime = 0.0;
						}
					}
				}
			}
		}
	}
}

/**
 *  @deprecated We have new event types now, allowing us to use a more straight forward analysis without guessing.
 *  I will let this here for some time so we can have a look, what else should be moved over, but in the end, We will remove this here.
 *  (kmt apr'23)
 */
@Deprecated(since = "apr23")
class ServiceTracker {
	public CarrierService service;
	public Double calculatedArrival =0.0;
	public Id<Carrier> carrierId ;
	public Id<Person> driverId ;
	public Double startTime =0.0;
	public Id<Person> driverIdGuess;
	public Double arrivalTimeGuess = 0.0;
	public double expectedArrival = 0.0;
	public double endTime;

	public ServiceTracker(CarrierService service) {
		this.service=service;
	}
	static class CarrierServiceTracker{
		Id<Carrier> carrierId;
		LinkedHashMap<Id<CarrierService>, ServiceTracker> serviceTrackers= new LinkedHashMap<>();

		public CarrierServiceTracker(Id<Carrier> id, CarrierService service) {
			this.serviceTrackers.put(service.getId(), new ServiceTracker(service));
			this.carrierId=id;
		}
	}
}
