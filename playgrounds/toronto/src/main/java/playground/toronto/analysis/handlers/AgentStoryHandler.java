package playground.toronto.analysis.handlers;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.vehicles.Vehicle;

public class AgentStoryHandler implements TransitDriverStartsEventHandler, ActivityEndEventHandler, ActivityStartEventHandler, 
	PersonDepartureEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, PersonArrivalEventHandler{

	private final Id<Person> pid;
	private String story;
	private HashMap<Id<Vehicle>, Id<TransitLine>> vehicleLineMap;
	
	public AgentStoryHandler(Id<Person> pid){
		this.pid = pid;
		this.vehicleLineMap = new HashMap<>();
		this.story = "Story for agent \"" + this.pid.toString() + "\":";
	}
	public AgentStoryHandler(String pid){
		this.pid = Id.create(pid, Person.class);
		this.vehicleLineMap = new HashMap<>();
		this.story = "Story for agent \"" + this.pid.toString() + "\":";
	}
	
	public String getStory(){
		return this.story;
	}
	
	@Override
	public void reset(int iteration) {
		this.vehicleLineMap = new HashMap<>();
		this.story = "Story for agent \"" + this.pid.toString() + "\":";
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (event.getPersonId().equals(this.pid)){
			this.story += "\n" + Time.writeTime(event.getTime()) + ": AgentArrivalEvent [mode='" +
					event.getLegMode() + "',link='" + event.getLinkId().toString() + "']";
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getPersonId().equals(this.pid)){
			this.story += "\n" + Time.writeTime(event.getTime()) + ": AgentDepartureEvent [mode='" +
					event.getLegMode() + "',link='" + event.getLinkId() + "']";
		}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (event.getPersonId().equals(this.pid)){
			this.story += "\n" + Time.writeTime(event.getTime()) + ": ActivityStartEvent [type='" + 
					event.getActType() + "']";
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (this.pid.equals(event.getPersonId())){
			this.story += "\n" + Time.writeTime(event.getTime()) + ": ActivityEndEvent [type='" +
					event.getActType() + "']";
		}
	}
	@Override
	public void handleEvent(
			org.matsim.api.core.v01.events.PersonLeavesVehicleEvent event) {
		if (event.getPersonId().equals(this.pid)){
			Id<TransitLine> lineId = this.vehicleLineMap.get(event.getVehicleId());
			if (lineId != null){
				this.story += "\n" + Time.writeTime(event.getTime()) + ": PersonLeavesVehicleEvent [line='" +
						lineId.toString() + "']";
			}else {
				this.story += "\n" + Time.writeTime(event.getTime()) + ": PersonLeavesVehicleEvent [veh='" +
					event.getVehicleId() + "']";
			}
		}
	}
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (event.getPersonId().equals(this.pid)){
			Id<TransitLine> lineId = this.vehicleLineMap.get(event.getVehicleId());
			if (lineId != null){
				this.story += "\n" + Time.writeTime(event.getTime()) + ": PersonEntersVehicleEvent [line='" +
						lineId.toString() + "']";
			}else {
				this.story += "\n" + Time.writeTime(event.getTime()) + ": PersonEntersVehicleEvent [veh='" +
					event.getVehicleId() + "']";
			}
		}
	}
	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		this.vehicleLineMap.put(event.getVehicleId(), event.getTransitLineId());
	}
}
