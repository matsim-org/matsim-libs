package city2000w.replanning;

import playground.mzilske.freight.api.Actor;

public interface PlanStrategy<T extends Actor> {
	
	public void run(T agent);
}
