/* *********************************************************************** *
 * project: org.matsim.*
 * OSMWay.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.gregor.sim2d_v4.io.osmparser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;

public class OSMWay {

	private final Map<String,String> tags = new HashMap<String,String>();
	
	private final Id id;
	
	private final List<Id> nodeRefs = new ArrayList<Id>();

	public OSMWay(Id id) {
		this.id = id;
	}
	
	public Id getId(){
		return this.id;
	}
	
	public void addTag(String key, String val) {
		this.tags.put(key,val);
	}
	
	public Map<String,String> getTags() {
		return this.tags;
	}
	
	public void addNodeRef(Id id) {
		this.nodeRefs.add(id);
	}
	
	public List<Id> getNodeRefs() {
		return this.nodeRefs;
	}
}
