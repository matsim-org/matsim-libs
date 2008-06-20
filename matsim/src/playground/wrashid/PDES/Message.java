package playground.wrashid.PDES;

public abstract class Message {
	
	private static long messageCounter=0;
	public double messageArrivalTime=0;
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
	
	public double getMessageArrivalTime() {
		return messageArrivalTime;
	}
	
	public void setMessageArrivalTime(double messageArrivalTime) {
		this.messageArrivalTime = messageArrivalTime;
	}
	
	public abstract void printMessageLogString();
	

}
