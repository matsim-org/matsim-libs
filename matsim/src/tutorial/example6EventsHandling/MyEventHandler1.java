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
 * This event handler prints some event information to the console.
 * @author dgrether
 *
 */
public class MyEventHandler1 implements BasicLinkEnterEventHandler,
	BasicLinkLeaveEventHandler, BasicAgentArrivalEventHandler,
	BasicAgentDepartureEventHandler{

	public void reset(int iteration) {
		System.out.println("reset...");
	}


	public void handleEvent(BasicLinkEnterEvent event) {
		System.out.println("BasicLinkEnterEvent");
		System.out.println("Time: " + event.getTime());
		System.out.println("LinkId: " + event.getLinkId());
		System.out.println("PersonId: " + event.getPersonId());
	}

	public void handleEvent(BasicLinkLeaveEvent event) {
		System.out.println("BasicLinkLeaveEvent");
		System.out.println("Time: " + event.getTime());
		System.out.println("LinkId: " + event.getLinkId());
		System.out.println("PersonId: " + event.getPersonId());
	}

	public void handleEvent(BasicAgentArrivalEvent event) {
		System.out.println("BasicAgentArrivalEvent");
		System.out.println("Time: " + event.getTime());
		System.out.println("LinkId: " + event.getLinkId());
		System.out.println("PersonId: " + event.getPersonId());
	}

	public void handleEvent(BasicAgentDepartureEvent event) {
		System.out.println("BasicAgentDepartureEvent");
		System.out.println("Time: " + event.getTime());
		System.out.println("LinkId: " + event.getLinkId());
		System.out.println("PersonId: " + event.getPersonId());
	}
}
