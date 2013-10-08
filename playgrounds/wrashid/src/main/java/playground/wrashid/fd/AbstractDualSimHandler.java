package playground.wrashid.fd;

import java.util.HashMap;
import java.util.HashSet;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.Wait2LinkEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.Wait2LinkEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;

import playground.wrashid.lib.obj.TwoKeyHashMapsWithDouble;

public abstract class AbstractDualSimHandler implements LinkLeaveEventHandler,
		LinkEnterEventHandler, AgentArrivalEventHandler,
		Wait2LinkEventHandler {

	public abstract boolean isJDEQSim();

	public abstract boolean isLinkPartOfStudyArea(Id linkId);

	public abstract void processLeaveLink(Id linkId, Id personId, double enterTime, double leaveTime);

	// personId
	private HashSet<Id> agentsTravellingOnLinks = new HashSet<Id>();

	// linkId, personId
	private TwoKeyHashMapsWithDouble<Id, Id> linkEnterTime = new TwoKeyHashMapsWithDouble<Id, Id>();

	@Override
	public void reset(int iteration) {

	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (isLinkPartOfStudyArea(event.getLinkId())) {
			if (agentsTravellingOnLinks.contains(event.getPersonId())) {
				processLeaveLink(event.getLinkId(), event.getPersonId(),
						linkEnterTime.get(event.getLinkId(), event.getPersonId()), event.getTime());
			}
		}
		agentsTravellingOnLinks.remove(event.getPersonId());
		linkEnterTime.removeValue(event.getLinkId(), event.getPersonId());
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		if (isLinkPartOfStudyArea(event.getLinkId())) {
			if (agentsTravellingOnLinks.contains(event.getPersonId())) {
				if (!isJDEQSim()) {
					processLeaveLink(event.getLinkId(), event.getPersonId(),
							linkEnterTime.get(event.getLinkId(), event.getPersonId()), event.getTime());
				}
			}
		}
		agentsTravellingOnLinks.remove(event.getPersonId());
		linkEnterTime.removeValue(event.getLinkId(), event.getPersonId());
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		agentsTravellingOnLinks.add(event.getPersonId());
		linkEnterTime.put(event.getLinkId(), event.getPersonId(),
				event.getTime());
	}

	@Override
	public void handleEvent(Wait2LinkEvent event) {
		if (isJDEQSim()) {
			agentsTravellingOnLinks.add(event.getPersonId());
			linkEnterTime.put(event.getLinkId(), event.getPersonId(),
					event.getTime());
		} else {
			agentsTravellingOnLinks.remove(event.getPersonId());
		}
	}
}
