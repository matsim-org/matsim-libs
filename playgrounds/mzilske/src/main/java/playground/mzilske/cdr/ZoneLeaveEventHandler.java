package playground.mzilske.cdr;

import org.matsim.core.events.handler.EventHandler;

public interface ZoneLeaveEventHandler extends EventHandler {
	
	public void handleEvent(ZoneLeaveEvent event);

}
