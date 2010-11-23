package playground.mzilske.pipeline;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;

public class ProgressTask implements ScenarioSinkSource {

	private static Logger logger = Logger.getLogger(ProgressTask.class);
	
	private ScenarioSink sink;
	
	@Override
	public void setSink(ScenarioSink sink) {
		this.sink = sink;
	}

	@Override
	public void initialize(Scenario scenario) {
		logger.info("Initialized.");
		sink.initialize(scenario);
	}

	@Override
	public void process(Scenario scenario) {
		logger.info("Processed.");
		sink.process(scenario);
	}

}
