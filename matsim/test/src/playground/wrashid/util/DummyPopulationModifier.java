package playground.wrashid.util;

import org.matsim.population.Population;

import playground.wrashid.DES.util.testable.PopulationModifier;

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
