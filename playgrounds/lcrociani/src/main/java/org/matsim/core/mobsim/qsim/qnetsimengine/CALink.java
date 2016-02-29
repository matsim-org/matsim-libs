package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.network.Link;

public class CALink {
	private final QLinkI ql;
	private final QNetwork network;
	
	public CALink(QNetwork network, QLinkI qLinkImpl) {
		this.network = network;
		this.ql = qLinkImpl;
	}

	public Link getLink() {
		return this.ql.getLink();
	}

	public void notifyMoveOverBorderNode(QVehicle vehicle, Id<Link> leftLinkId){
		double now = network.simEngine.getMobsim().getSimTimer().getTimeOfDay();
		network.simEngine.getMobsim().getEventsManager().processEvent(new LinkLeaveEvent(
				now, vehicle.getId(), leftLinkId));
		network.simEngine.getMobsim().getEventsManager().processEvent(new LinkEnterEvent(
				now, vehicle.getId(), getLink().getId()));
	}
}

