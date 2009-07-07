package org.matsim.core.mobsim.jdeqsim.util;

import org.matsim.core.population.PopulationImpl;


public class DummyPopulationModifier implements PopulationModifier {
	PopulationImpl population=null;
	
	// does not modify population at all
	// needed to pass population to tests
	public PopulationImpl modifyPopulation(PopulationImpl population) {
		this.population=population;
		return population;
	}

	public PopulationImpl getPopulation(){
		return population;
	} 

}
