package playground.pbouman.crowdedness.events;

import org.matsim.core.events.handler.EventHandler;

public interface PersonCrowdednessEventHandler extends EventHandler
{
	public void handleEvent(PersonCrowdednessEvent event);
}
