package tutorial.example6EventsHandling;

import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
/**
 * This EventHandler implementation counts the travel time of
 * all agents and provides the average travel time per
 * agent.
 * @author dgrether
 *
 */
public class MyEventHandler2 implements LinkEnterEventHandler,
	LinkLeaveEventHandler, AgentArrivalEventHandler,
	AgentDepartureEventHandler{

	private double travelTime = 0.0;
	
	private int popSize;
	
	public MyEventHandler2(int popSize) {
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
