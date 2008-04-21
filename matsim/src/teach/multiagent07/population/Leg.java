/* *********************************************************************** *
 * project: org.matsim.*
 * Leg.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package teach.multiagent07.population;

import java.util.ArrayList;

import org.matsim.basic.v01.BasicLegImpl;
import org.matsim.basic.v01.BasicRouteImpl;
import org.matsim.utils.misc.Time;

public class Leg extends BasicLegImpl {
	private double duration = Time.UNDEFINED_TIME;
	
	public Leg(String mode) {
		this.mode = mode;
	}

	public Leg(Leg leg) {
		this.mode = leg.getMode();
		this.duration = leg.getDuration();
		this.num = leg.getNum();
		this.route = new BasicRouteImpl();
		ArrayList newRoute = new ArrayList();
		
		for (Object node: leg.getRoute().getRoute()) {
			newRoute.add(node);
		}
		this.route.setRoute(newRoute);
	}

	/**
	 * @return the duration
	 */
	public double getDuration() {
		return duration;
	}

	/**
	 * @param duration the duration to set
	 */
	public void setDuration(double duration) {
		this.duration = duration;
	}
	
}
