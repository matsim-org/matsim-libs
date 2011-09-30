package playground.mzilske.freight.events;

import org.matsim.core.events.handler.EventHandler;

public interface ShipperTSPContractAcceptEventHandler extends EventHandler{
	public void handleEvent(ShipperTSPContractAcceptEvent event);
}
