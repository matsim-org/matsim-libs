package playground.wrashid.tryouts.plan;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.Gbl;
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
	protected Network net;

	public NewPopulation(final Network network, final Population population) {
		this.net = network;
		this.popWriter = new PopulationWriter(population, network);
		this.popWriter.writeStartPlans(Gbl.getConfig().plans().getOutputFile());
	}

	public NewPopulation(final Network network, final Population population, final String filename) {
		this.net = network;
		this.popWriter = new PopulationWriter(population, this.net);
		this.popWriter.writeStartPlans(filename);
	}

	public void writeEndPlans() {
		this.popWriter.writeEndPlans();
	}
}
