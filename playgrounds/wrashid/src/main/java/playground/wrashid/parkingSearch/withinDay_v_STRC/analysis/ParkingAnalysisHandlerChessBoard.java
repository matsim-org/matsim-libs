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
package playground.wrashid.parkingSearch.withinDay_v_STRC.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.Pair;
import org.matsim.contrib.parking.lib.obj.StringMatrix;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;

import playground.wrashid.lib.obj.Collections;
import playground.wrashid.lib.obj.IntegerValueHashMap;
import playground.wrashid.lib.obj.LinkedListValueHashMap;
import playground.wrashid.parkingChoice.trb2011.ParkingHerbieControler;
import playground.wrashid.parkingChoice.trb2011.counts.SingleDayGarageParkingsCount;
import playground.wrashid.parkingSearch.planLevel.occupancy.ParkingOccupancyBins;
import playground.wrashid.parkingSearch.withinDay_v_STRC.core.mobsim.ParkingInfrastructure_v2;
import playground.wrashid.parkingSearch.withindayFW.analysis.ParkingAnalysisHandler;
import playground.wrashid.parkingSearch.withindayFW.core.ParkingInfrastructure;
import playground.wrashid.parkingSearch.withindayFW.parkingOccupancy.ParkingOccupancyStats;
import playground.wrashid.parkingSearch.withindayFW.util.GlobalParkingSearchParams;
import playground.wrashid.parkingSearch.withindayFW.zhCity.ParkingAnalysisHandlerZH;
import playground.wrashid.parkingSearch.withindayFW.zhCity.ParkingCostOptimizerZH;

public class ParkingAnalysisHandlerChessBoard extends ParkingAnalysisHandler {

	protected static final Logger log = Logger.getLogger(ParkingAnalysisHandlerZH.class);

	private Set<String> selectedParkings;
	private double[] sumOfOccupancyCountsOfSelectedParkings;
	private final ParkingInfrastructure_v2 parkingInfrastructure;

	private double countsScalingFactor;

	public ParkingAnalysisHandlerChessBoard(Controler controler, ParkingInfrastructure_v2 parkingInfrastructure) {
		this.controler = controler;
		this.parkingInfrastructure = parkingInfrastructure;
	}

	




	@Override
	public void updateParkingOccupancyStatistics(ParkingOccupancyStats parkingOccupancy,
			IntegerValueHashMap<Id> facilityCapacities, int iteration) {
		super.updateParkingOccupancyStatistics(parkingOccupancy, facilityCapacities, iteration);

		if (GlobalParkingSearchParams.getScenarioId() == 2) {
			ParkingCostOptimizerZH parkingCostCalculator = (ParkingCostOptimizerZH) this.parkingInfrastructure
					.getParkingCostCalculator();

			parkingCostCalculator.logParkingPriceStats(controler, iteration);
			parkingCostCalculator.updatePrices(parkingOccupancy);
		}

		

	}

	private void writeUnusedParkingHistogram(String parkingType, IntegerValueHashMap<Id> unusedParkingCapacity, int iteration) {
		double[] values = Collections.convertIntegerCollectionToDoubleArray(unusedParkingCapacity.values());
		String fileName = controler.getControlerIO().getIterationFilename(iteration,
				"unusedParkingHistogramm"+ parkingType + ".png");

		GeneralLib.generateHistogram(fileName, values, 10,
				"Histogram "+ parkingType + " Unused Parking - It." + iteration, "number of unused Parking",
				"number of parking facilities");
		
	}

	@Override
	public void processParkingWalkTimes(LinkedListValueHashMap<Id, Pair<Id, Double>> parkingWalkTimesLog, int iteration) {
		
		
	}





	@Override
	public void processParkingSearchTimes(LinkedListValueHashMap<Id, Pair<Id, Double>> parkingSearchTimeLog, int iteration) {

		
	}


	@Override
	public void processParkingCost(LinkedListValueHashMap<Id, Pair<Id, Double>> parkingCostLog, int iteration) {
		
	}

	@Override
	public void printShareOfCarUsers() {
		Map<Id, ? extends Person> persons = controler.getPopulation().getPersons();
		int numberOfPerson = persons.size();
		int numberOfCarUsers = 0;
		for (Person person : persons.values()) {
			for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
				if (pe instanceof Leg) {
					Leg leg = (Leg) pe;

					if (leg.getMode().equals(TransportMode.car)) {
						numberOfCarUsers++;
						break;
					}

				}
			}
		}

		log.info("share of car users:" + numberOfCarUsers / 1.0 / numberOfPerson);
	}

}