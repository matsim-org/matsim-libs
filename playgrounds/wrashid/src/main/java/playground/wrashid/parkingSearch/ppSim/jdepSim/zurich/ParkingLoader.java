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
package playground.wrashid.parkingSearch.ppSim.jdepSim.zurich;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;

import playground.wrashid.parkingChoice.infrastructure.api.Parking;
import playground.wrashid.parkingChoice.trb2011.ParkingHerbieControler;
import playground.wrashid.parkingSearch.ppSim.jdepSim.Message;
import playground.wrashid.parkingSearch.ppSim.ttmatrix.TTMatrix;
import playground.wrashid.parkingSearch.ppSim.ttmatrix.TTMatrixFromStoredTable;
import playground.wrashid.parkingSearch.withindayFW.interfaces.ParkingCostCalculator;
import playground.wrashid.parkingSearch.withindayFW.util.GlobalParkingSearchParams;
import playground.wrashid.parkingSearch.withindayFW.zhCity.CityZones;
import playground.wrashid.parkingSearch.withindayFW.zhCity.ParkingInfrastructureZH;

public class ParkingLoader {

	public static double parkingsOutsideZHCityScaling = 1.0;
	public static double streetParkingCalibrationFactor = 1.0;
	public static double garageParkingCalibrationFactor = 1.0;
	public static double privateParkingCalibrationFactorZHCity = 1.0;
	public static double populationScalingFactor = 1.0;

	public static LinkedList<Parking> getParkingsForScenario() {
		String parkingDataBase = "C:/data/parkingSearch/psim/zurich/inputs/parkingFlat/";

		LinkedList<Parking> parkingCollection = getParkingCollectionZHCity(parkingDataBase);
		String parkingsFile = parkingDataBase + "publicParkingsOutsideZHCity.xml";

		readParkings(parkingsOutsideZHCityScaling, parkingsFile, parkingCollection);

		LinkedList<Parking> parkingWithNonPositveCapacity=new LinkedList<Parking>();
		for (Parking parking : parkingCollection) {
			if (parking.getIntCapacity()<=0){
				parkingWithNonPositveCapacity.add(parking);
			}
		}
		parkingCollection.removeAll(parkingWithNonPositveCapacity);
		
		int numberOfStreetParking = 0;
		int numberOfGarageParking = 0;
		int numberOfPrivateParking = 0;

		for (Parking parking : parkingCollection) {
			if (parking.getId().toString().contains("stp")) {
				numberOfStreetParking += parking.getIntCapacity();
			} else if (parking.getId().toString().contains("gp")) {
				numberOfGarageParking += parking.getIntCapacity();
			} else if (parking.getId().toString().contains("private")) {
				numberOfPrivateParking += parking.getIntCapacity();
			}
		}

		double totalNumberOfParkingZH = numberOfStreetParking + numberOfGarageParking + numberOfPrivateParking;

		System.out.println("streetParking to garageParking (%): " + numberOfStreetParking / 1.0 / numberOfGarageParking
				+ " - ref: 3.03");
		System.out.println("numberOfStreetParking (%): " + numberOfStreetParking / totalNumberOfParkingZH * 100
				+ " - ref: 18.5 - [" + numberOfStreetParking + "]");
		System.out.println("numberOfGarageParking (%):" + numberOfGarageParking / totalNumberOfParkingZH * 100 + " - ref: 6.1 - ["
				+ numberOfGarageParking + "]");
		System.out.println("numberOfPrivateParking (%):" + numberOfPrivateParking / totalNumberOfParkingZH * 100
				+ " - ref: 75.4 - [" + numberOfPrivateParking + "]");

		System.out.println("totalNumberOfParkingZH: " + Math.round(totalNumberOfParkingZH / 1000) + "k - ref: " + 267000
				* populationScalingFactor / 1000 + "k");

		return parkingCollection;
	}

	private static LinkedList<Parking> getParkingCollectionZHCity(String parkingDataBase) {

		// double
		// privateParkingsOutdoorCalibrationFactor=Double.parseDouble(controler.getConfig().findParam("parking",
		// "privateParkingsOutdoorCalibrationFactorZHCity"));

		LinkedList<Parking> parkingCollection = new LinkedList<Parking>();

		String streetParkingsFile = parkingDataBase + "streetParkings.xml";
		readParkings(streetParkingCalibrationFactor, streetParkingsFile, parkingCollection);

		String garageParkingsFile = parkingDataBase + "garageParkings.xml";
		readParkings(garageParkingCalibrationFactor, garageParkingsFile, parkingCollection);

		String privateIndoorParkingsFile = parkingDataBase + "privateParkings_v1_kti.xml";

		readParkings(privateParkingCalibrationFactorZHCity, privateIndoorParkingsFile, parkingCollection);

		return parkingCollection;
	}

	private static void readParkings(double parkingCalibrationFactor, String parkingsFile, LinkedList<Parking> parkingCollection) {
		ParkingHerbieControler.readParkings(parkingCalibrationFactor, parkingsFile, parkingCollection);
	}

	public static ParkingManagerZH getParkingManagerZH(LinkedList<Parking> parkings, Network network, TTMatrix ttMatrix) {
		String cityZonesFilePath = "C:/data/parkingSearch/psim/zurich/inputs/parkingZones/zones.csv";

		ParkingCostCalculator parkingCostCalculator = new ParkingCostCalculatorZH(new CityZones(cityZonesFilePath), parkings);

		HashMap<String, HashSet<Id>> parkingTypes = new HashMap<String, HashSet<Id>>();

		HashSet<Id> streetParking = new HashSet<Id>();
		HashSet<Id> garageParking = new HashSet<Id>();
		parkingTypes.put("streetParking", streetParking);
		parkingTypes.put("garageParking", garageParking);

		for (Parking parking : parkings) {
			if (parking.getId().toString().contains("stp-")) {
				streetParking.add(parking.getId());
			}
			
			if (parking.getId().toString().contains("publicPOutsideCityZH")) {
				streetParking.add(parking.getId());
			}

			if (parking.getId().toString().contains("gp-")) {
				garageParking.add(parking.getId());
			}
		}
		
		return new ParkingManagerZH(parkingTypes, parkingCostCalculator, parkings, network, ttMatrix);
	}

	public static ParkingManagerZH getParkingManagerZH(Network network, TTMatrix ttMatrix) {
		LinkedList<Parking> parkings = getParkingsForScenario();
		return getParkingManagerZH(parkings, network, ttMatrix);
	}

	public static void main(String[] args) {
		Network network = GeneralLib.readNetwork("c:/data/parkingSearch/psim/zurich/inputs/ktiRun24/output_network.xml.gz");
		TTMatrix ttMatrix = new TTMatrixFromStoredTable("C:/data/parkingSearch/psim/zurich/inputs/it.50.3600secBin.ttMatrix.txt",
				network);
		getParkingManagerZH(network, ttMatrix);
	}
	
}
