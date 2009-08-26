package org.matsim.core.events.parallelEventsHandler;

import org.matsim.core.events.AgentArrivalEventImpl;
import org.matsim.core.events.AgentDepartureEventImpl;
import org.matsim.core.events.BasicEventImpl;
import org.matsim.core.events.LinkEnterEventImpl;
import org.matsim.core.events.LinkLeaveEventImpl;

/*
 * Probably this class will be needed, when event handling will be adapted for parallel JDEQSim
 */
public class ComparableEvent implements Comparable<ComparableEvent> {
	private BasicEventImpl basicEvent;

	public ComparableEvent(BasicEventImpl be){
		this.basicEvent = be;
	}
	
	public BasicEventImpl getBasicEvent(){
		return basicEvent;
	}

	// for events with same time stamp: leave < arrival < departure < enter
	public int compareTo(ComparableEvent otherEvent) {
		if (basicEvent.getTime()<otherEvent.getBasicEvent().getTime()){
			return -1;
		} else if (basicEvent.getTime()>otherEvent.getBasicEvent().getTime()){
			return 1;
		}
		// for equal time: use the following order
		if (basicEvent instanceof LinkLeaveEventImpl){
			return -1;
		}
		if (otherEvent.getBasicEvent() instanceof LinkLeaveEventImpl){
			return 1;
		}
		if (basicEvent instanceof AgentArrivalEventImpl){
			return -1;
		}
		if (otherEvent.getBasicEvent() instanceof AgentArrivalEventImpl){
			return 1;
		}
		if (basicEvent instanceof AgentDepartureEventImpl){
			return -1;
		}
		if (otherEvent.getBasicEvent() instanceof AgentDepartureEventImpl){
			return 1;
		}
		if (basicEvent instanceof LinkEnterEventImpl){
			return -1;
		}
		if (otherEvent.getBasicEvent() instanceof LinkEnterEventImpl){
			return 1;
		}
		
		
		return 0;
	}
	
	
}
