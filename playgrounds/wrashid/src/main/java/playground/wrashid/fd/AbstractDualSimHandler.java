package playground.wrashid.fd;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentWait2LinkEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentWait2LinkEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;

public abstract class AbstractDualSimHandler implements LinkLeaveEventHandler,
		LinkEnterEventHandler, AgentArrivalEventHandler,
		AgentWait2LinkEventHandler {

	public abstract boolean isJDEQSim();

	public abstract boolean isLinkPartOfStudyArea(Id linkId);

	public abstract void processLeaveLink(Id linkId, Id personId, double time);

	// personId, linkId
	private HashMap<Id, Id> lastEnteredLink = new HashMap<Id, Id>();

	@Override
	public void reset(int iteration) {

	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (isLinkPartOfStudyArea(event.getLinkId())) {
			if (lastEnteredLink.get(event.getPersonId()) != null) {
				processLeaveLink(event.getLinkId(), event.getPersonId(),
						event.getTime());
			}
		}
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		if (isLinkPartOfStudyArea(event.getLinkId())) {
			if (lastEnteredLink.get(event.getPersonId()) != null) {
				if (!isJDEQSim()) {
					processLeaveLink(event.getLinkId(), event.getPersonId(),
							event.getTime());
				}
				lastEnteredLink.put(event.getPersonId(), null); // reset value
			}
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		lastEnteredLink.put(event.getPersonId(), event.getLinkId());
	}

	@Override
	public void handleEvent(AgentWait2LinkEvent event) {
		if (isJDEQSim()) {
			lastEnteredLink.put(event.getPersonId(), event.getLinkId());
		} else {
			lastEnteredLink.put(event.getPersonId(), null);
		}
	}
}
