package tutorial.example6EventsHandling;

import org.matsim.api.basic.v01.events.BasicAgentArrivalEvent;
import org.matsim.api.basic.v01.events.BasicAgentDepartureEvent;
import org.matsim.api.basic.v01.events.BasicLinkEnterEvent;
import org.matsim.api.basic.v01.events.BasicLinkLeaveEvent;
import org.matsim.api.basic.v01.events.handler.BasicAgentArrivalEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicAgentDepartureEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicLinkEnterEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicLinkLeaveEventHandler;
/**
 * This EventHandler implementation counts the travel time of
 * all agents and provides the average travel time per
 * agent.
 * @author dgrether
 *
 */
public class MyEventHandler2 implements BasicLinkEnterEventHandler,
	BasicLinkLeaveEventHandler, BasicAgentArrivalEventHandler,
	BasicAgentDepartureEventHandler{

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

	public void handleEvent(BasicLinkEnterEvent event) {
		this.travelTime -= event.getTime();
	}

	public void handleEvent(BasicLinkLeaveEvent event) {
		this.travelTime += event.getTime();
	}

	public void handleEvent(BasicAgentArrivalEvent event) {
		this.travelTime += event.getTime();
	}

	public void handleEvent(BasicAgentDepartureEvent event) {
		this.travelTime -= event.getTime();
	}
}
