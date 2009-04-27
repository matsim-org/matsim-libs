package org.matsim.core.mobsim.jdeqsim.util.testable;

import org.matsim.core.api.population.Population;

/**
 * This allows to modify the population at the beginning of the simulation.
 * Sometimes handy for writing tests (some refactorings could make this class
 * obsolete).
 * 
 * @author rashid_waraich
 */
public interface PopulationModifier {
	public Population modifyPopulation(Population population);

	public Population getPopulation();
}
