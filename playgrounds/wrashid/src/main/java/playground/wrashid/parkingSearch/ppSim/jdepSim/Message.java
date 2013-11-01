package playground.wrashid.parkingSearch.ppSim.jdepSim;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;

import playground.wrashid.parkingSearch.ppSim.ttmatrix.TTMatrix;

public abstract class Message implements Comparable<Message> {

	private double messageArrivalTime;
	int priority;
	static MessageQueue messageQueue;
	public static MessageQueue getMessageQueue() {
		return messageQueue;
	}

	private Person person;
	public static EventsManager eventsManager;
	public static TTMatrix ttMatrix;
	
	
	public static final int PRIORITY_END_ACT_MESSAGE = 250;
	public static final int PRIORITY_DEPARTUARE_MESSAGE = 200;
	public static final int PRIORITY_ENTER_ROAD_MESSAGE = 150;
	public static final int PRIORITY_LEAVE_ROAD_MESSAGE = 100;
	public static final int PRIORITY_ARRIVAL_MESSAGE = 50;
	public static final int PRIORITY_START_ACT_MESSAGE = 0;
	
	
	public int compareTo(Message otherMessage) {
		if (getMessageArrivalTime() > otherMessage.getMessageArrivalTime()) {
			return 1;
		} else if (getMessageArrivalTime() < otherMessage.getMessageArrivalTime()) {
			return -1;
		} else {
			// higher priority means for a queue, that it comes first
			return otherMessage.priority - priority;
		}
	}

	public double getMessageArrivalTime() {
		return messageArrivalTime;
	}

	public void setMessageArrivalTime(double messageArrivalTime) {
		this.messageArrivalTime = messageArrivalTime;
	}

	public abstract void processEvent();
	
	public void setMessageQueue(MessageQueue messageQueue){
		this.messageQueue=messageQueue;
	}
	
	public void setEventsManager(EventsManager eventsManager){
		this.eventsManager=eventsManager;
	}
	
	public void initPerson(Person person){
		this.setPerson(person);
	}

	public Person getPerson() {
		return person;
	}

	public void setPerson(Person person) {
		this.person = person;
	}

}
