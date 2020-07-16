package org.matsim.contrib.freight.eventhandler;

import org.matsim.contrib.freight.events.LSPTourStartEvent;
import org.matsim.core.events.handler.EventHandler;


public interface LSPTourStartEventHandler extends EventHandler {

	public void handleEvent( LSPTourStartEvent event );

}
