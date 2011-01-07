package playground.mzilske.pipeline;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.population.algorithms.PersonAlgorithm;

public class PopulationReaderTask implements PersonSource, Runnable {

	private PersonSink sink;
	
	private String filename;

	private Network network;
	
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
		ScenarioImpl scenario = new ScenarioImpl();
		scenario.setNetwork((NetworkImpl) network);
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
