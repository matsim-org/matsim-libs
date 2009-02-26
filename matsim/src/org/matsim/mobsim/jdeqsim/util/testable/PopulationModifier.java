package org.matsim.mobsim.jdeqsim.util.testable;

import org.matsim.interfaces.core.v01.Population;

public interface PopulationModifier {
	public Population modifyPopulation(Population population);

	public Population getPopulation();
}
