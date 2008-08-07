package tutorial;

import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.LinkEnterEnter;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.events.handler.EventHandlerAgentArrivalI;
import org.matsim.events.handler.EventHandlerAgentDepartureI;
import org.matsim.events.handler.EventHandlerLinkEnterI;
import org.matsim.events.handler.EventHandlerLinkLeaveI;
/**
 * This EventHandler implementation counts the travel time of
 * all agents and provides the average travel time per
 * agent.
 * @author dgrether
 *
 */
public class MyEventHandler implements EventHandlerLinkEnterI,
		EventHandlerLinkLeaveI, EventHandlerAgentArrivalI,
		EventHandlerAgentDepartureI{

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

	public void handleEvent(LinkEnterEnter event) {
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
