package org.matsim.core.mobsim.qsim.qnetsimengine;

import matsimConnector.environment.TransitionArea;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.network.Link;

public class CAQLink {
	private final QLinkI ql;
	private final TransitionArea transitionArea;
	private final QNetwork network;
	
	CAQLink(QNetwork network, QLinkI qLinkImpl, TransitionArea transitionArea) {
		this.network = network;
		this.ql = qLinkImpl;
		this.transitionArea = transitionArea;
	}

	public boolean isAcceptingFromUpstream() {
		return this.ql.isAcceptingFromUpstream();
	}

	public Link getLink() {
		return this.ql.getLink();
	}

	public void addFromUpstream(QVehicle veh) {
		this.ql.addFromUpstream(veh);
	}
	
	public TransitionArea getTransitionArea(){
		return transitionArea;
	}

	public void notifyMoveOverBorderNode(QVehicle vehicle, Id<Link> leftLinkId){
		double now = network.simEngine.getMobsim().getSimTimer().getTimeOfDay();
		network.simEngine.getMobsim().getEventsManager().processEvent(new LinkLeaveEvent(
				now, vehicle.getId(), leftLinkId));
	}
}
