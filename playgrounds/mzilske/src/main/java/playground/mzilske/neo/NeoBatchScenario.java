package playground.mzilske.neo;

import org.matsim.api.core.v01.ScenarioImpl;
import org.neo4j.graphdb.Transaction;
import org.neo4j.index.lucene.LuceneIndexBatchInserter;
import org.neo4j.index.lucene.LuceneIndexBatchInserterImpl;
import org.neo4j.kernel.impl.batchinsert.BatchInserter;
import org.neo4j.kernel.impl.batchinsert.BatchInserterImpl;

public class NeoBatchScenario extends ScenarioImpl {
	
	BatchInserter inserter;
	
	LuceneIndexBatchInserter index;
	
	Transaction transaction;

	private String directory;

	public NeoBatchScenario(String directory) {
		super();
		this.directory = directory;
		this.inserter = new BatchInserterImpl(directory);
		this.index  = new LuceneIndexBatchInserterImpl(inserter);
		long populationNode = inserter.getReferenceNode();
		
		NeoBatchPopulationImpl population = new NeoBatchPopulationImpl(inserter, index, populationNode);
		setPopulation(population);
	}

	public void shutdown() {
		index.optimize();
		index.shutdown();
		inserter.shutdown();
	}

}
