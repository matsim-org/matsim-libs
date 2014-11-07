package matsimConnector.events;

import org.matsim.core.events.handler.EventHandler;

public interface CAEventHandler extends EventHandler {

	public void handleEvent(CAAgentConstructEvent event);
	
	public void handleEvent(CAAgentMoveEvent event);
}
