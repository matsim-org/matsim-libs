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
package playground.ikaddoura.analysis.monetaryAmountsTripAnalysis_old;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

/**
 * @author ikaddoura
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

	public void writeResults1() {
		
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
			
			// first trip
			for (Id id : this.handler.getPersonId2amountFirstTrip().keySet()) {
				double fare = this.handler.getPersonId2amountFirstTrip().get(id);
				double departureTime = this.handler.getPersonId2firstTripDepartureTime().get(id);
				double distance = this.handler.getPersonId2distanceFirstTrip().get(id);
				
				bw.write(fare + ";" + departureTime + ";" + id + ";" + distance);
				bw.newLine();
			}
			
			// second trip
			for (Id id : this.handler.getPersonId2amountSecondTrip().keySet()) {
				
				double amount = this.handler.getPersonId2amountSecondTrip().get(id);
				double departureTime = this.handler.getPersonId2secondTripDepartureTime().get(id);
				double distance = this.handler.getPersonId2distanceSecondTrip().get(id);
				
				bw.write(amount + ";" + departureTime + ";" + id + ";" + distance);
				bw.newLine();
			}
			
			log.info("Output written to " + fileName);
			bw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeAvgAmounts() {
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
	
	public void writeAvgFares3() {
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
