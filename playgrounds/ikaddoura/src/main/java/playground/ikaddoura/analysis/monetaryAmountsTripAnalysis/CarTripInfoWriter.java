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
package playground.ikaddoura.analysis.monetaryAmountsTripAnalysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

/**
 * @author ikaddoura , lkroeger
 *
 */
public class CarTripInfoWriter {
	private static final Logger log = Logger.getLogger(CarTripInfoWriter.class);

	ExtCostEventHandler handler;
	String outputFolder;
	
	public CarTripInfoWriter(ExtCostEventHandler handler, String outputFolder) {
		this.handler = handler;
		this.outputFolder = outputFolder;
		
		String fileName = outputFolder;
		File file = new File(fileName);
		file.mkdirs();
	}

	public void writeDetailedResults() {
		
		String fileName = this.outputFolder + "/monetary_amounts_trip_infos.csv";
		File file = new File(fileName);
			
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write(fileName);
			bw.newLine();
			bw.write("____________________________________________________________________________");
			bw.newLine();

			bw.write("amount per trip;departure time [sec];person Id;distance [m]");
			bw.newLine();
			
			Map<Id,List<Double>> personId2listOfAmounts = this.handler.getPersonId2listOfAmounts();
			Map<Id,List<Double>> personId2listOfDepartureTimes = this.handler.getPersonId2listOfDepartureTimes();
			Map<Id,List<Double>> personId2listOfDistances = this.handler.getPersonId2listOfDistances();
			Map<Id,Integer> personId2numberOfTrips = this.handler.getPersonId2NumberOfTrips();
			
			for (Id id : personId2listOfAmounts.keySet()) {
				List<Double> fares = personId2listOfAmounts.get(id);
				List<Double> departureTimes = personId2listOfDepartureTimes.get(id);
				List<Double> distances = personId2listOfDistances.get(id);
				int numberOfTrips = personId2numberOfTrips.get(id);
				for(int i = 0 ; i < numberOfTrips ; i++){
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
	
	public void writeResultsTime() {
		String fileName = this.outputFolder + "/avg_amount_per_trip_departure_time.csv";
		File file = new File(fileName);
		Map<Double, Double> x2avgAmount = this.handler.getAvgAmountPerTripDepartureTime();

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write(fileName);
			bw.newLine();
			bw.write("____________________________________________________________________________");
			bw.newLine();

			bw.write("trip departure time;average amount");
			bw.newLine();

			for (Double x : x2avgAmount.keySet()) {
				
				bw.write(x + ";" + x2avgAmount.get(x));
				bw.newLine();
			}
			
			log.info("Output written to " + fileName);
			bw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
	
	public void writeResultsDistance() {
		String fileName = this.outputFolder + "/avg_amount_per_trip_distance.csv";
		File file = new File(fileName);
		Map<Double, Double> x2avgAmount = this.handler.getAvgAmountPerTripDistance();

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write(fileName);
			bw.newLine();
			bw.write("____________________________________________________________________________");
			bw.newLine();

			bw.write("trip distance;average amount");
			bw.newLine();

			for (Double x : x2avgAmount.keySet()) {
				
				bw.write(x + ";" + x2avgAmount.get(x));
				bw.newLine();
			}
			
			log.info("Output written to " + fileName);
			bw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
	

}
