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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.parking.lib.GeneralLib;

import playground.wrashid.parkingChoice.infrastructure.PublicParking;
import playground.wrashid.parkingChoice.infrastructure.api.PParking;
import playground.wrashid.parkingChoice.trb2011.ParkingHerbieControler;
import playground.wrashid.parkingSearch.ppSim.ttmatrix.TTMatrix;
import playground.wrashid.parkingSearch.ppSim.ttmatrix.TTMatrixFromStoredTable;
import playground.wrashid.parkingSearch.withindayFW.interfaces.ParkingCostCalculator;
import playground.wrashid.parkingSearch.withindayFW.zhCity.CityZones;

public class ParkingLoader {

	public static double parkingsOutsideZHCityScaling = 1.0;
	public static double streetParkingCalibrationFactor = 1.0;
	public static double garageParkingCalibrationFactor = 1.0;
	public static double privateParkingCalibrationFactorZHCity = 1.0;
	public static double populationScalingFactor = 1.0;

	public static LinkedList<PParking> getParkingsForScenario(String parkingDataBase) {

		LinkedList<PParking> parkingCollection = getParkingCollectionZHCity(parkingDataBase);
		String parkingsFile = parkingDataBase + "publicParkingsOutsideZHCity.xml";

		readParkings(parkingsOutsideZHCityScaling, parkingsFile, parkingCollection);

		LinkedList<PParking> parkingWithNonPositveCapacity = new LinkedList<PParking>();
		for (PParking parking : parkingCollection) {
			if (parking.getIntCapacity() <= 0) {
				parkingWithNonPositveCapacity.add(parking);
			}
		}
		parkingCollection.removeAll(parkingWithNonPositveCapacity);

		int numberOfStreetParking = 0;
		int numberOfGarageParking = 0;
		int numberOfPrivateParking = 0;

		for (PParking parking : parkingCollection) {
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

		System.out.println("totalNumberOfParkingZH: " + (totalNumberOfParkingZH / 1000) + "k - ref: " + 267000
				* populationScalingFactor / 1000 + "k");

		return parkingCollection;
	}

	private static LinkedList<PParking> getParkingCollectionZHCity(String parkingDataBase) {

		// double
		// privateParkingsOutdoorCalibrationFactor=Double.parseDouble(controler.getConfig().findParam("parking",
		// "privateParkingsOutdoorCalibrationFactorZHCity"));

		LinkedList<PParking> parkingCollection = new LinkedList<PParking>();

		String streetParkingsFile = parkingDataBase + "streetParkings_teleAtlast_ivtch.xml";
		readParkings(streetParkingCalibrationFactor, streetParkingsFile, parkingCollection);

		String garageParkingsFile = parkingDataBase + "garageParkings.xml";
		readParkings(garageParkingCalibrationFactor, garageParkingsFile, parkingCollection);

		String privateIndoorParkingsFile = parkingDataBase + "privateParkings_v1_kti.xml";

		readParkings(privateParkingCalibrationFactorZHCity, privateIndoorParkingsFile, parkingCollection);

		return parkingCollection;
	}

	private static void readParkings(double parkingCalibrationFactor, String parkingsFile, LinkedList<PParking> parkingCollection) {
		ParkingHerbieControler.readParkings(parkingCalibrationFactor, parkingsFile, parkingCollection);
	}

	public static ParkingManagerZH getParkingManagerZH(LinkedList<PParking> parkings, Network network, TTMatrix ttMatrix) {
		String cityZonesFilePath = ZHScenarioGlobal.loadStringParam("ParkingLoader.parkingZones");

		ParkingCostCalculator parkingCostCalculator = new ParkingCostCalculatorZH(new CityZones(cityZonesFilePath), parkings);

		HashMap<String, HashSet<Id<PParking>>> parkingTypes = new HashMap<>();

		HashSet<Id<PParking>> streetParking = new HashSet<>();
		HashSet<Id<PParking>> garageParking = new HashSet<>();
		HashSet<Id<PParking>> illegalParking = new HashSet<>();
		parkingTypes.put("streetParking", streetParking);
		parkingTypes.put("garageParking", garageParking);
		parkingTypes.put("illeagalParking", illegalParking);

		for (PParking parking : parkings) {
			if (parking.getId().toString().contains("stp-")) {
				streetParking.add(parking.getId());
			}

			if (parking.getId().toString().contains("publicPOutsideCityZH")) {
				streetParking.add(parking.getId());
			}

			if (parking.getId().toString().contains("gp-")) {
				garageParking.add(parking.getId());
			}

			if (parking.getId().toString().contains("illegal-")) {
				illegalParking.add(parking.getId());
			}
		}

		logAllParkingToTextFile(parkings);

		return new ParkingManagerZH(parkingTypes, parkingCostCalculator, parkings, network, ttMatrix);
	}

	private static void logAllParkingToTextFile(LinkedList<PParking> parkings) {
		ArrayList<String> list = new ArrayList<String>();
		list.add("parkingFacilityId\tcapacity\tx-coord\ty-coord");

		for (PParking p : parkings) {
			list.add(p.getId().toString() + "\t" + p.getIntCapacity() + "\t" + p.getCoord().getX() + "\t" + p.getCoord().getY());
		}

		GeneralLib.writeList(list, ZHScenarioGlobal.outputFolder + "parkingProperties.txt");
	}

	public static ParkingManagerZH getParkingManagerZH(Network network, TTMatrix ttMatrix) {
		LinkedList<PParking> parkings = getParkingsForScenario(ZHScenarioGlobal.loadStringParam("ParkingLoader.parkingDataBase"));
		addIllegalParking(network, parkings);
		addDummyParking(parkings);

		return getParkingManagerZH(parkings, network, ttMatrix);
	}

	private static void addDummyParking(LinkedList<PParking> parkings) {
		PublicParking parking = new PublicParking(new Coord((double) 100000000, (double) 100000000));
		parking.setMaxCapacity(100000000000.0);
		parking.setParkingId(Id.create("backupParking", PParking.class));
		parking.setType("public");
		parkings.add(parking);


		parking = new PublicParking(new Coord((double) 100000000, (double) 100000000));
		parking.setMaxCapacity(100000000000.0);
		parking.setParkingId(Id.create("gp-bkp", PParking.class));
		parking.setType("public");
		parkings.add(parking);
	}

	private static void addIllegalParking(Network network, LinkedList<PParking> parkings) {
		double shareOfLinksWithIllegalParking=ZHScenarioGlobal.loadDoubleParam("ParkingLoader.shareOfLinksWithIllegalParking");
		Coord coordinatesLindenhofZH = ParkingHerbieControler.getCoordinatesLindenhofZH();
		Random rand=new Random(19873); // fixing seed (scenarios with different simulation seeds should still have same illegal parking infrastructure)
		
		int i = 0;
		for (Link link : network.getLinks().values()) {
			if (GeneralLib.getDistance(coordinatesLindenhofZH, link.getCoord()) < 7000 && rand.nextDouble()<shareOfLinksWithIllegalParking) {
				PublicParking parking = new PublicParking(link.getCoord());
				parking.setMaxCapacity(1.0);
				parking.setParkingId(Id.create("illegal-" + i, PParking.class));
				parking.setType("public");
				parkings.add(parking);
				i++;
			}
		}
		System.out.println("number of illegal parking added: " + i / 1000.0 + "k");
	}

	public static void main(String[] args) {
		Network network = GeneralLib.readNetwork("c:/data/parkingSearch/psim/zurich/inputs/ktiRun24/output_network.xml.gz");
		TTMatrix ttMatrix = new TTMatrixFromStoredTable("C:/data/parkingSearch/psim/zurich/inputs/it.50.3600secBin.ttMatrix.txt",
				network);
		getParkingManagerZH(network, ttMatrix);
	}

}
