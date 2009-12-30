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



/**
 * The scenario is the entry point to MATSim 
 * scenarios. An implementation of Scenario
 * has to provide consistent implementations
 * for the different return types, e.g. Network, 
 * Facilities or Population.
 * @see org.matsim.core.scenario.ScenarioLoaderImpl
 * @author dgrether
 *
 */
public interface Scenario /*extends BasicScenario */{

	public Network getNetwork();

	public Population getPopulation() ;

	public Config getConfig();

	public Id createId(String string);

	public Coord createCoord(double x, double y);

	
	// the following are available via the Impl only
//	public ActivityFacilities getActivityFacilities() ;
//
//	public Knowledges getKnowledges();
//	
//	public Households getHouseholds();
//	
//	public BasicVehicles getVehicles();
//
//	public BasicLaneDefinitions getLaneDefinitions();
//	
//	public BasicSignalSystems getSignalSystems();
//	
//	public BasicSignalSystemConfigurations getSignalSystemConfigurations();
	
}