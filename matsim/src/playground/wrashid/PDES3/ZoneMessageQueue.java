package playground.wrashid.PDES3;

import java.util.Collections;
import java.util.LinkedList;
import java.util.PriorityQueue;

import org.matsim.mobsim.jdeqsim.Message;


public class ZoneMessageQueue {

	private PriorityQueue<Message> queue = new PriorityQueue<Message>();
	private LinkedList<Message> buffer = new LinkedList<Message>();
	private LinkedList<Message> deleteBuffer = new LinkedList<Message>();

	/*
	 * Putting a message into the queue
	 */
	public void putMessage(Message m) {
		queue.add(m);
	}

	/*
	 * Remove the message from the queue and discard it. - queue1.remove(m) does
	 * not function, because it discards all message with the same priority as m
	 * from the queue. - This java api bug is reported at:
	 * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6207984
	 */
	public void removeMessage(Message m) {
		queue.removeAll(Collections.singletonList(m));
	}

	/*
	 * get the first message in the queue (with least time stamp)
	 */
	public Message getNextMessage() {
		Message m = queue.poll();
		return m;
	}

	public boolean isEmpty() {
		return queue.isEmpty();
	}

	public int getQueueSize() {
		return queue.size();
	}

	public void bufferMessage(Message m) {
		synchronized (buffer) {
			buffer.add(m);
		}
	}

	public void addDeleteBufferMessage(Message m) {
		synchronized (deleteBuffer) {
			deleteBuffer.add(m);
		}
	}

}
