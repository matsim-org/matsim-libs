package city2000w.replanning;

import org.matsim.contrib.freight.api.Actor;

public interface PlanStrategy<T extends Actor> {
	
	public void run(T agent);
}
