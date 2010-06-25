package playground.mzilske.neo;

import java.io.File;

import org.matsim.api.core.v01.ScenarioImpl;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.index.IndexService;
import org.neo4j.index.lucene.LuceneIndexService;
import org.neo4j.kernel.EmbeddedGraphDatabase;

import playground.mzilske.vis.DirectoryUtils;

public class NeoScenario extends ScenarioImpl {
	
	GraphDatabaseService graphDb;
	
	Transaction transaction;

	private String directory;

	private IndexService index;

	public NeoScenario(String directory) {
		super();
		this.directory = directory;
		this.graphDb = new EmbeddedGraphDatabase(directory);
		this.index = new LuceneIndexService(graphDb);
		Transaction tx = graphDb.beginTx();
		Node populationNode = graphDb.getReferenceNode();
		NeoPopulationImpl population = new NeoPopulationImpl(graphDb, index, populationNode);
		setPopulation(population);
		tx.success();
		tx.finish();
	}

	public void beginTx() {
		transaction = graphDb.beginTx();
	}

	public void shutdown() {
		graphDb.shutdown();
	}

	public void failure() {
		transaction.failure();
	}

	public void finish() {
		transaction.finish();
	}

	public void success() {
		transaction.success();
	}

	public void delete() {
		DirectoryUtils.deleteDirectory(new File(directory));
	}
	

}
