package playground.benjamin.events;

import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
/**
 * This event handler prints some event information to the console.
 * @author dgrether
 *
 */
public class MyEventHandler1 implements LinkEnterEventHandler,
	LinkLeaveEventHandler, AgentArrivalEventHandler,
	AgentDepartureEventHandler{

	public void reset(int iteration) {
		System.out.println("reset...");
	}


	public void handleEvent(LinkEnterEvent event) {
		System.out.println("LinkEnterEvent");
		System.out.println("Time: " + event.getTime());
		System.out.println("LinkId: " + event.getLinkId());
		System.out.println("PersonId: " + event.getPersonId());
		System.out.println("==================================");
	}

	public void handleEvent(LinkLeaveEvent event) {
		System.out.println("LinkLeaveEvent");
		System.out.println("Time: " + event.getTime());
		System.out.println("LinkId: " + event.getLinkId());
		System.out.println("PersonId: " + event.getPersonId());
		System.out.println("==================================");
	}

	public void handleEvent(AgentArrivalEvent event) {
		System.out.println("AgentArrivalEvent");
		System.out.println("Time: " + event.getTime());
		System.out.println("LinkId: " + event.getLinkId());
		System.out.println("PersonId: " + event.getPersonId());
		System.out.println("==================================");
	}

	public void handleEvent(AgentDepartureEvent event) {
		System.out.println("AgentDepartureEvent");
		System.out.println("Time: " + event.getTime());
		System.out.println("LinkId: " + event.getLinkId());
		System.out.println("PersonId: " + event.getPersonId());
		System.out.println("==================================");
	}
}
