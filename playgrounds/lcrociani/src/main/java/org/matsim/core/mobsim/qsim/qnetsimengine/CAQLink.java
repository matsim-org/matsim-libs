package org.matsim.core.mobsim.qsim.qnetsimengine;

import matsimConnector.environment.TransitionArea;

import org.matsim.api.core.v01.network.Link;

public class CAQLink {
	private final QLinkInternalI ql;
	private final TransitionArea transitionArea;
	
	CAQLink(QLinkInternalI qLinkImpl, TransitionArea transitionArea) {
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
}
