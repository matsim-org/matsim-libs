package playground.wrashid.PDES2;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.matsim.network.Link;

import org.matsim.gbl.Gbl;
import org.matsim.population.Person;

import playground.wrashid.PDES.util.ConcurrentListMPSC;

public class ZoneMessageQueue {
	// The MessageQueue can only progress if messages from all incoming links
	// (from neighbouring zones) have arrived,
	// and then only until the first ZoneBorderMessage or EnterRequestMessage

	private int zoneId = 0;
	public double simTime = 0;
	private PriorityQueue<Message> queue1 = new PriorityQueue<Message>();

	public int counter = 0;
	public int incounter = 0;
	public double arrivalTimeOfLastRemovedMessage = 0;
	private Object bufferLock = new Object();
	// private LinkedList<Message>[] addMessageBuffer = new
	// LinkedList[SimulationParameters.numberOfMessageExecutorThreads];
	// private LinkedList<Message>[] deleteMessageBuffer = new
	// LinkedList[SimulationParameters.numberOfMessageExecutorThreads];
	public Lock lock = new ReentrantLock();
	public ConcurrentListMPSC<Message> buffer = new ConcurrentListMPSC<Message>(
			SimulationParameters.numberOfMessageExecutorThreads);

	public int numberOfIncomingLinks = 0;
	public LinkedList<Link> tempIncomingLinks = new LinkedList<Link>();
	// public HashMap<Road, Integer> messagesArrivedFromRoads = new
	// HashMap<Road, Integer>(); // TODO:
	// use
	// some
	// more
	// efficient
	// data
	// structure
	// than
	// linked
	// lists
	private int messagesArrivedFromRoads;
	public ConcurrentMap<Road, Integer> numberOfQueuedMessages[] = new ConcurrentHashMap[SimulationParameters.numberOfMessageExecutorThreads];
	private Message tmpLastRemovedMessage = null;
	private final Integer zero = new Integer(0);

	public void putMessage(Message m) {
		// the following assertions need to be commented out, because this
		// method is not synchronized
		// anymore and some of them would lead to
		// ConcurrentModificationException
		
		// assert (!queue1.contains(m)) : "inconsistency";
		// assert (m.messageArrivalTime >= 0) : "simulation time cannot be
		// negative";

		// assert (((Road) m.receivingUnit).getZoneId() == zoneId);

		incounter++;
			if (m.isAcrossBorderMessage) {
				//System.out.println(m);
				
				if (incrementNumberOfQueuedMessages((Road) m.sendingUnit)==1) {
					// if new message arrived for an incoming road
					// messagesArrivedFromRoads.put((Road) m.sendingUnit, zero);
					synchronized (lock) {
						messagesArrivedFromRoads++;
					}
				}
				if (!(m instanceof ZoneBorderMessage)) {
					// System.out.println(m + " - " + m.messageArrivalTime + " -
					// "
					// + arrivalTimeOfLastRemovedMessage);
				}
			}
		

		try {
			buffer.add(m, MessageExecutor.getThreadId());
		} catch (Exception e) {
			buffer.add(m, 0);
		}
		// queue1.add(m);

		if (!(m instanceof ZoneBorderMessage)) {
			// System.out.println(m + " - " + m.messageArrivalTime + " - " +
			// arrivalTimeOfLastRemovedMessage);
		}

		if (incounter % 10000 == 0) {
			// System.out.println("incounter:"+incounter);
		}
		

		// System.out.println(zoneId + " - " + messagesArrivedFromRoads.size() +
		// " - " + numberOfIncomingLinks);
		// System.out.println(queue1.size());
	}

	public ZoneMessageQueue(int zoneId) {
		for (int i = 0; i < SimulationParameters.numberOfMessageExecutorThreads; i++) {
			// addMessageBuffer[i] = new LinkedList<Message>();
			// deleteMessageBuffer[i] = new LinkedList<Message>();
			numberOfQueuedMessages[i] = new ConcurrentHashMap<Road, Integer>(1000); 
		}
		this.zoneId = zoneId;
	}

