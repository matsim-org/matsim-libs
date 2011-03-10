package playground.mzilske.pipeline;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;

public class ScenarioLoaderTask implements RunnableScenarioSource {

	private ScenarioSink sink;
	
	private Config config;
	
	public ScenarioLoaderTask(Config config) {
		this.config = config;
	}

	@Override
	public void run() {
		Scenario scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
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
