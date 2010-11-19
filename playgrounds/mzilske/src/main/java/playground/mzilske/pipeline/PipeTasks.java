package playground.mzilske.pipeline;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;

public class PipeTasks {

	private EventsManager eventsManager;
	
	private IteratorTask iterator;

	private LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;

	private ScenarioSource scenarioSource;

	private TravelCostCalculatorTask travelCostCalculator;

	private TravelTimeCalculatorTask travelTimeCalculator;
	
	private void assertNotNull(Object object) {
		if (object == null) {
			throw new IllegalStateException("Missing a required resource on the pipeline.");
		}
	}

	public EventsManager getEventsManager() {
		assertNotNull(eventsManager);
		return eventsManager;
	}

	public IteratorTask getIterator() {
		assertNotNull(iterator);
		return iterator;
	}

	public LeastCostPathCalculatorFactory getLeastCostPathCalculatorFactory() {
		assertNotNull(leastCostPathCalculatorFactory);
		return leastCostPathCalculatorFactory;
	}

	public ScenarioSource getScenarioSource() {
		assertNotNull(scenarioSource);
		ScenarioSource result = scenarioSource;
		scenarioSource = null;
		return result;
	}

	public TravelCostCalculatorTask getTravelCostCalculator() {
		assertNotNull(travelCostCalculator);
		return travelCostCalculator;
	}

	public TravelTimeCalculatorTask getTravelTimeCalculator() {
		assertNotNull(travelTimeCalculator);
		return travelTimeCalculator;
	}

	public void setEventsManager(EventsManager events) {
		this.eventsManager = events;
	}

	public void setIterator(IteratorTask iterator) {
		this.iterator = iterator;
	}

	public void setLeastCostPathCalculatorFactory(LeastCostPathCalculatorFactory leastCostPathCalculatorFactory) {
		this.leastCostPathCalculatorFactory = leastCostPathCalculatorFactory;
	}

	public void setScenarioSource(ScenarioSource task) {
		this.scenarioSource = task;
	}

	public void setTravelCostCalculator(TravelCostCalculatorTask task) {
		this.travelCostCalculator = task;
	}

	public void setTravelTimeCalculator(TravelTimeCalculatorTask task) {
		this.travelTimeCalculator = task;
	}

	public void assertIsComplete() {
		if (scenarioSource != null) {
			throw new IllegalStateException("There is an unconsumed ScenarioSource in the pipeline. Hint: Close the pipeline with a ScenarioGround.");
		}
		if (iterator != null) {
			throw new IllegalStateException("There is an unclosed Iterator in the pipeline. Hint: Close the iteration loop with an IterationTerminator.");
		}
	}

}
