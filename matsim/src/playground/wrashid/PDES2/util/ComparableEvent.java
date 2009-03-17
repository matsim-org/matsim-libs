package playground.wrashid.PDES2.util;

import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.BasicEventImpl;
import org.matsim.events.LinkEnterEvent;
import org.matsim.events.LinkLeaveEvent;

import playground.wrashid.PDES2.Message;

public class ComparableEvent implements Comparable {
		private BasicEventImpl basicEvent;

		public ComparableEvent(BasicEventImpl be){
			this.basicEvent = be;
		}
		
		public BasicEventImpl getBasicEvent(){
			return basicEvent;
		}

		// for events with same time stamp: leave < arrival < departure < enter
		public int compareTo(Object obj) {
			ComparableEvent otherEvent= (ComparableEvent) obj;
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
