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

package playground.johannes.gsv.synPop.osm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author johannes
 *
 */
public class OSMWay {

	private String id;
	
	private List<OSMNode> nodes;
	
	private Map<String, String> tags;
	
	public OSMWay(String id) {
		this.id = id;
		this.nodes = new ArrayList<OSMNode>();
		this.tags = new HashMap<String, String>();
	}
	
	public String getId() {
		return id;
	}
	
	public void addNode(OSMNode node) {
		nodes.add(node);
	}
	
	public List<OSMNode> getNodes() {
		return nodes;
	}
	
	public void addTag(String key, String value) {
		tags.put(key, value);
	}
	
	public Map<String, String> tags() {
		return tags;
	}
}
