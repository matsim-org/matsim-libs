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


	synchronized void putMessage(Message m) {
		queue1.add(m);
	}
	

	synchronized void removeMessage(Message m) {
		queue1.remove(m);
	}

	synchronized Message getNextMessage() {
		Message m = queue1.poll();
		return m;
	}

	synchronized public boolean isEmpty() {
		return !queue1.isEmpty();
	}

}
