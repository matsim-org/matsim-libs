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

public class OSMWay implements OSMElement {

	private final Map<String,String> tags = new HashMap<String,String>();
	
	private final long id;
	
	private final List<Long> nodeRefs = new ArrayList<>();

	public OSMWay(long id) {
		this.id = id;
	}
	
	@Override
	public long getId(){
		return this.id;
	}
	
	@Override
	public void addTag(String key, String val) {
		this.tags.put(key,val);
	}
	
	@Override
	public Map<String,String> getTags() {
		return this.tags;
	}
	
	public void addNodeRef(long id) {
		this.nodeRefs.add(id);
	}
	
	public List<Long> getNodeRefs() {
		return this.nodeRefs;
	}
}
