package playground.mzilske.freight;

import org.matsim.api.core.v01.Id;

public interface CostMemory {
	void memorizeCost(Id from, Id to, int size, double cost);
	
	public Double getCost(Id from, Id to, int size);
}
