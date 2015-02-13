package playground.balac.allcsmodestest.events.handler;

import org.matsim.core.events.handler.EventHandler;

import playground.balac.allcsmodestest.events.EndRentalEvent;

public interface EndRentalEventHandler extends EventHandler{
	
	public void handleEvent (EndRentalEvent event);
	
}
