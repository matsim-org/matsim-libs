package org.matsim.core.mobsim.qsim.qnetsimengine;

import matsimConnector.environment.TransitionArea;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.network.Link;

public class CAQLink {
	private final QLinkI ql;
	private final TransitionArea transitionArea;
	private final NetsimEngineContext context;
	
	CAQLink(QLinkI qLinkImpl, TransitionArea transitionArea, NetsimEngineContext context) {
		this.ql = qLinkImpl;
		this.transitionArea = transitionArea;
		this.context = context;
	}

	public boolean isAcceptingFromUpstream() {
		return this.ql.getAcceptingQLane().isAcceptingFromUpstream();
	}

	public Link getLink() {
		return this.ql.getLink();
	}

	public void addFromUpstream(QVehicle veh) {
		this.ql.getAcceptingQLane().addFromUpstream(veh);
	}
	
	public TransitionArea getTransitionArea(){
		return transitionArea;
	}

	public void notifyMoveOverBorderNode(QVehicle vehicle, Id<Link> leftLinkId){
		double now = context.getSimTimer().getTimeOfDay();
		context.getEventsManager().processEvent(new LinkLeaveEvent(
				now, vehicle.getId(), leftLinkId));
	}
}
