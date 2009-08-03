package org.matsim.core.events.parallelEventsHandler;

import org.matsim.core.events.AgentArrivalEvent;
import org.matsim.core.events.AgentDepartureEvent;
import org.matsim.core.events.BasicEventImpl;
import org.matsim.core.events.LinkEnterEvent;
import org.matsim.core.events.LinkLeaveEvent;

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
		if (basicEvent instanceof LinkLeaveEvent){
			return -1;
		}
		if (otherEvent.getBasicEvent() instanceof LinkLeaveEvent){
			return 1;
		}
		if (basicEvent instanceof AgentArrivalEvent){
			return -1;
		}
		if (otherEvent.getBasicEvent() instanceof AgentArrivalEvent){
			return 1;
		}
		if (basicEvent instanceof AgentDepartureEvent){
			return -1;
		}
		if (otherEvent.getBasicEvent() instanceof AgentDepartureEvent){
			return 1;
		}
		if (basicEvent instanceof LinkEnterEvent){
			return -1;
		}
		if (otherEvent.getBasicEvent() instanceof LinkEnterEvent){
			return 1;
		}
		
		
		return 0;
	}
	
	
}
