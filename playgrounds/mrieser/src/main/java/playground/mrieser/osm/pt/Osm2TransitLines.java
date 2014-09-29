/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.mrieser.osm.pt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.mrieser.osm.core.OsmNodeHandler;
import playground.mrieser.osm.core.OsmParser;
import playground.mrieser.osm.core.OsmParser.OsmNode;
import playground.mrieser.osm.core.OsmParser.OsmRelation;
import playground.mrieser.osm.core.OsmParser.OsmRelationMember;
import playground.mrieser.osm.core.OsmParser.OsmRelationMemberType;
import playground.mrieser.osm.core.OsmParser.OsmWay;
import playground.mrieser.osm.core.OsmRelationHandler;
import playground.mrieser.osm.core.OsmWayHandler;
import playground.mrieser.osm.core.TagFilter;

/**
 * @author mrieser / Senozon AG
 * @author scnadine / IVT
 */
public class Osm2TransitLines {

	private final static Logger log = Logger.getLogger(Osm2TransitLines.class);
	
	private final TransitSchedule schedule;
	private final Network network;
	
	public Osm2TransitLines(final TransitSchedule schedule, final Network network) {
		this.schedule = schedule;
		this.network = network;
	}
	
	public void convert(final String filename) {
		log.info("Parsing OSM file: extracting routes...");
		TransitRouteWaysCollector routes = collectTransitRouteWays(filename);
		log.info("Parsing OSM file: extracting ways...");
		WayNodesCollector ways = collectUsedNodes(filename, routes.wayIds);
		log.info("Parsing OSM file: extracting nodes and creating network...");
		createNetworkAndTransitRoutes(filename, ways.nodeIds, routes.wayIds);
	}
	
	private TransitRouteWaysCollector collectTransitRouteWays(final String filename) {
		OsmParser parser = new OsmParser();
		TransitRouteWaysCollector collector = new TransitRouteWaysCollector();
		parser.addHandler(collector);
		parser.readFile(filename);
		return collector;
	}

	private WayNodesCollector collectUsedNodes(final String filename, final Set<Long> wayIds) {
		OsmParser parser = new OsmParser();
		WayNodesCollector collector = new WayNodesCollector(wayIds);
		parser.addHandler(collector);
		parser.readFile(filename);
		return collector;
	}
	
	private void createNetworkAndTransitRoutes(final String filename, final Set<Long> nodeIds, final Set<Long> wayIds) {
		OsmParser parser = new OsmParser();
		NetworkCreator creator = new NetworkCreator(nodeIds, wayIds, this.network);
		parser.addHandler(creator);
		parser.readFile(filename);
	}

	private static class TransitRouteWaysCollector implements OsmRelationHandler {

		private final TagFilter filter;
		
		/*package*/ final Set<Long> wayIds = new HashSet<Long>();
		/*package*/ final List<OsmRelation> relations = new ArrayList<OsmRelation>();
		/*package*/ final Set<String> unhandledRouteTypes = new HashSet<String>();
		
		public TransitRouteWaysCollector() {
			this.filter = new TagFilter();
			this.filter.add("route", "train");
			this.filter.add("route", "bus");
			this.filter.add("route", "tram");
			this.filter.add("route", "ship");
		}
		
		@Override
		public void handleRelation(OsmRelation relation) {
			if (this.filter.matches(relation.tags)) {
				this.relations.add(relation);
				for (OsmRelationMember member : relation.members) {
					if (member.type == OsmRelationMemberType.WAY) {
						this.wayIds.add(member.refId);
					}
				}
			} else {
				if (relation.tags.containsKey("route")) {
					if (this.unhandledRouteTypes.add(relation.tags.get("route"))) {
						log.info("route-type " + relation.tags.get("route") + " not handled.");
					}
				}
			}
		}

	}

	private static class WayNodesCollector implements OsmWayHandler {

		/*package*/ final Set<Long> nodeIds = new HashSet<Long>();
		private final Set<Long> wayIds;
		
		public WayNodesCollector(final Set<Long> wayIds) {
			this.wayIds = wayIds;
		}
		
		@Override
		public void handleWay(OsmWay way) {
			if (this.wayIds.contains(way.id)) {
				this.nodeIds.addAll(way.nodes);
			}
		}

	}

	private static class NetworkCreator implements OsmNodeHandler, OsmWayHandler {

		private final Set<Long> nodeIds;
		private final Set<Long> wayIds;
		private final Network network;
		private final NetworkFactory factory;
		
		public NetworkCreator(final Set<Long> nodeIds, final Set<Long> wayIds, final Network network) {
			this.nodeIds = nodeIds;
			this.wayIds = wayIds;
			this.network = network;
			this.factory = network.getFactory();
		}
		
		@Override
		public void handleNode(OsmNode node) {
			if (this.nodeIds.contains(node.id)) {
				Node netNode = this.factory.createNode(Id.create(node.id, Node.class), node.coord);
				this.network.addNode(netNode);
			}
		}

		@Override
		public void handleWay(OsmWay way) {
			if (this.wayIds.contains(way.id)) {
				Node fromNode = this.network.getNodes().get(Id.create(way.nodes.get(0), Node.class));
				Node toNode = this.network.getNodes().get(Id.create(way.nodes.get(way.nodes.size() - 1), Node.class));
				Link link = this.factory.createLink(Id.create(way.id, Link.class), fromNode, toNode);
				this.network.addLink(link);
			}
		}

	}
	
}
