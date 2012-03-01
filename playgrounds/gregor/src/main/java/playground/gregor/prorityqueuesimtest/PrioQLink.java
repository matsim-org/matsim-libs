package playground.gregor.prorityqueuesimtest;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.internal.MatsimComparator;


public class PrioQLink {
	
	private final Queue<PrioQAgent> agents = new PriorityQueue<PrioQAgent>(500,new PrioQLinkLeaveTimeComparator());
//	private final Queue<PrioQAgent> agents = new LinkedList<PrioQAgent>();
	private final double length;
	private int numOfAgentsOnLink;
	private final Id id;

	
	public PrioQLink(double length, Id id) {
		this.length = length;
		this.id = id;
	}
	
	public double getLength() {
		return this.length;
	}

	public void push(PrioQAgent agent) {
		this.agents.add(agent);
	}
	
	public PrioQAgent tryPoll(double time) {
		if (this.agents.size() > 0 && this.agents.peek().getNextLeaveTime() <= time) {
			return this.agents.poll();
		}
		return null;
	}

	public int getNumOfAgentsOnLink() {
		return this.numOfAgentsOnLink;
	}
	
	private static final class PrioQLinkLeaveTimeComparator implements Comparator<PrioQAgent>, MatsimComparator {

		@Override
		public int compare(PrioQAgent arg0, PrioQAgent arg1) {
			int cmp = Double.compare(arg0.getNextLeaveTime(), arg1.getNextLeaveTime());
			if (cmp == 0) {
				// Both depart at the same time -> let the one with the larger id be first (=smaller)
				return arg0.getAgent().getId().compareTo(arg1.getAgent().getId());
			}
			return cmp;
		}
		
	}

	public void update() {
		this.numOfAgentsOnLink = this.agents.size();
	}

	public Id getId() {
		return this.id;
	}

}
