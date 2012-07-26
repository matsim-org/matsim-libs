/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.wrashid.parkingSearch.withindayFW.zhCity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioImpl;

import playground.wrashid.lib.DebugLib;
import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.Collections;
import playground.wrashid.lib.obj.DoubleValueHashMap;
import playground.wrashid.parkingChoice.infrastructure.api.Parking;
import playground.wrashid.parkingSearch.withindayFW.controllers.kti.HUPCControllerKTIzh;
import playground.wrashid.parkingSearch.withindayFW.core.ParkingAgentsTracker;
import playground.wrashid.parkingSearch.withindayFW.interfaces.ParkingCostCalculator;
import playground.wrashid.parkingSearch.withindayFW.parkingOccupancy.ParkingOccupancyStats;
import playground.wrashid.parkingSearch.withindayFW.util.GlobalParkingSearchParams;

import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.apache.commons.math.stat.descriptive.moment.StandardDeviation;

public class ParkingCostOptimizerZH implements ParkingCostCalculator {

	protected static final Logger log = Logger.getLogger(ParkingCostOptimizerZH.class);

	private final LinkedList<Parking> parkings;
	private DoubleValueHashMap<Id> publicParkingPricePerHourInTheMorning;
	private DoubleValueHashMap<Id> publicParkingPricePerHourInTheAfternoon;
	double priceIncValue;
	double targetOccupancy;

	public ParkingCostOptimizerZH(ParkingCostCalculatorZH parkingCostCalculatorZH, Controler controler) {
		this.parkings = parkingCostCalculatorZH.getParkings();

		String tmpString = controler.getConfig().findParam("parking", "ParkingCostOptimizerZH.priceIncValue");
		priceIncValue = Double.parseDouble(tmpString);

		tmpString = controler.getConfig().findParam("parking", "ParkingCostOptimizerZH.targetOccupancy");
		targetOccupancy = Double.parseDouble(tmpString);

		publicParkingPricePerHourInTheMorning = new DoubleValueHashMap<Id>();
		publicParkingPricePerHourInTheAfternoon = new DoubleValueHashMap<Id>();

		HashMap<Id, Double> originalTwoHourParkingCost = outputOriginalParkingCost(parkingCostCalculatorZH, controler);

		for (Parking parking : parkings) {
			Id parkingId = parking.getId();
			if (parkingId.toString().contains("stp") || parkingId.toString().contains("gp")) {
				tmpString = controler.getConfig().findParam("parking", "ParkingCostOptimizerZH.warmStartOfPrice");
				boolean warmStartOfPrice = Boolean.parseBoolean(tmpString);

				if (warmStartOfPrice) {
					publicParkingPricePerHourInTheMorning.put(parkingId, originalTwoHourParkingCost.get(parkingId) / 2);
					publicParkingPricePerHourInTheAfternoon.put(parkingId, originalTwoHourParkingCost.get(parkingId) / 2);
				} else {
					publicParkingPricePerHourInTheMorning.put(parkingId, 0.0);
					publicParkingPricePerHourInTheAfternoon.put(parkingId, 0.0);
				}
			}
		}

	}

	private HashMap<Id, Double> outputOriginalParkingCost(ParkingCostCalculatorZH parkingCostCalculatorZH, Controler controler) {
		HashMap<Id, Double> twoHourParkingCost = new HashMap<Id, Double>();

		for (Parking parking : parkings) {
			Id parkingId = parking.getId();
			if (parkingId.toString().contains("stp") || parkingId.toString().contains("gp")) {
				twoHourParkingCost.put(parkingId, parkingCostCalculatorZH.getParkingCost(parkingId, 0.0, 3600));
			}
		}

		String fileName = controler.getControlerIO().getOutputFilename("originalParkingCostTwoHours.txt");
		GeneralLib.writeHashMapToFile(twoHourParkingCost, "parkingId\ttwoHourParkingCost", fileName);

		return twoHourParkingCost;
	}

	public void logParkingPriceStats(Controler controler) {
		// String iterationFilenameTxt =
		// controler.getControlerIO().getIterationFilename(controler.getIterationNumber(),
		// "parkingPrices.txt");

		Collection<Double> streetParkingPricesMorning = new LinkedList<Double>();
		Collection<Double> garageParkingPricesMorning = new LinkedList<Double>();
		Collection<Double> streetParkingPricesAfternoon = new LinkedList<Double>();
		Collection<Double> garageParkingPricesAfternoon = new LinkedList<Double>();
		double[] morningArray;
		double[] afterNoonArray;

		separateStreetAndGarageParkingPrices(publicParkingPricePerHourInTheMorning, streetParkingPricesMorning,
				garageParkingPricesMorning);
		separateStreetAndGarageParkingPrices(publicParkingPricePerHourInTheAfternoon, streetParkingPricesAfternoon,
				garageParkingPricesAfternoon);

		morningArray = Collections.convertDoubleCollectionToArray(streetParkingPricesMorning);
		afterNoonArray = Collections.convertDoubleCollectionToArray(streetParkingPricesAfternoon);
		log.info("it-" + controler.getIterationNumber() + " average street parking price (morning/afternoon): "
				+ (new Mean()).evaluate(morningArray) + "/" + new Mean().evaluate(afterNoonArray));
		log.info("it-" + controler.getIterationNumber() + " standardDeviation street parking price (morning/afternoon):"
				+ (new StandardDeviation()).evaluate(morningArray) + "/" + new StandardDeviation().evaluate(afterNoonArray));

		morningArray = Collections.convertDoubleCollectionToArray(garageParkingPricesMorning);
		afterNoonArray = Collections.convertDoubleCollectionToArray(garageParkingPricesAfternoon);
		log.info("it-" + controler.getIterationNumber() + " average garage parking price (morning/afternoon): "
				+ (new Mean()).evaluate(morningArray) + "/" + new Mean().evaluate(afterNoonArray));
		log.info("it-" + controler.getIterationNumber() + " standardDeviation garage parking price (morning/afternoon):"
				+ (new StandardDeviation()).evaluate(morningArray) + "/" + new StandardDeviation().evaluate(afterNoonArray));

		if (GlobalParkingSearchParams.writeDetailedOutput(controler.getIterationNumber())) {
			writeoutPrices(controler);
		}

	}

