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
package playground.kturner.freightKt.analyse;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.CarrierVehicleType;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.vehicles.VehicleType;

import playground.kturner.freightKt.analyse.TripEventHandler.VehicleTypeSpezificCapabilities;

/**
 * @author ikaddoura , lkroeger
 *
 */
public class TripWriter {
	private static final Logger log = Logger.getLogger(TripWriter.class);

	TripEventHandler handler;
	String outputFolder;
	
	public TripWriter(TripEventHandler handler, String outputFolder) {
		this.handler = handler;
		String directory = outputFolder + (outputFolder.endsWith("/") ? "" : "/");
		this.outputFolder = directory;
		
		String fileName = outputFolder;
		File file = new File(fileName);
		file.mkdirs();
	}
	
/**
 * Schreibt die Informationen (TripDistance, diastance Tour) des Carriers für jeden Trip einzeln auf.
 * TODO: TravelTime, 
 * TODO: gesamte Reisezeit (Ende "start"-act bis Beginn "end"-act)
 * @param carrierIdString
 */
	public void writeDetailedResultsSingleCarrier(String carrierIdString) {
		
		String fileName = this.outputFolder + "trip_infos_" + carrierIdString + ".csv";
		File file = new File(fileName);
			
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write(fileName);
			bw.newLine();
			bw.write("____________________________________________________________________________");
			bw.newLine();

//			bw.write("departure time [sec];person Id;amount per trip [monetary units];distance [m];travel time [sec]");
			bw.write("person Id;distance trip[m];distance tour [m]");
			bw.newLine();

//			Map<Id<Person>,List<Double>> personId2listOfAmounts = this.handler.getPersonId2listOfAmounts(mode);
//			Map<Id<Person>,List<Double>> personId2listOfDepartureTimes = this.handler.getPersonId2listOfDepartureTimes(mode);
			Map<Id<Person>,List<Double>> personId2listOfDistances = this.handler.getPersonId2listOfDistances(carrierIdString);
//			Map<Id<Person>,List<Double>> personId2listOfTravelTimes = this.handler.getPersonId2listOfTravelTimes(mode);
			
//			KT:
			Map<Id<Person>, Double> personId2tourDistance = this.handler.getPersonId2TourDistances(carrierIdString);
				
			for (Id<Person> id :personId2listOfDistances.keySet()) {
//				List<Double> amounts = personId2listOfAmounts.get(id);
//				List<Double> departureTimes = personId2listOfDepartureTimes.get(id);
				List<Double> distances = personId2listOfDistances.get(id);
//				List<Double> travelTimes = personId2listOfTravelTimes.get(id);
				
				Double tourDistance = personId2tourDistance.get(id);
				
				
				for (int i = 0 ; i < distances.size() ; i++) {
//					double price = amounts.get(i);
//					double departureTime = departureTimes.get(i);
					double distance = distances.get(i);
//					double travelTime = travelTimes.get(i);
					
//					bw.write(departureTime + ";" + id + ";" + price + ";" + distance + ";" + travelTime);
					bw.write(id + ";" + distance+ ";" + tourDistance);
					bw.newLine();
				}
			}
			
			log.info("Output written to " + fileName);
			bw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Schreibt die Informationen (tour distance, tour travelTime) des Carriers für jede Tour (= jedes Fzg) einzeln auf.
	 * TODO: gesamte Reisezeit (Ende "start"-act bis Beginn "end"-act)
	 * @param carrierIdString
	 */
	public void writeVehicleResultsSingleCarrier(String carrierIdString) {
		
		String fileName = this.outputFolder + "tour_infos_" + carrierIdString + ".csv";
		File file = new File(fileName);
			
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write(fileName);
			bw.newLine();
			bw.write("____________________________________________________________________________");
			bw.newLine();

//			bw.write("departure time [sec];person Id;amount per trip [monetary units];distance [m];travel time [sec]");
			bw.write("person Id;distance tour [m] ; TravelTime tour [s]");
			bw.newLine();

			
		
//			KT:
			Map<Id<Person>, Double> personId2tourDistance = this.handler.getPersonId2TourDistances(carrierIdString);
			Map<Id<Person>, Double> personId2tourTravelTimes = this.handler.getPersonId2TravelTimes(carrierIdString);
			
			//Summe für gesammten Carrier
			Double totalTourDistance = 0.0;
			Double totalTourTravelTime =0.0;
			for (Id<Person> id :personId2tourDistance.keySet()) {
				totalTourDistance = totalTourDistance + personId2tourDistance.get(id);
				totalTourTravelTime = totalTourTravelTime + personId2tourTravelTimes.get(id);
			}
			
			bw.write("SUMME Carrier;" + totalTourDistance + ";" + totalTourTravelTime);
			bw.newLine();
			
			// Werte der einzelnen Agenten
			for (Id<Person> id :personId2tourDistance.keySet()) {

				Double tourDistance = personId2tourDistance.get(id);
				Double tourTravelTime = personId2tourTravelTimes.get(id);
				
				bw.write(id + ";" + tourDistance + ";" + tourTravelTime);
				bw.newLine();

			}
			
			log.info("Output written to " + fileName);
			bw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Schreibt die Informationen (#Fahrzeuge, distance, travelTime (Fahrzeit), FuelConsumption, CO2-Emissionen) 
	 * des Carriers für jeden FahrzeugTyp einzeln auf und bildet auch Gesamtsumme.
	 * TODO: gesamte Reisezeit (Ende "start"-act bis Beginn "end"-act)
	 */
	public void writeResultsPerVehicleTypes() {
		
		String fileName = this.outputFolder + "total_infos_per_vehicleType.csv";
		File file = new File(fileName);
			
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write(fileName);
			bw.newLine();
			bw.write("____________________________________________________________________________");
			bw.newLine();

//			bw.write("departure time [sec];person Id;amount per trip [monetary units];distance [m];travel time [sec]");
			bw.write("vehType Id;#ofVehicles;distance [m] ; distance [km] ;TravelTime [s]  ; " +
					"FuelConsumption[l]; Emission [t Co2];  FuelConsumptionRate[l/100m]; " +
					"EmissionRate [g/m]; ");
			bw.newLine();
	
		
//			KT:
			Map<Id<VehicleType>,Double> vehTypeId2TourDistances = new TreeMap<Id<VehicleType>,Double>();
			Map<Id<VehicleType>,Double> vehTypeId2TravelTimes = new TreeMap<Id<VehicleType>,Double>();
			Map<Id<VehicleType>,Integer> vehTypeId2NumberOfVehicles = new TreeMap<Id<VehicleType>,Integer>();
			Map<Id<VehicleType>, VehicleTypeSpezificCapabilities> vehTypId2Capabilities = new TreeMap<Id<VehicleType>, VehicleTypeSpezificCapabilities>();
			
			//Vorbereitung: Nur Aufnehmen, wenn nicht null;
			CarrierVehicleTypes vehicleTypes = this.handler.getVehicleTypes();
			for (Id<VehicleType> vehicleTypeId : vehicleTypes.getVehicleTypes().keySet()){
				if (vehTypeId2TourDistances.containsKey(vehicleTypeId)) {
					log.warn("vehicleType wurde bereits behandelt:" + vehicleTypeId.toString());
				} else { //TODO: umschreiben, dass nur die Werte bestimmt werden... oder man die Map einmal bestimmt.
					System.out.println(vehicleTypeId + " added mit Entfernung " +  this.handler.getVehTypId2TourDistances(vehicleTypeId).get(vehicleTypeId));
					Double distance = this.handler.getVehTypId2TourDistances(vehicleTypeId).get(vehicleTypeId);
					Double travelTime = this.handler.getVehTypId2TravelTimes(vehicleTypeId).get(vehicleTypeId);
					Integer nuOfVeh = this.handler.getVehTypId2VehicleNumber(vehicleTypeId).get(vehicleTypeId);
					VehicleTypeSpezificCapabilities capabilities = this.handler.getVehTypId2Capabilities().get(vehicleTypeId);
					if (distance != null) {
						vehTypeId2TourDistances.put(vehicleTypeId, distance );
					}
					if (travelTime != null){
						vehTypeId2TravelTimes.put(vehicleTypeId, travelTime);
					}
					if (nuOfVeh != null){
						vehTypeId2NumberOfVehicles.put(vehicleTypeId, nuOfVeh);
					} 
					if (capabilities != null){
						vehTypId2Capabilities.put(vehicleTypeId, capabilities);
					}
					
				}
			}
			
			//Gesamtsumme
			Double totalDistance = 0.0;
			Double totalTravelTime = 0.0;
			Integer totalNumberofVehicles = 0;
			Double totalFuelConsumtion = 0.0;
			Double totalEmissions = 0.0;
			for (Id<VehicleType> vehTypeId : vehTypeId2TourDistances.keySet()) {
				totalDistance = totalDistance + vehTypeId2TourDistances.get(vehTypeId);
				totalTravelTime = totalTravelTime + vehTypeId2TravelTimes.get(vehTypeId);
				totalNumberofVehicles = totalNumberofVehicles + vehTypeId2NumberOfVehicles.get(vehTypeId);
				totalFuelConsumtion = totalFuelConsumtion + vehTypeId2TourDistances.get(vehTypeId)*vehTypId2Capabilities.get(vehTypeId).getFuelConsumtion()/100;
				totalEmissions = totalEmissions + vehTypeId2TourDistances.get(vehTypeId)*vehTypId2Capabilities.get(vehTypeId).getEmissionsPerMeter()/1000000;
			}
			
			// Gesamtsumme
			bw.write("SUMME alle Carrier;"+ 
					totalNumberofVehicles + ";" + 
					totalDistance + ";" +
					totalDistance/1000 + ";" +
					totalTravelTime + ";" +
					totalFuelConsumtion + ";" +  // Spritverbrauch in Liter
					totalEmissions	// CO2-Ausstoss in t
					);
			bw.newLine();
			
			// Werte der einzelnen Fahrzeugtypen (alle Carrier)
			for (Id<VehicleType> vehTypeId : vehTypeId2TourDistances.keySet()) {

				Double tourDistance = vehTypeId2TourDistances.get(vehTypeId);
				Double tourTravelTime = vehTypeId2TravelTimes.get(vehTypeId);
				Integer numberofVehicles = vehTypeId2NumberOfVehicles.get(vehTypeId);
				VehicleTypeSpezificCapabilities capabilites = vehTypId2Capabilities.get(vehTypeId);
				
				bw.write(vehTypeId + ";" +
						numberofVehicles + ";" + 
						tourDistance + ";" + 
						tourDistance/1000 + ";" + 
						tourTravelTime + ";" + 
						tourDistance*capabilites.getFuelConsumtion()/100 + ";" +  // Spritverbrauch in Liter (Faktor wg l/100km als Grundangabe)
						tourDistance*capabilites.getEmissionsPerMeter()/1000000 +";" + 	// CO2-Ausstoss in t (= 1Mio g)
						capabilites.getFuelConsumtion() + ";" + 
						capabilites.getEmissionsPerMeter() 
						);
				bw.newLine();

			}
			
			bw.newLine();
			
			log.info("Output written to " + fileName);
			bw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	
//	//Neu KT: Noch anpassen
//public void writeDetailedResultsAllCarrier() {
//		
//		String fileName = this.outputFolder + "tour_infos_all_Carriers.csv";
//		File file = new File(fileName);
//			
//		try {
//			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
//			bw.write(fileName);
//			bw.newLine();
//			bw.write("____________________________________________________________________________");
//			bw.newLine();
//
//			bw.write("person Id;distance [m]");
//			bw.newLine();
//				
//			Map<Id<Person>, Double> personId2listOfTourDistances = this.handler.getPersonId2listOfTourDistances(carrierIdString);
//				
//			for (Id<Person> id :personId2listOfTourDistances.keySet()) {
//
//				double tourDistance = personId2listOfTourDistances.get(id);
//
//				bw.write(id + ";" + tourDistance);
//				bw.newLine();
//
//			}
//			
//			log.info("Output written to " + fileName);
//			bw.close();
//			
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
	
//	public void writeAvgTollPerTimeBin(String mode) {
//		String fileName = this.outputFolder + "avg_amount_per_trip_departure_time_" + mode + ".csv";
//		File file = new File(fileName);
//		Map<Double, Double> departureTime2avgAmount = this.handler.getAvgAmountPerTripDepartureTime(mode);
//
//		try {
//			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
//			bw.write(fileName);
//			bw.newLine();
//			bw.write("____________________________________________________________________________");
//			bw.newLine();
//
//			bw.write("trip departure time;average amount");
//			bw.newLine();
//
//			for (Double x : departureTime2avgAmount.keySet()) {
//				
//				bw.write(x + ";" + departureTime2avgAmount.get(x));
//				bw.newLine();
//			}
//			
//			log.info("Output written to " + fileName);
//			bw.close();
//			
//		} catch (IOException e) {
//			e.printStackTrace();
//		}	
//	}
//	
//	public void writeAvgTollPerDistance(String mode) {
//		String fileName = this.outputFolder + "avg_amount_per_trip_distance_" + mode + ".csv";
//		File file = new File(fileName);
//		Map<Double, Double> tripDistance2avgAmount = this.handler.getAvgAmountPerTripDistance(mode);
//
//		try {
//			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
//			bw.write(fileName);
//			bw.newLine();
//			bw.write("____________________________________________________________________________");
//			bw.newLine();
//
//			bw.write("trip distance;average amount");
//			bw.newLine();
//
//			for (Double x : tripDistance2avgAmount.keySet()) {
//				
//				bw.write(x + ";" + tripDistance2avgAmount.get(x));
//				bw.newLine();
//			}
//			
//			log.info("Output written to " + fileName);
//			bw.close();
//			
//		} catch (IOException e) {
//			e.printStackTrace();
//		}	
//	}
//	
//	public void writePersonId2totalAmount() {
//		
//		String fileName = this.outputFolder + "personId2totalAmount.csv";
//		File file = new File(fileName);
//			
//		try {
//			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
//			bw.write(fileName);
//			bw.newLine();
//			bw.write("____________________________________________________________________________");
//			bw.newLine();
//
//			bw.write("person Id;total amount [monetary units]");
//			bw.newLine();
//			
//			Map<Id<Person>,Double> personId2totalAmount = this.handler.getCausingAgentId2amountSumAllAgents();
//
//			for (Id<Person> id : personId2totalAmount.keySet()) {
//				double totalAmount = personId2totalAmount.get(id);
//				
//				bw.write(id + ";" + totalAmount);
//				bw.newLine();
//			}
//			
//			log.info("Output written to " + fileName);
//			bw.close();
//			
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//
//	public void writeAvgTravelTimePerTimeBin(String mode) {
//		String fileName = this.outputFolder + "avg_travelTime_per_trip_departure_time_" + mode + ".csv";
//		File file = new File(fileName);
//		Map<Double, Double> departureTime2avgTravelTime = this.handler.getAvgTravelTimePerTripDepartureTime(mode);
//
//		try {
//			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
//			bw.write(fileName);
//			bw.newLine();
//			bw.write("____________________________________________________________________________");
//			bw.newLine();
//
//			bw.write("trip departure time;average travel time [sec]");
//			bw.newLine();
//
//			for (Double x : departureTime2avgTravelTime.keySet()) {
//				
//				bw.write(x + ";" + departureTime2avgTravelTime.get(x));
//				bw.newLine();
//			}
//			
//			log.info("Output written to " + fileName);
//			bw.close();
//			
//		} catch (IOException e) {
//			e.printStackTrace();
//		}	
//		
//	}
	
}
