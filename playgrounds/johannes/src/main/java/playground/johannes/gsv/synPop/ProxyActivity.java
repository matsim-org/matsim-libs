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
public class ProxyActivity {

	private Map<String, Object> attributes = new HashMap<String, Object>();
	
	private Map<Object, Object> userData;
	
	public Object setAttribute(String key, Object value) {
		return attributes.put(key, value);
	}
	
	public Object getAttribute(String key) {
		return attributes.get(key);
	}
	
	public Map<String, Object> getAttributes() {
		return Collections.unmodifiableMap(attributes);
	}
	
	public ProxyActivity clone() {
		ProxyActivity clone = new ProxyActivity();
		
		for(Entry<String, Object> entry : attributes.entrySet()) {
			clone.setAttribute(entry.getKey(), entry.getValue());
		}
		
		return clone;
	}
	
	public void setUserData(Object key, Object value) {
		
	}
	
	private void initUserData() {
		if(userData == null) {
			userData = new HashMap<Object, Object>();
		}
	}
}
