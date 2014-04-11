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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;

import playground.ikaddoura.internalizationCar.MarginalCongestionEvent;
import playground.ikaddoura.internalizationCar.MarginalCongestionEventHandler;

/**
 * 
 * @author ikaddoura , lkroeger
 *
 */
public class ExtCostEventHandler implements PersonMoneyEventHandler, TransitDriverStartsEventHandler , PersonDepartureEventHandler , LinkEnterEventHandler, PersonEntersVehicleEventHandler , PersonLeavesVehicleEventHandler , MarginalCongestionEventHandler {
	private final static Logger log = Logger.getLogger(ExtCostEventHandler.class);
	private final double vtts_car;
	
	// this analysis uses either money events or congestion events
	private final boolean useMoneyEvents;
	
	private Map<Id,Integer> personId2actualTripNumber = new HashMap<Id, Integer>();
	private Map<Id,Map<Integer,String>> personId2tripNumber2legMode = new HashMap<Id,Map<Integer,String>>();
	
	private Map<Id,Map<Integer,Double>> personId2tripNumber2departureTime = new HashMap<Id, Map<Integer,Double>>();
	private Map<Id,Map<Integer,Double>> personId2tripNumber2tripDistance = new HashMap<Id, Map<Integer,Double>>();
	private Map<Id,Map<Integer,Double>> personId2tripNumber2amount = new HashMap<Id, Map<Integer,Double>>();
	private Map<Id,Double> driverId2totalDistance = new HashMap<Id,Double>();
	
	// for pt-distance calculation
	private Map<Id,Double> personId2distanceEnterValue = new HashMap<Id,Double>();
	
	private List<Id> ptDrivers = new ArrayList<Id>();
	private Scenario scenario;
	
	private double distance = 500.;
	private double maxDistance = 40 * distance;
	private double timeBinSize = 900.0;
	
	public ExtCostEventHandler(Scenario scenario, boolean useMoneyEvents) {
		this.scenario = scenario;
		this.useMoneyEvents = useMoneyEvents;
		log.info("UseMoneyEvents : " + useMoneyEvents);
		this.vtts_car = (this.scenario.getConfig().planCalcScore().getTraveling_utils_hr() - this.scenario.getConfig().planCalcScore().getPerforming_utils_hr()) / this.scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney();
		log.info("VTTS_car: " + vtts_car);
	}

	@Override
	public void reset(int iteration) {
		personId2actualTripNumber.clear();
		personId2tripNumber2departureTime.clear();
		personId2tripNumber2tripDistance.clear();
		personId2tripNumber2amount.clear();
		personId2tripNumber2legMode.clear();
		driverId2totalDistance.clear();
		personId2distanceEnterValue.clear();
	}

	@Override
	public void handleEvent(MarginalCongestionEvent event) {
		
		if (useMoneyEvents == false){
			double amount = event.getDelay() / 3600 * this.vtts_car;
			double eventTime = event.getTime();
			int tripNumber = 0;
			double maxDepTime = Double.MIN_VALUE;
			Map<Integer,Double> tripNumber2departureTime = personId2tripNumber2departureTime.get(event.getCausingAgentId());
			
			for(int tripNr : tripNumber2departureTime.keySet()) {
				if(eventTime > tripNumber2departureTime.get(tripNr)) {
					if (tripNumber2departureTime.get(tripNr) > maxDepTime) {
						tripNumber = tripNr;
					}
				}
			}
				
			double amountBefore = personId2tripNumber2amount.get(event.getCausingAgentId()).get(tripNumber);
			double updatedAmount = amountBefore + amount;
			Map<Integer,Double> tripNumber2amount = personId2tripNumber2amount.get(event.getCausingAgentId());
			tripNumber2amount.put(tripNumber, updatedAmount);
			personId2tripNumber2amount.put(event.getCausingAgentId(), tripNumber2amount);
		}
	}
	
