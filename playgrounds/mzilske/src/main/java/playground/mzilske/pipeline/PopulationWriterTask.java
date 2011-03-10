package playground.mzilske.pipeline;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

public class PopulationWriterTask implements PersonSink {
	
	private String filename;
	
	private ScenarioImpl scenario;

	private PopulationImpl population;

	private PopulationWriter populationWriter;

	private Network network;
	
	public PopulationWriterTask(String filename, Network network) {
		super();
		this.filename = filename;
		this.network = network;
		init();
	}
	
	private void init() {
		scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
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

}
