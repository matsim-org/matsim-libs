package playground.mzilske.pipeline;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentMoneyEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentMoneyEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.ScoringFunctionFactory;

public class ScoringTask implements ScenarioSinkSource, AgentArrivalEventHandler, AgentDepartureEventHandler, AgentStuckEventHandler, AgentMoneyEventHandler, ActivityStartEventHandler, ActivityEndEventHandler {

	private EventsToScore eventsToScore;
	private ScoringFunctionFactory scoringFunctionFactory;
	private double learningRate;
	private ScenarioSink sink;

	public ScoringTask(ScoringFunctionFactory scoringFunctionFactory, double learningRate) {
		this.scoringFunctionFactory = scoringFunctionFactory;
		this.learningRate = learningRate;
	}

	@Override
	public void setSink(ScenarioSink sink) {
		this.sink = sink;
	}
	
	public void initialize(Scenario scenario) {
		if (eventsToScore == null) {
			eventsToScore = new EventsToScore(scenario.getPopulation(), scoringFunctionFactory, learningRate);
		}
		sink.initialize(scenario);
	}

	@Override
	public void process(Scenario scenario) {
		eventsToScore.finish();
		sink.process(scenario);
	}

	public void handleEvent(ActivityEndEvent event) {
		eventsToScore.handleEvent(event);
	}

	public void handleEvent(ActivityStartEvent event) {
		eventsToScore.handleEvent(event);
	}

	public void handleEvent(AgentArrivalEvent event) {
		eventsToScore.handleEvent(event);
	}

	public void handleEvent(AgentDepartureEvent event) {
		eventsToScore.handleEvent(event);
	}

	public void handleEvent(AgentMoneyEvent event) {
		eventsToScore.handleEvent(event);
	}

	public void handleEvent(AgentStuckEvent event) {
		eventsToScore.handleEvent(event);
	}

	public void reset(int iteration) {
		eventsToScore.reset(iteration);
	}
	
	

}
