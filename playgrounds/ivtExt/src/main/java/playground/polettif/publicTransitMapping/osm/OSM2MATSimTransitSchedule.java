/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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


package playground.polettif.publicTransitMapping.osm;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.transitSchedule.api.*;
import playground.polettif.publicTransitMapping.osm.core.OsmParser;
import playground.polettif.publicTransitMapping.osm.core.TagFilter;
import playground.polettif.publicTransitMapping.osm.lib.OsmTag;
import playground.polettif.publicTransitMapping.osm.lib.OsmValue;
import playground.polettif.publicTransitMapping.tools.ScheduleCleaner;
import playground.polettif.publicTransitMapping.tools.ScheduleTools;

import java.util.*;

/**
 * Convert available public transit data from OSM to a MATSim Transit Schedule (stop facilities,
 * transitRoutes and routeProfiles). Creates an unmapped schedule with missing departures.
 *
 * @author polettif
 */
public class OSM2MATSimTransitSchedule {

	private static final Logger log = Logger.getLogger(OSM2MATSimTransitSchedule.class);

	private final CoordinateTransformation transformation;
	private final TransitSchedule transitSchedule;
	private final TransitScheduleFactory factory;
	private OsmParserHandler handler;

	// parser
	private Map<Long, OsmParser.OsmNode> nodes;
	private Map<Long, OsmParser.OsmRelation> relations;
	private Map<Long, OsmParser.OsmWay> ways;

	// filters
	private final TagFilter stop_area;
	private final TagFilter stop_position;
	private final TagFilter route_master;
	private final TagFilter ptRoute;

	private int routeNr = 0;

	public OSM2MATSimTransitSchedule(TransitSchedule schedule, CoordinateTransformation transformation) {
		this.transitSchedule = schedule;
		this.transformation = transformation;

		this.factory = transitSchedule.getFactory();

		// initialize filters
		stop_position = new TagFilter();
		stop_position.add(OsmTag.PUBLIC_TRANSPORT, OsmValue.STOP_POSITION);

		stop_area = new TagFilter();
		stop_area.add(OsmTag.PUBLIC_TRANSPORT, OsmValue.STOP_AREA);

		route_master = new TagFilter();
		route_master.add(OsmTag.ROUTE_MASTER, OsmValue.BUS);
		route_master.add(OsmTag.ROUTE_MASTER, OsmValue.TROLLEYBUS);
		route_master.add(OsmTag.ROUTE_MASTER, OsmValue.TRAM);
		route_master.add(OsmTag.ROUTE_MASTER, OsmValue.MONORAIL);
		route_master.add(OsmTag.ROUTE_MASTER, OsmValue.SUBWAY);
		route_master.add(OsmTag.ROUTE_MASTER, OsmValue.FERRY);

		ptRoute = new TagFilter();
		ptRoute.add(OsmTag.ROUTE, OsmValue.BUS);
		ptRoute.add(OsmTag.ROUTE, OsmValue.TROLLEYBUS);
		ptRoute.add(OsmTag.ROUTE, OsmValue.RAIL);
		ptRoute.add(OsmTag.ROUTE, OsmValue.TRAM);
		ptRoute.add(OsmTag.ROUTE, OsmValue.LIGHT_RAIL);
		ptRoute.add(OsmTag.ROUTE, OsmValue.FUNICULAR);
		ptRoute.add(OsmTag.ROUTE, OsmValue.MONORAIL);
		ptRoute.add(OsmTag.ROUTE, OsmValue.SUBWAY);
	}

	/**
	 * Converts the available public transit data of an osm file to a MATSim transit schedule
	 * @param args [0] osm file
	 *             [1] output schedule file
	 *             [2] output coordinate system (optional)
	 */
	public static void main(final String[] args) {
		CoordinateTransformation ct = args.length == 3 ? TransformationFactory.getCoordinateTransformation("WGS84", "CH1903_LV03_Plus") : null;
		OSM2MATSimTransitSchedule osm2mts = new OSM2MATSimTransitSchedule(ScheduleTools.createSchedule(), ct);
		osm2mts.parse(args[0]);
		osm2mts.createSchedule();
		osm2mts.writeFile(args[1]);
	}

	public void convertOsmFile(String filenameOSMinput) {
		parse(filenameOSMinput);
		createSchedule();
	}

	private void writeFile(String filenameMTSoutput) {
		new TransitScheduleWriter(transitSchedule).writeFile(filenameMTSoutput);
	}

