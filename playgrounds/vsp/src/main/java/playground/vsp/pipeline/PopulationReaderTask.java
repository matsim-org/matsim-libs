package playground.vsp.pipeline;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scenario.ScenarioUtils.ScenarioBuilder;
import org.matsim.population.algorithms.PersonAlgorithm;

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
		Scenario scenario = new ScenarioBuilder(ConfigUtils.createConfig()).setNetwork(network).build();
		PopulationImpl population = (PopulationImpl) scenario.getPopulation();
		population.setIsStreaming(true);
		population.addAlgorithm(new PersonAlgorithm() {

			@Override
			public void run(Person person) {
				sink.process(person);
			}
			
		});
		new MatsimPopulationReader(scenario).readFile(filename);
		sink.complete();
	}

}
