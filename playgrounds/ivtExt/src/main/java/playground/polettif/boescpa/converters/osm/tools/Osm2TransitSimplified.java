/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
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
 * *********************************************************************** *
 */

package playground.polettif.boescpa.converters.osm.tools;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.population.routes.RouteFactoryImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.pt.transitSchedule.api.*;
import playground.polettif.boescpa.converters.osm.networkCreator.osmWithPT.*;

import java.util.*;

/**
 * This is a simplified version of playground.scnadine.converters.osmPT.Osm2TransitLines.
 * Most of all, the ordering of stations and the creation of paths is removed.
 *
 * @author boescpa
 */
public class Osm2TransitSimplified {
	private final static Logger log = Logger.getLogger(Osm2TransitSimplified.class);

	private final TransitSchedule schedule;
	private final Network network;

	// /*package*/ final Set<Long> relationIds = new HashSet<Long>();
	/* package */final Map<Long, OsmParser.OsmRelation> relations = new HashMap<Long, OsmParser.OsmRelation>();
	/* package */final Map<Long, OsmParser.OsmWay> ways = new HashMap<Long, OsmParser.OsmWay>();
	/* package */final Map<Long, OsmParser.OsmNode> nodes = new HashMap<Long, OsmParser.OsmNode>();
	/* package */final Map<Long, OsmParser.OsmNode> stopNodes = new HashMap<Long, OsmParser.OsmNode>();

	/* package */final Set<Long> wayIds = new HashSet<Long>();
	/* package */final Set<Long> nodeIds = new HashSet<Long>();
	/* package */final Set<Long> stopNodeIds = new HashSet<Long>();

	/* package */final Map<Long, List<Id<Link>>> waysToLinks_DirA = new HashMap<>();
	/* package */final Map<Long, List<Id<Link>>> waysToLinks_DirB = new HashMap<>();

	public Osm2TransitSimplified(final TransitSchedule schedule,
							final Network network) {
		this.schedule = schedule;
		this.network = network;
	}

	public Osm2TransitSimplified(final Network network) {
		// TODO-boescpa If this constructor is called, only the network must be created!

		this.schedule = null;
		this.network = network;
	}

	public Osm2TransitSimplified(final TransitSchedule schedule) {
		// TODO-boescpa If this constructor is called, only the schedule must be created!

		this.schedule = schedule;
		this.network = null;
	}

	public void convert(final String filename) {
		log.info("Parsing OSM file: extracting routes and stops...");
		collectTransitRouteWays(filename, this.relations, this.wayIds,
				this.nodeIds, this.stopNodeIds);

		log.info("Parsing OSM file: extracting ways...");
		collectWays(filename, this.wayIds, this.nodeIds, this.ways);

		log.info("Parsing OSM file: extracting nodes...");
		collectNodes(filename, this.nodeIds, this.stopNodeIds, this.nodes,
				this.stopNodes);

		log.info("Creating the network...");
		createNetworkOfTransitRoutes(this.nodes, this.ways,
				this.waysToLinks_DirA, this.waysToLinks_DirB, this.network);

		log.info("Creating the schedule...");
		createSchedule(this.relations, this.stopNodes, this.ways, this.nodes,
				this.waysToLinks_DirA, this.waysToLinks_DirB, this.schedule,
				this.network);

		log.info("# stops in schedule " + this.schedule.getFacilities().size());

	}

	private TransitRouteWaysAndStopsCollector collectTransitRouteWays(final String filename, final Map<Long, OsmParser.OsmRelation> relations,final Set<Long> wayIds, final Set<Long> nodeIds,final Set<Long> stopNodeIds) {
		OsmParser parser = new OsmParser();
		TransitRouteWaysAndStopsCollector collector = new TransitRouteWaysAndStopsCollector(relations, wayIds, nodeIds, stopNodeIds);
		parser.addHandler(collector);
		parser.readFile(filename);
		return collector;
	}

	private WaysCollector collectWays(final String filename, final Set<Long> wayIds, final Set<Long> nodeIds, final Map<Long, OsmParser.OsmWay> ways) {
		OsmParser parser = new OsmParser();
		WaysCollector collector = new WaysCollector(wayIds, nodeIds, ways);
		parser.addHandler(collector);
		parser.readFile(filename);
		return collector;
	}

	private NodesCollector collectNodes(final String filename, final Set<Long> nodeIds, final Set<Long> stopNodeIds, final Map<Long, OsmParser.OsmNode> nodes, final Map<Long, OsmParser.OsmNode> stopNodes) {
		OsmParser parser = new OsmParser();
		NodesCollector collector = new NodesCollector(nodeIds, stopNodeIds,	nodes, stopNodes);
		parser.addHandler(collector);
		parser.readFile(filename);
		return collector;
	}

	private void createNetworkOfTransitRoutes(final Map<Long, OsmParser.OsmNode> nodes, final Map<Long, OsmParser.OsmWay> ways,	final Map<Long, List<Id<Link>>> waysToLinks_DirA,	final Map<Long, List<Id<Link>>> waysToLinks_DirB, final Network network) {
		NetworkCreator creator = new NetworkCreator(nodes, ways,
				waysToLinks_DirA, waysToLinks_DirB, network);
		creator.createNodes();
		creator.createLinks();
	}

	private void createSchedule(final Map<Long, OsmParser.OsmRelation> relations,	final Map<Long, OsmParser.OsmNode> stopNodes, final Map<Long, OsmParser.OsmWay> ways, final Map<Long, OsmParser.OsmNode> nodes, Map<Long, List<Id<Link>>> waysToLinks_DirA,	Map<Long, List<Id<Link>>> waysToLinks_DirB,
								final TransitSchedule schedule, final Network network) {
		ScheduleCreator creator = new ScheduleCreator(relations, stopNodes,	ways, nodes, waysToLinks_DirA, waysToLinks_DirB, schedule, network);
		creator.createStops();
		creator.createLines();
	}