	private void parse(String filenameOSMinput) {

		TagFilter nodeFilter = new TagFilter();
		nodeFilter.add(OsmTag.PUBLIC_TRANSPORT, OsmValue.STOP_POSITION);

		TagFilter wayFilter = new TagFilter();

		TagFilter relationFilter = new TagFilter();
		relationFilter.add(OsmTag.ROUTE, OsmValue.BUS);
		relationFilter.add(OsmTag.ROUTE, OsmValue.TROLLEYBUS);
		relationFilter.add(OsmTag.ROUTE, OsmValue.RAIL);
		relationFilter.add(OsmTag.ROUTE, OsmValue.TRAM);
		relationFilter.add(OsmTag.ROUTE, OsmValue.LIGHT_RAIL);
		relationFilter.add(OsmTag.ROUTE, OsmValue.FUNICULAR);
		relationFilter.add(OsmTag.ROUTE, OsmValue.MONORAIL);
		relationFilter.add(OsmTag.ROUTE, OsmValue.SUBWAY);
		relationFilter.add(OsmTag.ROUTE_MASTER, OsmValue.BUS);
		relationFilter.add(OsmTag.ROUTE_MASTER, OsmValue.TROLLEYBUS);
		relationFilter.add(OsmTag.ROUTE_MASTER, OsmValue.TRAM);
		relationFilter.add(OsmTag.ROUTE_MASTER, OsmValue.MONORAIL);
		relationFilter.add(OsmTag.ROUTE_MASTER, OsmValue.SUBWAY);
		relationFilter.add(OsmTag.ROUTE_MASTER, OsmValue.FERRY);

		handler = new OsmParserHandler();
		handler.addFilter(nodeFilter, wayFilter, relationFilter);
		OsmParser parser = new OsmParser();
		parser.addHandler(handler);
		parser.readFile(filenameOSMinput);
	}

	/**
	 * Converts relations, nodes and ways from osm to an
	 * unmapped MATSim Transit Schedule
	 */
	private void createSchedule() {
		Map<Id<TransitLine>, TransitLine> transitLinesDump = new HashMap<>();

		this.nodes = handler.getNodes();
		this.relations = handler.getRelations();
		this.ways = handler.getWays();

		/**
		 * Create TransitStopFacilities from public_transport=stop_position
		 */
		createStopFacilities();

		/**
		 * https://wiki.openstreetmap.org/wiki/Relation:route_master
		 */
		Set<Long> routesWithMaster = new HashSet<>();


		/**
		 * Create transitLines via route_masters
		 */
		for(OsmParser.OsmRelation relation : relations.values()) {
			if(route_master.matches(relation.tags)) {
				Id<TransitLine> lineId = createLineId(relation);
				TransitLine newTransitLine = factory.createTransitLine(lineId);
				newTransitLine.setName(relation.tags.get(OsmTag.NAME));

				for(OsmParser.OsmRelationMember member : relation.members) {
					OsmParser.OsmRelation route = relations.get(member.refId);
					// maybe member route does not exist in area
					if(route != null) {
						TransitRoute newTransitRoute = createTransitRoute(route);
						if(newTransitRoute != null) {
							newTransitLine.addRoute(newTransitRoute);
							routesWithMaster.add(member.refId);
						}
					}
				}
				transitLinesDump.put(lineId, newTransitLine);
			}
		}

		/**
		 * Create transitRoutes without route_masters
		 */
		for(OsmParser.OsmRelation relation : relations.values()) {
			if(ptRoute.matches(relation.tags) && !routesWithMaster.contains(relation.id)) {
				Id<TransitLine> lineId = createLineId(relation);

				if(!transitLinesDump.containsKey(lineId)) {
					transitLinesDump.put(lineId, factory.createTransitLine(lineId));
				}

				TransitLine transitLine = transitLinesDump.get(lineId);

				TransitRoute newTransitRoute = createTransitRoute(relation);
				if(newTransitRoute != null) {
					transitLine.addRoute(newTransitRoute);
				}
			}
		}

		// add lines to schedule
		for(TransitLine transitLine : transitLinesDump.values()) {
//			if(transitLine.getRoutes().size() > 0) {
				this.transitSchedule.addTransitLine(transitLine);
//			}
		}

		// remove non used facilities to schedule
		ScheduleCleaner.removeNotUsedStopFacilities(transitSchedule);
	}

	/**
	 * creates stop facilities from nodes and adds them to the schedule
	 */
	private void createStopFacilities() {
		Map<Id<TransitStopFacility>, TransitStopFacility> stopFacilities = this.transitSchedule.getFacilities();

		// create facilities from stop_area first
		for(OsmParser.OsmRelation relation : relations.values()) {
			if(stop_area.matches(relation.tags)) {
				String stopPostAreaId = relation.tags.get(OsmTag.NAME);

				// create a facility for each member
				for(OsmParser.OsmRelationMember member : relation.members) {
					if(member.role.equals(OsmValue.STOP)) {
						TransitStopFacility newStopFacility = createStopFacilityFromOsmNode(nodes.get(member.refId), stopPostAreaId);

						if(!stopFacilities.containsValue(newStopFacility)) {
							this.transitSchedule.addStopFacility(newStopFacility);
						}
					}
				}
			}
		}

		// create other facilities
		for(OsmParser.OsmNode node : nodes.values()) {
			if(stop_position.matches(node.tags)) {
				if(!stopFacilities.containsKey(Id.create(node.id, TransitStopFacility.class))) {
					this.transitSchedule.addStopFacility(createStopFacilityFromOsmNode(node));
				}
			}
		}
	}

