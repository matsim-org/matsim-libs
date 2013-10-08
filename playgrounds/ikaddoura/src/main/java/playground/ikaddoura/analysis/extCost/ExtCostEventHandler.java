/* *********************************************************************** *
 * project: org.matsim.*
 * MoneyEventHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.ikaddoura.analysis.extCost;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.api.experimental.events.AgentWaitingForPtEvent;
import org.matsim.core.api.experimental.events.handler.AgentWaitingForPtEventHandler;

/**
 * @author Ihab
 *
 */
public class ExtCostEventHandler implements LinkEnterEventHandler, AgentWaitingForPtEventHandler, PersonMoneyEventHandler, ActivityEndEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, TransitDriverStartsEventHandler, PersonDepartureEventHandler, PersonArrivalEventHandler {
	
	private final List<Id> ptDriverIDs = new ArrayList<Id>();
	private final List<Id> ptVehicleIDs = new ArrayList<Id>();
			
	private List<Id> personIDsSecondTrip = new ArrayList<Id>();
	
	private Map<Id, List<Id>> vehId2passengers = new HashMap<Id, List<Id>>();
	
	// departure time
	private Map<Id, Double> personId2firstTripDepartureTime = new HashMap<Id, Double>();
	private Map<Id, Double> personId2secondTripDepartureTime = new HashMap<Id, Double>();
	
	// fare
	private Map<Id, Double> personId2fareFirstTrip = new HashMap<Id, Double>();
	private Map<Id, Double> personId2fareSecondTrip = new HashMap<Id, Double>();
	
	// ############################################################################
	
	// location
	private Map<Id, Id> personId2BoardingLinkFirstTrip = new HashMap<Id, Id>();
	private Map<Id, Id> personId2BoardingLinkSecondTrip = new HashMap<Id, Id>();
	
	private Map<Id, Id> personId2AlightingLinkFirstTrip = new HashMap<Id, Id>();
	private Map<Id, Id> personId2AlightingLinkSecondTrip = new HashMap<Id, Id>();
	
	// distance
	private Map<Id, Double> personId2distanceFirstTrip = new HashMap<Id, Double>();
	private Map<Id, Double> personId2distanceSecondTrip = new HashMap<Id, Double>();
	
	// in-vehicle-time
	private Map<Id, Double> personId2boardingTimeFirstTrip = new HashMap<Id, Double>();
	private Map<Id, Double> personId2boardingTimeSecondTrip = new HashMap<Id, Double>();
	
	private Map<Id, Double> personId2alightingTimeFirstTrip = new HashMap<Id, Double>();
	private Map<Id, Double> personId2alightingTimeSecondTrip = new HashMap<Id, Double>();
	
	// waiting time
	private Map<Id, Double> personId2StartWaitingTimeFirstTrip = new HashMap<Id, Double>();
	private Map<Id, Double> personId2StartWaitingTimeSecondTrip = new HashMap<Id, Double>();
	
	// output: extCostPerTrip ; departureTime ; distance ; in-vehicle-time ; waiting time ; boardingLink ; alightingLink ; personId ; ( nrOfAgentsWaitingWhenDeparting ; nrOfAgentsInVehiclesWhenDeparting )
	
	@Override
	public void reset(int iteration) {
		this.ptDriverIDs.clear();
		this.ptVehicleIDs.clear();
		this.personIDsSecondTrip.clear();
		this.personId2fareFirstTrip.clear();
		this.personId2fareSecondTrip.clear();
		this.personId2firstTripDepartureTime.clear();
		this.personId2secondTripDepartureTime.clear();
	}