	private static class TransitRouteWaysAndStopsCollector implements
			OsmRelationHandler {

		private final TagFilter filter;
		private final Map<Long, OsmParser.OsmRelation> relations;
		private final Set<Long> wayIds;
		private final Set<Long> nodeIds;
		private final Set<Long> stopNodeIds;

		/* package */final Set<String> unhandledRouteTypes = new HashSet<String>();

		public TransitRouteWaysAndStopsCollector(final Map<Long, OsmParser.OsmRelation> relations, final Set<Long> wayIds, final Set<Long> nodeIds, final Set<Long> stopNodeIds) {
			this.relations = relations;
			this.wayIds = wayIds;
			this.nodeIds = nodeIds;
			this.stopNodeIds = stopNodeIds;
			this.filter = new TagFilter();
			this.filter.add("route", "train");
			this.filter.add("route", "rail");
			this.filter.add("route", "railway");
			this.filter.add("route", "light_rail");
			this.filter.add("route", "bus");
			this.filter.add("route", "trolleybus");
			this.filter.add("route", "tram");
			this.filter.add("route", "ship");
			this.filter.add("route", "ferry");
			this.filter.add("route", "cable_car");
			this.filter.add("route", "funicular");
			this.filter.add("route", "funiculair");
			this.filter.add("route", "subway");
		}

		@Override
		public void handleRelation(OsmParser.OsmRelation relation) {

			if (this.filter.matches(relation.tags)) {
				this.relations.put(relation.id, relation);
				for (OsmParser.OsmRelationMember member : relation.members) {
					if (member.type == OsmParser.OsmRelationMemberType.WAY) {
						this.wayIds.add(member.refId);
					} else if (member.type == OsmParser.OsmRelationMemberType.NODE) {
						this.nodeIds.add(member.refId);
						if (member.role.contains("stop")) {
							this.stopNodeIds.add(member.refId);
						}
					}
					// TODO: deal with relation members that are relations
				}
			} else {
				if (relation.tags.containsKey("route")) {
					if (this.unhandledRouteTypes
							.add(relation.tags.get("route"))) {
						log.info("route-type " + relation.tags.get("route")
								+ " not handled.");
					}
				}
			}
		}

	}

	private static class WaysCollector implements OsmWayHandler {

		private final TagFilter filter;
		private final Set<Long> wayIds;
		private final Set<Long> nodeIds;

		private final Map<Long, OsmParser.OsmWay> ways;

		public WaysCollector(final Set<Long> wayIds, final Set<Long> nodeIds,
							 final Map<Long, OsmParser.OsmWay> ways) {
			this.filter = new TagFilter();
			this.wayIds = wayIds;
			this.nodeIds = nodeIds;
			this.ways = ways;
			this.filter.add("public_transport", "platform");
		}

		@Override
		public void handleWay(OsmParser.OsmWay way) {
			if (this.wayIds.contains(way.id) && !this.filter.matches(way.tags)) {
				this.ways.put(way.id, way);
				this.nodeIds.addAll(way.nodes);
			}
		}
	}

	private static class NodesCollector implements OsmNodeHandler {

		private final Set<Long> nodeIds;
		private final Set<Long> stopNodeIds;

		private final Map<Long, OsmParser.OsmNode> nodes;
		private final Map<Long, OsmParser.OsmNode> stopNodes;

		public NodesCollector(final Set<Long> nodeIds,
							  final Set<Long> stopNodeIds, final Map<Long, OsmParser.OsmNode> nodes,
							  final Map<Long, OsmParser.OsmNode> stopNodes) {
			this.nodeIds = nodeIds;
			this.stopNodeIds = stopNodeIds;
			this.nodes = nodes;
			this.stopNodes = stopNodes;
		}

		@Override
		public void handleNode(OsmParser.OsmNode node) {
			if (this.nodeIds.contains(node.id)) {
				this.nodes.put(node.id, node);
			}

			if (this.stopNodeIds.contains(node.id)) {
				this.stopNodes.put(node.id, node);
			}
		}
	}

	private static class NetworkCreator {

		private final Map<Long, OsmParser.OsmNode> nodes;
		private final Map<Long, OsmParser.OsmWay> ways;

		private final Map<Long, List<Id<Link>>> waysToLinks_DirA;
		private final Map<Long, List<Id<Link>>> waysToLinks_DirB;

		private final Network network;
		private final NetworkFactory factory;

		public NetworkCreator(final Map<Long, OsmParser.OsmNode> nodes,
							  final Map<Long, OsmParser.OsmWay> ways,
							  final Map<Long, List<Id<Link>>> waysToLinks_DirA,
							  final Map<Long, List<Id<Link>>> waysToLinks_DirB,
							  final Network network) {
			this.nodes = nodes;
			this.ways = ways;
			this.waysToLinks_DirA = waysToLinks_DirA;
			this.waysToLinks_DirB = waysToLinks_DirB;
			this.network = network;
			this.factory = network.getFactory();
		}

		public void createNodes() {
			for (OsmParser.OsmNode node : this.nodes.values()) {
				Node netNode = this.factory.createNode(Id.create(node.id, Node.class),
						node.coord);
				this.network.addNode(netNode);
			}
		}

