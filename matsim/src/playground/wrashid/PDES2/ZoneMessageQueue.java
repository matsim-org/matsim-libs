package playground.wrashid.PDES2;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.matsim.network.Link;

import org.matsim.gbl.Gbl;
import org.matsim.population.Person;

public class ZoneMessageQueue {
	// The MessageQueue can only progress if messages from all incoming links (from neighbouring zones) have arrived,
	// and then only until the first ZoneBorderMessage or EnterRequestMessage
	
	private int zoneId=0;
	
	private PriorityQueue<Message> queue1 = new PriorityQueue<Message>();

	private volatile static int counter=0;
	private volatile static int incounter=0;
	public double arrivalTimeOfLastRemovedMessage=0;
	private Object bufferLock=new Object();
	private LinkedList<Message>[] addMessageBuffer=new LinkedList[SimulationParameters.numberOfMessageExecutorThreads];
	private LinkedList<Message>[] deleteMessageBuffer=new LinkedList[SimulationParameters.numberOfMessageExecutorThreads];
	public Lock lock=new ReentrantLock();
	
	public int numberOfIncomingLinks=0;
	public LinkedList<Link> tempIncomingLinks=new LinkedList<Link>();
	public LinkedList<Road> messagesArrivedFromRoads=new LinkedList<Road>(); //TODO: use some more efficient data structure than linked lists
	public HashMap<Road,Integer> numberOfQueuedMessages=new HashMap<Road,Integer>();
	
	
	synchronized public void putMessage(Message m) {
		assert(!queue1.contains(m)):"inconsistency";
		assert(m.messageArrivalTime>=0):"simulation time cannot be negative";
		
		assert(((Road)m.receivingUnit).getZoneId()==zoneId);
		
		
		if (m.isAcrossBorderMessage){
			incrementNumberOfQueuedMessages((Road)m.sendingUnit);
			if (!messagesArrivedFromRoads.contains(m.sendingUnit)){
				messagesArrivedFromRoads.add((Road)m.sendingUnit);
			}
		}
		
		queue1.add(m);

	
		if (!(m instanceof ZoneBorderMessage)){
			//System.out.println(m + " - " + m.messageArrivalTime + " - " + arrivalTimeOfLastRemovedMessage);
		}
		
		
		if (incounter % 10000 ==0){	
			//System.out.println("incounter:"+incounter);
		}
		incounter++;
	}
	
	public ZoneMessageQueue(int zoneId){
		for (int i=0;i<SimulationParameters.numberOfMessageExecutorThreads;i++){
			addMessageBuffer[i]=new LinkedList<Message>();
			deleteMessageBuffer[i]=new LinkedList<Message>();
		}
		this.zoneId=zoneId;
	}
	
	synchronized public double getArrivalTimeOfNextMessage(){
		return queue1.peek().messageArrivalTime;
	}
	

	public static int getCounter() {
		return counter;
	}

	synchronized public void removeMessage(Message m) {
		queue1.remove(m);
	}

	synchronized public Message getNextMessage() {

		//System.out.println(zoneId + " - " + messagesArrivedFromRoads.size() + " - " + numberOfIncomingLinks);
		if (messagesArrivedFromRoads.size()==numberOfIncomingLinks){
			//System.out.println("getNextMessage()");
			Message m = queue1.poll();
			
			if (m.isAcrossBorderMessage){
				decrementNumberOfQueuedMessages((Road)m.sendingUnit);
				if (getNumberOfQueueMessages((Road)m.sendingUnit)==0){
					messagesArrivedFromRoads.remove((Road)m.sendingUnit);
				}
			}
			
			arrivalTimeOfLastRemovedMessage=m.messageArrivalTime;
			return m;
		}

		
		return null;
	}

	// TODO: implement this properly!!!!!!!!!!
	synchronized public boolean isEmpty() {
		return false;
	}


	
	private void incrementNumberOfQueuedMessages(Road fromRoad){
		assert(fromRoad!=null);
		assert(numberOfQueuedMessages.get(fromRoad)!=null);
		numberOfQueuedMessages.put(fromRoad, numberOfQueuedMessages.get(fromRoad).intValue()+1);
	}
	
	private void decrementNumberOfQueuedMessages(Road fromRoad){
		numberOfQueuedMessages.put(fromRoad, numberOfQueuedMessages.get(fromRoad).intValue()-1);
	}
	
	private int getNumberOfQueueMessages(Road fromRoad){
		return numberOfQueuedMessages.get(fromRoad);
	}

	public void printSize(){
		System.out.println("zoneId:"+zoneId + "; size:"+queue1.size());
	}
	
	
	
	

}
