package playground.wrashid.PDES2.util;

import java.util.PriorityQueue;

import org.matsim.events.BasicEvent;

import playground.wrashid.PDES2.Message;

public class MyPriorityQueue implements Comparable {
	private PriorityQueue<Message> queue;

	public MyPriorityQueue(PriorityQueue<Message> queue){
		this.queue = queue;
	}
	
	public MyPriorityQueue(){
		this.queue = new PriorityQueue<Message>();
	}
	
	public PriorityQueue<Message> getQueue(){
		return queue;
	}
	
	
	public int compareTo(Object arg0) {
		MyPriorityQueue otherQueue= (MyPriorityQueue) arg0;
		if (queue.peek().messageArrivalTime<otherQueue.getQueue().peek().messageArrivalTime){
			return -1;
		} else if (queue.peek().messageArrivalTime>otherQueue.getQueue().peek().messageArrivalTime){
			return 1;
		}
		return 0;
	}

}
