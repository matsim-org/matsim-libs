package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.matsim.api.core.v01.network.Link;

public class CAQLink {
	private final QLinkInternalI ql;

	CAQLink(QLinkInternalI qLinkImpl) {
		this.ql = qLinkImpl;
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
}
