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

package playground.michalm.jtrrouter.matsim;

import playground.michalm.jtrrouter.*;

/**
 * @author michalm
 */
public class MATSimPlan extends Plan {
	// <person id="$plan.id">
	// <plan>
	// <act type="work" x="$plan.route.in.x" y="$plan.route.in.y"
	// link="$plan.route.in.link" end_time="$plan.startTime"/>
	// <leg mode="car">
	// <route>$plan.route.nodes</route>
	// </leg>
	// <act type="home" x="$plan.route.out.x" y="$plan.route.out.y"
	// link="$plan.route.out.link" end_time="$plan.endTime"/>
	// </plan>
	// </person>

	final int startTime;
	final int endTime;

	public MATSimPlan(int id, Route route, int startTime, int endTime) {
		super(id, route);

		this.startTime = startTime;
		this.endTime = endTime;
	}

	public int getStartTime() {
		return startTime;
	}

	public int getEndTime() {
		return endTime;
	}
}
