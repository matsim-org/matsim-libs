package playground.mzilske.pipeline;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.gbl.MatsimRandom;

public class IteratorTask implements ScenarioSinkSource {
	
	private ScenarioSink sink;
	
	private ProxySinkSource iterationLoopSource;
	
	private LoopbackSink loopbackSink;
	
	private int firstIteration;
	
	private int lastIteration;

	private long randomSeed;
	
	private Scenario initPostbox = null;

	private Scenario processPostbox = null;

	private EventsManagerImpl eventsManager;
	
	public IteratorTask(int firstIteration, int lastIteration, long randomSeed) {
		this.firstIteration = firstIteration;
		this.lastIteration = lastIteration;
		this.randomSeed = randomSeed;
		iterationLoopSource = new ProxySinkSource();
		loopbackSink = new LoopbackSink();
	}
	
	public ScenarioSource getIterationLoopSource() {
		return iterationLoopSource;
	}

	@Override
	public void initialize(Scenario scenario) {
		iterationLoopSource.initialize(scenario);
		sink.initialize(scenario);
		if (initPostbox == null) {
			throw new RuntimeException("Iteration chain not closed.");
		}
	}

	@Override
	public void process(Scenario scenario) {
		processPostbox = scenario;
		int iteration;
		for (iteration = firstIteration; iteration <= lastIteration; iteration++) {
			resetEvents(iteration);
			resetRandomNumbers(iteration);
			iterationLoopSource.process(processPostbox);
		}
	}
	
	private void resetEvents(int iteration) {
		eventsManager.resetCounter();
		eventsManager.resetHandlers(iteration);
	}

	private void resetRandomNumbers(int iteration) {
		MatsimRandom.reset(randomSeed + iteration);
		MatsimRandom.getRandom().nextDouble(); 
		// draw one because of strange
		// "not-randomness" in the first draw...
		// Fixme [kn] this should really be ten thousand draws instead of just
		// one
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
	
	private class LoopbackSink implements ScenarioSink {

		@Override
		public void initialize(Scenario scenario) {
			initPostbox = scenario;
		}

		@Override
		public void process(Scenario scenario) {
			processPostbox = scenario;
		}
		
	}

	@Override
	public void setSink(ScenarioSink sink) {
		this.sink = sink;
	}

	public ScenarioSink getIterationLoopSink() {
		return loopbackSink;
	}

	public void setEventsManager(EventsManager eventsManager) {
		this.eventsManager = (EventsManagerImpl) eventsManager;
	}
	
}
