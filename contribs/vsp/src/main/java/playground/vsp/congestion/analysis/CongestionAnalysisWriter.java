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
package playground.vsp.congestion.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

/**
 * @author ikaddoura , lkroeger
 *
 */
public class CongestionAnalysisWriter {
	private static final Logger log = Logger.getLogger(CongestionAnalysisWriter.class);

	CongestionAnalysisEventHandler handler;
	String outputFolder;
	
	public CongestionAnalysisWriter(CongestionAnalysisEventHandler handler, String outputFolder) {
		this.handler = handler;
		String directory = outputFolder + (outputFolder.endsWith("/") ? "" : "/");
		this.outputFolder = directory;
		
		String fileName = outputFolder;
		File file = new File(fileName);
		file.mkdirs();
	}

	public void writeDetailedResults(String mode) {
		
		String fileName = this.outputFolder + "monetary_amounts_trip_infos_" + mode + ".csv";
		File file = new File(fileName);
			
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write(fileName);
			bw.newLine();
			bw.write("____________________________________________________________________________");
			bw.newLine();

			bw.write("amount per trip;departure time [sec];person Id;distance [m]");
			bw.newLine();
			
			Map<Id<Person>,List<Double>> personId2listOfAmounts = this.handler.getPersonId2listOfAmounts(mode);
			Map<Id<Person>,List<Double>> personId2listOfDepartureTimes = this.handler.getPersonId2listOfDepartureTimes(mode);
			Map<Id<Person>,List<Double>> personId2listOfDistances = this.handler.getPersonId2listOfDistances(mode);
			
			for (Id<Person> id : personId2listOfAmounts.keySet()) {
				List<Double> fares = personId2listOfAmounts.get(id);
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
		String fileName = this.outputFolder + "avg_amount_per_trip_departure_time_" + mode + ".csv";
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
		String fileName = this.outputFolder + "avg_amount_per_trip_distance_" + mode + ".csv";
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
	
	public void writeCausingAgentId2totalAmount() {
		
		String fileName = this.outputFolder + "causingAgentId2totalAmount.csv";
		File file = new File(fileName);
			
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write(fileName);
			bw.newLine();
			bw.write("____________________________________________________________________________");
			bw.newLine();

			bw.write("person Id;total amount [monetary units]");
			bw.newLine();
			
			Map<Id<Person>,Double> personId2totalAmount = this.handler.getCausingAgentId2amountSumAllAgents();

			for (Id<Person> id : personId2totalAmount.keySet()) {
				double totalAmount = personId2totalAmount.get(id);
				
				bw.write(id + ";" + totalAmount);
				bw.newLine();
			}
			
			log.info("Output written to " + fileName);
			bw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeAffectedAgentId2totalAmount() {
			
			String fileName = this.outputFolder + "affectedAgentId2totalAmount.csv";
			File file = new File(fileName);
				
			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter(file));
				bw.write(fileName);
				bw.newLine();
				bw.write("____________________________________________________________________________");
				bw.newLine();
	
				bw.write("person Id;total amount [monetary units]");
				bw.newLine();
				
				Map<Id<Person>,Double> personId2totalAmount = this.handler.getAffectedAgentId2amountSumAllAgents();
	
				for (Id<Person> id : personId2totalAmount.keySet()) {
					double totalAmount = personId2totalAmount.get(id);
					
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
