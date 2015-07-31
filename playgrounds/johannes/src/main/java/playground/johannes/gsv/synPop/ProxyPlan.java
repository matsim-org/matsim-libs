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

import playground.johannes.synpop.data.Element;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.PlainElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

/**
 * @author johannes
 *
 */
public class ProxyPlan extends PlainElement implements Episode {

	private List<Element> activities = new ArrayList<Element>();
	
	private List<Element> legs = new ArrayList<Element>();
	
	public void addLeg(Element leg) {
		legs.add(leg);
	}
	
	public List<Element> getLegs() {
		return legs;
	}
	
	public void addActivity(Element activity) {
		activities.add(activity);
	}
	
	public List<Element> getActivities() {
		return activities;
	}
	
	public ProxyPlan clone() {
		ProxyPlan clone = new ProxyPlan();
		
		for(Entry<String, String> entry : getAttributes().entrySet()) {
			clone.setAttribute(entry.getKey(), entry.getValue());
		}
		
		for(Element act : activities) {
			clone.addActivity(((PlainElement)act).clone());
		}
		
		for(Element leg : legs) {
			clone.addLeg(((PlainElement)leg).clone());
		}
		
		return clone;
	}
}
