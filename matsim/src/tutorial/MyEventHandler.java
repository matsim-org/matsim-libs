package tutorial;

import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.LinkEnterEvent;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.events.handler.AgentArrivalEventHandler;
import org.matsim.events.handler.AgentDepartureEventHandler;
import org.matsim.events.handler.LinkEnterEventHandler;
import org.matsim.events.handler.LinkLeaveEventHandler;
/**
 * This EventHandler implementation counts the travel time of
 * all agents and provides the average travel time per
 * agent.
 * @author dgrether
 *
 */
public class MyEventHandler implements LinkEnterEventHandler,
		LinkLeaveEventHandler, AgentArrivalEventHandler,
		AgentDepartureEventHandler{

	private double travelTime = 0.0;

	private int popSize;

	public MyEventHandler(int popSize) {
		this.popSize = popSize;
	}

	public double getAverageTravelTime() {
		return this.travelTime / this.popSize;
	}

	public void reset(int iteration) {
		this.travelTime = 0.0;
	}

	public void handleEvent(LinkEnterEvent event) {
		this.travelTime -= event.time;
	}

	public void handleEvent(LinkLeaveEvent event) {
		this.travelTime += event.time;
	}

	public void handleEvent(AgentArrivalEvent event) {
		this.travelTime += event.time;
	}

	public void handleEvent(AgentDepartureEvent event) {
		this.travelTime -= event.time;
	}
}
