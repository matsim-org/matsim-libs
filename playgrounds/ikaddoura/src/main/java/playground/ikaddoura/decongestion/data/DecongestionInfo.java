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

package playground.ikaddoura.decongestion.data;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

/**
 * Stores the information which is requried during the computation of decongestion prices
 * Contains the input parameters, e.g. how often the interval-based output is written out, the number of iterations for which the price is kept constant, ...
 * 
 * @author ikaddoura
 */

public class DecongestionInfo {
	
	private final int WRITE_OUTPUT_ITERATION = 10;
	private final int UPDATE_PRICE_INTERVAL = 50;
	private final double TOLERATED_AVERAGE_DELAY_SEC = 1.;
	private final double TOLL_ADJUSTMENT_RATE = 0.5;
	private final double FRACTION_OF_ITERATIONS_TO_END_PRICE_ADJUSTMENT = 1.0;
	
	private final Scenario scenario;
	
	private final Map<Id<Link>, LinkInfo> linkId2info = new HashMap<>();
	private final Map<Id<Vehicle>, Id<Person>> vehicleId2personId = new HashMap<>();

	public DecongestionInfo(Scenario scenario) {
		this.scenario = scenario;
	}

	public int getWRITE_OUTPUT_ITERATION() {
		return WRITE_OUTPUT_ITERATION;
	}

	public int getUPDATE_PRICE_INTERVAL() {
		return UPDATE_PRICE_INTERVAL;
	}
	
	public Scenario getScenario() {
		return scenario;
	}
	
	public Map<Id<Vehicle>, Id<Person>> getVehicleId2personId() {
		return vehicleId2personId;
	}
	
	public Map<Id<Link>, LinkInfo> getlinkInfos() {
		return linkId2info;
	}

	public double getTOLERATED_AVERAGE_DELAY_SEC() {
		return TOLERATED_AVERAGE_DELAY_SEC;
	}

	public double getTOLL_ADJUSTMENT_RATE() {
		return TOLL_ADJUSTMENT_RATE;
	}

	public double getFRACTION_OF_ITERATIONS_TO_END_PRICE_ADJUSTMENT() {
		return FRACTION_OF_ITERATIONS_TO_END_PRICE_ADJUSTMENT;
	}

}