	@Override
	public void handleEvent(PersonMoneyEvent event) {
		
		if (useMoneyEvents == true) {
			double amount = event.getAmount();
			double eventTime = event.getTime();
			int tripNumber = 0;
			double maxDepTime = Double.MIN_VALUE;
			Map<Integer,Double> tripNumber2departureTime = personId2tripNumber2departureTime.get(event.getPersonId());
			
			for(int tripNr : tripNumber2departureTime.keySet()) {
				if(eventTime > tripNumber2departureTime.get(tripNr)) {
					if (tripNumber2departureTime.get(tripNr) > maxDepTime) {
						tripNumber = tripNr;
					}
				}
			}
				
			double amountBefore = personId2tripNumber2amount.get(event.getPersonId()).get(tripNumber);
			double updatedAmount = amountBefore + amount;
			Map<Integer,Double> tripNumber2amount = personId2tripNumber2amount.get(event.getPersonId());
			tripNumber2amount.put(tripNumber, updatedAmount);
			personId2tripNumber2amount.put(event.getPersonId(), tripNumber2amount);
		}
		
	}
	
	@Override
	public void handleEvent(LinkEnterEvent event) {
		System.out.println(event.toString());
		System.out.println(this.scenario.getNetwork().getLinks().toString());
		double linkLength = this.scenario.getNetwork().getLinks().get(event.getLinkId()).getLength();
		if(ptDrivers.contains(event.getVehicleId())){
			if(driverId2totalDistance.containsKey(event.getVehicleId())){
				driverId2totalDistance.put(event.getVehicleId(),driverId2totalDistance.get(event.getVehicleId()) + linkLength);
			} else {
				driverId2totalDistance.put(event.getVehicleId(),linkLength);
			}
		}else{
			// updating the trip Length
			int tripNumber = personId2actualTripNumber.get(event.getVehicleId());
			double distanceBefore = personId2tripNumber2tripDistance.get(event.getVehicleId()).get(tripNumber);
			double updatedDistance = distanceBefore + linkLength;
			Map<Integer,Double> tripNumber2tripDistance = personId2tripNumber2tripDistance.get(event.getVehicleId());
			tripNumber2tripDistance.put(tripNumber, updatedDistance);
			personId2tripNumber2tripDistance.put(event.getVehicleId(), tripNumber2tripDistance);
		}
	}
	
//	// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	
	public Map<Id,List<Double>> getPersonId2listOfDepartureTimes(String mode) {
		Map<Id,List<Double>> personId2listOfDepartureTimes = new HashMap<Id, List<Double>>();
		for(Id personId: personId2tripNumber2departureTime.keySet()){
			List<Double> times = new ArrayList<Double>();
			for(int i : personId2tripNumber2departureTime.get(personId).keySet()){
				if(personId2tripNumber2legMode.get(personId).get(i).toString().equals(mode)){
					double time = personId2tripNumber2departureTime.get(personId).get(i);
					times.add(time);
				}else{
				}
			}
			personId2listOfDepartureTimes.put(personId, times);
		}
		return personId2listOfDepartureTimes;
	}
	
	public Map<Id,List<Double>> getPersonId2listOfDistances(String mode) {
		Map<Id,List<Double>> personId2listOfDistances = new HashMap<Id, List<Double>>();
		for(Id personId: personId2tripNumber2tripDistance.keySet()){
			List<Double> distances = new ArrayList<Double>();
			for(int i : personId2tripNumber2tripDistance.get(personId).keySet()){
				if(personId2tripNumber2legMode.get(personId).get(i).toString().equals(mode)){
					double distance = personId2tripNumber2tripDistance.get(personId).get(i);
					distances.add(distance);
				}else{
				}
			}
			personId2listOfDistances.put(personId, distances);
		}
		return personId2listOfDistances;
	}
	
	public Map<Id,List<Double>> getPersonId2listOfAmounts(String mode) {
		Map<Id,List<Double>> personId2listOfAmounts = new HashMap<Id, List<Double>>();
		for(Id personId: personId2tripNumber2amount.keySet()){
			List<Double> amounts = new ArrayList<Double>();
			for(int i : personId2tripNumber2amount.get(personId).keySet()){
				if(personId2tripNumber2legMode.get(personId).get(i).toString().equals(mode)){
					double amount = personId2tripNumber2amount.get(personId).get(i);
					amounts.add(amount);
				}else{
				}
			}
			personId2listOfAmounts.put(personId, amounts);
		}
		return personId2listOfAmounts;
	}
	
