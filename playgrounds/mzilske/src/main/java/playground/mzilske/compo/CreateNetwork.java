package playground.mzilske.compo;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.mzilske.pipeline.RunnableScenarioSource;
import playground.mzilske.pipeline.ScenarioSink;

public class CreateNetwork implements RunnableScenarioSource {

	private ScenarioSink sink;
	
	private Config config;

	public CreateNetwork(Config config) {
		this.config = config;
	}

	@Override
	public void setSink(ScenarioSink sink) {
		this.sink = sink;
	}

	@Override
	public void run() {
		Scenario scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
		Network network = scenario.getNetwork();
		network.addNode(network.getFactory().createNode(scenario.createId("1"), scenario.createCoord(0.0, 0.0)));
		network.addNode(network.getFactory().createNode(scenario.createId("2"), scenario.createCoord(1.0, 1.0)));
		network.addLink(network.getFactory().createLink(scenario.createId("1-2"), scenario.createId("1"), scenario.createId("2")));
		sink.process(scenario);
	}

}
