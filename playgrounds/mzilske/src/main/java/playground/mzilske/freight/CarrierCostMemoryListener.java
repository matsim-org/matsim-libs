package playground.mzilske.freight;

import org.matsim.api.core.v01.Id;

public interface CarrierCostMemoryListener {
	public void inform(Id carrierId, CostMemory costMemory);
}
