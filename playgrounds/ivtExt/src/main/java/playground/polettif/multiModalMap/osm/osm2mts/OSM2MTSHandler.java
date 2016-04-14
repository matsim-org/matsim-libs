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


package playground.polettif.multiModalMap.osm.osm2mts;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.*;
import playground.polettif.multiModalMap.osm.core.OsmNodeHandler;
import playground.polettif.multiModalMap.osm.core.OsmParser;
import playground.polettif.multiModalMap.osm.core.OsmRelationHandler;
import playground.polettif.multiModalMap.osm.core.OsmWayHandler;

import java.util.*;

/*
	<relation> ...
	<member type="way" ref="37564441" role="forward"/>
    <member type="node" ref="440129144" role="stop"/>
    <member type="way" ref="37562757" role="backward"/>
    <member type="way" ref="5135398" role="backward"/>
    <member type="way" ref="25099183" role=""/>
    <member type="way" ref="39430702" role=""/>
    <member type="way" ref="56982667" role=""/>
    <member type="way" ref="212289347" role=""/>
    <member type="way" ref="212289346" role=""/>
    <member type="way" ref="56982632" role=""/>
    <member type="node" ref="440108358" role="stop_forward"/>
    <member type="node" ref="440108339" role="stop_backward"/>
    <member type="way" ref="183817568" role="forward"/>
    <member type="way" ref="16973242" role=""/>
    <member type="way" ref="319127930" role=""/>
    <member type="way" ref="210579057" role=""/>
    <member type="way" ref="24655790" role="forward"/>
    <member type="way" ref="149758941" role="forward"/>
    <member type="way" ref="149758948" role=""/>
    <member type="way" ref="319127926" role=""/>
    <member type="way" ref="319127925" role=""/>
    <member type="way" ref="319127927" role=""/>
    <member type="way" ref="256144796" role=""/>
    <member type="way" ref="256144802" role=""/>
    ...
    <tag k="network" v="RVL"/>
    <tag k="operator" v="SWEG"/>
    <tag k="ref" v="12"/>
    <tag k="route" v="bus"/>
    <tag k="type" v="route"/>
  </relation>
	 */
public class OSM2MTSHandler implements OsmNodeHandler, OsmRelationHandler, OsmWayHandler {

	private static final Logger log = Logger.getLogger(OSM2MTSHandler.class);
	private static final Coord DUMMY_COORD = new Coord(7.5741, 4754336);


	private TransitSchedule transitSchedule;
	private TransitScheduleFactory factory;
	private Map<Long, OsmParser.OsmNode> ptStopNodes = new HashMap<>();
	private	int routeNr = 1;



	public OSM2MTSHandler(TransitSchedule schedule) {
		this.transitSchedule = schedule;
		this.factory = schedule.getFactory();
	}

	@Override
	public void handleNode(OsmParser.OsmNode node) {

		// get stop facility and add to schedule
//		if("bus".equals((node.tags.get("highway")))) {
		if("stop_position".equals((node.tags.get("public_transport")))) {
			// add node to set, the stopFacility is created with the routes
			ptStopNodes.put(node.id, node);

		}
	}

	//@Override
	public void handleRelation(OsmParser.OsmRelation relation) {

		// todo types as enum

		// todo check if route_master

		// public transport routes
		if("route".equals((relation.tags.get("type")))) {

			// if relation is a bus route todo add route=train,light_rail,subway
			if("bus".equals((relation.tags.get("route"))) || "trolleybus".equals(relation.tags.get("route"))) {
				
				Id<TransitLine> lineId = createLineId(relation);


				// create line, if it does not yet exist
				TransitLine line;

				if(!transitSchedule.getTransitLines().containsKey(lineId)) {
					line = factory.createTransitLine(lineId);
					transitSchedule.addTransitLine(line);
				} else {
					line = transitSchedule.getTransitLines().get(lineId);
				}

				List<TransitRouteStop> stopSequenceForward = new ArrayList<>();
				List<TransitRouteStop> stopSequenceBackward = new ArrayList<>();

				// create different RouteStops and stopFacilities for forward and backward
				for(OsmParser.OsmRelationMember member : relation.members) {
					if(("stop".equals(member.role) || "stop_forward".equals(member.role) || "stop_backward".equals(member.role))) {

						// check if referenced stop is within area
						if(ptStopNodes.containsKey(member.refId)) {

							TransitStopFacility transitStopFacility;

							// add facility if it does not yet exist todo get coordinates!
							if(!transitSchedule.getFacilities().containsKey(Id.create(member.refId, TransitStopFacility.class))) {
								OsmParser.OsmNode node = ptStopNodes.get(member.refId);

								transitStopFacility = factory.createTransitStopFacility(Id.create(member.refId, TransitStopFacility.class), node.coord, false);
								transitStopFacility.setName(node.tags.get("name"));

								transitSchedule.addStopFacility(transitStopFacility);
							} else {
								transitStopFacility = transitSchedule.getFacilities().get(Id.create(member.refId, TransitStopFacility.class));
							}

							TransitRouteStop newRouteStop = factory.createTransitRouteStop(transitStopFacility, 0.0, 0.0);

							if(("stop_forward".equals(member.role) || "stop".equals(member.role)) && !"stop_backward".equals(member.role)) {
								stopSequenceForward.add(newRouteStop);
							} else if(("stop_backward".equals(member.role) || "stop".equals(member.role)) && !"stop_forward".equals(member.role)) {
								stopSequenceBackward.add(0, newRouteStop);
							}
						}
					}
				}

				// one relation has two routes, forward and back
				TransitRoute routeForward = factory.createTransitRoute(Id.create(++routeNr, TransitRoute.class), null, stopSequenceForward, "bus");
				TransitRoute routeBackward = factory.createTransitRoute(Id.create(++routeNr, TransitRoute.class), null, stopSequenceBackward, "bus");

				// todo remove dummy departures? Are needed for visualisation in via
				routeForward.addDeparture(factory.createDeparture(Id.create("departure"+routeNr, Departure.class), 60.0));
				routeBackward.addDeparture(factory.createDeparture(Id.create("departure"+routeNr, Departure.class), 120.0));

				// only add route if it has stops
				if(stopSequenceForward.size() > 0)
					line.addRoute(routeForward);

				if(stopSequenceBackward.size() > 0)
					line.addRoute(routeBackward);

				log.info("routes added to line "+line.getId());
			}

		}
	}

	
	private Id<TransitLine> createLineId(OsmParser.OsmRelation relation) {
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
	
	@Override
	public void handleWay(OsmParser.OsmWay way) {

	}
}

