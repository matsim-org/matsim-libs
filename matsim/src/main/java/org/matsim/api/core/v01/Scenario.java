/* *********************************************************************** *
 * project: org.matsim.*																															*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.api.core.v01;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.households.Households;
import org.matsim.lanes.data.v20.LaneDefinitions20;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicles;


/**
 * The scenario is the entry point to MATSim
 * scenarios. An implementation of Scenario
 * has to provide consistent implementations
 * for the different return types, e.g. Network,
 * Facilities or Population.
 *
 * @see org.matsim.core.scenario.ScenarioLoaderImpl
 *
 * @author dgrether
 */
public interface Scenario {

	Network getNetwork();

	Population getPopulation();

	TransitSchedule getTransitSchedule();
	
	Config getConfig();

	;

	/**
	 * Adds the given object to the scenario, such it can be
	 * retrieved with {@link #getScenarioElement(String)} using
	 * the name given here as a key.
	 *
	 * @param name the name to which the object should be associated
	 * @param o the object. <code>null</code> is not allowed.
	 * 
	 * @throws {@link NullPointerException} if the object is null
	 * @throws {@link IllegalStateException} if there is already an object
	 * associated to this name.
	 */
	void addScenarioElement(String name, Object o);

	/**
	 *
	 * @param name the name of the element to get
	 * @return the object associated with that name, or null if none is associated
	 */
	Object getScenarioElement(String name);

	ActivityFacilities getActivityFacilities();

	Vehicles getTransitVehicles();

	Vehicles getVehicles();

	Households getHouseholds();

	LaneDefinitions20 getLanes();

}