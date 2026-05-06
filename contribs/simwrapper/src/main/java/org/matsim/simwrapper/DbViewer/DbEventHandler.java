package org.matsim.simwrapper.DbViewer;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.core.api.experimental.events.EventsManager;


public final class DbEventHandler implements LinkEnterEventHandler, PersonDepartureEventHandler,

	PersonArrivalEventHandler, ActivityStartEventHandler, ActivityEndEventHandler {


	private final static Logger log = LogManager.getLogger(DbEventHandler.class);
	private final EventsManager eventsManager;

	@Inject
	DbEventHandler(EventsManager eventsManager) {
		this.eventsManager = eventsManager;
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		System.out.println("link enter event: " + event.getVehicleId().toString());
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		System.out.println("person departure event: " + event.getPersonId().toString());
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		System.out.println("person arrival event: " + event.getPersonId().toString());
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		System.out.println("person " + event.getPersonId().toString() + "ended activity " + event.getEventType().toString());
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		System.out.println("person " + event.getPersonId().toString() + "start activity " + event.getEventType().toString());

	}


//	@Override
//	public void handleEvent(LinkEnterEvent event) {
//		// define AgentState
//		AgentState state = agentStates.get(agentId);
//		state.linkSequence.add(event.getLinkId().toString());
//		agentStates.put(agentId, state);  // re-put for MapDB
//	}
//
//	@Override
//	public void handleEvent(ActivityStartEvent event) {
//		// leg complete — flush to both tables
//		AgentState state = agentStates.get(agentId);
//		String legId = agentId + "_" + legCounter;
//		String linkSequence = String.join("|", state.linkSequence);
//
//		// insert one row into leg_sequence
//		legSeqStmt.execute(legId, agentId, linkSequence);
//
//		// insert one row per link into link_traversal
//		for (String linkId : state.linkSequence) {
//			traversalStmt.execute(linkId, agentId, legId, hour, mode, ...);
//		}
//	}
}
