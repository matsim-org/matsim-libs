package playground.mzilske.d4d;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.population.algorithms.PersonAlgorithm;

import playground.vsp.pipeline.PersonSink;
import playground.vsp.pipeline.PersonSource;


public class PopulationReaderV5Task implements PersonSource, Runnable {

	private PersonSink sink;
	
	private String filename;

	private Network network;
	
	public PopulationReaderV5Task(String filename, Network network) {
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
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.setNetwork((NetworkImpl) network);
		PopulationImpl population = (PopulationImpl) scenario.getPopulation();
		population.setIsStreaming(true);
		population.addAlgorithm(new PersonAlgorithm() {

			@Override
			public void run(Person person) {
				sink.process(person);
			}
			
		});
		new AltPopulationReaderMatsimV5(scenario).readFile(filename);
		sink.complete();
	}

}
