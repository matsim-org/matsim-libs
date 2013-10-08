package playground.vsp.pipeline;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.ScoringFunctionFactory;

public class ScoringTask implements ScenarioSinkSource, PersonArrivalEventHandler, PersonDepartureEventHandler, PersonStuckEventHandler, PersonMoneyEventHandler, ActivityStartEventHandler, ActivityEndEventHandler {

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
			eventsToScore = new EventsToScore(scenario, scoringFunctionFactory, learningRate);
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

	public void handleEvent(PersonArrivalEvent event) {
		eventsToScore.handleEvent(event);
	}

	public void handleEvent(PersonDepartureEvent event) {
		eventsToScore.handleEvent(event);
	}

	public void handleEvent(PersonMoneyEvent event) {
		eventsToScore.handleEvent(event);
	}

	public void handleEvent(PersonStuckEvent event) {
		eventsToScore.handleEvent(event);
	}

	public void reset(int iteration) {
		eventsToScore.reset(iteration);
	}
	
	

}
