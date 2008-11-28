package playground.wrashid.DES;

import java.util.Collections;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.TreeMap;

import org.matsim.gbl.Gbl;
import org.matsim.population.Person;

public class MessageQueue {
	private PriorityQueue<Message> queue1 = new PriorityQueue<Message>();

	/*
	 * Putting a message into the queue
	 */
	public void putMessage(Message m) {
		queue1.add(m);
	}
	
	/*
	 * Remove the message from the queue and discard it.
	 * - queue1.remove(m) does not function, because it discards
	 * all message with the same priority as m from the queue.
	 * - This java api bug is reported at: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6207984 
	 */
	public void removeMessage(Message m) {
		queue1.removeAll(Collections.singletonList(m));
	}

	/*
	 * get the first message in the queue (with least time stamp)
	 */
	public Message getNextMessage() {
		Message m = queue1.poll();
		return m;
	}

	public boolean isEmpty() {
		return queue1.isEmpty();
	}
	
	public int getQueueSize(){
		return queue1.size();
	}



}