	synchronized public double getArrivalTimeOfNextMessage() {
		return queue1.peek().messageArrivalTime;
	}

	public int getCounter() {
		return counter;
	}

	synchronized public void removeMessage(Message m) {
		if (zoneId!=MessageExecutor.getThreadId()){
			System.out.println("concurrency occured");
		}
		emptyBuffer();
		
		queue1.removeAll(Collections.singletonList(m));
		// queue1.remove(m); => this does not function properly for priotyqueues
		// as intended
		
	}

	synchronized public Message getNextMessage() {

		Message m = null;
		// System.out.println(zoneId + " - " + messagesArrivedFromRoads.size() +
		// " - " + numberOfIncomingLinks);
		if (messagesArrivedFromRoads == numberOfIncomingLinks) {
			emptyBuffer();
			
			// - needed in particular, if used with 1 cpu (isEmpty)
			// - the second case: multiple cpu, it must be  ensured, that the inf+ timer messages are not removed
			if (queue1.isEmpty() || queue1.peek().messageArrivalTime>SimulationParameters.maxSimulationLength) {
				return null;
			}
			
			
			m = queue1.poll();

			// needed in particular, if used with 1 cpu
			if (queue1.isEmpty()) {
				return null;
				//System.out.println(messagesArrivedFromRoads);
			}

			// if (queue1.isEmpty() || m.isAcrossBorderMessage){

			// m = queue1.peek();
			// }

			if (m.isAcrossBorderMessage) {
				
					if (decrementNumberOfQueuedMessages((Road) m.sendingUnit) == 0) {
						synchronized (lock) {
						messagesArrivedFromRoads--;
					}
				}
			}

			// queue1.remove();

			arrivalTimeOfLastRemovedMessage = m.messageArrivalTime;
			simTime = m.messageArrivalTime;
			tmpLastRemovedMessage = m;
			return m;
		}
		if (incounter % 100000 == 0) {
			// System.out.println(incounter);
			// System.out.println(zoneId + " - " +
			// messagesArrivedFromRoads.size() + " - " + numberOfIncomingLinks);
			// System.out.println(zoneId + " - " +
			// arrivalTimeOfLastRemovedMessage);
			// if (tmpLastRemovedMessage!=null){
			// System.out.println(((Road)tmpLastRemovedMessage.sendingUnit).getLink().getLength());
			// }
		}

		return null;
	}

	
	public boolean isEmpty() {
		// the first condition is for single cpu
		// the second condition of multiple cpu
		if (queue1.isEmpty() || queue1.peek().messageArrivalTime>SimulationParameters.maxSimulationLength){
			return true;
		}
		return false;
	}

	private int incrementNumberOfQueuedMessages(Road fromRoad) {
		//int result=0;
		//int zoneId=fromRoad.getZoneId();
		//synchronized (numberOfQueuedMessages[zoneId]){
		//	result=numberOfQueuedMessages[zoneId].get(fromRoad).intValue() + 1;
		//	numberOfQueuedMessages[zoneId].put(fromRoad, result);
		//}
		//return result;
		int zId=fromRoad.getZoneId();

		    Integer oldVal, newVal;
		    do {
		      oldVal = numberOfQueuedMessages[zId].get(fromRoad);
		      newVal = (oldVal + 1);
		    } while (!numberOfQueuedMessages[zId].replace(fromRoad, oldVal, newVal));
		return newVal;
	}

	private int decrementNumberOfQueuedMessages(Road fromRoad) {
		int zId=fromRoad.getZoneId();
		Integer oldVal, newVal;
	    do {
	      oldVal = numberOfQueuedMessages[zId].get(fromRoad);
	      newVal = (oldVal - 1);
	    } while (!numberOfQueuedMessages[zId].replace(fromRoad, oldVal, newVal));
	    return newVal;
	}



	public void printSize() {
		System.out.println("zoneId:" + zoneId + "; size:" + queue1.size());
	}

	private void emptyBuffer() {
		Message m = buffer.remove();
		while (m != null) {
			queue1.add(m);
			m = buffer.remove();
		}
	}

}
