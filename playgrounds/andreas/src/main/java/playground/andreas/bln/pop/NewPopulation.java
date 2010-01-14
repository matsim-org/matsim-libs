package playground.andreas.bln.pop;

import org.apache.log4j.Logger;
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
	private static final Logger log = Logger.getLogger(NewPopulation.class);
	protected PopulationWriter popWriter;
	protected Network net;

	public NewPopulation(final Network network, final Population population) {
		this.net = network;
		this.popWriter = new PopulationWriter(population, this.net);
		this.popWriter.writeStartPlans(Gbl.getConfig().plans().getOutputFile());
	}

	public NewPopulation(final Network network, final Population population, final String filename) {
		this.popWriter = new PopulationWriter(population, network);
		this.popWriter.writeStartPlans(filename);
	}

	public void writeEndPlans() {
		log.info("Dumping plans to file");
		this.popWriter.writeEndPlans();
	}
}
