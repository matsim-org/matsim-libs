package tutorial;

import org.matsim.events.EventAgentArrival;
import org.matsim.events.EventAgentDeparture;
import org.matsim.events.EventLinkEnter;
import org.matsim.events.EventLinkLeave;
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
		System.out.println();
		System.out.println("Average travel time in iteration " + iteration + " is: "
				+ getAverageTravelTime());
		System.out.println();
		this.travelTime = 0.0;
	}

	public void handleEvent(EventLinkEnter event) {
		this.travelTime -= event.time;
	}

	public void handleEvent(EventLinkLeave event) {
		this.travelTime += event.time;
	}

	public void handleEvent(EventAgentArrival event) {
		this.travelTime += event.time;
	}

	public void handleEvent(EventAgentDeparture event) {
		this.travelTime -= event.time;
	}
}
