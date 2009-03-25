package org.matsim.api.core.v01;

import org.matsim.api.basic.v01.BasicScenario;
import org.matsim.core.api.facilities.Facilities;
import org.matsim.core.api.network.Network;
import org.matsim.core.api.population.Population;

public interface Scenario extends BasicScenario {

	public Network getNetwork() ;

	public Facilities getFacilities() ;

	public Population getPopulation() ;

}