package playground.mzilske.cdr;

import org.matsim.core.events.handler.EventHandler;


public interface ZoneEnterEventHandler extends EventHandler {
	
	public void handleEvent(ZoneEnterEvent event);

}
