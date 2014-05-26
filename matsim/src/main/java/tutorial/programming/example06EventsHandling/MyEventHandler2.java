package tutorial.programming.example06EventsHandling;

import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
/**
 * This EventHandler implementation counts the travel time of
 * all agents and provides the average travel time per
 * agent.
 * Actually, handling Departures and Arrivals should be sufficient for this (may 2014)
 * @author dgrether
 *
 */
public class MyEventHandler2 implements LinkEnterEventHandler,
	LinkLeaveEventHandler, PersonArrivalEventHandler,
	PersonDepartureEventHandler{

	private double travelTime = 0.0;
	
	private int popSize;
	
	public MyEventHandler2(int popSize) {
		this.popSize = popSize;
	}

	public double getAverageTravelTime() {
		return this.travelTime / this.popSize;
	}

	@Override
	public void reset(int iteration) {
		this.travelTime = 0.0;
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		this.travelTime -= event.getTime();
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		this.travelTime += event.getTime();
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		this.travelTime += event.getTime();
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		this.travelTime -= event.getTime();
	}
}
