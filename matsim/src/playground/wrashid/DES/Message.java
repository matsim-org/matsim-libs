package playground.wrashid.DES;

public abstract class Message implements Comparable {
	
	//private static long messageCounter=0;
	public double messageArrivalTime=0;
	public SimUnit sendingUnit;
	public SimUnit receivingUnit;
	public long messageType;
	//public long messageId;
	//public String queueKey=""; // only used because of implementation convenience (might be removed in future, if not needed)
	
	
	public Message() {
	}
	
	public double getMessageArrivalTime() {
		return messageArrivalTime;
	}
	
	public void setMessageArrivalTime(double messageArrivalTime) {
		this.messageArrivalTime = messageArrivalTime;
	}
	
	public abstract void printMessageLogString();

	/*
	 * The comparison is done according to the message arrival Time.
	 * If the time is equal of two messages, one is bigger then the other randomly.
	 */
	public int compareTo(Object obj){
		Message otherMessage= (Message) obj;
		if (messageArrivalTime>otherMessage.messageArrivalTime){
			return 1;
		} else if (messageArrivalTime<otherMessage.messageArrivalTime) {
			return -1;
		} else {
			return -1;
		}
	}

}
