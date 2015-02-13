package playground.balac.allcsmodestest.events.handler;

import org.matsim.core.events.handler.EventHandler;

import playground.balac.allcsmodestest.events.StartRentalEvent;


public interface StartRentalEventHandler extends EventHandler {
	
	public void handleEvent (StartRentalEvent event);

}
