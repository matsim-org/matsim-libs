package playground.wrashid.PDES;

public class Message {
	
	private static long messageCounter=0;
	public long messageArrivalTime=0;
	public SimUnit sendingUnit;
	public SimUnit receivingUnit;
	public long messageType;
	public long messageId;
	
	// all inheriting or extending modules must
	// invoke this Constructer first
	public Message() {
		messageId=messageCounter;
		messageCounter++;
	}
	
	public long getMessageArrivalTime() {
		return messageArrivalTime;
	}
	
	public void setMessageArrivalTime(long messageArrivalTime) {
		this.messageArrivalTime = messageArrivalTime;
	}

}
