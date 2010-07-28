package playground.mzilske.osm;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitStopFacility;
import org.openstreetmap.osmosis.core.container.v0_6.BoundContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityProcessor;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.EntityType;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.RelationMember;
import org.openstreetmap.osmosis.core.domain.v0_6.TagCollectionImpl;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.filter.common.IdTracker;
import org.openstreetmap.osmosis.core.filter.common.IdTrackerFactory;
import org.openstreetmap.osmosis.core.filter.common.IdTrackerType;
import org.openstreetmap.osmosis.core.lifecycle.ReleasableIterator;
import org.openstreetmap.osmosis.core.store.IndexedObjectStore;
import org.openstreetmap.osmosis.core.store.IndexedObjectStoreReader;
import org.openstreetmap.osmosis.core.store.SimpleObjectStore;
import org.openstreetmap.osmosis.core.store.SingleClassObjectSerializationFactory;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

public class TransitNetworkSink implements Sink {

	private SimpleObjectStore<NodeContainer> allNodes;

	private IndexedObjectStore<NodeContainer> stopNodeStore;

	private IndexedObjectStore<WayContainer> routeSegmentStore;

	private SimpleObjectStore<WayContainer> allWays;

	private SimpleObjectStore<RelationContainer> transitLines;

	private IdTracker stopNodes;

	private IdTracker routeWays;

	private IdTracker allWaysTracker;

	private IdTracker allNodesTracker;

	private int count = 0;

	private Network network;

	private TransitSchedule transitSchedule;

	public TransitNetworkSink(Network network, TransitSchedule transitSchedule, IdTrackerType idTrackerType) {
		this.network = network;
		this.transitSchedule = transitSchedule;
		stopNodes = IdTrackerFactory.createInstance(idTrackerType);
		routeWays = IdTrackerFactory.createInstance(idTrackerType);
		allWaysTracker = IdTrackerFactory.createInstance(idTrackerType);
		allNodesTracker = IdTrackerFactory.createInstance(idTrackerType);
		allNodes = new SimpleObjectStore<NodeContainer>(
				new SingleClassObjectSerializationFactory(NodeContainer.class),
				"afnd", true);
		stopNodeStore = new IndexedObjectStore<NodeContainer>(
				new SingleClassObjectSerializationFactory(NodeContainer.class),
		"stops");
		routeSegmentStore = new IndexedObjectStore<WayContainer>(
				new SingleClassObjectSerializationFactory(WayContainer.class),
		"routesegments");
		allWays = new SimpleObjectStore<WayContainer>(
				new SingleClassObjectSerializationFactory(WayContainer.class),
				"afwy", true);
		transitLines = new SimpleObjectStore<RelationContainer>(
				new SingleClassObjectSerializationFactory(RelationContainer.class),
				"afrl", true);
	}

	@Override
	public void process(EntityContainer entityContainer) {
		entityContainer.process(new EntityProcessor() {

			@Override
			public void process(BoundContainer arg0) {

			}

			@Override
			public void process(NodeContainer container) {

				// stuff all nodes into a file
				allNodesTracker.set(container.getEntity().getId());
				allNodes.add(container);

				// debug
				count++;
				if (count % 50000 == 0)
					System.out.println(count + " nodes processed so far");
			}

			@Override
			public void process(RelationContainer relationContainer) {
				Relation relation = relationContainer.getEntity();
				Map<String, String> tags = new TagCollectionImpl(relation.getTags()).buildMap();
				if ("route".equals(tags.get("type")) && "bus".equals(tags.get("route"))) {
					transitLines.add(relationContainer);
				}
			}

			@Override
			public void process(WayContainer container) {
				allWaysTracker.set(container.getEntity().getId());
				allWays.add(container);
			}

		});
	}

