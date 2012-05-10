/* *********************************************************************** *
 * project: org.matsim.*
 * InsertParkingActivitiesReplanner.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.wrashid.parkingSearch.withinday.t1;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.matsim.population.algorithms.PersonPrepareForSim;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.wrashid.parkingSearch.withinday.InsertParkingActivities;
import playground.wrashid.parkingSearch.withinday.ParkingInfrastructure;

public class InsertParkingActivitiesT1 extends InsertParkingActivities {


	
	/*
	 * use a InitialIdentifierImpl and set handleAllAgents to true
	 */
	public InsertParkingActivitiesT1(Scenario scenario, PlanAlgorithm planAlgorithm, ParkingInfrastructure parkingInfrastructure) {
		super(scenario,planAlgorithm,parkingInfrastructure);
	}
	
	

	
	
}
