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
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.network.Network;

/**
 * This analysis doesn't assume each agent to have exactly two trips.
 * 
 * @author ikaddoura , lkroeger
 *
 */
public class ExtCostEventHandler implements TransitDriverStartsEventHandler , PersonDepartureEventHandler, PersonArrivalEventHandler, LinkEnterEventHandler, PersonMoneyEventHandler, ActivityEndEventHandler {
	// necessary for calculating the trip distance during the iteration
	private Map<Id,Integer> personId2actualTripNumber = new HashMap<Id, Integer>();
	
	private Map<Id,Map<Integer,String>> personId2tripNumber2legMode = new HashMap<Id,Map<Integer,String>>();
	
	// departure times
	private Map<Id,Map<Integer,Double>> personId2tripNumber2departureTime = new HashMap<Id, Map<Integer,Double>>();
	
	// trip distances
	private Map<Id,Map<Integer,Double>> personId2tripNumber2tripDistance = new HashMap<Id, Map<Integer,Double>>();
	
	// fares
	private Map<Id,Map<Integer,Double>> personId2tripNumber2amount = new HashMap<Id, Map<Integer,Double>>();
	
	private Map<Id,Double> drivers2totalDistance = new HashMap<Id,Double>();
	
	// for pt-distance calculation
//	private Map<Id,Double> personId2enterValue = new HashMap<Id,Double>();
	
	private List<Id> ptDrivers = new ArrayList<Id>();
	
	private Network network;

	public ExtCostEventHandler(Network network) {
		this.network = network;
	}

	@Override
	public void reset(int iteration) {
		personId2actualTripNumber.clear();
		personId2tripNumber2departureTime.clear();
		personId2tripNumber2tripDistance.clear();
		personId2tripNumber2amount.clear();
		personId2tripNumber2legMode.clear();
	}

	@Override
	public void handleEvent(PersonMoneyEvent event) {
		double amount = event.getAmount();
		double eventTime = event.getTime();
		int x = 1;
		for(int i : personId2tripNumber2departureTime.get(event.getPersonId()).keySet()){
			if(eventTime > (personId2tripNumber2departureTime.get(event.getPersonId()).get(i))){
				x = i;
			}else{
			}
		}
		int tripNumber = x;
			
		double amountBefore = personId2tripNumber2amount.get(event.getPersonId()).get(tripNumber);
		double updatedAmount = amountBefore + amount;
		Map<Integer,Double> tripNumber2amount = personId2tripNumber2amount.get(event.getPersonId());
		tripNumber2amount.put(tripNumber, updatedAmount);
		personId2tripNumber2amount.put(event.getPersonId(), tripNumber2amount);
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		// instead, PersonDepartureEvents are used.
		
//		
//		if (event.getActType().equalsIgnoreCase("home")){
//			this.personId2firstTripDepartureTime.put(event.getPersonId(), event.getTime());
//		
//		} else if (event.getActType().equalsIgnoreCase("secondary")){
//			this.personIDsSecondTrip.add(event.getPersonId());
//			this.personId2secondTripDepartureTime.put(event.getPersonId(), event.getTime());
////			System.out.println(Time.writeTime(event.getTime(), Time.TIMEFORMAT_HHMMSS));
//		
//		} else if (event.getActType().equalsIgnoreCase("work")){
//			this.personIDsSecondTrip.add(event.getPersonId());
//			this.personId2secondTripDepartureTime.put(event.getPersonId(), event.getTime());
////			System.out.println(Time.writeTime(event.getTime(), Time.TIMEFORMAT_HHMMSS));
//		}
	}
	
