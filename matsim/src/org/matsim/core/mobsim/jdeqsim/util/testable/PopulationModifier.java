package org.matsim.core.mobsim.jdeqsim.util.testable;

import org.matsim.core.api.population.Population;

public interface PopulationModifier {
	public Population modifyPopulation(Population population);

	public Population getPopulation();
}
