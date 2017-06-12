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
package playground.ikaddoura.analysis.detailedPersonTripAnalysis.old;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import playground.ikaddoura.analysis.detailedPersonTripAnalysis.handler.BasicPersonTripAnalysisHandler;
import playground.ikaddoura.analysis.detailedPersonTripAnalysis.handler.CongestionAnalysisHandler;
import playground.ikaddoura.analysis.detailedPersonTripAnalysis.handler.NoiseAnalysisHandler;
import playground.ikaddoura.analysis.vtts.VTTSHandler;

/**
 * @author ikaddoura
 *
 */
public class PersonTripCongestionNoiseAnalysis {
	private static final Logger log = Logger.getLogger(PersonTripCongestionNoiseAnalysis.class);
	
	public void printPersonInformation(String outputPath,
			String mode,
			Map<Id<Person>, Double> personId2userBenefit,
			BasicPersonTripAnalysisHandler basicHandler,
			VTTSHandler vttsHandler,
			CongestionAnalysisHandler congestionHandler,
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
					+ "avg. VTTS per " + mode + " trip [monetary units per hour];"
					+ mode + " total travel time (day) [sec];"
					+ mode + " total travel distance (day) [m];"
					
					+ "travel related user benefits (based on the selected plans score) [monetary units];"
					+ "total toll payments (day) [monetary units];"
					+ "caused noise cost (day) [monetary units];"
					+ "affected noise cost (day) [monetary units];"
					+ "caused congestion (day) [sec];"
					+ "affected congestion (day) [sec];"
					+ "affected congestion cost (day) [monetary units]"
					
					);
			bw.newLine();
			
