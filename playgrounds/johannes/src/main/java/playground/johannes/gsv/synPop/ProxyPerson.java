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

import java.util.Map.Entry;

/**
 * @author johannes
 *
 */
public class ProxyPerson extends ProxyObject {

	private String id;
	
	private ProxyPlan plan;
	
	public ProxyPerson(String id) {
		this.id = id;		
	}
	
	public String getId() {
		return id;
	}
	
	public void setPlan(ProxyPlan plan) {
		this.plan = plan;
	}
	
	public ProxyPlan getPlan() {
		return plan;
	}
	
	public ProxyPerson clone() {
		ProxyPerson clone = new ProxyPerson(id);
		
		for(Entry<String, String> entry : getAttributes().entrySet()) {
			clone.setAttribute(entry.getKey(), entry.getValue());
		}
		
		clone.setPlan(plan.clone());
		
		return clone;
	}
}
