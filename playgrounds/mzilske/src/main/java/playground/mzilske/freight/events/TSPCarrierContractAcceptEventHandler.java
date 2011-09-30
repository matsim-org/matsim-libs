package playground.mzilske.freight.events;

import org.matsim.core.events.handler.EventHandler;

public interface TSPCarrierContractAcceptEventHandler extends EventHandler{
	public void handleEvent(TSPCarrierContractAcceptEvent event);
}
