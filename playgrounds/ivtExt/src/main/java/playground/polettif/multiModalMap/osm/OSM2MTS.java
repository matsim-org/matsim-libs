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


package playground.polettif.multiModalMap.osm;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.*;
import playground.polettif.multiModalMap.osm.core.OsmNodeHandler;
import playground.polettif.multiModalMap.osm.core.OsmParser;
import playground.polettif.multiModalMap.osm.core.OsmRelationHandler;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Convert available public transit data from OSM to a MATSim Transit Schedule (bus stops, transitRoutes and routes).
 *
 * @polettif
 */
public class OSM2MTS {

	private static final Logger log = Logger.getLogger(OSM2MTS.class);

	TransitScheduleFactoryImpl transitScheduleFactory = new TransitScheduleFactoryImpl();;

	public static void main(final String[] args) {
		String filenameOSM = "C:/Users/polettif/Desktop/basel/osmInput/basel.osm";
		String filenameMTS = "C:/Users/polettif/Desktop/basel/mts/busstops.xml";

		OSM2MTS osm2mts = new OSM2MTS();
		osm2mts.run(filenameOSM, filenameMTS);
	}

	public void run(String filenameOSMinput, String filenameMTSoutput) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		Handler handler = new Handler();
		OsmParser parser = new OsmParser();
		parser.addHandler(handler);
		parser.readFile(filenameOSMinput);

		new TransitScheduleWriter(handler.getTransitSchedule()).writeFile(filenameMTSoutput);


		new TransitScheduleWriter(handler.getTransitSchedule()).writeFile(filenameMTSoutput);
	}

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
	private class Handler implements OsmNodeHandler {

		private TransitSchedule transitSchedule = transitScheduleFactory.createTransitSchedule();

		private Map<Id<TransitLine>, List<Id<TransitRoute>>> transitLinesDump = new HashMap<>();
		private Map<Id<TransitRoute>, List<TransitRouteStop>> routeProfilesDump = new HashMap<>();

		int routeNr = 0;

		@Override
		public void handleNode(OsmParser.OsmNode node) {
			if("bus_stop".equals((node.tags.get("highway")))) {
				// add stopFacility
				TransitStopFacility stopFacility = transitScheduleFactory.createTransitStopFacility(Id.create(node.id, TransitStopFacility.class), node.coord, false); // TODO blocks lane
				stopFacility.setName(node.tags.get("name"));
				transitSchedule.addStopFacility(stopFacility);
			}
		}

		//@Override
		public void handleRelation(OsmParser.OsmRelation relation) {

			// if relation is a bus route
			if("route".equals((relation.tags.get("type"))) && "bus".equals((relation.tags.get("route")))) {

				// use either tag>ref or tag>name for line
				Id<TransitLine> lineId = null;
				if (relation.tags.containsKey("ref")) {
					lineId = Id.create(relation.tags.get("ref"), TransitLine.class);
				} else if (relation.tags.containsKey("ref")) {
					lineId = Id.create(relation.tags.get("name"), TransitLine.class);
				} else {
					log.warn("No line assigned to relation " + relation.id);
				}

				if(lineId != null) {
					// create line, if it does not yet exist
					if (!transitSchedule.getTransitLines().containsKey(lineId)) {
						transitSchedule.addTransitLine(transitScheduleFactory.createTransitLine(lineId));
					} else {
						routeNr = transitSchedule.getTransitLines().get(lineId).getRoutes().size();
					}

					// one relation has two routes, forward and back
					Id<TransitRoute> routeIdForward = Id.create(++routeNr, TransitRoute.class);
					Id<TransitRoute> routeIdBackward = Id.create(++routeNr, TransitRoute.class);

					// link transit routes and stops
					List<Id<TransitRoute>> routeIdList = (transitLinesDump.containsKey(lineId) ? transitLinesDump.get(lineId) : new LinkedList<>());
					routeIdList.add(routeIdForward);
					routeIdList.add(routeIdBackward);

					transitLinesDump.put(lineId, routeIdList);

					List<TransitRouteStop> stopSequenceForward = new LinkedList<>();
					List<TransitRouteStop> stopSequenceBackward = new LinkedList<>();

					// create different RouteStops and stopFacilities for forward and backward
					for (OsmParser.OsmRelationMember member : relation.members) {

						if("stop".equals(member.role) || "stop_forward".equals(member.role) || "stop_backward".equals(member.role)) {
							TransitRouteStop tmpRouteStop = transitScheduleFactory.createTransitRouteStop(transitSchedule.getFacilities().get(Id.create(member.refId, TransitStopFacility.class)), 0.0, 0.0);

							if (("stop_forward".equals(member.role) || "stop".equals(member.role)) && !"stop_backward".equals(member.role)) {
								stopSequenceForward.add(tmpRouteStop);
							} else if (("stop_backward".equals(member.role) || "stop".equals(member.role)) && !"stop_forward".equals(member.role)) {
								stopSequenceBackward.add(tmpRouteStop);
							}
						}
					}
					routeProfilesDump.put(routeIdForward, stopSequenceForward);
					routeProfilesDump.put(routeIdBackward, stopSequenceBackward);
				}
			}
		}

		public TransitSchedule getTransitSchedule() {

			// loop through all lines and routes and assing dump values to schedule
			for(Map.Entry<Id<TransitLine>, TransitLine> transitLine : this.transitSchedule.getTransitLines().entrySet()) {

				for(Id<TransitRoute> transitRouteId : transitLinesDump.get(transitLine.getKey())) {
					TransitRoute newTransitRoute = transitScheduleFactory.createTransitRoute(transitRouteId, null, routeProfilesDump.get(transitRouteId), "bus"); // TODO mode
					this.transitSchedule.getTransitLines().get(transitLine.getKey()).addRoute(newTransitRoute);
				}
			}
			return transitSchedule;
		}
	}

}