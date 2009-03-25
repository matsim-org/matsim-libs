package tutorial;

import org.matsim.core.events.AgentArrivalEvent;
import org.matsim.core.events.AgentDepartureEvent;
import org.matsim.core.events.LinkEnterEvent;
import org.matsim.core.events.LinkLeaveEvent;
import org.matsim.core.events.handler.AgentArrivalEventHandler;
import org.matsim.core.events.handler.AgentDepartureEventHandler;
import org.matsim.core.events.handler.LinkEnterEventHandler;
import org.matsim.core.events.handler.LinkLeaveEventHandler;
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
		this.travelTime -= event.getTime();
	}

	public void handleEvent(LinkLeaveEvent event) {
		this.travelTime += event.getTime();
	}

	public void handleEvent(AgentArrivalEvent event) {
		this.travelTime += event.getTime();
	}

	public void handleEvent(AgentDepartureEvent event) {
		this.travelTime -= event.getTime();
	}
}