	@Override
	public void handleEvent(LinkEnterEvent event) {
		double linkLength = this.network.getLinks().get(event.getLinkId()).getLength();
		if(ptDrivers.contains(event.getVehicleId())){
			if(drivers2totalDistance.containsKey(event.getVehicleId())){
				drivers2totalDistance.put(event.getVehicleId(),drivers2totalDistance.get(event.getVehicleId()) + linkLength);
			}else{
				drivers2totalDistance.put(event.getVehicleId(),linkLength);
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
	
//	public Map<Id, Integer> getPersonId2NumberOfTrips() {
//		Map<Id,Integer> personId2numberOfTrips = new HashMap<Id, Integer>();
//		for(Id personId : personId2actualTripNumber.keySet()){
//			int numberOfTrips = personId2actualTripNumber.get(personId);
//			personId2numberOfTrips.put(personId,numberOfTrips);
//		}
//		return personId2numberOfTrips;
//		// should be called only after the start of the last activity (= at the end of the iteration) 
//	}
	
	public Map<Id, Integer> getPersonId2NumberOfTripsCar() {
		Map<Id,Integer> personId2numberOfTripsCar = new HashMap<Id, Integer>();
		for(Id personId : personId2actualTripNumber.keySet()){
			int numberOfTrips = 0;
			for(int i : personId2tripNumber2legMode.get(personId).keySet()){
				if(personId2tripNumber2legMode.get(personId).get(i).toString().equals(TransportMode.car)){
					numberOfTrips++;
				}else{
				}
			}
			personId2numberOfTripsCar.put(personId,numberOfTrips);
		}
		return personId2numberOfTripsCar;
		// should be called only after the start of the last activity (= at the end of the iteration) 
	}
	
	public Map<Id, Integer> getPersonId2NumberOfTripsPt() {
		Map<Id,Integer> personId2numberOfTripsPt = new HashMap<Id, Integer>();
		for(Id personId : personId2actualTripNumber.keySet()){
			int numberOfTrips = 0;
			for(int i : personId2tripNumber2legMode.get(personId).keySet()){
				if(personId2tripNumber2legMode.get(personId).get(i).toString().equals(TransportMode.pt)){
					numberOfTrips++;
				}else{
				}
			}
			personId2numberOfTripsPt.put(personId,numberOfTrips);
		}
		return personId2numberOfTripsPt;
		// should be called only after the start of the last activity (= at the end of the iteration) 
	}
	
//	public Map<Id,List<Double>> getPersonId2listOfDepartureTimes() {
//		Map<Id,List<Double>> personId2listOfDepartureTimes = new HashMap<Id, List<Double>>();
//		for(Id personId: personId2tripNumber2departureTime.keySet()){
//			List<Double> times = new ArrayList<Double>();
//			for(int i : personId2tripNumber2departureTime.get(personId).keySet()){
//				double time = personId2tripNumber2departureTime.get(personId).get(i);
//				times.add(time);
//			}
//			personId2listOfDepartureTimes.put(personId, times);
//		}
//		return personId2listOfDepartureTimes;
//	}
	
	public Map<Id,List<Double>> getPersonId2listOfDepartureTimesCar() {
		Map<Id,List<Double>> personId2listOfDepartureTimesCar = new HashMap<Id, List<Double>>();
		for(Id personId: personId2tripNumber2departureTime.keySet()){
			List<Double> times = new ArrayList<Double>();
			for(int i : personId2tripNumber2departureTime.get(personId).keySet()){
				if(personId2tripNumber2legMode.get(personId).get(i).toString().equals(TransportMode.car)){
					double time = personId2tripNumber2departureTime.get(personId).get(i);
					times.add(time);
				}else{
				}
			}
			personId2listOfDepartureTimesCar.put(personId, times);
		}
		return personId2listOfDepartureTimesCar;
	}
	
	public Map<Id,List<Double>> getPersonId2listOfDepartureTimesPt() {
		Map<Id,List<Double>> personId2listOfDepartureTimesPt = new HashMap<Id, List<Double>>();
		for(Id personId: personId2tripNumber2departureTime.keySet()){
			List<Double> times = new ArrayList<Double>();
			for(int i : personId2tripNumber2departureTime.get(personId).keySet()){
				if(personId2tripNumber2legMode.get(personId).get(i).toString().equals(TransportMode.pt)){
					double time = personId2tripNumber2departureTime.get(personId).get(i);
					times.add(time);
				}else{
				}
			}
			personId2listOfDepartureTimesPt.put(personId, times);
		}
		return personId2listOfDepartureTimesPt;
	}

//	public Map<Id,List<Double>> getPersonId2listOfDistances() {
//		Map<Id,List<Double>> personId2listOfDistances = new HashMap<Id, List<Double>>();
//		for(Id personId: personId2tripNumber2tripDistance.keySet()){
//			List<Double> distances = new ArrayList<Double>();
//			for(int i : personId2tripNumber2tripDistance.get(personId).keySet()){
//				double distance = personId2tripNumber2tripDistance.get(personId).get(i);
//				distances.add(distance);
//			}
//			personId2listOfDistances.put(personId, distances);
//		}
//		return personId2listOfDistances;
//	}
	
	public Map<Id,List<Double>> getPersonId2listOfDistancesCar() {
		Map<Id,List<Double>> personId2listOfDistancesCar = new HashMap<Id, List<Double>>();
		for(Id personId: personId2tripNumber2tripDistance.keySet()){
			List<Double> distances = new ArrayList<Double>();
			for(int i : personId2tripNumber2tripDistance.get(personId).keySet()){
				if(personId2tripNumber2legMode.get(personId).get(i).toString().equals(TransportMode.car)){
					double distance = personId2tripNumber2tripDistance.get(personId).get(i);
					distances.add(distance);
				}else{
				}
			}
			personId2listOfDistancesCar.put(personId, distances);
		}
		return personId2listOfDistancesCar;
	}
	
	public Map<Id,List<Double>> getPersonId2listOfDistancesPt() {
		Map<Id,List<Double>> personId2listOfDistancesPt = new HashMap<Id, List<Double>>();
		for(Id personId: personId2tripNumber2tripDistance.keySet()){
			List<Double> distances = new ArrayList<Double>();
			for(int i : personId2tripNumber2tripDistance.get(personId).keySet()){
				if(personId2tripNumber2legMode.get(personId).get(i).toString().equals(TransportMode.pt)){
					double distance = personId2tripNumber2tripDistance.get(personId).get(i);
					distances.add(distance);
				}else{
				}
			}
			personId2listOfDistancesPt.put(personId, distances);
		}
		return personId2listOfDistancesPt;
	}

//	public Map<Id,List<Double>> getPersonId2listOfAmounts() {
//		Map<Id,List<Double>> personId2listOfAmounts = new HashMap<Id, List<Double>>();
//		for(Id personId: personId2tripNumber2amount.keySet()){
//			List<Double> amounts = new ArrayList<Double>();
//			for(int i : personId2tripNumber2amount.get(personId).keySet()){
//				double amount = personId2tripNumber2amount.get(personId).get(i);
//				amounts.add(amount);
//			}
//			personId2listOfAmounts.put(personId, amounts);
//		}
//		return personId2listOfAmounts;
//	}
	
	public Map<Id,List<Double>> getPersonId2listOfAmountsCar() {
		Map<Id,List<Double>> personId2listOfAmountsCar = new HashMap<Id, List<Double>>();
		for(Id personId: personId2tripNumber2amount.keySet()){
			List<Double> amounts = new ArrayList<Double>();
			for(int i : personId2tripNumber2amount.get(personId).keySet()){
				if(personId2tripNumber2legMode.get(personId).get(i).toString().equals(TransportMode.car)){
					double amount = personId2tripNumber2amount.get(personId).get(i);
					amounts.add(amount);
				}else{
				}
			}
			personId2listOfAmountsCar.put(personId, amounts);
		}
		return personId2listOfAmountsCar;
	}
	
	public Map<Id,List<Double>> getPersonId2listOfAmountsPt() {
		Map<Id,List<Double>> personId2listOfAmountsPt = new HashMap<Id, List<Double>>();
		for(Id personId: personId2tripNumber2amount.keySet()){
			List<Double> amounts = new ArrayList<Double>();
			for(int i : personId2tripNumber2amount.get(personId).keySet()){
				if(personId2tripNumber2legMode.get(personId).get(i).toString().equals(TransportMode.pt)){
					double amount = personId2tripNumber2amount.get(personId).get(i);
					amounts.add(amount);
				}else{
				}
			}
			personId2listOfAmountsPt.put(personId, amounts);
		}
		return personId2listOfAmountsPt;
	}

//	public Map<Double, Double> getAvgAmountPerTripDepartureTime() {
//		Map<Double, Double> tripDepTime2avgFare = new HashMap<Double, Double>();
//
//		Map<Double, List<Double>> tripDepTime2fares = new HashMap<Double, List<Double>>();
//		double startTime = 4. * 3600;
//		double periodLength = 7200;
//		double endTime = 24. * 3600;
//		
//		for (double time = startTime; time <= endTime; time = time + periodLength){
//			List<Double> fares = new ArrayList<Double>();
//			tripDepTime2fares.put(time, fares);
//		}
//		
//		Map<Integer, double[]> counter2allDepartureTimesAndAmounts = new HashMap<Integer, double[]>();
//		int i = 0;
//		
//		for(Id personId : personId2tripNumber2departureTime.keySet()){
//			for(int tripNumber : personId2tripNumber2departureTime.get(personId).keySet()){
//				double departureTime = personId2tripNumber2departureTime.get(personId).get(tripNumber);
//				double belongingAmount = personId2tripNumber2amount.get(personId).get(tripNumber);
//				double[] departureTimeAndAmount = new double[2];
//				departureTimeAndAmount[0] = departureTime;
//				departureTimeAndAmount[1] = belongingAmount;				
//				counter2allDepartureTimesAndAmounts.put(i, departureTimeAndAmount);
//				i++;
//			}
//		}
//		
//		for (Double time : tripDepTime2fares.keySet()){
//			for (int counter : counter2allDepartureTimesAndAmounts.keySet()){
//				if (counter2allDepartureTimesAndAmounts.get(counter)[0] < time && counter2allDepartureTimesAndAmounts.get(counter)[0] >= (time - periodLength)) {
//					if (tripDepTime2fares.containsKey(time)){
//						tripDepTime2fares.get(time).add(counter2allDepartureTimesAndAmounts.get(counter)[1]);
//					}
//				}
//			}
//		}
//		
//		for (Double time : tripDepTime2fares.keySet()){
//			double amountSum = 0.;
//			double counter = 0.;
//			for (Double amount : tripDepTime2fares.get(time)){
//				if (amount == null){
//					
//				} else {
//					amountSum = amountSum + amount;
//					counter++;
//				}
//			}
//			
//			double avgFare = 0.;
//			if (counter!=0.){
//				avgFare = (-1) * amountSum / counter;
//			}
//			tripDepTime2avgFare.put(time, avgFare);
//		}
//		return tripDepTime2avgFare;
//	}
	
	public Map<Double, Double> getAvgAmountPerTripDepartureTimeCar() {
		Map<Double, Double> tripDepTime2avgFareCar = new HashMap<Double, Double>();

		Map<Double, List<Double>> tripDepTime2fares = new HashMap<Double, List<Double>>();
		double startTime = 4. * 3600;
		double periodLength = 7200;
		double endTime = 24. * 3600;
		
		for (double time = startTime; time <= endTime; time = time + periodLength){
			List<Double> fares = new ArrayList<Double>();
			tripDepTime2fares.put(time, fares);
		}
		
		Map<Integer, double[]> counter2allDepartureTimesAndAmounts = new HashMap<Integer, double[]>();
		int i = 0;
		
		for(Id personId : personId2tripNumber2departureTime.keySet()){
			for(int tripNumber : personId2tripNumber2departureTime.get(personId).keySet()){
				if(personId2tripNumber2legMode.get(personId).get(tripNumber).toString().equals(TransportMode.car)){
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
			tripDepTime2avgFareCar.put(time, avgFare);
		}
		return tripDepTime2avgFareCar;
	}
	
	public Map<Double, Double> getAvgAmountPerTripDepartureTimePt() {
		Map<Double, Double> tripDepTime2avgFarePt = new HashMap<Double, Double>();

		Map<Double, List<Double>> tripDepTime2fares = new HashMap<Double, List<Double>>();
		double startTime = 4. * 3600;
		double periodLength = 7200;
		double endTime = 24. * 3600;
		
		for (double time = startTime; time <= endTime; time = time + periodLength){
			List<Double> fares = new ArrayList<Double>();
			tripDepTime2fares.put(time, fares);
		}
		
		Map<Integer, double[]> counter2allDepartureTimesAndAmounts = new HashMap<Integer, double[]>();
		int i = 0;
		
		for(Id personId : personId2tripNumber2departureTime.keySet()){
			for(int tripNumber : personId2tripNumber2departureTime.get(personId).keySet()){
				if(personId2tripNumber2legMode.get(personId).get(tripNumber).toString().equals(TransportMode.pt)){
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
			tripDepTime2avgFarePt.put(time, avgFare);
		}
		return tripDepTime2avgFarePt;
	}

//	public Map<Double, Double> getAvgAmountPerTripDistance() {
//		Map<Double, Double> tripDistance2avgAmount = new HashMap<Double, Double>();
//
//		Map<Double, List<Double>> tripDistance2amount = new HashMap<Double, List<Double>>();
//		double startDistance = 500.;
//		double groupsize = 500.;
//		double endDistance = 40 * 500.;
//		
//		for (double distance = startDistance; distance <= endDistance; distance = distance + groupsize){
//			List<Double> amounts = new ArrayList<Double>();
//			tripDistance2amount.put(distance, amounts);
//		}
//		
//		Map<Integer, double[]> counter2allDistancesAndAmounts = new HashMap<Integer, double[]>();
//		int i = 0;
//		
//		for(Id personId : personId2tripNumber2tripDistance.keySet()){
//			for(int tripNumber : personId2tripNumber2tripDistance.get(personId).keySet()){
//				double tripDistance = personId2tripNumber2tripDistance.get(personId).get(tripNumber);
//				double belongingAmount = personId2tripNumber2amount.get(personId).get(tripNumber);
//				double[] tripDistanceAndAmount = new double[2];
//				tripDistanceAndAmount[0] = tripDistance;
//				tripDistanceAndAmount[1] = belongingAmount;				
//				counter2allDistancesAndAmounts.put(i, tripDistanceAndAmount);
//				i++;
//			}
//		}
//		
//		for (Double dist : tripDistance2amount.keySet()){
//			for (int counter : counter2allDistancesAndAmounts.keySet()){
//				if (counter2allDistancesAndAmounts.get(counter)[0] < dist && counter2allDistancesAndAmounts.get(counter)[0] >= (dist - groupsize)) {
//					if (tripDistance2amount.containsKey(dist)){
//						tripDistance2amount.get(dist).add(counter2allDistancesAndAmounts.get(counter)[1]);
//					}
//				}
//			}
//		}
//		
//		for (Double dist : tripDistance2amount.keySet()){
//			double amountSum = 0.;
//			double counter = 0.;
//			for (Double amount : tripDistance2amount.get(dist)){
//				if (amount == null){
//					
//				} else {
//					amountSum = amountSum + amount;
//					counter++;
//				}
//			}
//			
//			double avgAmount = 0.;
//			if (counter!=0.){
//				avgAmount = (-1) * amountSum / counter;
//			}
//			tripDistance2avgAmount.put(dist, avgAmount);
//		}
//		return tripDistance2avgAmount;
//	}
	
	public Map<Double, Double> getAvgAmountPerTripDistanceCar() {
		Map<Double, Double> tripDistance2avgAmountCar = new HashMap<Double, Double>();

		Map<Double, List<Double>> tripDistance2amount = new HashMap<Double, List<Double>>();
		double startDistance = 500.;
		double groupsize = 500.;
		double endDistance = 40 * 500.;
		
		for (double distance = startDistance; distance <= endDistance; distance = distance + groupsize){
			List<Double> amounts = new ArrayList<Double>();
			tripDistance2amount.put(distance, amounts);
		}
		
		Map<Integer, double[]> counter2allDistancesAndAmounts = new HashMap<Integer, double[]>();
		int i = 0;
		
		for(Id personId : personId2tripNumber2tripDistance.keySet()){
			for(int tripNumber : personId2tripNumber2tripDistance.get(personId).keySet()){
				if(personId2tripNumber2legMode.get(personId).get(tripNumber).toString().equals(TransportMode.car)){
					double tripDistance = personId2tripNumber2tripDistance.get(personId).get(tripNumber);
					double belongingAmount = personId2tripNumber2amount.get(personId).get(tripNumber);
					double[] tripDistanceAndAmount = new double[2];
					tripDistanceAndAmount[0] = tripDistance;
					tripDistanceAndAmount[1] = belongingAmount;				
					counter2allDistancesAndAmounts.put(i, tripDistanceAndAmount);
					i++;
				}else{	
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
			tripDistance2avgAmountCar.put(dist, avgAmount);
		}
		return tripDistance2avgAmountCar;
	}
	
	public Map<Double, Double> getAvgAmountPerTripDistancePt() {
		Map<Double, Double> tripDistance2avgAmountPt = new HashMap<Double, Double>();

		Map<Double, List<Double>> tripDistance2amount = new HashMap<Double, List<Double>>();
		double startDistance = 500.;
		double groupsize = 500.;
		double endDistance = 40 * 500.;
		
		for (double distance = startDistance; distance <= endDistance; distance = distance + groupsize){
			List<Double> amounts = new ArrayList<Double>();
			tripDistance2amount.put(distance, amounts);
		}
		
		Map<Integer, double[]> counter2allDistancesAndAmounts = new HashMap<Integer, double[]>();
		int i = 0;
		
		for(Id personId : personId2tripNumber2tripDistance.keySet()){
			for(int tripNumber : personId2tripNumber2tripDistance.get(personId).keySet()){
				if(personId2tripNumber2legMode.get(personId).get(tripNumber).toString().equals(TransportMode.pt)){
					double tripDistance = personId2tripNumber2tripDistance.get(personId).get(tripNumber);
					double belongingAmount = personId2tripNumber2amount.get(personId).get(tripNumber);
					double[] tripDistanceAndAmount = new double[2];
					tripDistanceAndAmount[0] = tripDistance;
					tripDistanceAndAmount[1] = belongingAmount;				
					counter2allDistancesAndAmounts.put(i, tripDistanceAndAmount);
					i++;
				}else{
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
			tripDistance2avgAmountPt.put(dist, avgAmount);
		}
		return tripDistance2avgAmountPt;
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if(ptDrivers.contains(event.getPersonId())){
			// ptDrivers are not considered
		}else{
			if(event.getLegMode().toString().equals("transit_walk")){
				// pt_interactions are not considered
			}else{
//				if (event.getLegMode().equals(TransportMode.car)){
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
					}else{
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
//				}
			}
		}
	}
	
	@Override
	public void handleEvent(PersonArrivalEvent event) {
//		if(event.getLegMode().equals(TransportMode.pt)){
//			
//		}else{
//			// calculation of the trip length has been done by adding the length of the links
//		}
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		if(ptDrivers.contains(event.getDriverId())){
			// already listed
		}else{
			ptDrivers.add(event.getDriverId());
		}
	}

	
}
