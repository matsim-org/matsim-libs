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

import playground.johannes.synpop.data.PlainElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

/**
 * @author johannes
 *
 */
public class ProxyPerson extends PlainElement {

	private String id;
	
//	private ProxyPlan plan;
	
	private List<ProxyPlan> plans = new ArrayList<ProxyPlan>();
	
	public ProxyPerson(String id) {
		this.id = id;		
	}
	
	public String getId() {
		return id;
	}
	
	public void setPlan(ProxyPlan plan) {
//		this.plan = plan;
		if(plans.isEmpty())
			plans.add(plan);
		else
			plans.set(0, plan);
	}
	
	public void addPlan(ProxyPlan plan) {
		plans.add(plan);
	}
	
	public ProxyPlan getPlan() {
//		return plan;
		return plans.get(0);
	}
	
	public List<ProxyPlan> getPlans() {
		return plans;
	}
	
	public ProxyPerson clone() {
		return cloneWithNewId(id);
	}
	
	public ProxyPerson cloneWithNewId(String newId) {
		ProxyPerson clone = new ProxyPerson(newId);
		
		for(Entry<String, String> entry : getAttributes().entrySet()) {
			clone.setAttribute(entry.getKey(), entry.getValue());
		}
		
//		clone.setPlan(plan.clone());
		for(ProxyPlan plan : plans)
			clone.addPlan(plan.clone());
		
		return clone;
	}
}
