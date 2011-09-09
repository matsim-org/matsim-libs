package playground.mzilske.freight.api;

import org.matsim.api.core.v01.Id;
import org.matsim.core.events.handler.EventHandler;

import playground.mzilske.freight.CostMemory;
import playground.mzilske.freight.events.CostMemoryStatusEvent;

public interface CarrierCostMemoryStatusHandler extends EventHandler{
	
	public void handleEvent(CostMemoryStatusEvent event);
	
	public void inform(Id carrierId, CostMemory costMemory);
}
