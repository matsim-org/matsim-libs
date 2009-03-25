package org.matsim.mobsim.jdeqsim.util;

import org.matsim.core.api.population.Population;
import org.matsim.mobsim.jdeqsim.util.testable.PopulationModifier;


public class DummyPopulationModifier implements PopulationModifier {
	Population population=null;
	
	// does not modify population at all
	// needed to pass population to tests
	public Population modifyPopulation(Population population) {
		this.population=population;
		return population;
	}

	public Population getPopulation(){
		return population;
	} 

}