	/**
	 * creates a TransitStopFacility from an OsmNode
	 * @return the created facility
	 */
	private TransitStopFacility createStopFacilityFromOsmNode(OsmParser.OsmNode node, String stopPostAreaId) {
		Id<TransitStopFacility> id = Id.create(node.id, TransitStopFacility.class);
		Coord coord = transformation.transform(node.coord);
		TransitStopFacility newStopFacility = factory.createTransitStopFacility(id, coord, false);
		newStopFacility.setName(node.tags.get(OsmTag.NAME));
		if(stopPostAreaId != null ) { newStopFacility.setStopPostAreaId(stopPostAreaId); }
		return newStopFacility;
	}

	private TransitStopFacility createStopFacilityFromOsmNode(OsmParser.OsmNode node) {
		return createStopFacilityFromOsmNode(node, null);
	}

	/**
	 * Creates a TransitRoute from a relation.
	 * @return <code>null</code> if the route has stops outside of the area
	 */
	private TransitRoute createTransitRoute(OsmParser.OsmRelation relation) {
		List<TransitRouteStop> stopSequenceForward = new ArrayList<>();

		// create different RouteStops and stopFacilities for forward and backward
		for(int i = 0; i < relation.members.size() - 1; i++) {
			OsmParser.OsmRelationMember member = relation.members.get(i);

			// route Stops
			if(member.type.equals(OsmParser.OsmRelationMemberType.NODE) && (OsmValue.STOP.equals(member.role) || OsmValue.STOP_FORWARD.equals(member.role))) {
				Id<TransitStopFacility> id = Id.create(member.refId, TransitStopFacility.class);
				TransitStopFacility transitStopFacility = transitSchedule.getFacilities().get(id);
				if(transitStopFacility == null) {
					return null;
				}
				// create transitRouteStop
				TransitRouteStop newRouteStop = factory.createTransitRouteStop(transitStopFacility, 0.0, 0.0);
				stopSequenceForward.add(newRouteStop);
			}

			// route links
//			if(member.type.equals(OsmParser.OsmRelationMemberType.WAY) && !OsmValue.BACKWARD.equals(member.role)) {
//				linkSequenceForward.add(Id.createLinkId(member.refId));
//			}
		}

//		NetworkRoute networkRoute = (linkSequenceForward.size() == 0 ? null : RouteUtils.createNetworkRoute(linkSequenceForward, null));
		if(stopSequenceForward.size() == 0){
			return null;
		}

		// one relation has two routes, forward and back
		Id<TransitRoute> transitRouteId = Id.create(createStringId(relation)+ (++routeNr), TransitRoute.class);
		TransitRoute newTransitRoute = factory.createTransitRoute(transitRouteId, null, stopSequenceForward, relation.tags.get(OsmTag.ROUTE));
		newTransitRoute.addDeparture(factory.createDeparture(Id.create("departure" + routeNr, Departure.class), 60.0));

		return newTransitRoute;
	}

	private Id<TransitLine> createLineId(OsmParser.OsmRelation relation) {
		return Id.create(createStringId(relation), TransitLine.class);
	}

	private String createStringId(OsmParser.OsmRelation relation) {
		String id;
		boolean ref = false, operator=false, name=false;

		if(relation.tags.containsKey("name")) { name = true; }
		if(relation.tags.containsKey("ref")) { ref = true; }
		if(relation.tags.containsKey("operator")) { operator = true; }

		if(operator && ref) {
			id = relation.tags.get("operator")+": "+relation.tags.get("ref");
		}
		else if(operator && name) {
			id = relation.tags.get("operator")+": "+relation.tags.get("name");
		}
		else if(ref){
			id = relation.tags.get("ref");
		}
		else if(name) {
			id = relation.tags.get("name");
		}
		else {
			id = Long.toString(relation.id);
		}

		return id;
	}

	private Id<TransitLine> createLineId2(OsmParser.OsmRelation relation) {
		String id;
		boolean ref = false, operator=false, name=false;


		if(relation.tags.containsKey("ref")) { ref = true; }
		if(relation.tags.containsKey("operator")) { operator = true; }
		if(relation.tags.containsKey("name")) { name = true; }

		if(operator && ref) {
			id = relation.tags.get("operator")+"_"+relation.tags.get("ref");
		}
		else if(operator && name) {
			id = relation.tags.get("operator")+"_"+relation.tags.get("ref");
		}
		else if(name) {
			id = relation.tags.get("name");
		}
		else if(ref){
			id = relation.tags.get("ref");
		}
		else {
			id = Long.toString(relation.id);
		}

		try {
			return Id.create(id, TransitLine.class);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}