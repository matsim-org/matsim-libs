package org.matsim.core.mobsim.jdeqsim.util;

import org.matsim.core.population.PopulationImpl;

/**
 * This allows to modify the population at the beginning of the simulation.
 * Sometimes handy for writing tests (some refactorings could make this class
 * obsolete).
 * 
 * @author rashid_waraich
 */
public interface PopulationModifier {
	public PopulationImpl modifyPopulation(PopulationImpl population);

	public PopulationImpl getPopulation();
}
