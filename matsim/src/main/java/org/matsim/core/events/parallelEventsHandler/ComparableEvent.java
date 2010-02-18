package org.matsim.core.events.parallelEventsHandler;

import org.matsim.core.events.AgentArrivalEventImpl;
import org.matsim.core.events.AgentDepartureEventImpl;
import org.matsim.core.events.EventImpl;
import org.matsim.core.events.LinkEnterEventImpl;
import org.matsim.core.events.LinkLeaveEventImpl;

/*
 * Probably this class will be needed, when event handling will be adapted for parallel JDEQSim
 */
public class ComparableEvent implements Comparable<ComparableEvent> {
	private final EventImpl event;

	public ComparableEvent(EventImpl be){
		this.event = be;
	}

	public EventImpl getEvent(){
		return event;
	}

	// for events with same time stamp: leave < arrival < departure < enter
	public int compareTo(ComparableEvent otherEvent) {
		if (event.getTime()<otherEvent.getEvent().getTime()){
			return -1;
		} else if (event.getTime()>otherEvent.getEvent().getTime()){
			return 1;
		}
		// for equal time: use the following order
		if (event instanceof LinkLeaveEventImpl){
			return -1;
		}
		if (otherEvent.getEvent() instanceof LinkLeaveEventImpl){
			return 1;
		}
		if (event instanceof AgentArrivalEventImpl){
			return -1;
		}
		if (otherEvent.getEvent() instanceof AgentArrivalEventImpl){
			return 1;
		}
		if (event instanceof AgentDepartureEventImpl){
			return -1;
		}
		if (otherEvent.getEvent() instanceof AgentDepartureEventImpl){
			return 1;
		}
		if (event instanceof LinkEnterEventImpl){
			return -1;
		}
		if (otherEvent.getEvent() instanceof LinkEnterEventImpl){
			return 1;
		}


		return 0;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ComparableEvent)) {
			return false;
		}
		return this.event.equals(((ComparableEvent) obj).getEvent());
	}

	@Override
	public int hashCode() {
		return this.event.hashCode();
	}

}
