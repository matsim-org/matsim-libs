package playground.mzilske.freight.events;

import org.matsim.core.events.handler.EventHandler;

public interface QueryCarrierOffersEventHandler extends EventHandler{
	
	public void handleEvent(QueryCarrierOffersEvent event);

}
