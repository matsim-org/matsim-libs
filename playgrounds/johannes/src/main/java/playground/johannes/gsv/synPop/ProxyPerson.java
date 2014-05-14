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

import java.util.HashMap;
import java.util.Map;

/**
 * @author johannes
 *
 */
public class ProxyPerson {

	private String id;
	
	private Map<String, Object> attributes;
	
	private ProxyPlan plan;
	
	public ProxyPerson(String id) {
		this.id = id;
		
		attributes = new HashMap<String, Object>();
	}
	
	public String getId() {
		return id;
	}
	
	public Object setAttribute(String key, Object value) {
		return attributes.put(key, value);
	}
	
	public void setPlan(ProxyPlan plan) {
		this.plan = plan;
	}
	
	public ProxyPlan getPlan() {
		return plan;
	}
}
