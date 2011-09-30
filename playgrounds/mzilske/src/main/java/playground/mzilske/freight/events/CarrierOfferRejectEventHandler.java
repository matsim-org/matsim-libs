package playground.mzilske.freight.events;

import org.matsim.core.events.handler.EventHandler;

public interface CarrierOfferRejectEventHandler extends EventHandler{
	public void handleEvent(CarrierOfferRejectEvent event);
}
