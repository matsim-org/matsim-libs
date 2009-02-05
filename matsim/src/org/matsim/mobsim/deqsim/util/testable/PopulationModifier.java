package org.matsim.mobsim.deqsim.util.testable;

import org.matsim.population.Population;

public interface PopulationModifier {
	public Population modifyPopulation(Population population);

	public Population getPopulation();
}
