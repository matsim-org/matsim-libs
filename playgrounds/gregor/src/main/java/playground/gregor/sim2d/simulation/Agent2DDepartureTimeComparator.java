package playground.gregor.sim2d.simulation;

import java.util.Comparator;

public class Agent2DDepartureTimeComparator implements Comparator<Agent2D> {

	public int compare(Agent2D o1, Agent2D o2) {
		if (o1.getNextDepartureTime() < o2.getNextDepartureTime()) {
			return -1;
		} else if (o1.getNextDepartureTime() > o2.getNextDepartureTime()) {
			return 1;
		}
		return 0;
	}

}
