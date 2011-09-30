package playground.mzilske.freight.events;

import org.matsim.core.events.handler.EventHandler;

public interface QueryTSPOffersEventHandler extends EventHandler {
	public void handleEvent(QueryTSPOffersEvent event);
}
