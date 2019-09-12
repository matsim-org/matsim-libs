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

package playground.vsp.andreas.mzilske.osm;

import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.openstreetmap.osmosis.core.container.v0_6.BoundContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityProcessor;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.core.filter.common.IdTracker;
import org.openstreetmap.osmosis.core.filter.common.IdTrackerFactory;
import org.openstreetmap.osmosis.core.filter.common.IdTrackerType;
import org.openstreetmap.osmosis.core.lifecycle.ReleasableIterator;
import org.openstreetmap.osmosis.core.store.SimpleObjectStore;
import org.openstreetmap.osmosis.core.store.SingleClassObjectSerializationFactory;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.core.task.v0_6.SinkSource;

public class SimplifyTask implements SinkSource, EntityProcessor {

	private Sink sink;

	private IdTracker visitedNodes;

	private IdTracker requiredNodes;

	private SimpleObjectStore<NodeContainer> allNodes;
	
	private SimpleObjectStore<WayContainer> allWays;

	private int count = 0; // just for debug stats

	private IdTracker availableNodes;

	/**
	 * Creates a new instance.
	 * 
	 * @param idTrackerType
	 *            Defines the id tracker implementation to use.
	 */
	public SimplifyTask(IdTrackerType idTrackerType) {
		requiredNodes = IdTrackerFactory.createInstance(idTrackerType);
		visitedNodes = IdTrackerFactory.createInstance(idTrackerType);
		availableNodes = IdTrackerFactory.createInstance(idTrackerType);
		allNodes = new SimpleObjectStore<NodeContainer>(
				new SingleClassObjectSerializationFactory(NodeContainer.class),
				"afnd", true);
		allWays = new SimpleObjectStore<WayContainer>(
				new SingleClassObjectSerializationFactory(WayContainer.class),
				"afwy", true);
		
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void process(EntityContainer entityContainer) {
		// Ask the entity container to invoke the appropriate processing method
		// for the entity type.
		entityContainer.process(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void process(BoundContainer boundContainer) {
		// pass on the bounds information unchanged
		sink.process(boundContainer);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void process(NodeContainer container) {

		// stuff all nodes into a file
		availableNodes.set(container.getEntity().getId());
		allNodes.add(container);

		// debug
		count++;
		if (count % 50000 == 0)
			System.out.println(count + " nodes processed so far");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void process(WayContainer container) {
		Way way = container.getEntity();
		List<WayNode> wayNodes = way.getWayNodes();
		if (wayNodes.size() < 2) {
		
		} else {
			WayNode startNode = wayNodes.get(0);
			WayNode endNode = wayNodes.get(wayNodes.size()-1);

			requiredNodes.set(startNode.getNodeId());
			requiredNodes.set(endNode.getNodeId());
			
			for (WayNode wayNode : wayNodes) {
				long nodeId = wayNode.getNodeId();
				if (visitedNodes.get(nodeId)) {
					requiredNodes.set(nodeId);
				}
				visitedNodes.set(nodeId);
			}

			int prevRealNodeIndex = 0;
			WayNode prevRealNode = wayNodes.get(prevRealNodeIndex);
			for (int i = 1; i < wayNodes.size(); i++) {
				WayNode node = wayNodes.get(i);
				if (requiredNodes.get(node.getNodeId())) {
					if (prevRealNode.getNodeId() == node.getNodeId()) {
						/* We detected a loop between to "real" nodes.
						 * Set some nodes between the start/end-loop-node to "used" again.
						 * But don't set all of them to "used", as we still want to do some network-thinning.
						 * I decided to use sqrt(.)-many nodes in between...
						 */
						double increment = Math.sqrt(i - prevRealNodeIndex);
						double nextNodeToKeep = prevRealNodeIndex + increment;
						for (double j = nextNodeToKeep; j < i; j += increment) {
							int index = (int) Math.floor(j);
							WayNode intermediaryNode = wayNodes.get(index);
							requiredNodes.set(intermediaryNode.getNodeId());
						}
					}
					prevRealNodeIndex = i;
					prevRealNode = node;
				}
			}

			
		}
		allWays.add(container);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void process(RelationContainer container) {
		// Do nothing (Drop all relations)
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void complete() {
		ReleasableIterator<NodeContainer> nodeIterator;
		ReleasableIterator<WayContainer> wayIterator;
		NodeContainer nodeContainer;
		long nodeId;

		// Send on only the required nodes
		nodeIterator = allNodes.iterate();
		while (nodeIterator.hasNext()) {
			nodeContainer = nodeIterator.next();
			nodeId = nodeContainer.getEntity().getId();

			if (requiredNodes.get(nodeId)) {
				sink.process(nodeContainer);
			}
		}
		nodeIterator.release();
		
		wayIterator = allWays.iterate();
		while (wayIterator.hasNext()) {
			WayContainer wayContainer = wayIterator.next();
			Way way = wayContainer.getEntity().getWriteableInstance();
			ListIterator<WayNode> i = way.getWayNodes().listIterator();
			while (i.hasNext()) {
				WayNode wayNode = i.next();
				if (!requiredNodes.get(wayNode.getNodeId()) || !availableNodes.get(wayNode.getNodeId())) {
					i.remove();
				}
			}
			sink.process(wayContainer);
		}
		wayIterator.release();
		
		sink.complete();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void release() {
		allNodes.release();
		allWays.release();
		sink.release();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setSink(Sink sink) {
		this.sink = sink;
	}

	@Override
	public void initialize(Map<String, Object> map) {

	}
}