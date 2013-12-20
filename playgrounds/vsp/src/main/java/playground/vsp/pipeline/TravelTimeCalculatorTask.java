package playground.vsp.pipeline;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactory;

public class TravelTimeCalculatorTask implements ScenarioSinkSource, LinkEnterEventHandler, LinkLeaveEventHandler, PersonArrivalEventHandler, PersonStuckEventHandler {

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

	public void handleEvent(PersonArrivalEvent event) {
		travelTimeCalculator.handleEvent(event);
	}

	public void handleEvent(PersonStuckEvent event) {
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

	public TravelTime getTravelTimeCalculator() {
		return travelTimeCalculator.getLinkTravelTimes();
	}

	public Object getTravelTime() {
		return travelTimeCalculator;
	}

}