		public void createLinks() {
			for (OsmParser.OsmWay way : this.ways.values()) {
				List<Id<Link>> links_DirA = new LinkedList<>();
				List<Id<Link>> links_DirB = new LinkedList<>();

				// first create all links in the same direction as coded in the
				// way
				Node fromNode = this.network.getNodes().get(
						Id.create(way.nodes.get(0), Node.class));
				int linkCounter = 0;

				for (int i = 1; i < way.nodes.size(); i++) {
					Node toNode = this.network.getNodes().get(
							Id.create(way.nodes.get(i), Node.class));

					Id<Link> linkId = Id.create(way.id + "_" + linkCounter + "A", Link.class);
					Link link = this.factory.createLink(linkId, fromNode, toNode);
					//TODO:Calc and set link length
					this.network.addLink(link);
					links_DirA.add(linkId);

					linkCounter++;
					fromNode = toNode;
				}

				linkCounter--;

				for (int i = way.nodes.size() - 2; i >= 0; i--) {
					Node toNode = this.network.getNodes().get(
							Id.create(way.nodes.get(i), Node.class));

					Id<Link> linkId = Id.create(way.id + "_" + linkCounter + "B", Link.class);
					Link link = this.factory.createLink(linkId, fromNode, toNode);
					//TODO:Calc and set link length
					this.network.addLink(link);
					links_DirB.add(linkId);

					fromNode = toNode;
					linkCounter--;
				}

				this.waysToLinks_DirA.put(way.id, links_DirA);
				this.waysToLinks_DirB.put(way.id, links_DirB);

			}
		}

	}

	private static class ScheduleCreator {

		private final Map<Long, OsmParser.OsmRelation> relations;
		private final Map<Long, OsmParser.OsmNode> stopNodes;
		private final Map<Long, OsmParser.OsmWay> ways;
		private final Map<Long, OsmParser.OsmNode> nodes;

		private final Map<Long, List<Id<Link>>> waysToLinks_DirA;
		private final Map<Long, List<Id<Link>>> waysToLinks_DirB;

		private final TransitSchedule schedule;
		private final TransitScheduleFactory scheduleBuilder;
		private final Network network;
		private final TagFilter filter;
		private final NetworkFactory factory;


		public ScheduleCreator(final Map<Long, OsmParser.OsmRelation> relations,
							   final Map<Long, OsmParser.OsmNode> stopNodes,
							   final Map<Long, OsmParser.OsmWay> ways, final Map<Long, OsmParser.OsmNode> nodes,
							   Map<Long, List<Id<Link>>> waysToLinks_DirA,
							   Map<Long, List<Id<Link>>> waysToLinks_DirB,
							   final TransitSchedule schedule, final Network network) {
			this.relations = relations;
			this.stopNodes = stopNodes;
			this.ways = ways;
			this.nodes = nodes;
			this.waysToLinks_DirA = waysToLinks_DirA;
			this.waysToLinks_DirB = waysToLinks_DirB;
			this.schedule = schedule;
			this.scheduleBuilder = schedule.getFactory();
			this.network = network;
			this.factory = network.getFactory();
			this.filter = new TagFilter();
			this.filter.add("public_transport", "platform");
		}

		public void createStops() {
			for (OsmParser.OsmNode node : this.stopNodes.values()) {
				TransitStopFacility stopFacility = createStop(node);
				this.schedule.addStopFacility(stopFacility);
			}
		}

		///////////////////////////////////////////////
		//IN PROGRESS
		///////////////////////////////////////////////

