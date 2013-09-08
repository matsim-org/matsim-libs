package playground.wrashid.parkingSearch.ppSim.jdepSim;

public class Message implements Comparable<Message> {

	int messageArrivalTime;
	int priority;
	
	public int compareTo(Message otherMessage) {
		
		if (messageArrivalTime > otherMessage.messageArrivalTime) {
			return 1;
		} else if (messageArrivalTime < otherMessage.messageArrivalTime) {
			return -1;
		} else {
			// higher priority means for a queue, that it comes first
			return otherMessage.priority - priority;
		}
	}

}
