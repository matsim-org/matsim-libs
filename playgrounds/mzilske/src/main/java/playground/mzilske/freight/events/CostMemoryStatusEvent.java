package playground.mzilske.freight.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.Event;

import playground.mzilske.freight.CostMemory;

public class CostMemoryStatusEvent extends CarrierEventImpl implements Event{

	private CostMemory costMemory;
	
	public CostMemoryStatusEvent(Id carrierId, CostMemory costMemory) {
		super(carrierId);
		this.costMemory = costMemory;
	}

	public CostMemory getCostMemory() {
		return costMemory;
	}

	@Override
	public double getTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Map<String, String> getAttributes() {
		// TODO Auto-generated method stub
		return null;
	}

}
