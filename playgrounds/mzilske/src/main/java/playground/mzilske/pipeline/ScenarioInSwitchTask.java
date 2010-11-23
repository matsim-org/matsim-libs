package playground.mzilske.pipeline;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Scenario;

public class ScenarioInSwitchTask implements ScenarioMultiSinkSource {

	private List<ProxySinkSource> sinks;

	public ScenarioInSwitchTask(int nSinks) {
		sinks = new ArrayList<ProxySinkSource>();
		for (int i = 0; i < nSinks; i++) {
			sinks.add(i, new ProxySinkSource());
		}
	}

	@Override
	public void setSink(ScenarioSink sink) {
		for (ScenarioSource source : sinks) {
			source.setSink(sink);
		}
	}

	public ScenarioSink getSink(int index) {
		return sinks.get(index);
	}
	
	public int getSinkCount() {
		return sinks.size();
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

}
