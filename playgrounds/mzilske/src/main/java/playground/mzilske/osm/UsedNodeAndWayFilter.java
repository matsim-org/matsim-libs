// This software is released into the Public Domain.  See copying.txt for details.
package playground.mzilske.osm;

import org.openstreetmap.osmosis.core.container.v0_6.BoundContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityProcessor;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.EntityType;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.RelationMember;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.core.filter.common.IdTracker;
import org.openstreetmap.osmosis.core.filter.common.IdTrackerFactory;
import org.openstreetmap.osmosis.core.filter.common.IdTrackerType;
import org.openstreetmap.osmosis.core.lifecycle.ReleasableIterator;
import org.openstreetmap.osmosis.core.store.SimpleObjectStore;
import org.openstreetmap.osmosis.core.store.SingleClassObjectSerializationFactory;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.core.task.v0_6.SinkSource;


/**
 * Restricts output of nodes to those that are used in ways.
 * 
 * @author Brett Henderson
 * @author Karl Newman
 * @author Christoph Sommer 
 */
public class UsedNodeAndWayFilter implements SinkSource, EntityProcessor {
	private Sink sink;
	private SimpleObjectStore<NodeContainer> allNodes;
	private SimpleObjectStore<WayContainer> allWays;
	private SimpleObjectStore<RelationContainer> allRelations;
	private IdTracker requiredNodes;
	private IdTracker requiredWays;


	/**
	 * Creates a new instance.
	 *
	 * @param idTrackerType
	 *            Defines the id tracker implementation to use.
	 */
	public UsedNodeAndWayFilter(IdTrackerType idTrackerType) {
		allNodes = new SimpleObjectStore<NodeContainer>(
				new SingleClassObjectSerializationFactory(NodeContainer.class), "afnd", true);
		allWays = new SimpleObjectStore<WayContainer>(
				new SingleClassObjectSerializationFactory(WayContainer.class), "afwy", true);
		allRelations = new SimpleObjectStore<RelationContainer>(
				new SingleClassObjectSerializationFactory(RelationContainer.class), "afrl", true);

		requiredNodes = IdTrackerFactory.createInstance(idTrackerType);
		requiredWays = IdTrackerFactory.createInstance(idTrackerType);
		
	}


	/**
	 * {@inheritDoc}
	 */
	public void process(EntityContainer entityContainer) {
		// Ask the entity container to invoke the appropriate processing method
		// for the entity type.
		entityContainer.process(this);
	}


	/**
	 * {@inheritDoc}
	 */
	public void process(BoundContainer boundContainer) {
		// By default, pass it on unchanged
		sink.process(boundContainer);
	}


	/**
	 * {@inheritDoc}
	 */
	public void process(NodeContainer container) {
		allNodes.add(container);
	}


	/**
	 * {@inheritDoc}
	 */
	public void process(WayContainer container) {
		allWays.add(container);
	}


	/**
	 * {@inheritDoc}
	 */
	public void process(RelationContainer container) {
		Relation relation;

		relation = container.getEntity();
		for (RelationMember reference : relation.getMembers()) {
			if (reference.getMemberType().equals(EntityType.Node)) {
				requiredNodes.set(reference.getMemberId());
			} else if (reference.getMemberType().equals(EntityType.Way)) {
				requiredWays.set(reference.getMemberId());
			}
		}
		allRelations.add(container);
	}


	/**
	 * {@inheritDoc}
	 */
	public void complete() {

		ReleasableIterator<WayContainer> wayIterator = allWays.iterate();
		while (wayIterator.hasNext()) {
			WayContainer wayContainer = wayIterator.next();
			long wayId = wayContainer.getEntity().getId();
			if (!requiredWays.get(wayId)) {
				continue;
			}
			for (WayNode nodeReference : wayContainer.getEntity().getWayNodes()) {
				requiredNodes.set(nodeReference.getNodeId());
			}
		}
		wayIterator.release();


		// send on all required nodes
		ReleasableIterator<NodeContainer> nodeIterator = allNodes.iterate();
		while (nodeIterator.hasNext()) {
			NodeContainer nodeContainer = nodeIterator.next();
			long nodeId = nodeContainer.getEntity().getId();
			if (!requiredNodes.get(nodeId)) {
				continue;
			}
			sink.process(nodeContainer);
		}
		nodeIterator.release();
		nodeIterator = null;

		wayIterator = allWays.iterate();
		while (wayIterator.hasNext()) {
			WayContainer way = wayIterator.next();
			if (!requiredWays.get(way.getEntity().getId())) {
				continue;
			}
			sink.process(way);
		}
		wayIterator.release();
		wayIterator = null;

		// send on all relations
		ReleasableIterator<RelationContainer> relationIterator = allRelations.iterate();
		while (relationIterator.hasNext()) {
			sink.process(relationIterator.next());
		}
		relationIterator.release();
		relationIterator = null;

		// done
		sink.complete();
	}


	/**
	 * {@inheritDoc}
	 */
	public void release() {
		if (allNodes != null) {
			allNodes.release();
		}
		if (allWays != null) {
			allWays.release();			
		}
		if (allRelations != null) {
			allRelations.release();
		}
		sink.release();
	}


	/**
	 * {@inheritDoc}
	 */
	public void setSink(Sink sink) {
		this.sink = sink;
	}
}
