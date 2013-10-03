package playground.wrashid.parkingSearch.ppSim.jdepSim;

import java.util.PriorityQueue;

public class MessageQueue {

	private PriorityQueue<Message> queue = new PriorityQueue<Message>();
	private double simTime = 0;
	
	public void schedule(Message m) {
		queue.add(m);
	}
	
	public void startSimulation() {
		Message m;
		while (!queue.isEmpty()) {
			m = queue.poll();
			if (m != null) {
				simTime = m.getMessageArrivalTime();
				m.processEvent();
			}
		}
	}
	
}
