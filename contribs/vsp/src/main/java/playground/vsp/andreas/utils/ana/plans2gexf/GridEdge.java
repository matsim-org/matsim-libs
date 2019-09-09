/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.vsp.andreas.utils.ana.plans2gexf;

import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;

/**
 * Stores all information linked to that edge
 * 
 * @author aneumann
 *
 */
public class GridEdge {
	
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(GridEdge.class);
	private Id<GridEdge> id;
	private GridNode fromNode;
	private GridNode toNode;
	
	private int nEntries = 0;
	private HashMap<String, Integer> legMode2countMap = new HashMap<String, Integer>();
	
	
	public GridEdge(GridNode fromNode, GridNode toNode){
		this.id = Id.create(fromNode.getId() + "-" + toNode.getId(), GridEdge.class);
		this.fromNode = fromNode;
		this.toNode = toNode;
	}
	
	public void addLeg(Leg leg){
		this.nEntries++;
		
		String legMode = leg.getMode();
		if (this.legMode2countMap.get(legMode) == null) {
			this.legMode2countMap.put(legMode, new Integer(0));
		}
		
		this.legMode2countMap.put(legMode, new Integer(this.legMode2countMap.get(legMode) + 1));
	}

	public GridNode getFromNode() {
		return fromNode;
	}

	public GridNode getToNode() {
		return toNode;
	}

	public Id<GridEdge> getId() {
		return id;
	}
	
	public int getCountForMode(String mode){
		if (this.legMode2countMap.get(mode) == null) {
			return 0;
		} else {
			return this.legMode2countMap.get(mode);
		}
	}
	
	public int getnEntries() {
		return nEntries;
	}

	@Override
	public String toString() {
		StringBuffer strB = new StringBuffer();
		strB.append("Modes ");
		for (Entry<String, Integer> legEntry : this.legMode2countMap.entrySet()) {
			strB.append(" | " + legEntry.getKey() + " " + legEntry.getValue().toString());
		}
		return strB.toString();
	}
}