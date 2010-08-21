/* *********************************************************************** *
 * project: org.matsim.*
 * OSMEntityCollector
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.dgrether.osm;

import java.util.HashMap;
import java.util.Map;

import org.openstreetmap.osmosis.core.container.v0_6.BoundContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityProcessor;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Bound;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;


/**
 * @author dgrether
 *
 */
public class OSMEntityCollector implements Sink, EntityProcessor {

	private Map<Long, Node> allNodes;
	private Map<Long, Bound> allBounds;
	private Map<Long, Way> allWays;
	private Map<Long, Relation> allRelations;
	
	public OSMEntityCollector(){
		this.allNodes =  new HashMap<Long, Node>();
		this.allBounds =  new HashMap<Long, Bound>();
		this.allWays =  new HashMap<Long, Way>();
		this.allRelations =  new HashMap<Long, Relation>();
	}
	
	
	@Override
	public void process(EntityContainer ec) {
		ec.process(this);
	}

	@Override
	public void complete() {	
	}

	@Override
	public void release() {
		//do nothing as the data might be needed after release is called
	}
	
	public void reset(){
		this.allBounds.clear();
		this.allNodes.clear();
		this.allWays.clear();
		this.allRelations.clear();
	}

	@Override
	public void process(BoundContainer bc) {
		this.allBounds.put(bc.getEntity().getId(), bc.getEntity());
	}

	@Override
	public void process(NodeContainer nc) {
		this.allNodes.put(nc.getEntity().getId(), nc.getEntity());
	}

	@Override
	public void process(WayContainer wc) {
		this.allWays.put(wc.getEntity().getId(), wc.getEntity());
	}

	@Override
	public void process(RelationContainer rc) {
		this.allRelations.put(rc.getEntity().getId(), rc.getEntity());
	}


	
	public Map<Long, Node> getAllNodes() {
		return allNodes;
	}


	
	public Map<Long, Bound> getAllBounds() {
		return allBounds;
	}


	
	public Map<Long, Way> getAllWays() {
		return allWays;
	}


	
	public Map<Long, Relation> getAllRelations() {
		return allRelations;
	}



}
