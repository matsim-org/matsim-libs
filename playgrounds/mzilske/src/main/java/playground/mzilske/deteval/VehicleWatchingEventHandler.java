package playground.mzilske.deteval;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;

public class VehicleWatchingEventHandler implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, AgentStuckEventHandler {

	private static Logger logger = Logger.getLogger(VehicleWatchingEventHandler.class);
	
	Map<Id, Id> vehicle2Person = new HashMap<Id, Id>();
	
	Map<Id, List<Integer>> person2stuckIterations = new HashMap<Id, List<Integer>>();
	
	int iteration = 0;
	
	int nStuck = 0;
	
	@Override
	public void reset(int iteration) {
		logger.info("Stuck agents: " + nStuck);
		nStuck = 0;
		this.iteration = iteration;
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if (!event.getPersonId().equals(vehicle2Person.get(event.getVehicleId()))) {
			logger.warn("Person " + event.getPersonId() + " exits vehicle which is occupied by " + vehicle2Person.get(event.getVehicleId()));
		} else {
			logger.info("Person " + event.getPersonId() + " exits vehicle correctly.");
		}
		vehicle2Person.put(event.getVehicleId(), null);
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (vehicle2Person.get(event.getVehicleId()) != null) {
			logger.warn("Person " + event.getPersonId() + " enters vehicle which is occupied by " + vehicle2Person.get(event.getVehicleId()));
		}
		vehicle2Person.put(event.getVehicleId(), event.getPersonId());
	}

	@Override
	public void handleEvent(AgentStuckEvent event) {
		List<Integer> stuckHistory = person2stuckIterations.get(event.getPersonId());
		if (stuckHistory == null) {
			stuckHistory = new ArrayList<Integer>();
			person2stuckIterations.put(event.getPersonId(), stuckHistory);
		}
		stuckHistory.add(iteration);
	}

	public void dump() {
		for (Entry<Id, List<Integer>> entry : person2stuckIterations.entrySet()) {
			List<Integer> history = entry.getValue();
			logger.info("Agent " + entry.getKey() + " was stuck in iterations " + history.toString());
		}
		
	}
	
}
