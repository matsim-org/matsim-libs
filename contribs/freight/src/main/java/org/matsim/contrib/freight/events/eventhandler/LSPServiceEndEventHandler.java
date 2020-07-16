package org.matsim.contrib.freight.events.eventhandler;


import org.matsim.contrib.freight.events.LSPServiceEndEvent;
import org.matsim.core.events.handler.EventHandler;

public interface LSPServiceEndEventHandler extends EventHandler{
	

		public void handleEvent( LSPServiceEndEvent event );

	
}
