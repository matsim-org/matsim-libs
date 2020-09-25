package org.matsim.contrib.freight.events.eventhandler;

import org.matsim.contrib.freight.events.LSPTourEndEvent;
import org.matsim.core.events.handler.EventHandler;


public interface LSPTourEndEventHandler extends EventHandler {

	public void handleEvent( LSPTourEndEvent event );

}
