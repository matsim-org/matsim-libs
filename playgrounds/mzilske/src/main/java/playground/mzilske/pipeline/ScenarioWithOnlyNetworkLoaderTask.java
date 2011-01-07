package playground.mzilske.pipeline;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.network.MatsimNetworkReader;

public class ScenarioWithOnlyNetworkLoaderTask implements RunnableScenarioSource {

	private ScenarioSink sink;
	
	private String filename;
	
	public ScenarioWithOnlyNetworkLoaderTask(String filename) {
		this.filename = filename;
	}

	@Override
	public void run() {
		Scenario scenario = new ScenarioImpl();
		new MatsimNetworkReader(scenario).readFile(filename);
		sink.initialize(scenario);
		sink.process(scenario);
	}

	@Override
	public void setSink(ScenarioSink sink) {
		this.sink = sink;
	}

}
