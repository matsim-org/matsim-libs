package org.matsim.api.core.v01;

import org.matsim.api.basic.v01.BasicScenario;
import org.matsim.core.api.facilities.Facilities;
import org.matsim.core.api.network.Network;
import org.matsim.core.api.population.Population;


/**
 * The scenario is the entry point to MATSim 
 * scenarios. An implementation of Scenario
 * has to provide consistent implementations
 * for the different return types, e.g. Network, 
 * Facilities or Population.
 * @see org.matsim.api.core.v01.ScenarioLoader
 * @author dgrether
 *
 */
public interface Scenario extends BasicScenario {

	public Network getNetwork() ;

	public Facilities getFacilities() ;

	public Population getPopulation() ;

}