		public void createLines() {

			for (OsmParser.OsmRelation relation : this.relations.values()) {

				System.out.println();

				try {

					List<OsmParser.OsmNode> relationStops = new ArrayList<OsmParser.OsmNode>();
					Map<Long, OsmParser.OsmWay> relationWays = new HashMap<Long, OsmParser.OsmWay>();
					List<Long> sortedWays = new LinkedList<Long>();
					List<Id<Link>> sortedLinks = new LinkedList<Id<Link>>();

					for (OsmParser.OsmRelationMember member : relation.members) {

						if (member.type == OsmParser.OsmRelationMemberType.RELATION) {

							// TODO: deal with relation members that are
							// relations

						}

						else if (member.type == OsmParser.OsmRelationMemberType.NODE) {
							if (member.role.contains("stop")) {
								relationStops.add(this.nodes.get(member.refId));
							}
							// TODO: deal with stops that
							// are part of the relation but not marked as stops;
							// mainly a data problem, should be corrected in the
							// data; talk to Anna Lena
							// work around: check if node has tag: railway =
							// tram_stop or highway = bus_stop than add as stop
							// for rail not a good idea, should be corrected in
							// the data! => could also be moved to TransitRouteWaysAndStopsCollector
						} else if (member.type == OsmParser.OsmRelationMemberType.WAY) {

							if (this.ways.get(member.refId) != null) {
								relationWays.put(member.refId, this.ways.get(member.refId));
							}
						}
					}


					String relationName;
					if (relation.tags.get("name") != null) {
						String name = relation.tags.get("name");
						String ref = relation.tags.get("ref");
						if (name.contains(ref)) {
							relationName = name;
						}
						else {
							relationName = relation.tags.get("route")+" "+ref+": "+name;
						}
					}
					else if (relation.tags.get("description") != null) {

						String name = relation.tags.get("description");
						String ref = relation.tags.get("ref");
						if (name.contains(ref)) {
							relationName = name;
						}
						else {
							relationName = relation.tags.get("route")+" "+ref+": "+name;
						}
					}
					else if (relation.tags.get("route").equals("train") || relation.tags.get("route").equals("rail") || relation.tags.get("route").equals("railway")){
						relationName = "train_"+relation.tags.get("ref");
					}
					else {
						relationName = relation.tags.get("ref");
					}

					// create route profile
					List<Id<TransitStopFacility>> relationStopIds = new ArrayList<>();
					for (OsmParser.OsmNode node : relationStops) {

						Id<TransitStopFacility> nodeId = Id.create(node.id, TransitStopFacility.class);
						if (this.schedule.getFacilities().containsKey(nodeId)) {
							relationStopIds.add(nodeId);
						} else {
							log.warn("stop "+ nodeId+ " ("+ node.tags.get("name")+ ") is not yet a stop facility. Add stop facility.");
							TransitStopFacility stopFacility = createStop(node);
							this.schedule.addStopFacility(stopFacility);
							relationStopIds.add(nodeId);
						}

					}

					// create link route
					List<Long> wayIds = new LinkedList<Long>();
					for (long wayId : relationWays.keySet()) {

						wayIds.add(wayId);
					}

					log.info("sort ways for relation "+relation.id+", "+ relationName);
					log.info("# ways = "+relationWays.size());
					sortWays(sortedWays, sortedLinks, wayIds, relationWays);

					NetworkRoute route = null;
					RouteFactoryImpl routeFactory = new RouteFactoryImpl();
					if (sortedLinks.size() > 1) {
						route = routeFactory.createRoute(NetworkRoute.class, sortedLinks.get(0), sortedLinks.get(sortedLinks.size() - 1));
						route.setLinkIds(sortedLinks.get(0),	sortedLinks.subList(1, sortedLinks.size() - 1), sortedLinks.get(sortedLinks.size() - 1));
					}
					else if (sortedLinks.size() == 1) {
						log.warn("relation " + relation.id+ " contains only 1 link!");
						route = routeFactory.createRoute(NetworkRoute.class, sortedLinks.get(0), sortedLinks.get(0));
					} else {
						log.warn("relation " + relation.id + " contains no links!");
					}



					//create stop route
					log.info("sort stops for relation "+relation.id+", "+ relationName);
					log.info("# stops = "+relationStops.size());
					List<Id<TransitStopFacility>> sortedStops = sortStops(relationStopIds);

					List<TransitRouteStop> stops = new LinkedList<TransitRouteStop>();

					for (Id<TransitStopFacility> stopId : sortedStops) {
						TransitStopFacility stop = this.schedule.getFacilities().get(stopId);
						double departureOffset = calcDepartureOffSet(stops,	stop);
						TransitRouteStop routeStop = scheduleBuilder.createTransitRouteStop(stop, departureOffset, departureOffset + 30);
						stops.add(routeStop);
					}



					// create transit route
					TransitRoute transitRoute = scheduleBuilder.createTransitRoute(Id.create("0", TransitRoute.class), route, stops, relation.tags.get("route"));

					String routeName = relationName.concat("_directionA");
					transitRoute.setDescription(routeName);

					// TODO: check if the return direction exists as separate
					// relation, if not create return direction (same line but
					// different route)
					// when no return direction in same relation: create,
					// because if in doubt better have two than none!

					// create transit line
					Id<TransitLine> lineId = Id.create(relation.id, TransitLine.class);
					TransitLine line = scheduleBuilder.createTransitLine(lineId);

					line.addRoute(transitRoute);
					if (relation.tags.containsKey("name")) {
						line.setName(relation.tags.get("name"));
					}

					schedule.addTransitLine(line);

				} catch (Exception e) {
					log.warn(e.getMessage());
				}

			}
		}


		// sorting methods
		private List<Id<TransitStopFacility>> sortStops(List<Id<TransitStopFacility>> stops) {
			List<Id<TransitStopFacility>> sortedStops = new LinkedList<>();
			// TODO: sort stops into correct order -> how?????
			sortedStops = stops;
			log.info("sorted stops: "+sortedStops);
			return sortedStops;
		}

