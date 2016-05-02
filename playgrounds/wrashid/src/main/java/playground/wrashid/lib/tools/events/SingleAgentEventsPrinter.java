package playground.wrashid.lib.tools.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.handler.TeleportationArrivalEventHandler;

public class SingleAgentEventsPrinter implements ActivityEndEventHandler, ActivityStartEventHandler, PersonArrivalEventHandler, 
PersonDepartureEventHandler, PersonStuckEventHandler, PersonMoneyEventHandler, 
VehicleEntersTrafficEventHandler , LinkEnterEventHandler, LinkLeaveEventHandler, TeleportationArrivalEventHandler{

	private final Id<Person> filterEventsForAgentId;

	public SingleAgentEventsPrinter(Id<Person> agentId){
		this.filterEventsForAgentId = agentId;
	}
	
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (event.getDriverId().equals(filterEventsForAgentId)){
			System.out.println(event.toString());
		}
	}


	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (event.getDriverId().equals(filterEventsForAgentId)){
			System.out.println(event.toString());
		}
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		if (event.getPersonId().equals(filterEventsForAgentId)){
			System.out.println(event.toString());
		}
	}

	@Override
	public void handleEvent(PersonMoneyEvent event) {
		if (event.getPersonId().equals(filterEventsForAgentId)){
			System.out.println(event.toString());
		}			
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		if (event.getPersonId().equals(filterEventsForAgentId)){
			System.out.println(event.toString());
		}			
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getPersonId().equals(filterEventsForAgentId)){
			System.out.println(event.toString());
		}			
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (event.getPersonId().equals(filterEventsForAgentId)){
			System.out.println(event.toString());
		}			
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (event.getPersonId().equals(filterEventsForAgentId)){
			System.out.println(event.toString());
		}		
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (event.getPersonId().equals(filterEventsForAgentId)){
			System.out.println(event.toString());
		}		
	}

	@Override
	public void handleEvent(TeleportationArrivalEvent event) {
		if (event.getPersonId().equals(filterEventsForAgentId)){
			System.out.println(event.toString());
		}
	}
	
}
