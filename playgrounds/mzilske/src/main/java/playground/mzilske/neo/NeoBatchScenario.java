package playground.mzilske.neo;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.utils.geometry.CoordImpl;
import org.neo4j.graphdb.Transaction;
import org.neo4j.index.lucene.LuceneIndexBatchInserter;
import org.neo4j.index.lucene.LuceneIndexBatchInserterImpl;
import org.neo4j.kernel.impl.batchinsert.BatchInserter;
import org.neo4j.kernel.impl.batchinsert.BatchInserterImpl;

public class NeoBatchScenario implements Scenario {
	
	BatchInserter inserter;
	
	LuceneIndexBatchInserter index;
	
	Transaction transaction;

	private NeoBatchNetworkImpl network;

	private NeoBatchPopulationImpl population;

	public NeoBatchScenario(String directory) {
		super();
		this.inserter = new BatchInserterImpl(directory);
		this.index  = new LuceneIndexBatchInserterImpl(inserter);
		long populationNode = inserter.getReferenceNode();
		long nodeNode = inserter.getReferenceNode();
		long linkNode = inserter.getReferenceNode();
		network = new NeoBatchNetworkImpl(inserter, index, nodeNode, linkNode);
		population = new NeoBatchPopulationImpl(inserter, index, populationNode);
	}

	public void shutdown() {
		index.optimize();
		index.shutdown();
		inserter.shutdown();
	}

	@Override
	public void addScenarioElement(Object o) {
		throw new RuntimeException();
	}

	@Override
	public Coord createCoord(double x, double y) {
		return new CoordImpl(x, y);
	}

	@Override
	public Id createId(String string) {
		return new IdImpl(string);
	}

	@Override
	public Config getConfig() {
		throw new RuntimeException();
	}

	@Override
	public Network getNetwork() {
		return network;
	}

	@Override
	public Population getPopulation() {
		return population;
	}

	@Override
	public <T> T getScenarioElement(Class<? extends T> klass) {
		throw new RuntimeException();
	}

	@Override
	public boolean removeScenarioElement(Object o) {
		throw new RuntimeException();
	}

	public void vacuum() {
		index.optimize();
	}
	
	

}
