package playground.wrashid.DES;

import java.util.Collections;
import java.util.PriorityQueue;

public class MessageQueue {
	private PriorityQueue<Message> queue1 = new PriorityQueue<Message>();
	private int queueSize=0;

	/*
	 * Putting a message into the queue
	 */
	public void putMessage(Message m) {
		queue1.add(m);
		queueSize++;
	}

	/*
	 * Remove the message from the queue and discard it. - queue1.remove(m) does
	 * not function, because it discards all message with the same priority as m
	 * from the queue. - This java api bug is reported at:
	 * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6207984
	 */
	public void removeMessage(Message m) {
		m.killMessage();
		queueSize--;
		//queue1.removeAll(Collections.singletonList(m));
	}

	/*
	 * get the first message in the queue (with least time stamp)
	 */
	public Message getNextMessage() {
		Message m = null;
		if (queue1.peek()!=null){
			// skip over dead messages
			while ((m=queue1.poll())!=null && !m.isAlive()){
			
			}
			// only decrement, if message fetched
			if (m!=null){
				queueSize--;
			}
		}
		
		return m;
	}

	public boolean isEmpty() {
		return queue1.size()==0;
	}

	public int getQueueSize() {
		return queueSize;
	}

}