package playground.mzilske.freight.events;

import org.matsim.core.events.handler.EventHandler;

public interface OfferAcceptEventHandler extends EventHandler{
	
	public void handleEvent(OfferAcceptEvent event);

}
