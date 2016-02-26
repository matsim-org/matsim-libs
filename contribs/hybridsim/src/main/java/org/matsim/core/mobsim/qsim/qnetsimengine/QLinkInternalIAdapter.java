/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.matsim.api.core.v01.network.Link;

public class QLinkInternalIAdapter {
	private final QLinkI ql;

	QLinkInternalIAdapter(QLinkI qLinkImpl) {
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
