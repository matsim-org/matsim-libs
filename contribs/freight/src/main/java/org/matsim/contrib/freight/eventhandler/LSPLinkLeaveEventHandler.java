package org.matsim.contrib.freight.eventhandler;

import org.matsim.contrib.freight.events.LSPFreightLinkLeaveEvent;
import org.matsim.core.events.handler.EventHandler;

public interface LSPLinkLeaveEventHandler extends EventHandler{

	public void handleEvent( LSPFreightLinkLeaveEvent event );

}
