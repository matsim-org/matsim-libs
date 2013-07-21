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
package playground.ikaddoura.analysis.extCost;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import org.matsim.api.core.v01.Id;

/**
 * @author ikaddoura
 *
 */
public class PtTripInfoWriter {
	
	ExtCostEventHandler handler = new ExtCostEventHandler();
	String outputFolder;
	
	public PtTripInfoWriter(ExtCostEventHandler handler, String outputFolder) {
		this.handler = handler;
		this.outputFolder = outputFolder;
		
		String fileName = outputFolder;
		File file = new File(fileName);
		file.mkdirs();
	}

	public void writeResults1() {
		
		String fileName = this.outputFolder + "/ptTripInfos.csv";
		File file = new File(fileName);
			
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write(fileName);
			bw.newLine();
			bw.write("____________________________________________________________________________");
			bw.newLine();

			// output: extCostPerTrip ; departureTime ; distance ; in-vehicle-time ; waiting time ; boardingLink ; alightingLink ; personId ; ( nrOfAgentsWaitingWhenDeparting ; nrOfAgentsInVehiclesWhenDeparting )
			bw.write("farePerTrip;departureTime_sec;inVehicleTime_sec;waitingTime_sec;boardingLink;alightingLink;personId;distance");
			bw.newLine();
			
			// first trip
			for (Id id : this.handler.getPersonId2fareFirstTrip().keySet()) {
				
				double fare = this.handler.getPersonId2fareFirstTrip().get(id);
				double departureTime = this.handler.getPersonId2firstTripDepartureTime().get(id);
				double inVehTime = this.handler.getPersonId2inVehTimeFirstTrip().get(id);
				double waitingTime = this.handler.getPersonId2waitingTimeFirstTrip().get(id);
				Id boardingLink = this.handler.getPersonId2BoardingLinkFirstTrip().get(id);
				Id alightingLink = this.handler.getPersonId2AlightingLinkFirstTrip().get(id);
				double distance = this.handler.getPersonId2distanceFirstTrip().get(id);
				
				bw.write(fare + ";" + departureTime + ";" + inVehTime + ";" + waitingTime + ";" + boardingLink + ";" + alightingLink + ";" + id + ";" + distance);
				bw.newLine();
			}
			
			// second trip
			for (Id id : this.handler.getPersonId2fareSecondTrip().keySet()) {
				
				double fare = this.handler.getPersonId2fareSecondTrip().get(id);
				double departureTime = this.handler.getPersonId2secondTripDepartureTime().get(id);
				double inVehTime = this.handler.getPersonId2inVehTimeSecondTrip().get(id);
				double waitingTime = this.handler.getPersonId2waitingTimeSecondTrip().get(id);
				Id boardingLink = this.handler.getPersonId2BoardingLinkSecondTrip().get(id);
				Id alightingLink = this.handler.getPersonId2AlightingLinkSecondTrip().get(id);
				double distance = this.handler.getPersonId2distanceSecondTrip().get(id);
				
				bw.write(fare + ";" + departureTime + ";" + inVehTime + ";" + waitingTime + ";" + boardingLink + ";" + alightingLink + ";" + id + ";" + distance);
				bw.newLine();
			}
			
			System.out.println("Output written to " + fileName);
			bw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeAvgFares1() {
		String fileName = this.outputFolder + "/avgFarePerTripInVehTime.csv";
		File file = new File(fileName);
		Map<Double, Double> x2avgFare = this.handler.getAvgFarePerInVehTime();

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write(fileName);
			bw.newLine();
			bw.write("____________________________________________________________________________");
			bw.newLine();

			bw.write("tripInVehTime;avgFare");
			bw.newLine();

			for (Double x : x2avgFare.keySet()) {
				
				bw.write(x + ";" + x2avgFare.get(x));
				bw.newLine();
			}
			
			System.out.println("Output written to " + fileName);
			bw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
	
	public void writeAvgFares2() {
		String fileName = this.outputFolder + "/avgFarePerTripDepartureTime.csv";
		File file = new File(fileName);
		Map<Double, Double> x2avgFare = this.handler.getAvgFarePerTripDepartureTime();

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write(fileName);
			bw.newLine();
			bw.write("____________________________________________________________________________");
			bw.newLine();

			bw.write("tripDepartureTime;avgFare");
			bw.newLine();

			for (Double x : x2avgFare.keySet()) {
				
				bw.write(x + ";" + x2avgFare.get(x));
				bw.newLine();
			}
			
			System.out.println("Output written to " + fileName);
			bw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
	
	public void writeAvgFares3() {
		String fileName = this.outputFolder + "/avgFarePerTripDistance.csv";
		File file = new File(fileName);
		Map<Double, Double> x2avgFare = this.handler.getAvgFarePerTripDistance();

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write(fileName);
			bw.newLine();
			bw.write("____________________________________________________________________________");
			bw.newLine();

			bw.write("tripDistance;avgFare");
			bw.newLine();

			for (Double x : x2avgFare.keySet()) {
				
				bw.write(x + ";" + x2avgFare.get(x));
				bw.newLine();
			}
			
			System.out.println("Output written to " + fileName);
			bw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
	
	public void writeAvgFares4() {
		String fileName = this.outputFolder + "/avgFarePerTripWaitingTime.csv";
		File file = new File(fileName);
		Map<Double, Double> x2avgFare = this.handler.getAvgFarePerWaitingTime();

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write(fileName);
			bw.newLine();
			bw.write("____________________________________________________________________________");
			bw.newLine();

			bw.write("tripWaitingTime;avgFare");
			bw.newLine();

			for (Double x : x2avgFare.keySet()) {
				
				bw.write(x + ";" + x2avgFare.get(x));
				bw.newLine();
			}
			
			System.out.println("Output written to " + fileName);
			bw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
	
	public void writeAvgFares5() {
		String fileName = this.outputFolder + "/avgFarePerBoardingLink.csv";
		File file = new File(fileName);
		Map<Id, Double> x2avgFare = this.handler.getAvgFarePerBoardingLinkId();

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write(fileName);
			bw.newLine();
			bw.write("____________________________________________________________________________");
			bw.newLine();

			bw.write("boardingLink;avgFare");
			bw.newLine();

			for (Id x : x2avgFare.keySet()) {
				
				bw.write(x + ";" + x2avgFare.get(x));
				bw.newLine();
			}
			
			System.out.println("Output written to " + fileName);
			bw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}

}
