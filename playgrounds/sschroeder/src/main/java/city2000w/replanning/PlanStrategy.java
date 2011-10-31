package city2000w.replanning;

import org.matsim.contrib.freight.carrier.Actor;

public interface PlanStrategy<T extends Actor> {
	
	public void run(T agent);
}
