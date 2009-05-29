package playground.andreas.bln;

import org.matsim.core.api.population.Population;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.PopulationWriter;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

/**
 * Helper class, for handling plansfiles * 
 * 
 * @author aneumann, Yu
 *
 */
public abstract class NewPopulation extends AbstractPersonAlgorithm {
	protected PopulationWriter popWriter;
	protected NetworkLayer net;

	public NewPopulation(final Population population) {
		this.popWriter = new PopulationWriter(population);
		this.popWriter.writeStartPlans();
	}

	public NewPopulation(final Population population, final String filename) {
		this.popWriter = new PopulationWriter(population, filename, "v4");
		this.popWriter.writeStartPlans();
	}


	/**
	 *
	 */
	public NewPopulation(final NetworkLayer network, final Population population) {
		this(population);
		this.net = network;
	}

	public void writeEndPlans() {
		this.popWriter.writeEndPlans();
	}
}
