package playground.vsp.pipeline;

import org.matsim.api.core.v01.Scenario;

public interface ScenarioSink {
	
	void initialize(Scenario scenario);

	void process(Scenario scenario);

}
