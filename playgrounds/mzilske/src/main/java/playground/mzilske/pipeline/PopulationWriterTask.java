package playground.mzilske.pipeline;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;

public class PopulationWriterTask implements PersonSink {
	
	private String filename;
	
	private ScenarioImpl scenario;

	private PopulationImpl population;

	private PopulationWriter populationWriter;

	private Network network;
	
	public PopulationWriterTask(String filename) {
		super();
		this.filename = filename;
	}
	
	private void init() {
		scenario = new ScenarioImpl();
		population = (PopulationImpl) scenario.getPopulation();
		population.setIsStreaming(true);
		populationWriter = new PopulationWriter(population, network);
		population.addAlgorithm(populationWriter);
		populationWriter.startStreaming(filename);
	}

	public void process(Person person) {
		population.addPerson(person);
	}
	
	public void complete() {
		populationWriter.closeStreaming();
	}

	public void setNetwork(Network network) {
		this.network = network;
		init();
	}

}
