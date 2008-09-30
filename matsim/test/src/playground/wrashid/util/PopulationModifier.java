package playground.wrashid.util;

import org.matsim.population.Population;

public interface PopulationModifier {
	public Population modifyPopulation(Population population);
	public Population getPopulation();
}
