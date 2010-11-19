package playground.mzilske.pipeline;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioLoaderImpl;

public class ScenarioLoaderTask implements RunnableScenarioSource {

	private ScenarioSink sink;
	
	private Config config;
	
	public ScenarioLoaderTask(Config config) {
		this.config = config;
	}

	@Override
	public void run() {
		Scenario scenario = new ScenarioImpl(config);
		ScenarioLoaderImpl scenarioLoaderImpl = new ScenarioLoaderImpl(scenario);
		scenarioLoaderImpl.loadScenario();
		sink.initialize(scenario);
		sink.process(scenario);
	}

	@Override
	public void setSink(ScenarioSink sink) {
		this.sink = sink;
	}

}
