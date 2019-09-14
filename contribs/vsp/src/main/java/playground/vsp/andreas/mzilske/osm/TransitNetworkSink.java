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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
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
	
	static final Logger log = Logger.getLogger(TransitNetworkSink.class);

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

	private CoordinateTransformation coordinateTransformation;

	private TreeSet<String> transitModes;

	public TransitNetworkSink(Network network, TransitSchedule transitSchedule, CoordinateTransformation coordinateTransformation, IdTrackerType idTrackerType) {
		this.network = network;
		this.transitSchedule = transitSchedule;
		this.coordinateTransformation = coordinateTransformation;
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
					log.info(count + " nodes processed so far");
			}

			@Override
			public void process(RelationContainer relationContainer) {
				Relation relation = relationContainer.getEntity();
				Map<String, String> tags = new TagCollectionImpl(relation.getTags()).buildMap();
				if ("route".equals(tags.get("type"))){
					if(tags.get("route") == null){
						log.info("Got an empty route tag for tags " + tags.toString());
					} else {
						if(getTransitModes().contains(tags.get("route"))) {//&& "tram".equals(tags.get("route"))) {
							transitLines.add(relationContainer);
						}
					}
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
		log.info("Processing transit lines...");
		while (transitLineIterator.hasNext()) {
			Relation relation = transitLineIterator.next().getEntity();
			Map<String, String> tags = new TagCollectionImpl(relation.getTags()).buildMap();
			String route = tags.get("route");
			String ref = tags.get("ref");
			if(ref != null){
				ref = ref.replace('"', ' ').trim();
				ref = ref.replace('&', ' ').trim();
			}
			String operator = tags.get("operator");
			String name = tags.get("name");
			if(name != null){
				name = name.replace('"', ' ').trim();
				name = name.replace('&', ' ').trim();
			}
			String networkOperator = tags.get("network");
			System.out.println(networkOperator + " // " + route + " // " + ref + " // " + operator + " // " + name);
			TransitLine line = transitSchedule.getFactory().createTransitLine(Id.create(operator + "-" + route + "-" + ref + "-" + name + "-" + relation.getId(), TransitLine.class));
			LinkedList<Node> stopsH = new LinkedList<>();
			LinkedList<Node> stopsR = new LinkedList<>();
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
						log.info("--- Missing way: " + relationMember.getMemberId());
					}
				} 
			}
			
			for (RelationMember relationMember : relation.getMembers()) {
				if (relationMember.getMemberType().equals(EntityType.Node)) {
					if (allNodesTracker.get(relationMember.getMemberId())) {
//						System.out.println(relationMember.getMemberId());
						Node node = nodeReader.get(relationMember.getMemberId()).getEntity();
						Coord coordinate = coordinateTransformation.transform(new Coord(node.getLongitude(), node.getLatitude()));
						String role = relationMember.getMemberRole();
						if (role.isEmpty() || role.startsWith("stop")  /* || role.startsWith("platform") */ ) {
							stopsH.addLast(node);
							stopsR.addFirst(node);
							stitcher.addForwardStop(node);
							stitcher.addBackwardStop(node);
						} else if (role.startsWith("forward")) {
							stopsH.addLast(node);
							stitcher.addForwardStop(node);
						} else if (role.startsWith("backward")) {
							stopsR.addFirst(node);
							stitcher.addBackwardStop(node);
						} else {
							log.info("--- Unknown role: " + role);
						}
					} else {
						log.info("--- Missing node: " + relationMember.getMemberId());
					}
				}
			}
			
			
			
			List<Id<Link>> linkIdsH = stitcher.getForwardRoute();
			List<Id<Link>> linkIdsR = stitcher.getBackwardRoute();
			
			if (linkIdsH.size() >= 2) {
				NetworkRoute networkRouteH = createNetworkRoute(linkIdsH);
				List<Id<Link>> stopLinkIdsH = stitcher.getForwardStopLinks();
				List<Double> forwardTravelTimes = stitcher.getForwardTravelTimes();
				assert (stopLinkIdsH.size() == stopsH.size() - 1);
				List<TransitRouteStop> stops = enterStopLinkIds(stopsH, stopLinkIdsH, forwardTravelTimes, line.getId() + "H", network.getLinks().get(linkIdsH.get(0)).getFromNode());
				TransitRoute routeH = transitSchedule.getFactory().createTransitRoute(Id.create("H", TransitRoute.class), networkRouteH, stops, TransportMode.pt);
				line.addRoute(routeH);
			}

			if (linkIdsR.size() >= 2) {
				NetworkRoute networkRouteR = createNetworkRoute(linkIdsR);
				List<Id<Link>> stopLinkIdsR = stitcher.getBackwardStopLinks();
				List<Double> backwardTravelTimes = stitcher.getBackwardTravelTimes();
				assert (stopLinkIdsR.size() == stopsR.size() - 1);
				List<TransitRouteStop> stops = enterStopLinkIds(stopsR, stopLinkIdsR, backwardTravelTimes, line.getId() + "R", network.getLinks().get(linkIdsR.get(0)).getFromNode());
				TransitRoute routeR = transitSchedule.getFactory().createTransitRoute(Id.create("R", TransitRoute.class), networkRouteR, stops, TransportMode.pt);
				line.addRoute(routeR);
			}

			transitSchedule.addTransitLine(line);
			
		}
		transitLineIterator.release();
		nodeReader.release();
		wayReader.release();
	}

	private List<TransitRouteStop> enterStopLinkIds(List<Node> stopNodes, List<Id<Link>> stopLinkIdsH, List<Double> travelTimes, String routeRef, org.matsim.api.core.v01.network.Node node) {
		System.out.println(stopNodes.size() + "__" + stopLinkIdsH.size());
		int stopNo = 0;
		List<TransitRouteStop> transitRouteStops = new ArrayList<TransitRouteStop>();
		Iterator<Id<Link>> i = stopLinkIdsH.iterator();
		Iterator<Node> j = stopNodes.iterator();
		Iterator<Double> k = travelTimes.iterator();
		Node firstStopNode = j.next();
		Coord firstCoordinate = coordinateTransformation.transform(new Coord(firstStopNode.getLongitude(), firstStopNode.getLatitude()));
		Map<String, String> firstStopTags = new TagCollectionImpl(firstStopNode.getTags()).buildMap();
		String firstStopName = firstStopTags.get("name");
		if(firstStopName != null){
			firstStopName = firstStopName.replace('"', ' ').trim();
			firstStopName = firstStopName.replace('&', ' ').trim();
		} else {
			firstStopName = "";
		}
		Link entryLink = network.getFactory().createLink(Id.create(routeRef + firstStopName + "_ENTRY", Link.class), node, node);
		network.addLink(entryLink);
		TransitStopFacility firstFacility = transitSchedule.getFactory().createTransitStopFacility(createTransitStopId(routeRef, firstStopName, stopNo), firstCoordinate, false);
		transitSchedule.addStopFacility(firstFacility);
		stopNo++;
		double time = 0;
		transitRouteStops.add(transitSchedule.getFactory().createTransitRouteStop(firstFacility, time, time));
		Id<Link> linkId = entryLink.getId();
		firstFacility.setLinkId(linkId);
		while(i.hasNext()) {
			Id<Link> nextLinkId = i.next();
			if (nextLinkId != null) {
				linkId = nextLinkId;
			}
			Node stopNode = j.next();
			Coord coordinate = coordinateTransformation.transform(new Coord(stopNode.getLongitude(), stopNode.getLatitude()));
			Map<String, String> stopTags = new TagCollectionImpl(stopNode.getTags()).buildMap();
			String stopName = stopTags.get("name");
			if(stopName != null){
				stopName = stopName.replace('"', ' ').trim();
				stopName = stopName.replace('&', ' ').trim();
			} else {
				stopName = "";
			}
			TransitStopFacility facility = transitSchedule.getFactory().createTransitStopFacility(createTransitStopId(routeRef, stopName, stopNo), coordinate, false);
			stopNo++;
			transitSchedule.addStopFacility(facility);
			time += k.next();
			transitRouteStops.add(transitSchedule.getFactory().createTransitRouteStop(facility, time, time));
			facility.setLinkId(linkId);
		}
		return transitRouteStops;
	}

	private Id<TransitStopFacility> createTransitStopId(String ref, String stopName, int stopNo) {
		return Id.create(ref + "_" + stopName + "_" + stopNo, TransitStopFacility.class);
	}

	private NetworkRoute createNetworkRoute(List<Id<Link>> plinkIds) {
		LinkedList<Id<Link>> linkIds = new LinkedList<Id<Link>>(plinkIds);
		NetworkRoute networkRouteH = RouteUtils.createLinkNetworkRouteImpl(linkIds.getFirst(), linkIds.getLast());
		Id<Link> first = linkIds.removeFirst();
		Id<Link> last = linkIds.removeLast();
		networkRouteH.setLinkIds(first, linkIds, last);
		return networkRouteH;
	}

	@Override
	public void release() {

	}

	public void setTransitModes(String[] transitModes) {
		if(transitModes == null){
			this.transitModes = new TreeSet<String>();
		} else {
			TreeSet<String> modes = new TreeSet<String>();
			for (String string : transitModes) {
				modes.add(string);
			}
			this.transitModes = modes;
		}
	}



	public TreeSet<String> getTransitModes() {
		return this.transitModes;
	}

	@Override
	public void initialize(Map<String, Object> map) {

	}
}