		private void sortWays(List<Long> sortedWays, List<Id<Link>> sortedLinks, List<Long> unsortedWays, Map<Long, OsmParser.OsmWay> relationWays) {

			log.info("unsorted ways = "+unsortedWays);

			if (unsortedWays.size() > 1) {

				// store links in a Hashmap: keys: nodes, values: links connected to these nodes
				HashMap<Long, List<Long>> unprocessedNodesAndConnectedWays = new HashMap<Long, List<Long>>();

				for (long wayId : unsortedWays) {

					OsmParser.OsmWay way = relationWays.get(wayId);

					long fromNodeId = way.getStartNode();
					long toNodeId = way.getEndNode();

					List<Long> fromNodeWays;
					if (unprocessedNodesAndConnectedWays.containsKey(fromNodeId)) {
						fromNodeWays = unprocessedNodesAndConnectedWays.get(fromNodeId);
					}
					else {
						fromNodeWays = new ArrayList<Long>();
					}
					fromNodeWays.add(wayId);
					unprocessedNodesAndConnectedWays.put(fromNodeId, fromNodeWays);

					if (fromNodeId != toNodeId) {
						List<Long> toNodeLinks;
						if (unprocessedNodesAndConnectedWays.containsKey(toNodeId)) {
							toNodeLinks = unprocessedNodesAndConnectedWays.get(toNodeId);
						}
						else {
							toNodeLinks = new ArrayList<Long>();
						}
						toNodeLinks.add(wayId);
						unprocessedNodesAndConnectedWays.put(toNodeId, toNodeLinks);
					}



				}

				System.out.println("Node to link map: ");
				for (long nodeId : unprocessedNodesAndConnectedWays.keySet()) {
					System.out.print("Node "+nodeId+": ");
					for (long linkId : unprocessedNodesAndConnectedWays.get(nodeId)) {
						System.out.print(linkId+", ");
					}
					System.out.println();
				}
				System.out.println();


				List<Long> forkNodes = new ArrayList<Long>();
				List<Long> intermediateLoopNodes = new ArrayList<Long>();
				List<Long> endNodes = new ArrayList<Long>();
				List<Long> loopWays = new ArrayList<Long>();
				Map<Long, List<Long>> loopWayNodes	= new HashMap<Long, List<Long>>();

				for (long nodeId : unprocessedNodesAndConnectedWays.keySet()) {
					int numberOfConnectedLinks = unprocessedNodesAndConnectedWays.get(nodeId).size();
					if (numberOfConnectedLinks == 3) {
						forkNodes.add(nodeId);
					}
					else if (numberOfConnectedLinks == 4) {
						intermediateLoopNodes.add(nodeId);
					}
					else if (numberOfConnectedLinks == 1) {
						endNodes.add(nodeId);
					}
				}

				for (long wayId : unsortedWays) {
					OsmParser.OsmWay way = relationWays.get(wayId);
					if (way.getStartNode() == way.getEndNode()) {
						loopWays.add(wayId);
						for (long nodeId : way.nodes) {
							List<Long> wayIds = loopWayNodes.get(nodeId);
							if (wayIds == null) {
								wayIds = new ArrayList<Long>();
							}
							wayIds.add(wayId);
							loopWayNodes.put(nodeId, wayIds);
						}
					}
				}

				//if there are loopways: remove any nodes that belong to a loopway from the set of end nodes
				if (!loopWayNodes.isEmpty()) {
					Iterator<Long> it = endNodes.iterator();
					while(it.hasNext()) {
						long nodeId = it.next();
						if (loopWayNodes.keySet().contains(nodeId)) {
							it.remove();
						}
					}
				}

				log.info("# end nodes = "+endNodes.size());
				if (endNodes.size() > 0) {
					log.info("End nodes: "+endNodes);
				}
				log.info("# fork nodes = "+forkNodes.size());
				if (forkNodes.size() > 0) {
					log.info("Fork nodes: "+forkNodes);
				}
				log.info("# intermediate loops = "+intermediateLoopNodes.size());
				if (intermediateLoopNodes.size() > 0) {
					log.info("Intermediate loop nodes: "+intermediateLoopNodes);
				}
				log.info("# loop ways = "+loopWays.size());
				if (loopWays.size() > 0) {
					log.info("Loop ways: "+loopWays);
				}


				// determine a potential start node
				// if there is at least one node with just one connected links: this has to be an end of the route
				// else start with the from node of a random link
				long startNodeId = 0;
				if (endNodes.size() > 0) {
					startNodeId = endNodes.get(0);
				}
				else {
					startNodeId = relationWays.get(unsortedWays.get(0)).getStartNode();
				}
				developWays(sortedWays, sortedLinks, relationWays, loopWays, loopWayNodes, unprocessedNodesAndConnectedWays, startNodeId);
				log.info("Way development done...");
			}
			else {
				sortedWays.add(unsortedWays.get(0));
				List<Id<Link>> linkIds = translateWayToLinks(sortedWays.get(0), relationWays, relationWays.get(sortedWays.get(0)).getStartNode());
				for (Id<Link> linkId : linkIds) {
					sortedLinks.add(linkId);
				}
			}
			log.info("sorted ways = "+sortedWays);
			log.info("sorted links = "+sortedLinks);


			List<Long>remainingWays = new ArrayList<Long>();
			for (long wayId : unsortedWays) {
				if (!sortedWays.contains(wayId)) {
					remainingWays.add(wayId);
				}
			}
			log.info("remaining unsorted ways = "+remainingWays);

		}

		// /////////////////////////////////////////////////////
		// helper methods
		// /////////////////////////////////////////////////////

		private void developWays(List<Long> sortedWays, List<Id<Link>> sortedLinks, Map<Long, OsmParser.OsmWay> relationWays, List<Long> loopWays, Map<Long, List<Long>> loopWayNodes, HashMap<Long, List<Long>> unprocessedNodesAndConnectedWays, long startNodeId) {

			//TODO: dealing with links that are used twice, e.g. Bus 185: Zürich, Wollishofen => Adliswil, Bahnhof or Bus 811: Uster, Bahnhof - Rundkurs via Uster, Haberweid
			//TODO: dealing with "forward" and "backward" links

			boolean developFurther = true;

			long nodeId = startNodeId;
			List<Long> loopNodes = new ArrayList<Long>();
			List<Long> forkNodes = new ArrayList<Long>();

			while (developFurther) {

				if (unprocessedNodesAndConnectedWays.isEmpty()) {
					developFurther = false;
					log.info("forward processing finished at node "+nodeId);
					System.out.println();
				}
				else {

					System.out.println("Current node = "+nodeId);
					System.out.println("loop nodes: "+loopNodes);
					System.out.println("fork nodes: "+forkNodes);

					if (!unprocessedNodesAndConnectedWays.containsKey(nodeId)) {

						if (loopWayNodes.keySet().contains(nodeId)) {
							nodeId = developLoopWay(sortedWays, sortedLinks, relationWays, loopWayNodes, unprocessedNodesAndConnectedWays, nodeId);
						}
						else if (getClosestNeighbour(nodeId, unprocessedNodesAndConnectedWays) != -1) {

							nodeId = developClosestNeighbour(sortedLinks, unprocessedNodesAndConnectedWays, nodeId);

						}
						else if (!loopNodes.isEmpty()) {

							System.out.println("Start adding loops.\n# loop nodes = "+loopNodes.size());
							while (!loopNodes.isEmpty()) {
								long loopNodeId = loopNodes.get(0);
								insertLoop(sortedWays, sortedLinks, relationWays, loopWayNodes, unprocessedNodesAndConnectedWays, loopNodes, forkNodes, loopNodeId);
							}
							if (forkNodes.isEmpty()) {
								developFurther = false;
								log.info("forward processing finished after loop insertion");
							}
						}
						else if (!forkNodes.isEmpty()) {
							System.out.println("Start developing forks. \n#fork nodes = "+forkNodes.size());

							while (!forkNodes.isEmpty()) {
								long forkNodeId = forkNodes.get(0);
								developFork(sortedWays, sortedLinks, relationWays, loopWayNodes, unprocessedNodesAndConnectedWays, loopNodes, forkNodes, forkNodeId);
							}
							developFurther = false;
							log.info("forward processing finished after fork node development");
						}
						else {
							log.warn("WARNING: Something is odd, please check data for this route. There are links that are not connected to the rest of the route or links have to be used twice.");
							developFurther = false;
						}
					}
					else {

						if (unprocessedNodesAndConnectedWays.get(nodeId).size() == 1 && loopWays.contains(unprocessedNodesAndConnectedWays.get(nodeId).get(0))) {
							nodeId = developLoopWay(sortedWays, sortedLinks, relationWays, loopWayNodes, unprocessedNodesAndConnectedWays, nodeId);
						}
						else{
							nodeId = developWay(sortedWays, sortedLinks, relationWays, loopWayNodes, unprocessedNodesAndConnectedWays, loopNodes, forkNodes, nodeId);
						}

					}

				}
			}
		}

