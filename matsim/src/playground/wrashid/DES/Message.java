package playground.wrashid.DES;

public abstract class Message implements Comparable<Message> {

	private double messageArrivalTime = 0;
	private SimUnit sendingUnit;
	private SimUnit receivingUnit;
	protected int priority = 0;

	public Message() {
	}

	public double getMessageArrivalTime() {
		return messageArrivalTime;
	}

	public void setMessageArrivalTime(double messageArrivalTime) {
		this.messageArrivalTime = messageArrivalTime;
	}

	public abstract void processEvent();

	/*
	 * The comparison is done according to the message arrival Time. If the time
	 * is equal of two messages, then the priority of the messages is compared
	 */
	public int compareTo(Message otherMessage) {
		if (messageArrivalTime > otherMessage.messageArrivalTime) {
			return 1;
		} else if (messageArrivalTime < otherMessage.messageArrivalTime) {
			return -1;
		} else {
			// higher priority means for a queue, that it comes first
			return otherMessage.getPriority() - priority;
		}
	}

	public int getPriority() {
		return priority;
	}

	public SimUnit getSendingUnit() {
		return sendingUnit;
	}

	public void setSendingUnit(SimUnit sendingUnit) {
		this.sendingUnit = sendingUnit;
	}

	public SimUnit getReceivingUnit() {
		return receivingUnit;
	}

	public void setReceivingUnit(SimUnit receivingUnit) {
		this.receivingUnit = receivingUnit;
	}
	
	public abstract void handleMessage();

}
