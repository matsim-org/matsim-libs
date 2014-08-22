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
import java.util.Map.Entry;

/**
 * @author johannes
 *
 */
public class ProxyPlan extends ProxyObject {

	private List<ProxyObject> activities = new ArrayList<ProxyObject>();
	
	private List<ProxyObject> legs = new ArrayList<ProxyObject>();
	
	public void addLeg(ProxyObject leg) {
		legs.add(leg);
	}
	
	public List<ProxyObject> getLegs() {
		return legs;
	}
	
	public void addActivity(ProxyObject activity) {
		activities.add(activity);
	}
	
	public List<ProxyObject> getActivities() {
		return activities;
	}
	
	public ProxyPlan clone() {
		ProxyPlan clone = new ProxyPlan();
		
		for(Entry<String, String> entry : getAttributes().entrySet()) {
			clone.setAttribute(entry.getKey(), entry.getValue());
		}
		
		for(ProxyObject act : activities) {
			clone.addActivity(act.clone());
		}
		
		for(ProxyObject leg : legs) {
			clone.addLeg(leg.clone());
		}
		
		return clone;
	}
}