	@Override
	public void handleEvent(PersonMoneyEvent event) {
		
		if (this.personIDsSecondTrip.contains(event.getPersonId())){
			// second trip
			if (personId2fareSecondTrip.containsKey(event.getPersonId())){
				double amountSum = this.personId2fareSecondTrip.get(event.getPersonId()) + event.getAmount();
				this.personId2fareSecondTrip.put(event.getPersonId(), amountSum);
			} else {
				this.personId2fareSecondTrip.put(event.getPersonId(), event.getAmount());
			}
			
		} else {
			// first trip
			if (personId2fareFirstTrip.containsKey(event.getPersonId())){
				double amountSum = this.personId2fareFirstTrip.get(event.getPersonId()) + event.getAmount();
				this.personId2fareFirstTrip.put(event.getPersonId(), amountSum);
			} else {
				this.personId2fareFirstTrip.put(event.getPersonId(), event.getAmount());
			}
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
				
		if (event.getActType().equalsIgnoreCase("Home")){
			this.personId2firstTripDepartureTime.put(event.getPersonId(), event.getTime());
		
		} else if (event.getActType().equalsIgnoreCase("Other")){
			this.personIDsSecondTrip.add(event.getPersonId());
			this.personId2secondTripDepartureTime.put(event.getPersonId(), event.getTime());
		
		} else if (event.getActType().equalsIgnoreCase("Work")){
			this.personIDsSecondTrip.add(event.getPersonId());
			this.personId2secondTripDepartureTime.put(event.getPersonId(), event.getTime());
		}
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		
		if (!ptDriverIDs.contains(event.getPersonId()) && ptVehicleIDs.contains(event.getVehicleId())){
			
			// update number of passengers in vehicle
			if (this.vehId2passengers.containsKey(event.getVehicleId())){
				this.vehId2passengers.get(event.getVehicleId()).remove(event.getPersonId());
			} else {
				throw new RuntimeException("A person is leaving a public vehicle without entering it before. Aborting...");
			}
			
			if (this.personIDsSecondTrip.contains(event.getPersonId())){
				// second trip
				this.personId2alightingTimeSecondTrip.put(event.getPersonId(), event.getTime());
			} else {
				// first trip
				this.personId2alightingTimeFirstTrip.put(event.getPersonId(), event.getTime());
			}

		} else {
			// public transit driver
		}	
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		
		if (!ptDriverIDs.contains(event.getPersonId()) && ptVehicleIDs.contains(event.getVehicleId())){
			
			// update number of passengers in vehicle
			if (this.vehId2passengers.containsKey(event.getVehicleId())){
				this.vehId2passengers.get(event.getVehicleId()).add(event.getPersonId());
			} else {
				List<Id> passengersInVeh = new ArrayList<Id>();
				passengersInVeh.add(event.getPersonId());
				this.vehId2passengers.put(event.getVehicleId(), passengersInVeh);
			}
			
			if (this.personIDsSecondTrip.contains(event.getPersonId())){
				// second trip
				this.personId2boardingTimeSecondTrip.put(event.getPersonId(), event.getTime());
			} else {
				// first trip
				this.personId2boardingTimeFirstTrip.put(event.getPersonId(), event.getTime());
			}
			
			

		} else {
			// public transit driver
		}
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		
		if (!this.ptDriverIDs.contains(event.getDriverId())){
			this.ptDriverIDs.add(event.getDriverId());
		}
		
		if (!this.ptVehicleIDs.contains(event.getVehicleId())){
			this.ptVehicleIDs.add(event.getVehicleId());
		}
		
		this.vehId2passengers.put(event.getVehicleId(), new ArrayList<Id>());
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (event.getLegMode().equals(TransportMode.pt)){
			if (this.personIDsSecondTrip.contains(event.getPersonId())){
				// second trip
				this.personId2AlightingLinkSecondTrip.put(event.getPersonId(), event.getLinkId());
			} else {
				// first trip
				this.personId2AlightingLinkFirstTrip.put(event.getPersonId(), event.getLinkId());
			}

		} else {
			// public transit driver or car
		}	
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().equals(TransportMode.pt)){
			if (this.personIDsSecondTrip.contains(event.getPersonId())){
				// second trip
				this.personId2BoardingLinkSecondTrip.put(event.getPersonId(), event.getLinkId());
			} else {
				// first trip
				this.personId2BoardingLinkFirstTrip.put(event.getPersonId(), event.getLinkId());
			}

		} else {
			// public transit driver or car
		}
		
	}

