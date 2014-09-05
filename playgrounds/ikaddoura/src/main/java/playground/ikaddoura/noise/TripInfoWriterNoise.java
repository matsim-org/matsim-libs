/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.ikaddoura.noise;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;

/**
 * @author ikaddoura , lkroeger
 *
 */
public class TripInfoWriterNoise {
	private static final Logger log = Logger.getLogger(TripInfoWriterNoise.class);

	ExtCostEventHandlerNoise handler;
	String outputFolder;
	
	public TripInfoWriterNoise(ExtCostEventHandlerNoise handler, String outputFolder) {
		this.handler = handler;
		this.outputFolder = outputFolder;
		
		String fileName = outputFolder;
		File file = new File(fileName);
		file.mkdirs();
	}

	public void writeDetailedResults(String mode) {
		
		String fileName = this.outputFolder + "/monetary_amounts_trip_infos_" + mode + ".csv";
		File file = new File(fileName);
			
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write(fileName);
			bw.newLine();
			bw.write("____________________________________________________________________________");
			bw.newLine();

			bw.write("amount per trip;departure time [sec];person Id;distance [m]");
			bw.newLine();
			
			Map<Id,List<Double>> personId2listOfAmounts = this.handler.getPersonId2listOfAmounts(mode);
//			log.info("111: "+personId2listOfAmounts);
			Map<Id,List<Double>> personId2listOfDepartureTimes = this.handler.getPersonId2listOfDepartureTimes(mode);
//			log.info("222: "+personId2listOfDepartureTimes);
			Map<Id,List<Double>> personId2listOfDistances = this.handler.getPersonId2listOfDistances(mode);
//			log.info("333: "+personId2listOfDistances);
			
			for (Id id : personId2listOfAmounts.keySet()) {
				List<Double> fares = personId2listOfAmounts.get(id);
//				log.info(fares);
				List<Double> departureTimes = personId2listOfDepartureTimes.get(id);
				List<Double> distances = personId2listOfDistances.get(id);
				for(int i = 0 ; i < fares.size() ; i++){
					double fare = fares.get(i);
					double departureTime = departureTimes.get(i);
					double distance = distances.get(i);
					bw.write(fare + ";" + departureTime + ";" + id + ";" + distance);
					bw.newLine();
				}
			}
			
			log.info("Output written to " + fileName);
			bw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeAvgTollPerTimeBin(String mode) {
		String fileName = this.outputFolder + "/avg_amount_per_trip_departure_time_" + mode + ".csv";
		File file = new File(fileName);
		Map<Double, Double> departureTime2avgAmount = this.handler.getAvgAmountPerTripDepartureTime(mode);

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write(fileName);
			bw.newLine();
			bw.write("____________________________________________________________________________");
			bw.newLine();

			bw.write("trip departure time;average amount");
			bw.newLine();

			for (Double x : departureTime2avgAmount.keySet()) {
				
				bw.write(x + ";" + departureTime2avgAmount.get(x));
				bw.newLine();
			}
			
			log.info("Output written to " + fileName);
			bw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
	
	public void writeAvgTollPerDistance(String mode) {
		String fileName = this.outputFolder + "/avg_amount_per_trip_distance_" + mode + ".csv";
		File file = new File(fileName);
		Map<Double, Double> tripDistance2avgAmount = this.handler.getAvgAmountPerTripDistance(mode);

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write(fileName);
			bw.newLine();
			bw.write("____________________________________________________________________________");
			bw.newLine();

			bw.write("trip distance;average amount");
			bw.newLine();

			for (Double x : tripDistance2avgAmount.keySet()) {
				
				bw.write(x + ";" + tripDistance2avgAmount.get(x));
				bw.newLine();
			}
			
			log.info("Output written to " + fileName);
			bw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
	
	public void writePersonId2totalAmount() {
		
		String fileName = this.outputFolder + "/personId2totalAmount.csv";
		File file = new File(fileName);
			
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write(fileName);
			bw.newLine();
			bw.write("____________________________________________________________________________");
			bw.newLine();

			bw.write("person Id;total amount [monetary units]");
			bw.newLine();
			
			Map<Id,List<Double>> personId2listOfAmounts = this.handler.getPersonId2listOfAmounts(TransportMode.car);
			Map<Id,Double> personId2totalAmount = this.handler.getPersonId2amountSumAllAgents();

			for (Id id : personId2totalAmount.keySet()) {
				double totalAmount = personId2totalAmount.get(id);
				
				// to check if person-based analysis is consistent with trip-based analysis
				List<Double> fares = personId2listOfAmounts.get(id);
				double amountSumFromList = 0.;
				for(Double amount : fares){
					amountSumFromList = amountSumFromList + amount;
				}
				
				if (Math.abs(amountSumFromList - totalAmount) >= 0.001) {
					log.warn("Inconsistent data: Total amount from trip-based analysis: " + amountSumFromList + " // total amount from person-based analysis: " + totalAmount);
				}
				
				bw.write(id + ";" + totalAmount);
				bw.newLine();
			}
			
			log.info("Output written to " + fileName);
			bw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writePersonId2totalAmountAffected() {
		
		String fileName = this.outputFolder + "/personId2totalAmountAffected.csv";
		File file = new File(fileName);
			
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write(fileName);
			bw.newLine();
			bw.write("____________________________________________________________________________");
			bw.newLine();

			bw.write("person Id;total amount [monetary units]");
			bw.newLine();
			
			Map<Id,Double> personId2totalAmountAffected = this.handler.getPersonId2amountSumAffectedAllAgents();

			for (Id id : personId2totalAmountAffected.keySet()) {
				double totalAmount = personId2totalAmountAffected.get(id);
				
				bw.write(id + ";" + totalAmount);
				bw.newLine();
			}
			
			log.info("Output written to " + fileName);
			bw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
