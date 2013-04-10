/* *********************************************************************** *
 * project: org.matsim.*
 * ParkingInfrastructure.java
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

package playground.wrashid.parkingSearch.withinDay_v_STRC.core.mobsim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.algorithms.WorldConnectLocations;

import playground.christoph.parking.core.interfaces.ParkingCostCalculator;
import playground.christoph.parking.core.mobsim.ParkingInfrastructure;

public class ParkingInfrastructure_v2 extends ParkingInfrastructure {

	public ParkingInfrastructure_v2(Scenario scenario, ParkingCostCalculator parkingCostCalculator, HashMap<Id, String> parkingTypes) {
		super(scenario, parkingCostCalculator);
	}

}
