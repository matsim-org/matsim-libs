package playground.vsp.pipeline;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

public class PopulationWriterTask implements PersonSink {
	
	private final String filename;

    private PopulationImpl population;

	private PopulationWriter populationWriter;

	private final Network network;
	
	public PopulationWriterTask(String filename, Network network) {
		super();
		this.filename = filename;
		this.network = network;
		init();
	}
	
	private void init() {
        MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		population = (PopulationImpl) scenario.getPopulation();
		population.setIsStreaming(true);
		populationWriter = new PopulationWriter(population, network);
		population.addAlgorithm(populationWriter);
		populationWriter.startStreaming(filename);
		Logger.getLogger(this.getClass()).info("Will write to: " + filename ) ;
	}

	@Override
	public void process(Person person) {
		population.addPerson(person);
	}
	
	@Override
	public void complete() {
		populationWriter.closeStreaming();
		Logger.getLogger(this.getClass()).info("... writing to " + filename + " completed.") ;
	}

}
