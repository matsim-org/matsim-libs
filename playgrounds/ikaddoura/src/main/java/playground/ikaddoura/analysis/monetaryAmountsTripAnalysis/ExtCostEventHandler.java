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
package playground.ikaddoura.analysis.monetaryAmountsTripAnalysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Network;

/**
 * WARNING: This analysis assumes each agent to have exactly two trips. TODO: Adjust for more than 2 trips.
 * 
 * @author Ihab
 *
 */
public class ExtCostEventHandler implements PersonDepartureEventHandler, LinkEnterEventHandler, PersonMoneyEventHandler, ActivityEndEventHandler {
				
	private List<Id> personIDsSecondTrip = new ArrayList<Id>();
		
	// departure time
	private Map<Id, Double> personId2firstTripDepartureTime = new HashMap<Id, Double>();
	private Map<Id, Double> personId2secondTripDepartureTime = new HashMap<Id, Double>();
	
	// fare
	private Map<Id, Double> personId2amountFirstTrip = new HashMap<Id, Double>();
	private Map<Id, Double> personId2amountSecondTrip = new HashMap<Id, Double>();
		
	// distance
	private Map<Id, Double> personId2distanceFirstTrip = new HashMap<Id, Double>();
	private Map<Id, Double> personId2distanceSecondTrip = new HashMap<Id, Double>();
		
	private Network network;
	
	public ExtCostEventHandler(Network network) {
		this.network = network;
	}

	@Override
	public void reset(int iteration) {
		this.personIDsSecondTrip.clear();
		this.personId2amountFirstTrip.clear();
		this.personId2amountSecondTrip.clear();
		this.personId2firstTripDepartureTime.clear();
		this.personId2secondTripDepartureTime.clear();
		this.personId2distanceFirstTrip.clear();
		this.personId2distanceSecondTrip.clear();
	}

