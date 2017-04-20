/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.schedule;

import org.matsim.api.core.v01.network.Link;

public class StayTaskImpl extends AbstractTask implements StayTask {
	private final Link link;
	private final String name;

	public StayTaskImpl(double beginTime, double endTime, Link link) {
		this(beginTime, endTime, link, null);
	}

	public StayTaskImpl(double beginTime, double endTime, Link link, String name) {
		super(beginTime, endTime);
		this.link = link;
		this.name = name;
	}

	@Override
	public Link getLink() {
		return link;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return "S(" + (name != null ? name : "") + "@" + link.getId() + ")" + commonToString();
	}
}