	@Override
	public void handleEvent(AgentWaitingForPtEvent event) {
		
		if (this.personIDsSecondTrip.contains(event.getPersonId())){
			// second trip
			this.personId2StartWaitingTimeSecondTrip.put(event.getPersonId(), event.getTime());
		} else {
			// first trip
			this.personId2StartWaitingTimeFirstTrip.put(event.getPersonId(), event.getTime());
		}
	}
	
	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (this.ptVehicleIDs.contains(event.getVehicleId()) && this.ptDriverIDs.contains(event.getPersonId())){
			// public vehicle
			
			// TODO: get length of link from network
			double linkLength = 500.;
			
			for (Id passenger : this.vehId2passengers.get(event.getVehicleId())){
				if (this.personIDsSecondTrip.contains(passenger)){
					// second trip
					if (this.personId2distanceSecondTrip.containsKey(passenger)) {
						double updatedDistance = this.personId2distanceSecondTrip.get(passenger) + linkLength;
						this.personId2distanceSecondTrip.put(passenger, updatedDistance);
					} else {
						this.personId2distanceSecondTrip.put(passenger, linkLength);
					}
				} else {
					// first trip
					if (this.personId2distanceFirstTrip.containsKey(passenger)) {
						double updatedDistance = this.personId2distanceFirstTrip.get(passenger) + linkLength;
						this.personId2distanceFirstTrip.put(passenger, updatedDistance);
					} else {
						this.personId2distanceFirstTrip.put(passenger, linkLength);
					}
				}
			}
			
		} else {
			// car
		}
	}
	
	
	
	// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	
	public Map<Id, Double> getPersonId2firstTripDepartureTime() {
		return personId2firstTripDepartureTime;
	}

	public Map<Id, Double> getPersonId2secondTripDepartureTime() {
		return personId2secondTripDepartureTime;
	}

	public Map<Id, Double> getPersonId2fareFirstTrip() {
		return personId2fareFirstTrip;
	}

	public Map<Id, Double> getPersonId2fareSecondTrip() {
		return personId2fareSecondTrip;
	}

	public Map<Id, Id> getPersonId2BoardingLinkFirstTrip() {
		return personId2BoardingLinkFirstTrip;
	}

	public Map<Id, Id> getPersonId2BoardingLinkSecondTrip() {
		return personId2BoardingLinkSecondTrip;
	}

	public Map<Id, Id> getPersonId2AlightingLinkFirstTrip() {
		return personId2AlightingLinkFirstTrip;
	}

	public Map<Id, Id> getPersonId2AlightingLinkSecondTrip() {
		return personId2AlightingLinkSecondTrip;
	}

	public Map<Id, Double> getPersonId2distanceFirstTrip() {
		return personId2distanceFirstTrip;
	}

	public Map<Id, Double> getPersonId2distanceSecondTrip() {
		return personId2distanceSecondTrip;
	}

	public Map<Id, Double> getPersonId2inVehTimeFirstTrip() {
		Map<Id, Double> personId2inVehTimeFirstTrip = new HashMap<Id, Double>();
		for (Id id : this.personId2alightingTimeFirstTrip.keySet()){
			double inVehTime = this.personId2alightingTimeFirstTrip.get(id) - this.personId2boardingTimeFirstTrip.get(id);
			personId2inVehTimeFirstTrip.put(id, inVehTime);
		}
		
		return personId2inVehTimeFirstTrip;
	}

	public Map<Id, Double> getPersonId2inVehTimeSecondTrip() {
		Map<Id, Double> personId2inVehTimeSecondTrip = new HashMap<Id, Double>();

		for (Id id : this.personId2alightingTimeSecondTrip.keySet()){
			double inVehTime = this.personId2alightingTimeSecondTrip.get(id) - this.personId2boardingTimeSecondTrip.get(id);
			personId2inVehTimeSecondTrip.put(id, inVehTime);
		}		
		
		return personId2inVehTimeSecondTrip;
	}

	public Map<Id, Double> getPersonId2waitingTimeFirstTrip() {
		Map<Id, Double> personId2waitingTimeFirstTrip = new HashMap<Id, Double>();
		for (Id id : this.personId2boardingTimeFirstTrip.keySet()){
			double waitingTime = this.personId2boardingTimeFirstTrip.get(id) - this.personId2StartWaitingTimeFirstTrip.get(id);
			personId2waitingTimeFirstTrip.put(id, waitingTime);
		}

		return personId2waitingTimeFirstTrip;
	}

	public Map<Id, Double> getPersonId2waitingTimeSecondTrip() {
		Map<Id, Double> personId2waitingTimeSecondTrip = new HashMap<Id, Double>();
		for (Id id : this.personId2boardingTimeSecondTrip.keySet()){
			double waitingTime = this.personId2boardingTimeSecondTrip.get(id) - this.personId2StartWaitingTimeSecondTrip.get(id);
			personId2waitingTimeSecondTrip.put(id, waitingTime);
		}
		
		return personId2waitingTimeSecondTrip;
	}

	public Map<Double, Double> getAvgFarePerTripDepartureTime() {
		Map<Double, Double> tripDepTime2avgFare = new HashMap<Double, Double>();

		Map<Double, List<Double>> tripDepTime2fares = new HashMap<Double, List<Double>>();
		double startTime = 4. * 3600;
		double periodLength = 7200;
		double endTime = 24. * 3600;
		
		for (double time = startTime; time <= endTime; time = time + periodLength){
			List<Double> fares = new ArrayList<Double>();
			tripDepTime2fares.put(time, fares);
		}
			
		for (Double time : tripDepTime2fares.keySet()){
			for (Id personId : this.personId2firstTripDepartureTime.keySet()){
				if (this.personId2firstTripDepartureTime.get(personId) < time && this.personId2firstTripDepartureTime.get(personId) >= (time - periodLength)) {
					if (tripDepTime2fares.containsKey(time)){
						tripDepTime2fares.get(time).add(this.personId2fareFirstTrip.get(personId));
					}
				}
			}
			
			for (Id personId : this.personId2secondTripDepartureTime.keySet()){
				if (this.personId2secondTripDepartureTime.get(personId) < time && this.personId2secondTripDepartureTime.get(personId) >= (time - periodLength)) {
					if (tripDepTime2fares.containsKey(time)){
						tripDepTime2fares.get(time).add(this.personId2fareSecondTrip.get(personId));
					}
				}
			}
		}
		
		for (Double time : tripDepTime2fares.keySet()){
			double fareSum = 0.;
			double counter = 0.;
			for (Double fare : tripDepTime2fares.get(time)){
				if (fare == null){
					
				} else {
					fareSum = fareSum + fare;
					counter++;
				}
			}
			
			double avgFare = 0.;
			if (counter!=0.){
				avgFare = (-1) * fareSum / counter;
			}
			tripDepTime2avgFare.put(time, avgFare);
		}
		return tripDepTime2avgFare;
	}
	
	public Map<Double, Double> getAvgFarePerTripDistance() {
		Map<Double, Double> tripDistance2avgFare = new HashMap<Double, Double>();

		Map<Double, List<Double>> tripDistance2fares = new HashMap<Double, List<Double>>();
		double startDistance = 500.;
		double groupsize = 500.;
		double endDistance = 40 * 500.;
		
		for (double distance = startDistance; distance <= endDistance; distance = distance + groupsize){
			List<Double> fares = new ArrayList<Double>();
			tripDistance2fares.put(distance, fares);
		}
			
		for (Double dist : tripDistance2fares.keySet()){
			for (Id personId : this.personId2distanceFirstTrip.keySet()){
				if (this.personId2distanceFirstTrip.get(personId) <= dist && this.personId2distanceFirstTrip.get(personId) > (dist - groupsize)) {
					if (tripDistance2fares.containsKey(dist)){
						tripDistance2fares.get(dist).add(this.personId2fareFirstTrip.get(personId));
					}
				}
			}
			
			for (Id personId : this.personId2distanceSecondTrip.keySet()){
				if (this.personId2distanceSecondTrip.get(personId) <= dist && this.personId2distanceSecondTrip.get(personId) > (dist - groupsize)) {
					if (tripDistance2fares.containsKey(dist)){
						tripDistance2fares.get(dist).add(this.personId2fareSecondTrip.get(personId));
					}
				}
			}
		}
		
		for (Double dist : tripDistance2fares.keySet()){
			double fareSum = 0.;
			double counter = 0.;
			for (Double fare : tripDistance2fares.get(dist)){
				if (fare == null){
					
				} else {
					fareSum = fareSum + fare;
					counter++;
				}
			}
			
			double avgFare = 0.;
			if (counter!=0.){
				avgFare = (-1) * fareSum / counter;
			}
			tripDistance2avgFare.put(dist, avgFare);
		}
		return tripDistance2avgFare;
	}
	
	public Map<Double, Double> getAvgFarePerInVehTime() {
		Map<Double, Double> x2avgFare = new HashMap<Double, Double>();
		
		Map<Id, Double> personId2inVehTimeFirstTrip = this.getPersonId2inVehTimeFirstTrip();
		Map<Id, Double> personId2inVehTimeSecondTrip = this.getPersonId2inVehTimeSecondTrip();

		double max = 0;
		for (Double inVehTime : personId2inVehTimeFirstTrip.values()) {
			if (inVehTime > max) {
				max = inVehTime;
			}
		}
		for (Double inVehTime : personId2inVehTimeSecondTrip.values()) {
			if (inVehTime > max) {
				max = inVehTime;
			}
		}
		Map<Double, List<Double>> x2fares = new HashMap<Double, List<Double>>();
		double xMin = 100.;
		double xSize = 100.;
		double xMax = 3 * 1000.;
		
		System.out.println("MAX: " + max);
		System.out.println("xMAX: " + xMax);
		
		for (double x = xMin; x <= xMax; x = x + xSize){
			List<Double> fares = new ArrayList<Double>();
			x2fares.put(x, fares);
		}
					
		for (Double x : x2fares.keySet()){
			for (Id personId : personId2inVehTimeFirstTrip.keySet()){
				if (personId2inVehTimeFirstTrip.get(personId) < x && personId2inVehTimeFirstTrip.get(personId) >= (x - xSize)) {
					if (x2fares.containsKey(x)){
						x2fares.get(x).add(this.personId2fareFirstTrip.get(personId));
					}
				}
			}
			
			for (Id personId : personId2inVehTimeSecondTrip.keySet()){
				if (personId2inVehTimeSecondTrip.get(personId) < x && personId2inVehTimeSecondTrip.get(personId) >= (x - xSize)) {
					if (x2fares.containsKey(x)){
						x2fares.get(x).add(this.personId2fareSecondTrip.get(personId));
					}
				}
			}
		}
		
		for (Double x : x2fares.keySet()){
			double fareSum = 0.;
			double counter = 0.;
			for (Double fare : x2fares.get(x)){
				if (fare == null){
					
				} else {
					fareSum = fareSum + fare;
					counter++;
				}
			}
			
			double avgFare = 0.;
			if (counter!=0.){
				avgFare = (-1) * fareSum / counter;
			}
			x2avgFare.put(x, avgFare);
		}
		return x2avgFare;
	}
	
	public Map<Double, Double> getAvgFarePerWaitingTime() {
		Map<Double, Double> x2avgFare = new HashMap<Double, Double>();
		Map<Id, Double> personId2waitingTimeFirstTrip = this.getPersonId2waitingTimeFirstTrip();
		Map<Id, Double> personId2waitingTimeSecondTrip = this.getPersonId2waitingTimeSecondTrip();

		
		Map<Double, List<Double>> x2fares = new HashMap<Double, List<Double>>();
		double xMin = 0.;
		double xSize = 900.;
		double xMax = 10 * 900.;
		
		for (double x = xMin; x <= xMax; x = x + xSize){
			List<Double> fares = new ArrayList<Double>();
			x2fares.put(x, fares);
		}
			
		for (Double x : x2fares.keySet()){
			for (Id personId : personId2waitingTimeFirstTrip.keySet()){
				if (personId2waitingTimeFirstTrip.get(personId) < x && personId2waitingTimeFirstTrip.get(personId) >= (x - xSize)) {
					if (x2fares.containsKey(x)){
						x2fares.get(x).add(this.personId2fareFirstTrip.get(personId));
					}
				}
			}
			
			for (Id personId : personId2waitingTimeSecondTrip.keySet()){
				if (personId2waitingTimeSecondTrip.get(personId) < x && personId2waitingTimeSecondTrip.get(personId) >= (x - xSize)) {
					if (x2fares.containsKey(x)){
						x2fares.get(x).add(this.personId2fareSecondTrip.get(personId));
					}
				}
			}
		}
		
		for (Double x : x2fares.keySet()){
			double fareSum = 0.;
			double counter = 0.;
			for (Double fare : x2fares.get(x)){
				if (fare == null){
					
				} else {
					fareSum = fareSum + fare;
					counter++;
				}
			}
			
			double avgFare = 0.;
			if (counter!=0.){
				avgFare = (-1) * fareSum / counter;
			}
			x2avgFare.put(x, avgFare);
		}
		return x2avgFare;
	}
	
	public Map<Id, Double> getAvgFarePerBoardingLinkId() {
		Map<Id, Double> x2avgFare = new HashMap<Id, Double>();

		Map<Id, List<Double>> x2fares = new HashMap<Id, List<Double>>();
		List<Id> linkIds = new ArrayList<Id>();
		
		for (Id id : this.personId2BoardingLinkFirstTrip.values()){
			if (linkIds.contains(id)) {
				//
			} else {
				linkIds.add(id);
			}
		}
		
//		for (Id id : this.personId2BoardingLinkSecondTrip.values()){
//			if (linkIds.contains(id)) {
//				//
//			} else {
//				linkIds.add(id);
//			}
//		}
		
		for (Id id : this.personId2AlightingLinkFirstTrip.values()){
			if (linkIds.contains(id)) {
				//
			} else {
				linkIds.add(id);
			}
		}
		
//		for (Id id : this.personId2AlightingLinkSecondTrip.values()){
//			if (linkIds.contains(id)) {
//				//
//			} else {
//				linkIds.add(id);
//			}
//		}
		
		for (Id id : linkIds){
			List<Double> fares = new ArrayList<Double>();
			x2fares.put(id, fares);
		}
			
		for (Id x : x2fares.keySet()){
			for (Id personId : this.getPersonId2BoardingLinkFirstTrip().keySet()){
				if (this.getPersonId2BoardingLinkFirstTrip().get(personId).toString().equals(x.toString())) {
					if (x2fares.containsKey(x)){
						x2fares.get(x).add(this.personId2fareFirstTrip.get(personId));
					}
				}
			}
			
			for (Id personId : this.getPersonId2BoardingLinkSecondTrip().keySet()){
				if (this.getPersonId2BoardingLinkSecondTrip().get(personId).toString().equals(x.toString())) {
					if (x2fares.containsKey(x)){
						x2fares.get(x).add(this.personId2fareSecondTrip.get(personId));
					}
				}
			}
		}
		
		for (Id x : x2fares.keySet()){
			double fareSum = 0.;
			double counter = 0.;
			for (Double fare : x2fares.get(x)){
				if (fare == null){
					
				} else {
					fareSum = fareSum + fare;
					counter++;
				}
			}
			
			double avgFare = 0.;
			if (counter!=0.){
				avgFare = (-1) * fareSum / counter;
			}
			x2avgFare.put(x, avgFare);
		}
		return x2avgFare;
	}
}
