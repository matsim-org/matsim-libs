package playground.vsp.pipeline;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.population.io.StreamingDeprecated;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

public class PopulationReaderTask implements PersonSource, Runnable {

	private PersonSink sink;
	
	private final String filename;

	private final Network network;
	
	public PopulationReaderTask(String filename, Network network) {
		super();
		this.filename = filename;
		this.network = network;
	}

	@Override
	public void setSink(PersonSink sink) {
		this.sink = sink;
	}

	@Override
	public void run() {
//		Scenario scenario = new ScenarioBuilder(ConfigUtils.createConfig()).setNetwork(network).build();
		MutableScenario scenario = ScenarioUtils.createMutableScenario(ConfigUtils.createConfig()) ;
//		Population population = (Population) scenario.getPopulation();
		StreamingPopulationReader population = new StreamingPopulationReader( scenario ) ;
		StreamingDeprecated.setIsStreaming(population, true);
		population.addAlgorithm(new PersonAlgorithm() {
		
			@Override
			public void run(Person person) {
				sink.process(person);
			}
			
		});
		new PopulationReader(scenario).readFile(filename);
		sink.complete();
	}

}
