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

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioImpl;

import playground.wrashid.lib.DebugLib;
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

	public ParkingCostOptimizerZH(LinkedList<Parking> parkings, Controler controler) {
		this.parkings = parkings;
		
		String tmpString = controler.getConfig().findParam("parking", "ParkingCostOptimizerZH.priceIncValue");
		priceIncValue=Double.parseDouble(tmpString);
		
		tmpString = controler.getConfig().findParam("parking", "ParkingCostOptimizerZH.targetOccupancy");
		targetOccupancy=Double.parseDouble(tmpString);

		publicParkingPricePerHourInTheMorning=new DoubleValueHashMap<Id>();
		publicParkingPricePerHourInTheAfternoon=new DoubleValueHashMap<Id>();
		
		for (Parking parking : parkings) {
			if (parking.getId().toString().contains("stp") || parking.getId().toString().contains("gp")) {
				publicParkingPricePerHourInTheMorning.put(parking.getId(), 0.0);
				publicParkingPricePerHourInTheAfternoon.put(parking.getId(), 0.0);
			}
		}
	}
	
	public void logParkingPriceStats(Controler controler){
	//	String iterationFilenameTxt = controler.getControlerIO().getIterationFilename(controler.getIterationNumber(),
	//			"parkingPrices.txt");
	
		Collection<Double> streetParkingPricesMorning=new LinkedList<Double>();
		Collection<Double> garageParkingPricesMorning=new LinkedList<Double>();
		Collection<Double> streetParkingPricesAfternoon=new LinkedList<Double>();
		Collection<Double> garageParkingPricesAfternoon=new LinkedList<Double>();
		double[] morningArray;
		double[] afterNoonArray;
		
		separateStreetAndGarageParkingPrices(publicParkingPricePerHourInTheMorning,streetParkingPricesMorning, garageParkingPricesMorning);
		separateStreetAndGarageParkingPrices(publicParkingPricePerHourInTheAfternoon,streetParkingPricesAfternoon, garageParkingPricesAfternoon);
		
		morningArray=Collections.convertDoubleCollectionToArray(streetParkingPricesMorning);
		afterNoonArray=Collections.convertDoubleCollectionToArray(streetParkingPricesAfternoon);
		log.info("it-"+ controler.getIterationNumber() + " average street parking price (morning/afternoon): " + (new Mean()).evaluate(morningArray) + "/" + new Mean().evaluate(afterNoonArray));
		log.info("it-"+ controler.getIterationNumber() + " standardDeviation street parking price (morning/afternoon):" + (new StandardDeviation()).evaluate(morningArray) + "/" + new StandardDeviation().evaluate(afterNoonArray));
		
		morningArray=Collections.convertDoubleCollectionToArray(garageParkingPricesMorning);
		afterNoonArray=Collections.convertDoubleCollectionToArray(garageParkingPricesAfternoon);
		log.info("it-"+ controler.getIterationNumber() + " average garage parking price (morning/afternoon): " + (new Mean()).evaluate(morningArray) + "/" + new Mean().evaluate(afterNoonArray));
		log.info("it-"+ controler.getIterationNumber() + " standardDeviation garage parking price (morning/afternoon):" + (new StandardDeviation()).evaluate(morningArray) + "/" + new StandardDeviation().evaluate(afterNoonArray));
	}

	private void separateStreetAndGarageParkingPrices(DoubleValueHashMap<Id> parkingPrices, Collection<Double> streetParkingPrices, Collection<Double> garageParkingPrices) {
		for (Id parkingId: parkingPrices.keySet()) {
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
			if (parkingOccupancy.parkingOccupancies.get(parking.getId())==null){
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