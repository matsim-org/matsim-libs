package org.matsim.contrib.freight.replanning;

import org.matsim.contrib.freight.carrier.Actor;

public interface PlanStrategy<T extends Actor> {
	
	public void run(T agent);
}