		private void insertLoop(List<Long> sortedWays, List<Id<Link>> sortedLinks, Map<Long, OsmParser.OsmWay> relationWays, Map<Long, List<Long>> loopWayNodes, HashMap<Long, List<Long>> unprocessedNodesAndConnectedWays, List<Long> loopNodes, List<Long> forkNodes, long nodeId) {

			System.out.println("Insert loop links at node "+nodeId+"...");
			List<Long> sortedLinksBefore = new ArrayList<Long>(sortedWays);
			sortedWays.clear();

			boolean beforeLoop = true;
			int i = 0;

			while(beforeLoop && i < sortedLinksBefore.size()) {

				OsmParser.OsmWay way  = relationWays.get(sortedLinksBefore.get(i));
				if (way.getStartNode() == nodeId || way.getEndNode() == nodeId) {
					beforeLoop = false;
				}
				sortedWays.add(way.id);
				i++;

			}
			long nextNodeId = developWay(sortedWays, sortedLinks, relationWays, loopWayNodes, unprocessedNodesAndConnectedWays, loopNodes, forkNodes, nodeId);
			while (nextNodeId != nodeId) {
				nextNodeId = developWay(sortedWays, sortedLinks, relationWays, loopWayNodes, unprocessedNodesAndConnectedWays, loopNodes, forkNodes, nextNodeId);
			}

			while (i < sortedLinksBefore.size()) {
				sortedWays.add(sortedLinksBefore.get(i));
				i++;
			}

			loopNodes.remove(nodeId);

			System.out.println("------");
		}

		private void developFork(List<Long> sortedWays, List<Id<Link>> sortedLinks, Map<Long, OsmParser.OsmWay> relationWays, Map<Long, List<Long>> loopWayNodes, HashMap<Long, List<Long>> unprocessedNodesAndConnectedWays, List<Long> loopNodes, List<Long> forkNodes, long nodeId) {

			System.out.println("Develop fork at node "+nodeId+"...");
			forkNodes.remove(nodeId);

			//split existing ways and links into those before and after the link
			boolean beforeForkWays = true;
			List<Long>sortedWaysBeforeFork = new ArrayList<Long>();
			List<Long>sortedWaysAfterFork = new ArrayList<Long>();

			for (long wayId : sortedWays) {
				if (beforeForkWays) {
					sortedWaysBeforeFork.add(wayId);
				}
				else {
					sortedWaysAfterFork.add(wayId);
				}

				OsmParser.OsmWay way = relationWays.get(wayId);
				if (way.getEndNode() == nodeId || way.getStartNode() == nodeId) {
					beforeForkWays = false;
				}
			}

			System.out.println("sortedWaysBeforeFork = "+sortedWaysBeforeFork);
			System.out.println("sortedWaysAfterFork = "+sortedWaysAfterFork);

			List<Id<Link>>sortedLinksBeforeFork = new ArrayList<>();
			List<Id<Link>>sortedLinksAfterFork = new ArrayList<>();
			boolean beforeForkLinks = true;

			for (Id<Link> linkId : sortedLinks) {
				if (beforeForkLinks) {
					sortedLinksBeforeFork.add(linkId);
				}
				else {
					sortedLinksAfterFork.add(linkId);
				}

				Link link = network.getLinks().get(linkId);
				if (link.getToNode().getId().equals(Id.create(nodeId, Node.class))) {
					beforeForkLinks = false;
				}
			}



			//develop the remaining section after the fork
			//stop if there are no more connected ways or if the developed ways merges with the existing "after" ways
			//TODO: integrate loopsWays (!), new forks (?), new loops(?) => or check them of earlier (i.e. restructure how the lists "loopNodes" and "forkNodes" are filled
			List<Long>sortedNewWaysAfterFork = new ArrayList<Long>();
			List<Id<Link>>sortedNewLinksAfterFork = new ArrayList<Id<Link>>();
			while (unprocessedNodesAndConnectedWays.containsKey(nodeId) && !forkNodes.contains(nodeId)) {
				nodeId = developWay(sortedNewWaysAfterFork, sortedNewLinksAfterFork, relationWays, loopWayNodes, unprocessedNodesAndConnectedWays, loopNodes, forkNodes, nodeId);
			}

			System.out.println("sortedNewWaysAfterFork = "+sortedNewWaysAfterFork);
			System.out.println("sortedNewLinksAfterFork = "+sortedNewLinksAfterFork);

			//check which side of the fork is longer
			//TODO: take into account when the fork merges back
			double lengthAfterOld = 0;
			double lengthAfterNew = 0;
			for (Id<Link> linkId : sortedLinksAfterFork) {
				Link link = network.getLinks().get(linkId);
				lengthAfterOld += link.getLength();
			}
			for (Id<Link> linkId : sortedNewLinksAfterFork) {
				Link link = network.getLinks().get(linkId);
				lengthAfterNew += link.getLength();
			}

			System.out.println("old length = "+lengthAfterOld);
			System.out.println("new length = "+lengthAfterNew);

			if (lengthAfterNew > lengthAfterOld) {
				// replace the existing "after" with the new after
				System.out.println("replace with new fork side.");
				sortedWays.clear();
				for (Long way : sortedWaysBeforeFork) {
					sortedWays.add(way);
				}
				for (long way : sortedNewWaysAfterFork) {
					sortedWays.add(way);
				}

				sortedLinks.clear();
				for (Id<Link> link : sortedLinksBeforeFork) {
					sortedLinks.add(link);
				}
				for (Id<Link> link : sortedNewLinksAfterFork) {
					sortedLinks.add(link);
				}
				System.out.println("New sorted ways = "+sortedWays);
			}

		}