			for (Id<Person> id : basicHandler.getScenario().getPopulation().getPersons().keySet()) {
				
				double userBenefit = Double.NEGATIVE_INFINITY;
				if (personId2userBenefit.containsKey(id)) {
					userBenefit = personId2userBenefit.get(id);
				}
				int mode_trips = 0;
				String mode_stuckAbort = "no";
				List<Double> mode_vtts = new ArrayList<>();
				double mode_travelTime = 0.;
				double mode_travelDistance = 0.;
				
				double tollPayments = 0.;
				double causedNoiseCost = 0.;
				double affectedNoiseCost = 0.;
				double causedCongestion = 0.;
				double affectedCongestion = 0.;
				double affectedCongestionCost = 0.;
				
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

						if (congestionHandler.getPersonId2tripNumber2causedDelay().containsKey(id) && congestionHandler.getPersonId2tripNumber2causedDelay().get(id).containsKey(trip)) {
							causedCongestion = causedCongestion + congestionHandler.getPersonId2tripNumber2causedDelay().get(id).get(trip);
						}
						
						if (congestionHandler.getPersonId2tripNumber2affectedDelay().containsKey(id) && congestionHandler.getPersonId2tripNumber2affectedDelay().get(id).containsKey(trip)) {
							affectedCongestion = affectedCongestion + congestionHandler.getPersonId2tripNumber2affectedDelay().get(id).get(trip);

							double vttsThisTrip = Double.NEGATIVE_INFINITY;
							if (vttsHandler.getPersonId2TripNr2VTTSh().containsKey(id) && vttsHandler.getPersonId2TripNr2VTTSh().get(id).containsKey(trip)) {
								vttsThisTrip = vttsHandler.getPersonId2TripNr2VTTSh().get(id).get(trip);
							}
							if (vttsThisTrip == Double.NEGATIVE_INFINITY) {
								log.warn("No vtts to convert affected delays into delay costs.");
							} else {
								affectedCongestionCost = affectedCongestionCost + vttsThisTrip * (congestionHandler.getPersonId2tripNumber2affectedDelay().get(id).get(trip) / 3600.);
							}
						}		
						
						if (ignoreModes || basicHandler.getPersonId2tripNumber2legMode().get(id).get(trip).equals(mode)) {
							
							mode_trips++;
							
							if (basicHandler.getPersonId2tripNumber2stuckAbort().containsKey(id) && basicHandler.getPersonId2tripNumber2stuckAbort().get(id).containsKey(trip)) {
								if (basicHandler.getPersonId2tripNumber2stuckAbort().get(id).get(trip)) {
									mode_stuckAbort = "yes";
								}
							}
							
							if (vttsHandler.getPersonId2TripNr2VTTSh().containsKey(id) && vttsHandler.getPersonId2TripNr2VTTSh().get(id).containsKey(trip)) {
								mode_vtts.add(vttsHandler.getPersonId2TripNr2VTTSh().get(id).get(trip));
							}
							
							if (basicHandler.getPersonId2tripNumber2travelTime().containsKey(id) && basicHandler.getPersonId2tripNumber2travelTime().get(id).containsKey(trip)) {
								mode_travelTime = mode_travelTime + basicHandler.getPersonId2tripNumber2travelTime().get(id).get(trip);
							}
							
							if (basicHandler.getPersonId2tripNumber2tripDistance().containsKey(id) && basicHandler.getPersonId2tripNumber2tripDistance().get(id).containsKey(trip)) {
								mode_travelDistance = mode_travelDistance + basicHandler.getPersonId2tripNumber2tripDistance().get(id).get(trip);
							}			
						}
					}
				}
				
				double mode_avgVTTS = 0.;
				int n = 0;
				double s = 0.;
				for (Double d : mode_vtts) {
					s = s + d;
					n++;
				}
				if (n > 0) {
					mode_avgVTTS = s / n;
				}
				
				bw.write(id + ";"
						+ mode_trips + ";"
						+ mode_stuckAbort + ";"
						+ mode_avgVTTS + ";"
						+ mode_travelTime + ";"
						+ mode_travelDistance + ";"
						
						+ userBenefit + ";"
						+ tollPayments + ";"
						+ causedNoiseCost + ";"
						+ affectedNoiseCost + ";"
						+ causedCongestion + ";"
						+ affectedCongestion + ";"
						+ affectedCongestionCost
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
			VTTSHandler vttsHandler,
			CongestionAnalysisHandler congestionHandler,
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
					+ "VTTS (trip) [monetary units per hour];"
					+ "departure time (trip) [sec];"
					+ "arrival time (trip) [sec];"
					+ "travel time (trip) [sec];"
					+ "travel distance (trip) [m];"
					+ "affected congestion (trip) [sec];"
					+ "affected congestion cost (trip) [monetary units];"
					+ "caused congestion (trip) [sec];"
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
						
						String vtts = "unknown";
						if (vttsHandler.getPersonId2TripNr2VTTSh().containsKey(id) && vttsHandler.getPersonId2TripNr2VTTSh().get(id).containsKey(trip)) {
							vtts = String.valueOf(vttsHandler.getPersonId2TripNr2VTTSh().get(id).get(trip));
						}
						
						String departureTime = "unknown";
						if (basicHandler.getPersonId2tripNumber2departureTime().containsKey(id) && basicHandler.getPersonId2tripNumber2departureTime().get(id).containsKey(trip)) {
							departureTime = String.valueOf(basicHandler.getPersonId2tripNumber2departureTime().get(id).get(trip));
						}
						
						String arrivalTime = "unknown";
						if (basicHandler.getPersonId2tripNumber2arrivalTime().containsKey(id) && basicHandler.getPersonId2tripNumber2arrivalTime().get(id).containsKey(trip)){
							arrivalTime = String.valueOf(basicHandler.getPersonId2tripNumber2arrivalTime().get(id).get(trip));
						}
						
						String travelTime = "unknown";
						if (basicHandler.getPersonId2tripNumber2travelTime().containsKey(id) && basicHandler.getPersonId2tripNumber2travelTime().get(id).containsKey(trip)) {
							travelTime = String.valueOf(basicHandler.getPersonId2tripNumber2travelTime().get(id).get(trip));
						}
						
						String travelDistance = "unknown";
						if (basicHandler.getPersonId2tripNumber2tripDistance().containsKey(id) && basicHandler.getPersonId2tripNumber2tripDistance().get(id).containsKey(trip)) {
							travelDistance = String.valueOf(basicHandler.getPersonId2tripNumber2tripDistance().get(id).get(trip));
						}
										
						double affectedDelay = 0.;
						double affectedDelayCost = 0.;
						if (congestionHandler.getPersonId2tripNumber2affectedDelay().containsKey(id) && congestionHandler.getPersonId2tripNumber2affectedDelay().get(id).containsKey(trip)) {
							affectedDelay = congestionHandler.getPersonId2tripNumber2affectedDelay().get(id).get(trip);
							
							double vttsThisTrip = Double.NEGATIVE_INFINITY;
							if (vttsHandler.getPersonId2TripNr2VTTSh().containsKey(id) && vttsHandler.getPersonId2TripNr2VTTSh().get(id).containsKey(trip)) {
								vttsThisTrip = vttsHandler.getPersonId2TripNr2VTTSh().get(id).get(trip);
							}
							if (vttsThisTrip == Double.NEGATIVE_INFINITY) {
								log.warn("No vtts to convert affected delays into delay costs.");
							} else {
								affectedDelayCost = vttsThisTrip * (affectedDelay / 3600.);
							}
						}
						
						double causedDelay = 0.;
						if (congestionHandler.getPersonId2tripNumber2causedDelay().containsKey(id) && congestionHandler.getPersonId2tripNumber2causedDelay().get(id).containsKey(trip)) {
							causedDelay = congestionHandler.getPersonId2tripNumber2causedDelay().get(id).get(trip);
						}
						
						double causedNoiseCost = 0.;
						if (noiseHandler.getPersonId2tripNumber2causedNoiseCost().containsKey(id) && noiseHandler.getPersonId2tripNumber2causedNoiseCost().get(id).containsKey(trip)) {
							causedNoiseCost = noiseHandler.getPersonId2tripNumber2causedNoiseCost().get(id).get(trip);
						}
						
						bw.write(id + ";"
						+ trip + ";"
						+ transportModeThisTrip + ";"
						+ stuckAbort + ";"
						+ vtts + ";"
						+ departureTime + ";"
						+ arrivalTime + ";"
						+ travelTime + ";"
						+ travelDistance + ";"
						+ affectedDelay + ";"
						+ affectedDelayCost + ";"
						+ causedDelay + ";"
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
			VTTSHandler vttsHandler,
			CongestionAnalysisHandler congestionHandler,
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
			List<Double> mode_vtts = new ArrayList<>();
			double mode_TravelTime = 0.;
			double mode_TravelDistance = 0.;
			
			int allTrips = 0;
			int allStuckAndAbortTrips = 0;
			double affectedNoiseCost = 0.;
			double tollPayments = 0.;
			double causedNoiseCost = 0.;
			double causedCongestion = 0.;
			double affectedCongestion = 0.;
			double affectedCongestionCost = 0.;
			
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

						if (congestionHandler.getPersonId2tripNumber2causedDelay().containsKey(id) && congestionHandler.getPersonId2tripNumber2causedDelay().get(id).containsKey(trip)) {
							causedCongestion = causedCongestion + congestionHandler.getPersonId2tripNumber2causedDelay().get(id).get(trip);
						}
						
						if (congestionHandler.getPersonId2tripNumber2affectedDelay().containsKey(id) && congestionHandler.getPersonId2tripNumber2affectedDelay().get(id).containsKey(trip)) {
							affectedCongestion = affectedCongestion + congestionHandler.getPersonId2tripNumber2affectedDelay().get(id).get(trip);
						
							double vttsThisTrip = Double.NEGATIVE_INFINITY;
							if (vttsHandler.getPersonId2TripNr2VTTSh().containsKey(id) && vttsHandler.getPersonId2TripNr2VTTSh().get(id).containsKey(trip)) {
								vttsThisTrip = vttsHandler.getPersonId2TripNr2VTTSh().get(id).get(trip);
							}
							if (vttsThisTrip == Double.NEGATIVE_INFINITY) {
								log.warn("No vtts to convert affected delays into delay costs.");
							} else {
								affectedCongestionCost = affectedCongestionCost + vttsThisTrip * (congestionHandler.getPersonId2tripNumber2affectedDelay().get(id).get(trip) / 3600.);
							}
							
						}
						
						// only for the predefined mode
						
						if (ignoreModes || basicHandler.getPersonId2tripNumber2legMode().get(id).get(trip).equals(mode)) {
							
							mode_trips++;
							
							if (basicHandler.getPersonId2tripNumber2stuckAbort().containsKey(id) && basicHandler.getPersonId2tripNumber2stuckAbort().get(id).containsKey(trip)) {
								if (basicHandler.getPersonId2tripNumber2stuckAbort().get(id).get(trip)) {
									mode_StuckAndAbortTrips++;
								}
							}
							
							if (vttsHandler.getPersonId2TripNr2VTTSh().containsKey(id) && vttsHandler.getPersonId2TripNr2VTTSh().get(id).containsKey(trip)) {
								mode_vtts.add(vttsHandler.getPersonId2TripNr2VTTSh().get(id).get(trip));
							}
							
							if (basicHandler.getPersonId2tripNumber2travelTime().containsKey(id) && basicHandler.getPersonId2tripNumber2travelTime().get(id).containsKey(trip)) {
								mode_TravelTime = mode_TravelTime + basicHandler.getPersonId2tripNumber2travelTime().get(id).get(trip);
							}
							
							if (basicHandler.getPersonId2tripNumber2tripDistance().containsKey(id) && basicHandler.getPersonId2tripNumber2tripDistance().get(id).containsKey(trip)) {
								mode_TravelDistance = mode_TravelDistance + basicHandler.getPersonId2tripNumber2tripDistance().get(id).get(trip);
							}		
						}
					}
				}	
			}
			
			double mode_avgVTTS = 0.;
			int n = 0;
			double s = 0.;
			for (Double d : mode_vtts) {
				s = s + d;
				n++;
			}
			if (n > 0) {
				mode_avgVTTS = s / n;
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
			
			bw.write("average " + mode + " VTTS [monetary units per hour];" + mode_avgVTTS);
			bw.newLine();
			
			bw.write(mode + " travel time (sample size) [hours];" + mode_TravelTime / 3600.);
			bw.newLine();
			
			bw.newLine();
									
			bw.write("number of trips (sample size, all modes);" + allTrips);
			bw.newLine();
			
			bw.write("number of stuck and abort trips (sample size, all modes);" + allStuckAndAbortTrips);
			bw.newLine();
			
			bw.write("affected congestion [hours];" + affectedCongestion / 3600.);
			bw.newLine();
			
			bw.write("caused congestion [hours];" + causedCongestion / 3600.);
			bw.newLine();
			
			bw.newLine();
			
			bw.write("affected congestion cost (sample size) [monetary units];" + affectedCongestionCost);
			bw.newLine();
			
			bw.write("affected noise damage costs (sample size) [monetary units];" + affectedNoiseCost);
			bw.newLine();
			
			bw.write("caused noise damage costs (sample size) [monetary units];" + causedNoiseCost);
			bw.newLine();
			
			bw.write("travel related user benefits (sample size) (including toll payments) [monetary units];" + userBenefits);
			bw.newLine();
			
			bw.write("toll revenues (sample size) [monetary units];" + tollPayments);
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
