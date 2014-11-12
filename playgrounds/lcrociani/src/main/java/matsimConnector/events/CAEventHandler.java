package matsimConnector.events;

import matsimConnector.run.CAAgentMoveToOrigin;

import org.matsim.core.events.handler.EventHandler;

public interface CAEventHandler extends EventHandler {

	public void handleEvent(CAAgentConstructEvent event);
	
	public void handleEvent(CAAgentMoveEvent event);
	
	public void handleEvent(CAAgentExitEvent event);
	
	public void handleEvent(CAAgentMoveToOrigin event);
	
	public void handleEvent(CAAgentLeaveEnvironmentEvent event);
	
	public void handleEvent(CAAgentEnterEnvironmentEvent event);
}
