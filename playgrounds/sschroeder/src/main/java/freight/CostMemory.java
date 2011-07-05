package freight;

import org.matsim.api.core.v01.Id;

public interface CostMemory {

	public void memorizeCost(Id from, Id to, int size, double cost);
}
