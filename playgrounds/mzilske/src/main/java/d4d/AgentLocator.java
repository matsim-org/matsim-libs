package d4d;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;

public class AgentLocator implements LinkEnterEventHandler {

	Map<Id, Id> locations = new HashMap<Id, Id>();
	
	@Override
	public void reset(int iteration) {
		locations.clear();
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		locations.put(event.getPersonId(), event.getLinkId());
	}
	
	

}