	@Override
	public void handleEvent(PersonMoneyEvent event) {
		
		if (this.personIDsSecondTrip.contains(event.getPersonId())){
			// second trip
			if (personId2amountSecondTrip.containsKey(event.getPersonId())){
				double amountSum = this.personId2amountSecondTrip.get(event.getPersonId()) + event.getAmount();
				this.personId2amountSecondTrip.put(event.getPersonId(), amountSum);
			} else {
				this.personId2amountSecondTrip.put(event.getPersonId(), event.getAmount());
			}
			
		} else {
			// first trip
			if (personId2amountFirstTrip.containsKey(event.getPersonId())){
				double amountSum = this.personId2amountFirstTrip.get(event.getPersonId()) + event.getAmount();
				this.personId2amountFirstTrip.put(event.getPersonId(), amountSum);
			} else {
				this.personId2amountFirstTrip.put(event.getPersonId(), event.getAmount());
			}
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		
		if (event.getActType().equalsIgnoreCase("home")){
			this.personId2firstTripDepartureTime.put(event.getPersonId(), event.getTime());
		
		} else if (event.getActType().equalsIgnoreCase("secondary")){
			this.personIDsSecondTrip.add(event.getPersonId());
			this.personId2secondTripDepartureTime.put(event.getPersonId(), event.getTime());
//			System.out.println(Time.writeTime(event.getTime(), Time.TIMEFORMAT_HHMMSS));
		
		} else if (event.getActType().equalsIgnoreCase("work")){
			this.personIDsSecondTrip.add(event.getPersonId());
			this.personId2secondTripDepartureTime.put(event.getPersonId(), event.getTime());
//			System.out.println(Time.writeTime(event.getTime(), Time.TIMEFORMAT_HHMMSS));
		}
	}
	
	@Override
	public void handleEvent(LinkEnterEvent event) {
			
			double linkLength = this.network.getLinks().get(event.getLinkId()).getLength();

				if (this.personIDsSecondTrip.contains(event.getVehicleId())){
					// second trip
					
					if (this.personId2distanceSecondTrip.containsKey(event.getVehicleId())) {
						double updatedDistance = this.personId2distanceSecondTrip.get(event.getVehicleId()) + linkLength;
						this.personId2distanceSecondTrip.put(event.getVehicleId(), updatedDistance);
					} else {
						this.personId2distanceSecondTrip.put(event.getVehicleId(), linkLength);
					}
				} else {
					// first trip

					if (this.personId2distanceFirstTrip.containsKey(event.getVehicleId())) {
						double updatedDistance = this.personId2distanceFirstTrip.get(event.getVehicleId()) + linkLength;
						this.personId2distanceFirstTrip.put(event.getVehicleId(), updatedDistance);
					} else {
						this.personId2distanceFirstTrip.put(event.getVehicleId(), linkLength);
					}
				}
	}
	

	// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	
	public Map<Id, Double> getPersonId2firstTripDepartureTime() {
		return personId2firstTripDepartureTime;
	}

	public Map<Id, Double> getPersonId2secondTripDepartureTime() {
		return personId2secondTripDepartureTime;
	}

	public Map<Id, Double> getPersonId2amountFirstTrip() {
		return personId2amountFirstTrip;
	}

	public Map<Id, Double> getPersonId2amountSecondTrip() {
		return personId2amountSecondTrip;
	}

	public Map<Id, Double> getPersonId2distanceFirstTrip() {
		return personId2distanceFirstTrip;
	}

	public Map<Id, Double> getPersonId2distanceSecondTrip() {
		return personId2distanceSecondTrip;
	}

	public Map<Double, Double> getAvgAmountPerTripDepartureTime() {
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
						tripDepTime2fares.get(time).add(this.personId2amountFirstTrip.get(personId));
					}
				}
			}
			
			for (Id personId : this.personId2secondTripDepartureTime.keySet()){
				if (this.personId2secondTripDepartureTime.get(personId) < time && this.personId2secondTripDepartureTime.get(personId) >= (time - periodLength)) {
					if (tripDepTime2fares.containsKey(time)){
						tripDepTime2fares.get(time).add(this.personId2amountSecondTrip.get(personId));
					}
				}
			}
		}
		
		for (Double time : tripDepTime2fares.keySet()){
			double amountSum = 0.;
			double counter = 0.;
			for (Double amount : tripDepTime2fares.get(time)){
				if (amount == null){
					
				} else {
					amountSum = amountSum + amount;
					counter++;
				}
			}
			
			double avgFare = 0.;
			if (counter!=0.){
				avgFare = (-1) * amountSum / counter;
			}
			tripDepTime2avgFare.put(time, avgFare);
		}
		return tripDepTime2avgFare;
	}
	
	public Map<Double, Double> getAvgAmountPerTripDistance() {
		Map<Double, Double> tripDistance2avgAmount = new HashMap<Double, Double>();

		Map<Double, List<Double>> tripDistance2amount = new HashMap<Double, List<Double>>();
		double startDistance = 500.;
		double groupsize = 500.;
		double endDistance = 40 * 500.;
		
		for (double distance = startDistance; distance <= endDistance; distance = distance + groupsize){
			List<Double> amounts = new ArrayList<Double>();
			tripDistance2amount.put(distance, amounts);
		}
			
		for (Double dist : tripDistance2amount.keySet()){
			for (Id personId : this.personId2distanceFirstTrip.keySet()){
				if (this.personId2distanceFirstTrip.get(personId) <= dist && this.personId2distanceFirstTrip.get(personId) > (dist - groupsize)) {
					if (tripDistance2amount.containsKey(dist)){
						tripDistance2amount.get(dist).add(this.personId2amountFirstTrip.get(personId));
					}
				}
			}
			
			for (Id personId : this.personId2distanceSecondTrip.keySet()){
				if (this.personId2distanceSecondTrip.get(personId) <= dist && this.personId2distanceSecondTrip.get(personId) > (dist - groupsize)) {
					if (tripDistance2amount.containsKey(dist)){
						tripDistance2amount.get(dist).add(this.personId2amountSecondTrip.get(personId));
					}
				}
			}
		}
		
		for (Double dist : tripDistance2amount.keySet()){
			double amountSum = 0.;
			double counter = 0.;
			for (Double amount : tripDistance2amount.get(dist)){
				if (amount == null){
					
				} else {
					amountSum = amountSum + amount;
					counter++;
				}
			}
			
			double avgAmount = 0.;
			if (counter!=0.){
				avgAmount = (-1) * amountSum / counter;
			}
			tripDistance2avgAmount.put(dist, avgAmount);
		}
		return tripDistance2avgAmount;
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		// set 0 amounts - otherwise these agents appear nowhere
		
		if (event.getLegMode().equals(TransportMode.car)){
			if (this.personIDsSecondTrip.contains(event.getPersonId())){
				// second trip
				this.personId2amountSecondTrip.put(event.getPersonId(), 0.0);
				this.personId2distanceSecondTrip.put(event.getPersonId(), 0.0);
				
			} else {
				// first trip
				this.personId2amountFirstTrip.put(event.getPersonId(), 0.0);
				this.personId2distanceFirstTrip.put(event.getPersonId(), 0.0);
			}
		}
	}
	
}
