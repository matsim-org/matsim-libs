package playground.wrashid.tryouts.plan;

import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.PopulationImpl;
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

	public NewPopulation(final PopulationImpl population) {
		this.popWriter = new PopulationWriter(population);
		this.popWriter.writeStartPlans(Gbl.getConfig().plans().getOutputFile());
	}

	public NewPopulation(final PopulationImpl population, final String filename) {
		this.popWriter = new PopulationWriter(population);
		this.popWriter.writeStartPlans(filename);
	}


	/**
	 *
	 */
	public NewPopulation(final NetworkLayer network, final PopulationImpl population) {
		this(population);
		this.net = network;
	}

	public void writeEndPlans() {
		this.popWriter.writeEndPlans();
	}
}
