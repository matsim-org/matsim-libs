/* *********************************************************************** *
 * project: org.matsim.*
 * OSM.java
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.matsim.core.utils.collections.Tuple;

public class OSM {
	
	private final List<OSMNode> nodes = new ArrayList<OSMNode>();
	private final List<OSMWay> ways = new ArrayList<OSMWay>();
	private final Set<Long> refNodes = new HashSet<>();
	private final List<OSMRelation> relations = new ArrayList<OSMRelation>();

	
	private final Set<String> keys = new HashSet<String>();
	private final Set<Tuple<String,String>> keyValues  = new HashSet<Tuple<String,String>>();
	
	
	public void addKey(String key) {
		this.keys.add(key);
	}
	public void addKeyValue(String key, String value) {
		this.keyValues.add(new Tuple<String,String>(key,value));
	}
	
	/*package*/ List<String> getKeys() {
		return new ArrayList<String>(this.keys);
	}
	
	/*package*/ List<Tuple<String,String>> getKeyValues() {
		return new ArrayList<Tuple<String,String>>(this.keyValues);
	}
	
	public List<OSMNode> getNodes() {
		if (this.nodes.size() > this.refNodes.size()) {
			Iterator<OSMNode> it = this.nodes.iterator();
			while (it.hasNext()) {
				OSMElement node = it.next();
				if (!this.refNodes.contains(node.getId())) {
					it.remove();
				}
			}
		}
		return this.nodes;
	}
	
	/*package*/ List<OSMNode> getUnfilteredNodes() {
		return this.nodes;
	}
	
	/*package*/ Set<Long> getRefNodes() {
		return this.refNodes;
	}
	
	public List<OSMWay> getWays() {
		return this.ways;
	}

	public List<OSMRelation> getRelations() {
		return this.relations ;
	}
}
