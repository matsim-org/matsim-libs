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
package org.matsim.core.api;
import org.matsim.api.basic.v01.BasicScenario;
import org.matsim.core.api.facilities.ActivityFacilities;
import org.matsim.core.api.network.Network;
import org.matsim.core.api.population.Population;
import org.matsim.core.basic.network.BasicLaneDefinitions;
import org.matsim.core.basic.signalsystems.BasicSignalSystems;
import org.matsim.core.basic.signalsystemsconfig.BasicSignalSystemConfigurations;
import org.matsim.core.basic.v01.vehicles.BasicVehicles;
import org.matsim.households.Households;
import org.matsim.knowledges.Knowledges;



/**
 * The scenario is the entry point to MATSim 
 * scenarios. An implementation of Scenario
 * has to provide consistent implementations
 * for the different return types, e.g. Network, 
 * Facilities or Population.
 * @see org.matsim.core.api.ScenarioLoader
 * @author dgrether
 *
 */
public interface Scenario extends BasicScenario {

	public Network getNetwork();

	public ActivityFacilities getActivityFacilities() ;

	public Population getPopulation() ;
	
	public Knowledges getKnowledges();
	
	public Households getHouseholds();
	
	public BasicVehicles getVehicles();

	public BasicLaneDefinitions getLaneDefinitions();
	
	public BasicSignalSystems getSignalSystems();
	
	public BasicSignalSystemConfigurations getSignalSystemConfigurations();
	
}