	@Override
	public void complete() {
		ReleasableIterator<RelationContainer> transitLineIterator = transitLines.iterate();
		while (transitLineIterator.hasNext()) {
			Relation relation = transitLineIterator.next().getEntity();
			for (RelationMember relationMember : relation.getMembers()) {
				if (relationMember.getMemberType().equals(EntityType.Node)) {
					stopNodes.set(relationMember.getMemberId());
				} else if (relationMember.getMemberType().equals(EntityType.Way)) {
					routeWays.set(relationMember.getMemberId());
				} 
			}
		}
		transitLineIterator.release();

		ReleasableIterator<NodeContainer> nodeIterator = allNodes.iterate();
		while (nodeIterator.hasNext()) {
			NodeContainer nodeContainer = nodeIterator.next();
			Node node = nodeContainer.getEntity();
			if (stopNodes.get(node.getId())) {
				System.out.println(node.getId());
				stopNodeStore.add(node.getId(), nodeContainer);
			}
		}
		nodeIterator.release();
		stopNodeStore.complete();

		ReleasableIterator<WayContainer> wayIterator = allWays.iterate();
		while (wayIterator.hasNext()) {
			WayContainer wayContainer = wayIterator.next();
			Way way = wayContainer.getEntity();
			if (routeWays.get(way.getId())) {
				routeSegmentStore.add(way.getId(), wayContainer);
			}
		}
		wayIterator.release();
		routeSegmentStore.complete();

		transitLineIterator = transitLines.iterate();

		IndexedObjectStoreReader<NodeContainer> nodeReader = stopNodeStore.createReader();
		IndexedObjectStoreReader<WayContainer> wayReader = routeSegmentStore.createReader();
		while (transitLineIterator.hasNext()) {
			Relation relation = transitLineIterator.next().getEntity();
			Map<String, String> tags = new TagCollectionImpl(relation.getTags()).buildMap();
			String ref = tags.get("ref");
			String operator = tags.get("operator");
			String name = tags.get("name");
			String networkOperator = tags.get("network");
			System.out.println(networkOperator + " // " + ref + " // " + operator + " // " + name);
			TransitLine line = transitSchedule.getFactory().createTransitLine(new IdImpl(ref + "-" + relation.getId()));
			LinkedList<TransitRouteStop> stopsH = new LinkedList<TransitRouteStop>();
			LinkedList<TransitRouteStop> stopsR = new LinkedList<TransitRouteStop>();
			Stitcher stitcher = new Stitcher(network);
			
			for (RelationMember relationMember : relation.getMembers()) {
				if (relationMember.getMemberType().equals(EntityType.Way)) {
					if (allWaysTracker.get(relationMember.getMemberId())) {
						Way way = wayReader.get(relationMember.getMemberId()).getEntity();
						String role = relationMember.getMemberRole();
						if (role.isEmpty() || role.startsWith("route")) {
							stitcher.addBoth(way);
						} else if (role.startsWith("forward")) {
							stitcher.addForward(way);
						} else if (role.startsWith("backward")) {
							stitcher.addBackward(way);
						}
					} else {
						System.out.println("Missing way: " + relationMember.getMemberId());
					}
				} 
			}
			
			for (RelationMember relationMember : relation.getMembers()) {
				if (relationMember.getMemberType().equals(EntityType.Node)) {
					if (allNodesTracker.get(relationMember.getMemberId())) {
						System.out.println(relationMember.getMemberId());
						Node node = nodeReader.get(relationMember.getMemberId()).getEntity();
						Coord coordinate = new CoordImpl(node.getLongitude(), node.getLatitude());
						TransitStopFacility facility = transitSchedule.getFactory().createTransitStopFacility(new IdImpl(node.getId()), coordinate, false);
						String role = relationMember.getMemberRole();
						if (role.startsWith("stop")) {
							stopsH.addLast(transitSchedule.getFactory().createTransitRouteStop(facility, 0, 0));
							stopsR.addFirst(transitSchedule.getFactory().createTransitRouteStop(facility, 0, 0));
							stitcher.addForwardStop(node);
							stitcher.addBackwardStop(node);
						} else if (role.startsWith("forward:stop")) {
							stopsH.addLast(transitSchedule.getFactory().createTransitRouteStop(facility, 0, 0));
							stitcher.addForwardStop(node);
						} else if (role.startsWith("backward:stop")) {
							stopsR.addFirst(transitSchedule.getFactory().createTransitRouteStop(facility, 0, 0));
							stitcher.addBackwardStop(node);
						} else {
							System.out.println("Unknown role: " + role);
						}
					} else {
						System.out.println("Missing node: " + relationMember.getMemberId());
					}
				}
			}
			
			
			
			List<Id> linkIdsH = stitcher.getForwardRoute();
			List<Id> linkIdsR = stitcher.getBackwardRoute();
			
			if (linkIdsH.size() >= 2) {
				NetworkRoute networkRouteH = createNetworkRoute(linkIdsH);
				TransitRoute routeH = transitSchedule.getFactory().createTransitRoute(new IdImpl("H"), networkRouteH, stopsH, TransportMode.pt);
				line.addRoute(routeH);
			}

			if (linkIdsR.size() >= 2) {
				NetworkRoute networkRouteR = createNetworkRoute(linkIdsR);
				TransitRoute routeR = transitSchedule.getFactory().createTransitRoute(new IdImpl("R"), networkRouteR, stopsR, TransportMode.pt);
				line.addRoute(routeR);
			}

			transitSchedule.addTransitLine(line);
		}
		transitLineIterator.release();
		nodeReader.release();
		wayReader.release();
	}

	private NetworkRoute createNetworkRoute(List<Id> plinkIds) {
		LinkedList<Id> linkIds = new LinkedList<Id>(plinkIds);
		NetworkRoute networkRouteH = (NetworkRoute) ((NetworkFactoryImpl) network.getFactory()).createRoute(TransportMode.car, linkIds.getFirst(), linkIds.getLast());
		Id first = linkIds.removeFirst();
		Id last = linkIds.removeLast();
		networkRouteH.setLinkIds(first, linkIds, last);
		return networkRouteH;
	}

	@Override
	public void release() {

	}

}