	public Map<Double, Double> getAvgAmountPerTripDepartureTime(String mode) {
		Map<Double, Double> tripDepTime2avgFare = new HashMap<Double, Double>();

		Map<Double, List<Double>> tripDepTime2fares = new HashMap<Double, List<Double>>();
		double startTime = this.timeBinSize;
		double periodLength = this.timeBinSize;
		double endTime = 30. * 3600;
		
		for (double time = startTime; time <= endTime; time = time + periodLength){
			List<Double> fares = new ArrayList<Double>();
			tripDepTime2fares.put(time, fares);
		}
		
		Map<Integer, double[]> counter2allDepartureTimesAndAmounts = new HashMap<Integer, double[]>();
		int i = 0;
		
		for(Id personId : personId2tripNumber2departureTime.keySet()){
			for(int tripNumber : personId2tripNumber2departureTime.get(personId).keySet()){
				if(personId2tripNumber2legMode.get(personId).get(tripNumber).toString().equals(mode)){
					double departureTime = personId2tripNumber2departureTime.get(personId).get(tripNumber);
					double belongingAmount = personId2tripNumber2amount.get(personId).get(tripNumber);
					double[] departureTimeAndAmount = new double[2];
					departureTimeAndAmount[0] = departureTime;
					departureTimeAndAmount[1] = belongingAmount;				
					counter2allDepartureTimesAndAmounts.put(i, departureTimeAndAmount);
					i++;
				}else{
				}
			}
		}
		
		for (Double time : tripDepTime2fares.keySet()){
			for (int counter : counter2allDepartureTimesAndAmounts.keySet()){
				if (counter2allDepartureTimesAndAmounts.get(counter)[0] < time && counter2allDepartureTimesAndAmounts.get(counter)[0] >= (time - periodLength)) {
					if (tripDepTime2fares.containsKey(time)){
						tripDepTime2fares.get(time).add(counter2allDepartureTimesAndAmounts.get(counter)[1]);
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
	
	public Map<Double, Double> getAvgAmountPerTripDistance(String mode) {
		Map<Double, Double> tripDistance2avgAmount = new HashMap<Double, Double>();

		Map<Double, List<Double>> tripDistance2amount = new HashMap<Double, List<Double>>();
		double startDistance = this.distance;
		double groupsize = this.distance;
		double endDistance = this.maxDistance;
		
		for (double distance = startDistance; distance <= endDistance; distance = distance + groupsize){
			List<Double> amounts = new ArrayList<Double>();
			tripDistance2amount.put(distance, amounts);
		}
		
		Map<Integer, double[]> counter2allDistancesAndAmounts = new HashMap<Integer, double[]>();
		int i = 0;
		
		for(Id personId : personId2tripNumber2tripDistance.keySet()){
			for(int tripNumber : personId2tripNumber2tripDistance.get(personId).keySet()){
				if(personId2tripNumber2legMode.get(personId).get(tripNumber).toString().equals(mode)){
					double tripDistance = personId2tripNumber2tripDistance.get(personId).get(tripNumber);
					double belongingAmount = personId2tripNumber2amount.get(personId).get(tripNumber);
					double[] tripDistanceAndAmount = new double[2];
					tripDistanceAndAmount[0] = tripDistance;
					tripDistanceAndAmount[1] = belongingAmount;				
					counter2allDistancesAndAmounts.put(i, tripDistanceAndAmount);
					i++;
				}
			}
		}
		
		for (Double dist : tripDistance2amount.keySet()){
			for (int counter : counter2allDistancesAndAmounts.keySet()){
				if (counter2allDistancesAndAmounts.get(counter)[0] < dist && counter2allDistancesAndAmounts.get(counter)[0] >= (dist - groupsize)) {
					if (tripDistance2amount.containsKey(dist)){
						tripDistance2amount.get(dist).add(counter2allDistancesAndAmounts.get(counter)[1]);
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
		if(ptDrivers.contains(event.getPersonId())){
			// ptDrivers are not considered
		}else{
			if(event.getLegMode().toString().equals("transit_walk")){
				// pt_interactions are not considered
			} else {
				if(personId2actualTripNumber.containsKey(event.getPersonId())){
					// This is at least the second trip of the person
					personId2actualTripNumber.put(event.getPersonId(), personId2actualTripNumber.get(event.getPersonId())+1);
					Map<Integer,Double> tripNumber2departureTime = personId2tripNumber2departureTime.get(event.getPersonId());
					tripNumber2departureTime.put(personId2actualTripNumber.get(event.getPersonId()), event.getTime());
					personId2tripNumber2departureTime.put(event.getPersonId(), tripNumber2departureTime);
					Map<Integer,Double> tripNumber2tripDistance = personId2tripNumber2tripDistance.get(event.getPersonId());
					tripNumber2tripDistance.put(personId2actualTripNumber.get(event.getPersonId()), 0.0);
					personId2tripNumber2tripDistance.put(event.getPersonId(), tripNumber2tripDistance);
						
					Map<Integer,Double> tripNumber2amount = personId2tripNumber2amount.get(event.getPersonId());
					tripNumber2amount.put(personId2actualTripNumber.get(event.getPersonId()), 0.0);
					personId2tripNumber2amount.put(event.getPersonId(), tripNumber2amount);
						
					Map<Integer,String> tripNumber2legMode = personId2tripNumber2legMode.get(event.getPersonId());
					tripNumber2legMode.put(personId2actualTripNumber.get(event.getPersonId()), event.getLegMode());
					personId2tripNumber2legMode.put(event.getPersonId(), tripNumber2legMode);
				
				} else {
					// This is the first trip of the person
					personId2actualTripNumber.put(event.getPersonId(), 1);
					Map<Integer,Double> tripNumber2departureTime = new HashMap<Integer, Double>();
					tripNumber2departureTime.put(1, event.getTime());
					personId2tripNumber2departureTime.put(event.getPersonId(), tripNumber2departureTime);
					Map<Integer,Double> tripNumber2tripDistance = new HashMap<Integer, Double>();
					tripNumber2tripDistance.put(1, 0.0);
					personId2tripNumber2tripDistance.put(event.getPersonId(), tripNumber2tripDistance);
					
					Map<Integer,Double> tripNumber2amount = new HashMap<Integer, Double>();
					tripNumber2amount.put(1, 0.0);
					personId2tripNumber2amount.put(event.getPersonId(), tripNumber2amount);
						
					Map<Integer,String> tripNumber2legMode = new HashMap<Integer,String>();
					tripNumber2legMode.put(1, event.getLegMode());
					personId2tripNumber2legMode.put(event.getPersonId(), tripNumber2legMode);
				}
			}
		}
	}
	
	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		if (ptDrivers.contains(event.getDriverId())) {
			// already listed
		} else {
			ptDrivers.add(event.getDriverId());
		}
	}
	
	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if(ptDrivers.contains(event.getPersonId())){
			// ptDrivers are not considered
		} else {
			int tripNumber = personId2actualTripNumber.get(event.getPersonId());
			Map<Integer,String> tripNumber2legMode = personId2tripNumber2legMode.get(event.getPersonId());
			if((tripNumber2legMode.get(tripNumber)).equals(TransportMode.car)){
			// car drivers not considered here
			} else {
				double distanceTravelled = (driverId2totalDistance.get(event.getVehicleId()) - personId2distanceEnterValue.get(event.getVehicleId())); 
				
				Map<Integer,Double> tripNumber2distance = personId2tripNumber2tripDistance.get(event.getPersonId());
				tripNumber2distance.put(tripNumber, tripNumber2distance.get(tripNumber) + distanceTravelled);
				
				personId2distanceEnterValue.remove(event.getPersonId());
			}
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if(ptDrivers.contains(event.getPersonId())){
			// ptDrivers are not considered
		} else {
			int tripNumber = personId2actualTripNumber.get(event.getPersonId());
			Map<Integer,String> tripNumber2legMode = personId2tripNumber2legMode.get(event.getPersonId());
			if((tripNumber2legMode.get(tripNumber)).equals(TransportMode.car)){
			// car drivers not considered here
			} else {
				personId2distanceEnterValue.put(event.getPersonId(), driverId2totalDistance.get(event.getVehicleId()));
			}
		}
	}

}