		private long developWay(List<Long> sortedWays, List<Id<Link>> sortedLinks, Map<Long, OsmParser.OsmWay> relationWays, Map<Long, List<Long>> loopWayNodes, HashMap<Long, List<Long>> unprocessedNodesAndConnectedWays, List<Long> loopNodes, List<Long> forkNodes, long nodeId) {

			List<Long> connectedWays = unprocessedNodesAndConnectedWays.get(nodeId);
			OsmParser.OsmWay way = relationWays.get(connectedWays.get(0));
			System.out.println("connected ways: "+connectedWays);


			List<Id<Link>> linkIds = null;

			if (connectedWays.size() == 1) {
				sortedWays.add(way.id);
				//			System.out.println("Remove node "+nodeId);
				//			unprocessedNodesAndConnectedWays.remove(nodeId);
				loopNodes.remove(nodeId);
				removeWayIdFromMap(unprocessedNodesAndConnectedWays, way.id);
				removeWayFromLoopWayMap(loopWayNodes, way.id);

			}
			else if (connectedWays.size() == 2) {
				forkNodes.add(nodeId);
				sortedWays.add(way.id);
				removeWayIdFromMap(unprocessedNodesAndConnectedWays, way.id);
				removeWayFromLoopWayMap(loopWayNodes, way.id);

			}
			else if (connectedWays.size() > 2) {
				loopNodes.add(nodeId);
				sortedWays.add(way.id);
				removeWayIdFromMap(unprocessedNodesAndConnectedWays, way.id);
				removeWayFromLoopWayMap(loopWayNodes, way.id);
			}

			if (nodeId == way.getStartNode()) {
				nodeId = way.getEndNode();
				linkIds = translateWayToLinks(way.id, relationWays, way.getStartNode());
			}
			else {
				nodeId = way.getStartNode();
				linkIds = translateWayToLinks(way.id, relationWays, way.getEndNode());
			}

			for (Id<Link> linkId : linkIds) {
				sortedLinks.add(linkId);
			}

			return nodeId;
		}

		private long developLoopWay(List<Long> sortedWays, List<Id<Link>> sortedLinks, Map<Long, OsmParser.OsmWay> relationWays, Map<Long, List<Long>> loopWayNodes, HashMap<Long, List<Long>> unprocessedNodesAndConnectedWays, long nodeId) {

			List<Long> connectedLoopWays = loopWayNodes.get(nodeId);
			System.out.println("connected loop ways = "+connectedLoopWays);

			//check if one of the loop ways is connected to the unprocessed nodes
			boolean foundConnection = false;
			int i = 0;
			OsmParser.OsmWay way = null;
			long nextNodeId = -1;

			while(!foundConnection && i < connectedLoopWays.size()) {
				way = relationWays.get(connectedLoopWays.get(i));
				for (long lwNodeId : way.nodes) {
					if (unprocessedNodesAndConnectedWays.keySet().contains(lwNodeId) && (unprocessedNodesAndConnectedWays.get(lwNodeId).size() > 1 || unprocessedNodesAndConnectedWays.get(lwNodeId).get(0) != way.id)){
						nextNodeId = lwNodeId;
						foundConnection = true;
					}
				}
				i++;
			}

			System.out.println("selected loop way "+way.id+", nextNodeId = "+nextNodeId);
			sortedWays.add(way.id);

			long endNodeId;
			if (nextNodeId == -1) {
				endNodeId = nodeId;
			}
			else {
				endNodeId = nextNodeId;
			}

			List<Id<Link>> linkIds = translateLoopWayToLinks(way.id, relationWays, nodeId, endNodeId);
			if (!linkIds.isEmpty()) {
				for (Id<Link> linkId : linkIds) {
					sortedLinks.add(linkId);
				}
			}
			removeWayIdFromMap(unprocessedNodesAndConnectedWays, way.id);
			removeWayFromLoopWayMap(loopWayNodes, way.id);

			nodeId = endNodeId;

			return nodeId;
		}

		private long developClosestNeighbour(List<Id<Link>> sortedLinks, HashMap<Long, List<Long>> unprocessedNodesAndConnectedWays, long nodeId) {

			System.out.println("Develop closest neighbour for node "+nodeId+"...");

			long closestNeighbour = getClosestNeighbour(nodeId, unprocessedNodesAndConnectedWays);

			// check if there is a link connecting the two nodes, if not create this link and add to network
			Map<Id<Link>, ? extends Link> outLinks = network.getNodes().get(Id.create(nodeId, Node.class)).getOutLinks();
			Id<Link> nextLink = null;
			for (Link link : outLinks.values()) {
				if (link.getToNode().getId().equals(Id.create(closestNeighbour, Node.class))) {
					nextLink = link.getId();
				}
			}

			if (nextLink == null) {

				Node node1 = network.getNodes().get(Id.create(nodeId, Node.class));
				Node node2 = network.getNodes().get(Id.create(closestNeighbour, Node.class));
				Id<Link> linkIdA = Id.create(nodeId + "To" +closestNeighbour +"_0A", Link.class);
				Link linkA = this.factory.createLink(linkIdA, node1, node2);
				this.network.addLink(linkA);

				Id<Link> linkIdB = Id.create(nodeId + "To" +closestNeighbour +"_0B", Link.class);
				Link linkB = this.factory.createLink(linkIdB, node2, node1);
				this.network.addLink(linkB);

				nextLink = linkIdA;
			}

			// add link to sorted links and
			sortedLinks.add(nextLink);
			//then continue with closest neighbour node
			return closestNeighbour;
		}

