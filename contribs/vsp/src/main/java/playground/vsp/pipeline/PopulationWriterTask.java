package playground.vsp.pipeline;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.population.io.StreamingDeprecated;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

public class PopulationWriterTask implements PersonSink {
	
	private final String filename;

    private Population reader;

	private StreamingPopulationWriter populationWriter;

	private final Network network;
	
	public PopulationWriterTask(String filename, Network network) {
		super();
		this.filename = filename;
		this.network = network;
		init();
	}
	
	private void init() {
        MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
//		reader = (Population) scenario.getPopulation();
		StreamingPopulationReader reader = new StreamingPopulationReader( scenario ) ;
		StreamingDeprecated.setIsStreaming(reader, true);
		populationWriter = new StreamingPopulationWriter();
		final PersonAlgorithm algo = populationWriter;
		reader.addAlgorithm(algo);
		populationWriter.startStreaming(filename);
		Logger.getLogger(this.getClass()).info("Will write to: " + filename ) ;
	}

	@Override
	public void process(Person person) {
		reader.addPerson(person);
	}
	
	@Override
	public void complete() {
		populationWriter.closeStreaming();
		Logger.getLogger(this.getClass()).info("... writing to " + filename + " completed.") ;
	}

}
