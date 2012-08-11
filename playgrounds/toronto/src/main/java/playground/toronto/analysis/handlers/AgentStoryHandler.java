package playground.toronto.analysis.handlers;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.utils.misc.Time;

public class AgentStoryHandler implements ActivityEndEventHandler, ActivityStartEventHandler, 
	AgentDepartureEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, AgentArrivalEventHandler{

	Id pid;
	String story;
	int eventsHandled;
	
	public AgentStoryHandler(Id pid){
		this.pid = pid;
		this.story = "Story for agent \"" + this.pid.toString() + "\":";
		this.eventsHandled = 0;
	}
	
	public String getStory(){
		return this.story;
	}
	
	@Override
	public void reset(int iteration) {
		this.eventsHandled = 0;
		this.story = "Story for agent \"" + this.pid.toString() + "\":";
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		if (event.getPersonId().equals(this.pid)){
			this.story += "\n" + Time.writeTime(event.getTime()) + ": AgentArrivalEvent [mode='" +
					event.getLegMode() + "',link='" + event.getLinkId().toString() + "']";
			this.eventsHandled++;
		}
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if (event.getPersonId().equals(this.pid)){
			this.story += "\n" + Time.writeTime(event.getTime()) + ": PersonLeavesVehicleEvent [veh='" + 
					event.getVehicleId() + "']";
			this.eventsHandled++;
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {		
		if (event.getPersonId().equals(this.pid)){
			this.story += "\n" + Time.writeTime(event.getTime()) + ": PersonEntersVehicleEvent [veh='" +
					event.getVehicleId() + "']";
			this.eventsHandled++;
		}
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		if (event.getPersonId().equals(this.pid)){
			this.story += "\n" + Time.writeTime(event.getTime()) + ": AgentDepartureEvent [mode='" +
					event.getLegMode() + "',link='" + event.getLinkId() + "']";
			this.eventsHandled++;
		}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (event.getPersonId().equals(this.pid)){
			this.story += "\n" + Time.writeTime(event.getTime()) + ": ActivityStartEvent [type='" + 
					event.getActType() + "']";
			this.eventsHandled++;
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (this.pid.equals(event.getPersonId())){
			this.story += "\n" + Time.writeTime(event.getTime()) + ": ActivityEndEvent [type='" +
					event.getActType() + "']";
			this.eventsHandled++;
		}
	}
}
