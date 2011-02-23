package air;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;

public class SfFlightTimeEventHandler implements AgentArrivalEventHandler,
		AgentDepartureEventHandler {
	
	public Map<Id, Double> arrivalTime = new HashMap<Id, Double>();
	public Map<Id, Double> departureTime = new HashMap<Id, Double>();
	public Map<Id, Double> flightTime = new HashMap<Id, Double>();


	public SfFlightTimeEventHandler() {
		this.arrivalTime = null;
		this.departureTime = null;
		this.flightTime = null;
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		this.arrivalTime.put(event.getPersonId(), event.getTime());
		this.flightTime.put(event.getPersonId(), event.getTime()-this.departureTime.get(event.getPersonId()));
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		this.departureTime.put(event.getPersonId(), event.getTime());
	}
	
	public Map<Id, Double> returnArrival() {
		return this.arrivalTime;	
	}

	public Map<Id, Double> returnDeparture() {
		return this.arrivalTime;		
	}
	
	public Map<Id, Double> returnFlightTime() {
		return this.flightTime;		
	}
	
}