	private void writeoutPrices(Controler controler) {
		DoubleValueHashMap<Id> inputValues = publicParkingPricePerHourInTheMorning;
		String outputFileName = "publicParkingPricePerHourInTheMorning.txt";
		outputParkingPrices(controler, inputValues, outputFileName);

		inputValues = publicParkingPricePerHourInTheAfternoon;
		outputFileName = "publicParkingPricePerHourInTheAfternoon.txt";
		outputParkingPrices(controler, inputValues, outputFileName);
	}

	private void outputParkingPrices(Controler controler, DoubleValueHashMap<Id> inputValues, String outputFileName) {
		ArrayList<String> list = new ArrayList<String>();
		list.add("parkingFacilityId\tprice");

		for (Id parkingId : inputValues.keySet()) {
			StringBuffer stringBuffer = new StringBuffer();

			stringBuffer.append(parkingId);
			stringBuffer.append("\t");
			stringBuffer.append(inputValues.get(parkingId));
			list.add(stringBuffer.toString());
		}

		String fileName = controler.getControlerIO().getIterationFilename(controler.getIterationNumber(), outputFileName);
		GeneralLib.writeList(list, fileName);
	}

	private void separateStreetAndGarageParkingPrices(DoubleValueHashMap<Id> parkingPrices,
			Collection<Double> streetParkingPrices, Collection<Double> garageParkingPrices) {
		for (Id parkingId : parkingPrices.keySet()) {
			if (parkingId.toString().contains("stp")) {
				streetParkingPrices.add(parkingPrices.get(parkingId));
			}

			if (parkingId.toString().contains("gp")) {
				garageParkingPrices.add(parkingPrices.get(parkingId));
			}
		}
	}

	public void updatePrices(ParkingOccupancyStats parkingOccupancy) {

		for (Parking parking : parkings) {
			if (parkingOccupancy.parkingOccupancies.get(parking.getId()) == null) {
				continue;
			}

			int[] occupancy = parkingOccupancy.parkingOccupancies.get(parking.getId()).getOccupancy();
			boolean parking85PercentReached = false;
			if (parking.getId().toString().contains("stp") || parking.getId().toString().contains("gp")) {

				for (int i = 0; i <= 47; i++) {
					if (occupancy[i] > parking.getCapacity() * targetOccupancy) {
						publicParkingPricePerHourInTheMorning.incrementBy(parking.getId(), priceIncValue);
						parking85PercentReached = true;
						break;
					}
				}

				if (!parking85PercentReached) {
					if (publicParkingPricePerHourInTheMorning.get(parking.getId()) >= priceIncValue) {
						publicParkingPricePerHourInTheMorning.decrementBy(parking.getId(), priceIncValue);

					}
				}

				parking85PercentReached = false;

				for (int i = 48; i < 96; i++) {
					if (occupancy[i] > parking.getCapacity() * targetOccupancy) {
						publicParkingPricePerHourInTheAfternoon.incrementBy(parking.getId(), priceIncValue);
						parking85PercentReached = true;
						break;
					}
				}

				if (!parking85PercentReached) {
					if (publicParkingPricePerHourInTheAfternoon.get(parking.getId()) >= priceIncValue) {
						publicParkingPricePerHourInTheAfternoon.decrementBy(parking.getId(), priceIncValue);
					}
				}
			}
		}
	}

	@Override
	public Double getParkingCost(Id parkingFacilityId, double arrivalTime, double parkingDuration) {

		if (parkingFacilityId.toString().contains("private") || parkingFacilityId.toString().contains("OutsideCity")) {
			return 0.0;
		} else if (parkingFacilityId.toString().contains("gp") || parkingFacilityId.toString().contains("stp")) {
			if (arrivalTime < 3600 * 12) {
				return publicParkingPricePerHourInTheMorning.get(parkingFacilityId) * Math.ceil(parkingDuration / (30 * 60));
			} else {
				return publicParkingPricePerHourInTheAfternoon.get(parkingFacilityId) * Math.ceil(parkingDuration / (30 * 60));
			}
		}

		DebugLib.stopSystemAndReportInconsistency("parking id:" + parkingFacilityId);

		return null;
	}
}