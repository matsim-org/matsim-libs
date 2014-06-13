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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author johannes
 *
 */
public class ProxyPerson {

	private String id;
	
	private Map<String, Object> attributes;
	
	private Map<Object, Object> userData;
	
	private ProxyPlan plan;
	
	public ProxyPerson(String id) {
		this.id = id;
		
		attributes = new HashMap<String, Object>();
		userData = new HashMap<Object, Object>();
	}
	
	public String getId() {
		return id;
	}
	
	public Object setAttribute(String key, Object value) {
		return attributes.put(key, value);
	}
	
	public Object getAttribute(String key) {
		return attributes.get(key);
	}
	
	public Map<String, Object> getAttributes() {
		return Collections.unmodifiableMap(attributes);
	}
	
	public void setPlan(ProxyPlan plan) {
		this.plan = plan;
	}
	
	public ProxyPlan getPlan() {
		return plan;
	}
	
	public Object getUserData(Object key) {
		return userData.get(key);
	}
	
	public void setUserData(Object key, Object value) {
		userData.put(key, value);
	}
	
	public ProxyPerson clone() {
		ProxyPerson clone = new ProxyPerson(id);
		
		for(Entry<String, Object> entry : attributes.entrySet()) {
			clone.setAttribute(entry.getKey(), entry.getValue());
		}
		
		clone.setPlan(plan.clone());
		
		return clone;
	}
}
