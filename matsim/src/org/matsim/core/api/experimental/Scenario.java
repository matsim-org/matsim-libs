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
package org.matsim.core.api.experimental;
import org.matsim.api.basic.v01.BasicScenario;
import org.matsim.core.api.experimental.population.Population;
import org.matsim.core.facilities.ActivityFacilities;
import org.matsim.core.network.NetworkLayer;
import org.matsim.households.Households;
import org.matsim.knowledges.Knowledges;
import org.matsim.lanes.basic.BasicLaneDefinitions;
import org.matsim.signalsystems.basic.BasicSignalSystems;
import org.matsim.signalsystems.config.BasicSignalSystemConfigurations;
import org.matsim.vehicles.BasicVehicles;



/**
 * The scenario is the entry point to MATSim 
 * scenarios. An implementation of Scenario
 * has to provide consistent implementations
 * for the different return types, e.g. Network, 
 * Facilities or Population.
 * @see org.matsim.core.api.experimental.ScenarioLoader
 * @author dgrether
 *
 */
public interface Scenario extends BasicScenario {

	public NetworkLayer getNetwork();

	public ActivityFacilities getActivityFacilities() ;

	public Population getPopulation() ;
	
	public Knowledges getKnowledges();
	
	public Households getHouseholds();
	
	public BasicVehicles getVehicles();

	public BasicLaneDefinitions getLaneDefinitions();
	
	public BasicSignalSystems getSignalSystems();
	
	public BasicSignalSystemConfigurations getSignalSystemConfigurations();
	
}