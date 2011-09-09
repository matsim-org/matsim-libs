package playground.mzilske.freight.events;

import org.matsim.core.events.handler.EventHandler;

public interface OfferRejectEventHandler extends EventHandler{
	public void handleEvent(OfferRejectEvent event);
}
