package playground.mzilske.pipeline;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;

public class ScenarioOutSwitchTask implements ScenarioSinkMultiSource {
	
	private static Logger logger = Logger.getLogger(ScenarioOutSwitchTask.class);

	private List<ProxySinkSource> sinks;
	
	int iteration = 0;

	private NumberExpression ne;
	
	public ScenarioOutSwitchTask(String numberExpression) {
		ne = new NumberExpression(numberExpression);
		sinks = new ArrayList<ProxySinkSource>(2);
		sinks.add(0, new ProxySinkSource());
		sinks.add(1, new ProxySinkSource());		
	}
	
	public ScenarioSource getSource(int index) {
		return sinks.get(index);
	}

	@Override
	public void initialize(Scenario scenario) {
		for (ScenarioSink sink : sinks) {
			sink.initialize(scenario);
		}
	}

	@Override
	public void process(Scenario scenario) {
		ScenarioSink sink = sinks.get(switchCriterion(iteration));
		iteration++;
		sink.process(scenario);
	}

	private int switchCriterion(int iteration2) {
		if (ne.matches(iteration2)) {
			logger.info("Iteration " + iteration2 + " matches.");
			return 1;
		} else {
			logger.info("Iteration " + iteration2 + " doesn't match.");
			return 0;
		}
	}

	private static class ProxySinkSource implements ScenarioSinkSource {

		private ScenarioSink sink;

		public ProxySinkSource() {
			// Nothing to do.
		}

		public void setSink(ScenarioSink sink) {
			this.sink = sink;
		}

		public void process(Scenario scenario) {
			sink.process(scenario);
		}

		@Override
		public void initialize(Scenario scenario) {
			sink.initialize(scenario);
		}

	}

	@Override
	public int getSourceCount() {
		return sinks.size();
	}

}
