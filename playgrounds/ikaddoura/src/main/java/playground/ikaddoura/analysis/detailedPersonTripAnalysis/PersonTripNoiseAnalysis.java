/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.ikaddoura.analysis.detailedPersonTripAnalysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import playground.ikaddoura.analysis.detailedPersonTripAnalysis.handler.BasicPersonTripAnalysisHandler;
import playground.ikaddoura.analysis.detailedPersonTripAnalysis.handler.NoiseAnalysisHandler;

/**
 * @author ikaddoura
 *
 */
public class PersonTripNoiseAnalysis {
	private static final Logger log = Logger.getLogger(PersonTripNoiseAnalysis.class);
	
	public void printPersonInformation(String outputPath,
			String mode,
			Map<Id<Person>, Double> personId2userBenefit,
			BasicPersonTripAnalysisHandler basicHandler,
			NoiseAnalysisHandler noiseHandler) {
		
		boolean ignoreModes = false;
		if (mode == null) {
			mode = "all_transport_modes";
			ignoreModes = true;
		}

		String fileName = outputPath + "person_info_" + mode + ".csv";
		File file = new File(fileName);			

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));

			bw.write( "person Id;"
					+ "number of " + mode + " trips;"
					+ "at least one stuck and abort " + mode + " trip (yes/no);"
					+ mode + " total travel time (day) [sec];"
					+ mode + " total in-vehicle time (day) [sec];"
					+ mode + " total waiting time (for taxi/pt) (day) [sec];"
					+ mode + " total travel distance (day) [m];"
					
					+ "travel related user benefits (based on the selected plans score) [monetary units];"
					+ "total toll payments (day) [monetary units];"
					+ "caused noise cost (day) [monetary units];"
					+ "affected noise cost (day) [monetary units]"					
					);
			bw.newLine();
			
			for (Id<Person> id : basicHandler.getScenario().getPopulation().getPersons().keySet()) {
				
				double userBenefit = Double.NEGATIVE_INFINITY;
				if (personId2userBenefit.containsKey(id)) {
					userBenefit = personId2userBenefit.get(id);
				}
				int mode_trips = 0;
				String mode_stuckAbort = "no";
				double mode_travelTime = 0.;
				double mode_inVehTime = 0.;
				double mode_waitingTime = 0.;
				double mode_travelDistance = 0.;
				
				double tollPayments = 0.;
				double causedNoiseCost = 0.;
				double affectedNoiseCost = 0.;
				
				if (noiseHandler.getPersonId2affectedNoiseCost().containsKey(id)) {
					affectedNoiseCost = affectedNoiseCost + noiseHandler.getPersonId2affectedNoiseCost().get(id);
				}
				
				if (basicHandler.getPersonId2tripNumber2legMode().containsKey(id)) {
					for (Integer trip : basicHandler.getPersonId2tripNumber2legMode().get(id).keySet()) {
						
						if (basicHandler.getPersonId2tripNumber2payment().containsKey(id) && basicHandler.getPersonId2tripNumber2payment().get(id).containsKey(trip)) {
							tollPayments = tollPayments + basicHandler.getPersonId2tripNumber2payment().get(id).get(trip);
						}
						
						if (noiseHandler.getPersonId2tripNumber2causedNoiseCost().containsKey(id) && noiseHandler.getPersonId2tripNumber2causedNoiseCost().get(id).containsKey(trip)) {
							causedNoiseCost = causedNoiseCost + noiseHandler.getPersonId2tripNumber2causedNoiseCost().get(id).get(trip);
						}
						
						if (ignoreModes || basicHandler.getPersonId2tripNumber2legMode().get(id).get(trip).equals(mode)) {
							
							mode_trips++;
							
							if (basicHandler.getPersonId2tripNumber2stuckAbort().containsKey(id) && basicHandler.getPersonId2tripNumber2stuckAbort().get(id).containsKey(trip)) {
								if (basicHandler.getPersonId2tripNumber2stuckAbort().get(id).get(trip)) {
									mode_stuckAbort = "yes";
								}
							}
							
							if (basicHandler.getPersonId2tripNumber2travelTime().containsKey(id) && basicHandler.getPersonId2tripNumber2travelTime().get(id).containsKey(trip)) {
								mode_travelTime = mode_travelTime + basicHandler.getPersonId2tripNumber2travelTime().get(id).get(trip);
							}
							
							if (basicHandler.getPersonId2tripNumber2inVehicleTime().containsKey(id) && basicHandler.getPersonId2tripNumber2inVehicleTime().get(id).containsKey(trip)) {
								mode_inVehTime = mode_inVehTime + basicHandler.getPersonId2tripNumber2inVehicleTime().get(id).get(trip);
							}
							
							if (basicHandler.getPersonId2tripNumber2waitingTime().containsKey(id) && basicHandler.getPersonId2tripNumber2waitingTime().get(id).containsKey(trip)) {
								mode_waitingTime = mode_waitingTime + basicHandler.getPersonId2tripNumber2waitingTime().get(id).get(trip);
							}
							
							if (basicHandler.getPersonId2tripNumber2tripDistance().containsKey(id) && basicHandler.getPersonId2tripNumber2tripDistance().get(id).containsKey(trip)) {
								mode_travelDistance = mode_travelDistance + basicHandler.getPersonId2tripNumber2tripDistance().get(id).get(trip);
							}			
						}
					}
				}
				
				bw.write(id + ";"
						+ mode_trips + ";"
						+ mode_stuckAbort + ";"
						+ mode_travelTime + ";"
						+ mode_inVehTime + ";"
						+ mode_waitingTime + ";"
						+ mode_travelDistance + ";"
						
						+ userBenefit + ";"
						+ tollPayments + ";"
						+ causedNoiseCost + ";"
						+ affectedNoiseCost
						);
				
						bw.newLine();		
			}
			
			log.info("Output written to " + fileName);
			bw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public void printTripInformation(String outputPath,
			String mode,
			BasicPersonTripAnalysisHandler basicHandler,
			NoiseAnalysisHandler noiseHandler) {
		
		boolean ignoreModes = false;
		if (mode == null) {
			mode = "all_transport_modes";
			ignoreModes = true;
		}
				
		String fileName = outputPath + "trip_info_" + mode + ".csv";
		File file = new File(fileName);			

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			
			bw.write( "person Id;"
					+ "trip no.;"
					+ "mode;"
					+ "stuck and abort trip (yes/no);"
					+ "departure time (trip) [sec];"
					+ "enter vehicle time (trip) [sec];"
					+ "leave vehicle time (trip) [sec];"
					+ "arrival time (trip) [sec];"
					+ "travel time (trip) [sec];"
					+ "in-vehicle time (trip) [sec];"
					+ "waiting time (for taxi/pt) (trip) [sec];"
					+ "travel distance (trip) [m];"
					+ "toll payments (trip) [monetary units];"
					+ "approximate caused noise cost (trip) [monetary units]"); // TODO make this accurate?!
			
			bw.newLine();
			
			for (Id<Person> id : basicHandler.getPersonId2tripNumber2legMode().keySet()) {
				
				for (Integer trip : basicHandler.getPersonId2tripNumber2legMode().get(id).keySet()) {
										
					if (ignoreModes || basicHandler.getPersonId2tripNumber2legMode().get(id).get(trip).equals(mode)) {
						
						String transportModeThisTrip = basicHandler.getPersonId2tripNumber2legMode().get(id).get(trip);
						
						String stuckAbort = "no";
						if (basicHandler.getPersonId2tripNumber2stuckAbort().containsKey(id) && basicHandler.getPersonId2tripNumber2stuckAbort().get(id).containsKey(trip)) {
							if (basicHandler.getPersonId2tripNumber2stuckAbort().get(id).get(trip)) {
								stuckAbort = "yes";
							}
						}
						
						String departureTime = "unknown";
						if (basicHandler.getPersonId2tripNumber2departureTime().containsKey(id) && basicHandler.getPersonId2tripNumber2departureTime().get(id).containsKey(trip)) {
							departureTime = String.valueOf(basicHandler.getPersonId2tripNumber2departureTime().get(id).get(trip));
						}
						
						String enterVehicleTime = "unknown";
						if (basicHandler.getPersonId2tripNumber2enterVehicleTime().containsKey(id) && basicHandler.getPersonId2tripNumber2enterVehicleTime().get(id).containsKey(trip)) {
							enterVehicleTime = String.valueOf(basicHandler.getPersonId2tripNumber2enterVehicleTime().get(id).get(trip));
						}
						
						String leaveVehicleTime = "unknown";
						if (basicHandler.getPersonId2tripNumber2leaveVehicleTime().containsKey(id) && basicHandler.getPersonId2tripNumber2leaveVehicleTime().get(id).containsKey(trip)) {
							leaveVehicleTime = String.valueOf(basicHandler.getPersonId2tripNumber2leaveVehicleTime().get(id).get(trip));
						}
						
						String arrivalTime = "unknown";
						if (basicHandler.getPersonId2tripNumber2arrivalTime().containsKey(id) && basicHandler.getPersonId2tripNumber2arrivalTime().get(id).containsKey(trip)){
							arrivalTime = String.valueOf(basicHandler.getPersonId2tripNumber2arrivalTime().get(id).get(trip));
						}
						
						String travelTime = "unknown";
						if (basicHandler.getPersonId2tripNumber2travelTime().containsKey(id) && basicHandler.getPersonId2tripNumber2travelTime().get(id).containsKey(trip)) {
							travelTime = String.valueOf(basicHandler.getPersonId2tripNumber2travelTime().get(id).get(trip));
						}
						
						String inVehTime = "unknown";
						if (basicHandler.getPersonId2tripNumber2inVehicleTime().containsKey(id) && basicHandler.getPersonId2tripNumber2inVehicleTime().get(id).containsKey(trip)) {
							inVehTime = String.valueOf(basicHandler.getPersonId2tripNumber2inVehicleTime().get(id).get(trip));
						}
						
						String waitingTime = "unknown";
						if (basicHandler.getPersonId2tripNumber2waitingTime().containsKey(id) && basicHandler.getPersonId2tripNumber2waitingTime().get(id).containsKey(trip)) {
							waitingTime = String.valueOf(basicHandler.getPersonId2tripNumber2waitingTime().get(id).get(trip));
						}
						
						String travelDistance = "unknown";
						if (basicHandler.getPersonId2tripNumber2tripDistance().containsKey(id) && basicHandler.getPersonId2tripNumber2tripDistance().get(id).containsKey(trip)) {
							travelDistance = String.valueOf(basicHandler.getPersonId2tripNumber2tripDistance().get(id).get(trip));
						}
						
						String tollPayment = "unknown";
						if (basicHandler.getPersonId2tripNumber2payment().containsKey(id) && basicHandler.getPersonId2tripNumber2payment().get(id).containsKey(trip)) {
							tollPayment = String.valueOf(basicHandler.getPersonId2tripNumber2payment().get(id).get(trip));
						}
						
						String causedNoiseCost = "unknown";
						if (noiseHandler.getPersonId2tripNumber2causedNoiseCost().containsKey(id) && noiseHandler.getPersonId2tripNumber2causedNoiseCost().get(id).containsKey(trip)) {
							causedNoiseCost = String.valueOf(noiseHandler.getPersonId2tripNumber2causedNoiseCost().get(id).get(trip));
						}
						
						bw.write(id + ";"
						+ trip + ";"
						+ transportModeThisTrip + ";"
						+ stuckAbort + ";"
						+ departureTime + ";"
						+ enterVehicleTime + ";"
						+ leaveVehicleTime + ";"
						+ arrivalTime + ";"
						+ travelTime + ";"
						+ inVehTime + ";"
						+ waitingTime + ";"
						+ travelDistance + ";"
						+ tollPayment + ";"
						+ causedNoiseCost
						);
						bw.newLine();						
					}
				}
			}
			
			log.info("Output written to " + fileName);
			bw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void printAggregatedResults(String outputPath,
			String mode,
			Map<Id<Person>, Double> personId2userBenefit,
			BasicPersonTripAnalysisHandler basicHandler,
			NoiseAnalysisHandler noiseHandler) {
		
		boolean ignoreModes = false;
		if (mode == null) {
			mode = "all_transport_modes";
			ignoreModes = true;
		}
	
		String fileName = outputPath + "aggregated_info_" + mode + ".csv";
		File file = new File(fileName);			

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			
			double userBenefits = 0.;
			for (Double userBenefit: personId2userBenefit.values()) {
				userBenefits = userBenefits + userBenefit;
			}
			
			int mode_trips = 0;
			int mode_StuckAndAbortTrips = 0;
			double mode_TravelTime = 0.;
			double mode_inVehTime = 0.;
			double mode_waitingTime = 0.;
			double mode_TravelDistance = 0.;
			
			int allTrips = 0;
			int allStuckAndAbortTrips = 0;
			double affectedNoiseCost = 0.;
			double tollPayments = 0.;
			double causedNoiseCost = 0.;
			
			for (Id<Person> id : basicHandler.getScenario().getPopulation().getPersons().keySet()) {
				
				if (noiseHandler.getPersonId2affectedNoiseCost().containsKey(id)) {
					affectedNoiseCost = affectedNoiseCost + noiseHandler.getPersonId2affectedNoiseCost().get(id);
				}
				
				if (basicHandler.getPersonId2tripNumber2legMode().containsKey(id)) {
					
					for (Integer trip : basicHandler.getPersonId2tripNumber2legMode().get(id).keySet()) {
						
						// for all modes
						
						allTrips++;
						
						if (basicHandler.getPersonId2tripNumber2stuckAbort().containsKey(id) && basicHandler.getPersonId2tripNumber2stuckAbort().get(id).containsKey(trip)) {
							if (basicHandler.getPersonId2tripNumber2stuckAbort().get(id).get(trip)) {
								allStuckAndAbortTrips++;
							}
						}
						
						if (basicHandler.getPersonId2tripNumber2payment().containsKey(id) && basicHandler.getPersonId2tripNumber2payment().get(id).containsKey(trip)) {
							tollPayments = tollPayments + basicHandler.getPersonId2tripNumber2payment().get(id).get(trip);
						}
						
						if (noiseHandler.getPersonId2tripNumber2causedNoiseCost().containsKey(id) && noiseHandler.getPersonId2tripNumber2causedNoiseCost().get(id).containsKey(trip)) {
							causedNoiseCost = causedNoiseCost + noiseHandler.getPersonId2tripNumber2causedNoiseCost().get(id).get(trip);
						}
						
						// only for the predefined mode
						
						if (ignoreModes || basicHandler.getPersonId2tripNumber2legMode().get(id).get(trip).equals(mode)) {
							
							mode_trips++;
							
							if (basicHandler.getPersonId2tripNumber2stuckAbort().containsKey(id) && basicHandler.getPersonId2tripNumber2stuckAbort().get(id).containsKey(trip)) {
								if (basicHandler.getPersonId2tripNumber2stuckAbort().get(id).get(trip)) {
									mode_StuckAndAbortTrips++;
								}
							}
							
							if (basicHandler.getPersonId2tripNumber2travelTime().containsKey(id) && basicHandler.getPersonId2tripNumber2travelTime().get(id).containsKey(trip)) {
								mode_TravelTime = mode_TravelTime + basicHandler.getPersonId2tripNumber2travelTime().get(id).get(trip);
							}
							
							if (basicHandler.getPersonId2tripNumber2inVehicleTime().containsKey(id) && basicHandler.getPersonId2tripNumber2inVehicleTime().get(id).containsKey(trip)) {
								mode_inVehTime = mode_inVehTime + basicHandler.getPersonId2tripNumber2inVehicleTime().get(id).get(trip);
							}
							
							if (basicHandler.getPersonId2tripNumber2waitingTime().containsKey(id) && basicHandler.getPersonId2tripNumber2waitingTime().get(id).containsKey(trip)) {
								mode_waitingTime = mode_waitingTime + basicHandler.getPersonId2tripNumber2waitingTime().get(id).get(trip);
							}
							
							if (basicHandler.getPersonId2tripNumber2tripDistance().containsKey(id) && basicHandler.getPersonId2tripNumber2tripDistance().get(id).containsKey(trip)) {
								mode_TravelDistance = mode_TravelDistance + basicHandler.getPersonId2tripNumber2tripDistance().get(id).get(trip);
							}		
						}
					}
				}	
			}
			
			bw.write("path;" + outputPath);
			bw.newLine();

			bw.newLine();
			
			bw.write("number of " + mode + " trips (sample size);" + mode_trips);
			bw.newLine();
			
			bw.write("number of " + mode + " stuck and abort trip (sample size)s;" + mode_StuckAndAbortTrips);
			bw.newLine();
			
			bw.newLine();
						
			bw.write(mode + " travel distance (sample size) [km];" + mode_TravelDistance / 1000.);
			bw.newLine();
			
			bw.write(mode + " travel time (sample size) [hours];" + mode_TravelTime / 3600.);
			bw.newLine();
			
			bw.write(mode + " in-vehicle time (sample size) [hours];" + mode_inVehTime / 3600.);
			bw.newLine();
			
			bw.write(mode + " waiting time (for taxi/pt) (sample size) [hours];" + mode_waitingTime / 3600.);
			bw.newLine();
			
			bw.newLine();
									
			bw.write("number of trips (sample size, all modes);" + allTrips);
			bw.newLine();
			
			bw.write("number of stuck and abort trips (sample size, all modes);" + allStuckAndAbortTrips);
			bw.newLine();
			
			bw.write("affected noise damage costs (sample size) [monetary units];" + affectedNoiseCost);
			bw.newLine();
			
			bw.write("caused noise damage costs (sample size) [monetary units];" + causedNoiseCost);
			bw.newLine();
			
			bw.write("travel related user benefits (sample size) (including toll payments) [monetary units];" + userBenefits);
			bw.newLine();
			
			bw.write("revenues (sample size) (tolls/fares payed by private car users or passengers) [monetary units];" + tollPayments);
			bw.newLine();
			
			bw.write("total payments (sample size) (including pt and taxi drivers) [monetary units];" + basicHandler.getTotalPayments());
			bw.newLine();
			
			double welfare = tollPayments + userBenefits - affectedNoiseCost;
			bw.write("system welfare (sample size) [monetary units];" + welfare);
			bw.newLine();
			
			log.info("Output written to " + fileName);
			bw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
}
