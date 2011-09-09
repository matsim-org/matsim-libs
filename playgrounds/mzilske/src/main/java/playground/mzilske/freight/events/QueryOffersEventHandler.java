package playground.mzilske.freight.events;

import org.matsim.core.events.handler.EventHandler;

public interface QueryOffersEventHandler extends EventHandler{
	
	public void handleEvent(QueryOffersEvent event);

}
