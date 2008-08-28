package playground.wrashid.PDES.util;

import org.matsim.events.BasicEvent;

import playground.wrashid.PDES2.Message;

public class ComparableEvent implements Comparable {
		private BasicEvent basicEvent;

		public ComparableEvent(BasicEvent be){
			this.basicEvent = be;
		}
		
		public BasicEvent getBasicEvent(){
			return basicEvent;
		}

		@Override
		public int compareTo(Object obj) {
			ComparableEvent otherEvent= (ComparableEvent) obj;
			if (basicEvent.time<otherEvent.getBasicEvent().time){
				return -1;
			} 
			return 0;
		}
		
		
}