		private List<Id<Link>> translateWayToLinks(long wayId, Map<Long, OsmParser.OsmWay> relationWays, long startNodeId) {
			List<Id<Link>> linkIds = null;
			OsmParser.OsmWay way = relationWays.get(wayId);
			if (startNodeId == way.getStartNode()) {
				linkIds = waysToLinks_DirA.get(wayId);
			}
			else {
				linkIds = waysToLinks_DirB.get(wayId);
			}
			return linkIds;
		}

		private List<Id<Link>> translateLoopWayToLinks(long wayId, Map<Long, OsmParser.OsmWay> relationWays, long startNodeId, long endNodeId) {
			Id<Node> startNode = Id.create(startNodeId, Node.class);
			Id<Node> endNode = Id.create(endNodeId, Node.class);

			List<Id<Link>> linkIds = new LinkedList<>();
			OsmParser.OsmWay way = relationWays.get(wayId);

			if (waysToLinks_DirA.containsKey(way.id) && !waysToLinks_DirA.get(way.id).isEmpty()) {
				List<Id<Link>> lwLinkIds = waysToLinks_DirA.get(way.id);


				int i = 0;
				boolean endNodeReached = false;
				boolean afterStartNode = false;
				while (!endNodeReached) {
					Id<Link> linkId = lwLinkIds.get(i);
					Link link = network.getLinks().get(linkId);
					if (link.getFromNode().getId().equals(startNode)) {
						afterStartNode = true;
					}
					if (afterStartNode) {
						linkIds.add(linkId);

						if (link.getToNode().getId().equals(endNode)) {
							endNodeReached = true;
						}
					}

					if (i < lwLinkIds.size()-1) {
						i++;
					}
					else {
						i = 0;
					}

				}
			}
			return linkIds;
		}

		private void removeWayIdFromMap(HashMap<Long, List<Long>> nodeWayMap, long id) {

			//			System.out.println("Remove way "+id+" from map.");
			Iterator<Long> itN = nodeWayMap.keySet().iterator();

			while (itN.hasNext()) {
				long nodeId = itN.next();
				List<Long> wayIds = nodeWayMap.get(nodeId);
				Iterator<Long> itW = wayIds.iterator();

				while (itW.hasNext()) {
					long wayId = itW.next();

					if (wayId == id) {
						itW.remove();
					}
				}
				if (nodeWayMap.get(nodeId).isEmpty()) {
					//					System.out.println("Remove node "+nodeId+" from map.");
					itN.remove();
				}
			}
		}

		private void removeWayFromLoopWayMap(Map<Long, List<Long>> loopWayNodes, long wayId) {

			//			System.out.println("Remove way "+wayId+" from loop way nodes map.");
			Iterator<Long> itN = loopWayNodes.keySet().iterator();

			while (itN.hasNext()) {
				long nodeId = itN.next();
				List<Long> wayIds = loopWayNodes.get(nodeId);
				Iterator<Long> itW = wayIds.iterator();

				while (itW.hasNext()) {
					long wId = itW.next();

					if (wId == wayId) {
						itW.remove();
					}
				}
				if (loopWayNodes.get(nodeId).isEmpty()) {
					//					System.out.println("Remove node "+nodeId+" from loop way nodes map.");
					itN.remove();
				}
			}
		}

		private Long getClosestNeighbour(long nodeId, HashMap<Long, List<Long>> unprocessedNodesAndConnectedWays) {
			long closestNeighbour = -1;
			double shortestDistance = Double.MAX_VALUE;

			Node node1 = network.getNodes().get(Id.create(nodeId, Node.class));

			for (long node2Id : unprocessedNodesAndConnectedWays.keySet()) {
				Node node2 = network.getNodes().get(Id.create(node2Id, Node.class));
				double nodeDistance = calcWGS84CoordDistance(node1.getCoord(), node2.getCoord());
				if (nodeDistance <= shortestDistance) {
					closestNeighbour = node2Id;
					shortestDistance = nodeDistance;
				}
			}


			return closestNeighbour;
		}

		private double calcDepartureOffSet(List<TransitRouteStop> stops, TransitStopFacility stop) {
			double departureOffset = 0;
			// Currently, departure offset fixed to two minutes between stops
			// More advanced solutions to be done in postprocessing (e.g. when
			// local coordinate system is known)
			departureOffset = stops.size() * 120;
			return departureOffset;
		}

		private TransitStopFacility createStop(OsmParser.OsmNode node) {
			TransitStopFacility stopFacility = this.scheduleBuilder.createTransitStopFacility(Id.create(node.id, TransitStopFacility.class), node.coord,false);
			stopFacility.setName(node.tags.get("name"));
			return stopFacility;
		}

		// approximation of the distance using the Haversine formula (which assumes a perfect sphere)
		private double calcWGS84CoordDistance(Coord coord1, Coord coord2) {

			double earthRadius = 6371000; // in metres
			double dLat = Math.toRadians(coord2.getY() - coord1.getY());
			double dLng = Math.toRadians(coord2.getX() - coord1.getX());
			double a = Math.sin(dLat/2) * Math.sin(dLat/2) + Math.cos(Math.toRadians(coord1.getY())) * Math.cos(Math.toRadians(coord2.getY())) * Math.sin(dLng/2) * Math.sin(dLng/2);
			double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
			double distance = earthRadius * c;

			return distance;
		}


	}
}
