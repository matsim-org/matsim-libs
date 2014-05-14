/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.synPop;

import java.util.ArrayList;
import java.util.List;

/**
 * @author johannes
 *
 */
public class ProxyPlan {

	private List<ProxyActivity> activities;
	
	private List<ProxyLeg> legs = new ArrayList<ProxyLeg>();
	
	public void addLeg(ProxyLeg leg) {
		legs.add(leg);
	}
	
	public List<ProxyLeg> getLegs() {
		return legs;
	}
	
	public void addActivity(ProxyActivity activity) {
		activities.add(activity);
	}
	
	public List<ProxyActivity> getActivities() {
		return activities;
	}
}
