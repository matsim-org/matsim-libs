package playground.mzilske.pipeline;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorConfigGroup;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactory;

public class TravelTimeCalculatorTask implements ScenarioSinkSource, LinkEnterEventHandler, LinkLeaveEventHandler, AgentArrivalEventHandler, AgentStuckEventHandler {

	private TravelTimeCalculatorFactory travelTimeCalculatorFactory;
	
	private TravelTimeCalculator travelTimeCalculator;

	private TravelTimeCalculatorConfigGroup config;

	private ScenarioSink sink;

	public TravelTimeCalculatorTask(TravelTimeCalculatorConfigGroup group, TravelTimeCalculatorFactory travelTimeCalculatorFactory) {
		this.config = group;
		this.travelTimeCalculatorFactory = travelTimeCalculatorFactory;
	}

	@Override
	public void setSink(ScenarioSink sink) {
		this.sink = sink;
	}
	
	public void initialize(Scenario scenario) {
		if (travelTimeCalculator == null) {
			travelTimeCalculator = travelTimeCalculatorFactory.createTravelTimeCalculator(scenario.getNetwork(), config);
		}
		sink.initialize(scenario);
	}

	@Override
	public void process(Scenario scenario) {
		sink.process(scenario);
	}

	public void handleEvent(AgentArrivalEvent event) {
		travelTimeCalculator.handleEvent(event);
	}

	public void handleEvent(AgentStuckEvent event) {
		travelTimeCalculator.handleEvent(event);
	}

	public void handleEvent(LinkEnterEvent e) {
		travelTimeCalculator.handleEvent(e);
	}

	public void handleEvent(LinkLeaveEvent e) {
		travelTimeCalculator.handleEvent(e);
	}

	public void reset(int iteration) {
		travelTimeCalculator.reset(iteration);
	}

	public PersonalizableTravelTime getTravelTimeCalculator() {
		return travelTimeCalculator;
	}

	public Object getTravelTime() {
		return travelTimeCalculator;
	}

}
