/* *********************************************************************** *
 * project: org.matsim.*
 * DgOSMJunctionDetector
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
package playground.dgrether.signalsystems.osm;

import java.util.HashSet;
import java.util.ListIterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.openstreetmap.osmosis.core.container.v0_6.BoundContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityProcessor;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.core.lifecycle.ReleasableIterator;
import org.openstreetmap.osmosis.core.store.IndexedObjectStore;
import org.openstreetmap.osmosis.core.store.IndexedObjectStoreReader;
import org.openstreetmap.osmosis.core.store.SimpleObjectStore;
import org.openstreetmap.osmosis.core.store.SingleClassObjectSerializationFactory;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.core.task.v0_6.SinkSource;


/**
 * @author dgrether
 *
 */
public class DgOSMJunctionDetector implements SinkSource, EntityProcessor {
	
	private static final Logger log = Logger.getLogger(DgOSMJunctionDetector.class);
	
	private Sink sink;
	
	private IndexedObjectStore<Node> allNodes;
	
	private SimpleObjectStore<WayContainer> allWays;
	
	private long nodeCount = 0;
	private long wayCount = 0;

	private Set<String> signalizedNodeKeyValueSet = new HashSet<String>();

	private IndexedObjectStore<Node> signalizedNodes;
	
	public DgOSMJunctionDetector(){
		this.allNodes = new IndexedObjectStore<Node>(
				new SingleClassObjectSerializationFactory(Node.class),
				"qwer");
		this.allWays = new SimpleObjectStore<WayContainer>(
				new SingleClassObjectSerializationFactory(WayContainer.class),
				"wert", true);
		this.signalizedNodes = new IndexedObjectStore<Node>(
				new SingleClassObjectSerializationFactory(Node.class), "ertz");
		
		this.signalizedNodeKeyValueSet.add("highway.traffic_signals");
	}
	
	@Override
	public void process(EntityContainer ec) {
		ec.process(this);
	}

	@Override
	public void complete() {
		IndexedObjectStoreReader<Node> nodeReader = this.allNodes.createReader();
		
		ReleasableIterator<WayContainer> wayIterator;
		wayIterator = this.allWays.iterate();
		WayContainer wayContainer = null;
		Way way = null;
		while (wayIterator.hasNext()){
			wayContainer = wayIterator.next();
			way = wayContainer.getEntity();
			ListIterator<WayNode> i = way.getWayNodes().listIterator();
			while (i.hasNext()) {
				WayNode wayNode = i.next();
				Node n = nodeReader.get(wayNode.getNodeId());
			}
			
		}
		
	}

	@Override
	public void release() {
		this.sink.release();
		this.nodeCount = 0;
		this.wayCount = 0;
	}

	@Override
	public void setSink(Sink sink) {
		this.sink = sink;
	}

	@Override
	public void process(BoundContainer boundContainer) {
		sink.process(boundContainer);		
	}

	@Override
	public void process(NodeContainer container) {
		allNodes.add(container.getEntity().getId(), container.getEntity());
		this.nodeCount++;
		if (this.nodeCount % 1000 == 0){
			log.info(this.nodeCount + " nodes processed...");
		}
		
    for (Tag tag : container.getEntity().getTags()) {
          String keyValue = tag.getKey() + "." + tag.getValue();
          if (signalizedNodeKeyValueSet.contains(keyValue)) {
                this.signalizedNodes.add(container.getEntity().getId(), container.getEntity());
                break;
          }
    }
	}

	@Override
	public void process(WayContainer container) {
		this.allWays.add(container);
		this.wayCount++;
		if (this.wayCount % 1000 == 0){
			log.info(this.wayCount + " ways processed...");
		}
	}

	@Override
	public void process(RelationContainer container) {
		this.sink.process(container);
	}

}
