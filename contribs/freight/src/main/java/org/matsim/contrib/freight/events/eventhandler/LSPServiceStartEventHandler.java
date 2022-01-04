package org.matsim.contrib.freight.events.eventhandler;

import org.matsim.contrib.freight.events.LSPServiceStartEvent;
import org.matsim.core.events.handler.EventHandler;


public interface LSPServiceStartEventHandler extends EventHandler {

	public void handleEvent( LSPServiceStartEvent event );

}
