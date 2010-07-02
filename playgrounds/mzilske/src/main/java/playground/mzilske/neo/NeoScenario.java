package playground.mzilske.neo;

import java.io.File;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.utils.geometry.CoordImpl;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.index.IndexService;
import org.neo4j.index.lucene.LuceneIndexService;
import org.neo4j.kernel.EmbeddedGraphDatabase;

import playground.mzilske.vis.DirectoryUtils;

public class NeoScenario implements Scenario {
	
	private GraphDatabaseService graphDb;

	private String directory;

	private IndexService index;

	private NeoNetworkImpl network;

	private NeoPopulationImpl population;

	public NeoScenario(String directory, Map<String, String> config) {
		super();
		this.directory = directory;
		this.graphDb = new EmbeddedGraphDatabase(directory, config);
		this.index = new LuceneIndexService(graphDb);
		Transaction tx = graphDb.beginTx();
		Node populationNode = graphDb.getReferenceNode();
		network = new NeoNetworkImpl(graphDb, index, populationNode, populationNode);
		population = new NeoPopulationImpl(graphDb, index, populationNode);
		tx.success();
		tx.finish();
	}

	public Transaction beginTx() {
		return graphDb.beginTx();
	}

	public void shutdown() {
		index.shutdown();
		graphDb.shutdown();
	}

	public void delete() {
		DirectoryUtils.deleteDirectory(new File(directory));
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

}
