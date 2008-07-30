package playground.wrashid.PDES;

import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.PriorityBlockingQueue;

import org.matsim.gbl.Gbl;
import org.matsim.plans.Person;

public class MessageQueue {
	//TreeMap<String, Message> queue = new TreeMap<String, Message>();
	PriorityQueue<Message> queue1 = new PriorityQueue<Message>(10000);
	long counter = 0;

	// A Map (TreeMap, HashMap, etc.) can only contain one value for the same
	// key
	// This means, it is not possible to insert two messages into the queue,
	// keyed with
	// the same time
	// Inorder to prevent this, a counter is attached to the time, for
	// "assuring" uniqueness
	// (if more than 2^64 (approx. 10^19) events are scheduled for the same
	// time, than the uniqueness will not hold anymore...)
	// (although this case seems quite impossible, never the less it is
	// mentioned as a precondition for using this scheduler)
	synchronized void putMessage(Message m) {
		long rlong = counter++;
		// padding with zeros is needed, inorder to be able to use firstKey
		// property of the queue
		// String zeroPadded=feedWithZeros(m.getMessageArrivalTime());
		/*
		String zeroPadded = padWithZeros(m.getMessageArrivalTime());

		m.queueKey = zeroPadded.concat("." + (new Long(rlong)).toString());
		queue.put(m.queueKey, m);
*/
		
		queue1.add(m);

	}
	

	synchronized void removeMessage(Message m) {
		//queue.remove(m.queueKey);
		queue1.remove(m);
	}

	synchronized Message getNextMessage() {
		//Message m = queue.remove(queue.firstKey());
		Message m = queue1.poll();
		return m;
	}

	synchronized public boolean isEmpty() {
		//return !queue.isEmpty();
		return !queue1.isEmpty();
	}

}
