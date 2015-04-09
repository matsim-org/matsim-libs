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
import java.util.LinkedList;

import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.apache.commons.math.stat.descriptive.moment.StandardDeviation;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.DoubleValueHashMap;
import org.matsim.core.controler.Controler;

import playground.wrashid.lib.obj.Collections;
import playground.wrashid.parkingChoice.infrastructure.api.PParking;
import playground.wrashid.parkingSearch.withindayFW.interfaces.ParkingCostCalculator;
import playground.wrashid.parkingSearch.withindayFW.parkingOccupancy.ParkingOccupancyStats;
import playground.wrashid.parkingSearch.withindayFW.util.GlobalParkingSearchParams;

public class ParkingCostOptimizerZH implements ParkingCostCalculator {

	protected static final Logger log = Logger.getLogger(ParkingCostOptimizerZH.class);

	private final LinkedList<PParking> parkings;
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

		for (PParking parking : parkings) {
			Id parkingId = parking.getId();
			if (parkingId.toString().contains("stp") || parkingId.toString().contains("gp")) {
				
				if (parkingId.toString().contains("gp-31")){
					DebugLib.emptyFunctionForSettingBreakPoint();
				}
				
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

		for (PParking parking : parkings) {
			Id parkingId = parking.getId();
			if (parkingId.toString().contains("stp") || parkingId.toString().contains("gp")) {
				twoHourParkingCost.put(parkingId, parkingCostCalculatorZH.getParkingCost(parkingId, 0.0, 7200));
			}
		}

		String fileName = controler.getControlerIO().getOutputFilename("originalParkingCostTwoHours.txt");
		GeneralLib.writeHashMapToFile(twoHourParkingCost, "parkingId\ttwoHourParkingCost", fileName);

		return twoHourParkingCost;
	}

	public void logParkingPriceStats(Controler controler, int iteration) {
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
		log.info("it-" + iteration + " average street parking price (morning/afternoon): "
				+ (new Mean()).evaluate(morningArray) + "/" + new Mean().evaluate(afterNoonArray));
		log.info("it-" + iteration + " standardDeviation street parking price (morning/afternoon):"
				+ (new StandardDeviation()).evaluate(morningArray) + "/" + new StandardDeviation().evaluate(afterNoonArray));

		morningArray = Collections.convertDoubleCollectionToArray(garageParkingPricesMorning);
		afterNoonArray = Collections.convertDoubleCollectionToArray(garageParkingPricesAfternoon);
		log.info("it-" + iteration + " average garage parking price (morning/afternoon): "
				+ (new Mean()).evaluate(morningArray) + "/" + new Mean().evaluate(afterNoonArray));
		log.info("it-" + iteration + " standardDeviation garage parking price (morning/afternoon):"
				+ (new StandardDeviation()).evaluate(morningArray) + "/" + new StandardDeviation().evaluate(afterNoonArray));

		if (GlobalParkingSearchParams.writeDetailedOutput(iteration)) {
			writeoutPrices(controler, iteration);
		}

	}

	private void writeoutPrices(Controler controler, int iteration) {
		DoubleValueHashMap<Id> inputValues = publicParkingPricePerHourInTheMorning;
		String outputFileName = "publicParkingPricePerHourInTheMorning.txt";
		outputParkingPrices(controler, iteration, inputValues, outputFileName);

		inputValues = publicParkingPricePerHourInTheAfternoon;
		outputFileName = "publicParkingPricePerHourInTheAfternoon.txt";
		outputParkingPrices(controler, iteration, inputValues, outputFileName);
		
		printParkingPriceHistograms(controler, iteration);
	}

	private void printParkingPriceHistograms(Controler controler, int iteration) {
		double[] values = Collections.convertDoubleCollectionToArray(filterParkingPrices(publicParkingPricePerHourInTheMorning,"stp"));
		String timeOfDay="Morning";
		String parkingType="street";
		outputPriceHistorgram(controler, iteration, values, timeOfDay, parkingType);
		
		values = Collections.convertDoubleCollectionToArray(filterParkingPrices(publicParkingPricePerHourInTheAfternoon,"stp"));
		timeOfDay="Afternoon";
		parkingType="street";
		outputPriceHistorgram(controler, iteration, values, timeOfDay, parkingType);
		
		values = Collections.convertDoubleCollectionToArray(filterParkingPrices(publicParkingPricePerHourInTheMorning,"gp"));
		timeOfDay="Morning";
		parkingType="garage";
		outputPriceHistorgram(controler, iteration, values, timeOfDay, parkingType);
		
		values = Collections.convertDoubleCollectionToArray(filterParkingPrices(publicParkingPricePerHourInTheAfternoon,"gp"));
		timeOfDay="Afternoon";
		parkingType="garage";
		outputPriceHistorgram(controler, iteration, values, timeOfDay, parkingType);
	}

	private void outputPriceHistorgram(Controler controler, int iteration, double[] values, String timeOfDay, String parkingType) {
		String outputFileName = parkingType + "ParkingPricePerHourInThe" + timeOfDay + ".png";
		
		
		String fileName = controler.getControlerIO().getIterationFilename(iteration,
				outputFileName);

		GeneralLib.generateHistogram(fileName, values, 10,
				"Histogram " + parkingType + " Pricing Price Per Hour In the " + timeOfDay + " - It." + iteration, "price [chf]",
				"number of parkings");
	}
	
	private Collection<Double> filterParkingPrices(DoubleValueHashMap<Id> prices, String filter){
		LinkedList<Double> result=new LinkedList<Double>();
		
		for (Id id:prices.keySet()){
			if (id.toString().contains(filter)){
				result.add(prices.get(id));
			}
		}
		
		return result;
	}

	private void outputParkingPrices(Controler controler, int iteration, DoubleValueHashMap<Id> inputValues, String outputFileName) {
		ArrayList<String> list = new ArrayList<String>();
		list.add("parkingFacilityId\tprice");

		for (Id parkingId : inputValues.keySet()) {
			StringBuffer stringBuffer = new StringBuffer();

			stringBuffer.append(parkingId);
			stringBuffer.append("\t");
			stringBuffer.append(inputValues.get(parkingId));
			list.add(stringBuffer.toString());
		}

		String fileName = controler.getControlerIO().getIterationFilename(iteration, outputFileName);
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

		for (PParking parking : parkings) {
			
			if (parking.getId().toString().contains("stp") || parking.getId().toString().contains("gp")) {
				if (parkingOccupancy.parkingOccupancies.get(parking.getId()) == null) {
					// no demand at parking
					if (publicParkingPricePerHourInTheMorning.get(parking.getId()) >= priceIncValue) {
						publicParkingPricePerHourInTheMorning.decrementBy(parking.getId(), priceIncValue);
					}
					
					if (publicParkingPricePerHourInTheAfternoon.get(parking.getId()) >= priceIncValue) {
						publicParkingPricePerHourInTheAfternoon.decrementBy(parking.getId(), priceIncValue);
					}
					continue;
				}

				int[] occupancy = parkingOccupancy.parkingOccupancies.get(parking.getId()).getOccupancy();
				boolean parking85PercentReached = false;
				
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
		double totalCost=0;
		boolean isAfternoonTariff=false;;
		if (parkingFacilityId.toString().contains("private") || parkingFacilityId.toString().contains("OutsideCity")) {
			return totalCost;
		} else if (parkingFacilityId.toString().contains("gp") || parkingFacilityId.toString().contains("stp")) {
			arrivalTime = GeneralLib.projectTimeWithin24Hours(arrivalTime);
			
			double restParkingDuration=parkingDuration;
			
			if (arrivalTime<3600 * 12){
				double morningParkingInterval = GeneralLib.getIntervalDuration(arrivalTime, 3600 * 12);
				if (morningParkingInterval>parkingDuration){
					totalCost+=getPublicParkingPricePerHourInTheMorning(parkingFacilityId,parkingDuration);
					return totalCost;
				} else {
					totalCost+=getPublicParkingPricePerHourInTheMorning(parkingFacilityId,morningParkingInterval);
				
					restParkingDuration-=morningParkingInterval;
					
					isAfternoonTariff=true;
					
				}
			} else {
				double afterNoonParkingInterval = GeneralLib.getIntervalDuration(arrivalTime, 3600 * 24);
				if (afterNoonParkingInterval>parkingDuration){
					totalCost+=getPublicParkingPricePerHourInTheAfternoon(parkingFacilityId,parkingDuration);
					return totalCost;
				} else {
					totalCost+=getPublicParkingPricePerHourInTheAfternoon(parkingFacilityId,afterNoonParkingInterval);
				
					restParkingDuration-=afterNoonParkingInterval;
					
					isAfternoonTariff=false;
					
					
				}
			}
		
			while (restParkingDuration>0){
				if (restParkingDuration>3600 * 12){
					restParkingDuration-=3600 * 12;
					
					if (isAfternoonTariff){
						totalCost+=getPublicParkingPricePerHourInTheAfternoon(parkingFacilityId,3600 * 12);
					} else {
						totalCost+=getPublicParkingPricePerHourInTheMorning(parkingFacilityId,3600 * 12);
					}
				} else {
					if (isAfternoonTariff){
						totalCost+=getPublicParkingPricePerHourInTheAfternoon(parkingFacilityId,restParkingDuration);
					} else {
						totalCost+=getPublicParkingPricePerHourInTheMorning(parkingFacilityId,restParkingDuration);
					}
					
					restParkingDuration=0;
				}
				
				isAfternoonTariff=!isAfternoonTariff;
			}
			
			
			return totalCost;
		}

		DebugLib.stopSystemAndReportInconsistency("parking id:" + parkingFacilityId);

		return null;
	}
	
	public double getPublicParkingPricePerHourInTheMorning(Id parkingFacilityId, double parkingDuration){
		return publicParkingPricePerHourInTheMorning.get(parkingFacilityId) * Math.ceil(parkingDuration / (60 * 60));
	}
	
	public double getPublicParkingPricePerHourInTheAfternoon(Id parkingFacilityId, double parkingDuration){
		return publicParkingPricePerHourInTheAfternoon.get(parkingFacilityId) * Math.ceil(parkingDuration / (60 * 60));
	}
	
}