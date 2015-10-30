package playground.wrashid.fd;

import java.util.HashSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.Wait2LinkEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.Wait2LinkEventHandler;
import org.matsim.contrib.parking.lib.obj.TwoKeyHashMapWithDouble;


public abstract class AbstractDualSimHandler implements LinkLeaveEventHandler,
		LinkEnterEventHandler, PersonArrivalEventHandler,
		Wait2LinkEventHandler {

	public abstract boolean isJDEQSim();

	public abstract boolean isLinkPartOfStudyArea(Id linkId);

	public abstract void processLeaveLink(Id linkId, Id personId, double enterTime, double leaveTime);

	// personId
	private HashSet<Id> agentsTravellingOnLinks = new HashSet<Id>();

	// linkId, personId
	private TwoKeyHashMapWithDouble<Id, Id> linkEnterTime = new TwoKeyHashMapWithDouble<Id, Id>();

	@Override
	public void reset(int iteration) {

	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (isLinkPartOfStudyArea(event.getLinkId())) {
			if (agentsTravellingOnLinks.contains(event.getDriverId())) {
				processLeaveLink(event.getLinkId(), event.getDriverId(),
						linkEnterTime.get(event.getLinkId(), event.getDriverId()), event.getTime());
			}
		}
		agentsTravellingOnLinks.remove(event.getDriverId());
		linkEnterTime.removeValue(event.getLinkId(), event.getDriverId());
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
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
		agentsTravellingOnLinks.add(event.getDriverId());
		linkEnterTime.put(event.getLinkId(), event.getDriverId(),
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
