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
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.*;
import playground.polettif.multiModalMap.mapping.PTMapperUtils;
import playground.polettif.multiModalMap.osm.core.OsmParser;

import java.util.*;

/**
 * Convert available public transit data from OSM to a MATSim Transit Schedule (stop facilities,
 * transitRoutes and routeProfiles). Creates an unmapped schedule with missing departures.
 *
 * TODO implement in network creator?
 *
 * @author polettif
 */
public class OSM2MTS {

	private static final Logger log = Logger.getLogger(OSM2MTS.class);
	private static final String V_STOP = "stop";
	private static final String V_STOP_FORWARD = "stop_forward";
	private static final String V_STOP_BACKWARD = "stop_backward";
	private static final String TAG_NAME = "name";
	private static final String TAG_ROUTE = "route";

	private CoordinateTransformation transformation;
	private TransitSchedule transitSchedule;
	private TransitScheduleFactory factory;
	private OSM2MTSHandler handler;

	public OSM2MTS(CoordinateTransformation transformation) {
		this.transformation = transformation;

		this.transitSchedule = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getTransitSchedule();
		this.factory = transitSchedule.getFactory();
	}

	public static void main(final String[] args) {
		String filenameOSM = "C:/Users/polettif/Desktop/basel/osmInput/basel.osm";
		String filenameMTS = "C:/Users/polettif/Desktop/basel/mts/basel.xml";

		OSM2MTS osm2mts = new OSM2MTS(TransformationFactory.getCoordinateTransformation("WGS84", "CH1903_LV03_Plus"));
		osm2mts.parse(filenameOSM);
		osm2mts.convert();
		osm2mts.writeFile(filenameMTS);
	}

	private void writeFile(String filenameMTSoutput) {
		new TransitScheduleWriter(transitSchedule).writeFile(filenameMTSoutput);
	}

	public void parse(String filenameOSMinput) {
		handler = new OSM2MTSHandler();
		OsmParser parser = new OsmParser();
		parser.addHandler(handler);
		parser.readFile(filenameOSMinput);
	}

	/**
	 * Converts relations, nodes and ways from osm to an
	 * unmapped MATSim Transit Schedule
	 */
	public void convert() {
		int routeNr = 1;

		Map<Id<TransitLine>, TransitLine> transitLinesDump = new HashMap<>();

		Map<Long, OsmParser.OsmNode> nodes = handler.getNodes();
		Map<Long, OsmParser.OsmRelation> relations = handler.getRelations();
		Map<Long, OsmParser.OsmWay> ways = handler.getWays();

		for(OsmParser.OsmRelation relation : relations.values()) {


			// todo combine multiple routes in a line
			Id<TransitLine> lineId = createLineId(relation);

			// create line, if it does not yet exist
			TransitLine line;
			if(!transitSchedule.getTransitLines().containsKey(lineId)) {
				line = factory.createTransitLine(lineId);
				transitLinesDump.put(lineId, line);
			} else {
				line = transitLinesDump.get(lineId);
			}

			if(lineId.toString().equals("BLT_17")) {
				log.debug("");
			}

			List<TransitRouteStop> stopSequenceForward = new ArrayList<>();
			List<TransitRouteStop> stopSequenceBackward = new ArrayList<>();

			// create different RouteStops and stopFacilities for forward and backward
			for(int i=0; i<relation.members.size()-1; i++) {
				OsmParser.OsmRelationMember member = relation.members.get(i);

				// todo move to filter?
				if((V_STOP.equals(member.role) || V_STOP_FORWARD.equals(member.role) || V_STOP_BACKWARD.equals(member.role))) {

					// check if referenced stop is within area
					if(nodes.containsKey(member.refId)) {
						TransitStopFacility transitStopFacility;

						// add facility if it does not yet exist
						if(!transitSchedule.getFacilities().containsKey(Id.create(member.refId, TransitStopFacility.class))) {
							OsmParser.OsmNode stopFacilityNode = nodes.get(member.refId);

							Coord coord = transformation.transform(stopFacilityNode.coord);

							transitStopFacility = factory.createTransitStopFacility(Id.create(member.refId, TransitStopFacility.class), coord, false);
							transitStopFacility.setName(stopFacilityNode.tags.get(TAG_NAME));

							// todo other attributes

							transitSchedule.addStopFacility(transitStopFacility);
						} else {
							transitStopFacility = transitSchedule.getFacilities().get(Id.create(member.refId, TransitStopFacility.class));
						}

						// create transitRouteStop
						TransitRouteStop newRouteStop = factory.createTransitRouteStop(transitStopFacility, 0.0, 0.0);

						if(V_STOP.equals(member.role)) {
							stopSequenceForward.add(newRouteStop);
							stopSequenceBackward.add(newRouteStop);
						}/* else if(V_STOP_FORWARD.equals(member.role)) {
							stopSequenceForward.add(newRouteStop);
						} else if(V_STOP_BACKWARD.equals(member.role)) {
							stopSequenceBackward.add(newRouteStop);
						}*/
					}
				}
			}

			// one relation has two routes, forward and back
			TransitRoute routeForward = factory.createTransitRoute(Id.create(++routeNr, TransitRoute.class), null, stopSequenceForward, relation.tags.get(TAG_ROUTE));
			routeForward.addDeparture(factory.createDeparture(Id.create("departure" + routeNr, Departure.class), 60.0));

			Collections.reverse(stopSequenceBackward);
			TransitRoute routeBackward = factory.createTransitRoute(Id.create(++routeNr, TransitRoute.class), null, stopSequenceBackward, relation.tags.get(TAG_ROUTE));
			routeBackward.addDeparture(factory.createDeparture(Id.create("departure" + routeNr, Departure.class), 120.0));
			// todo remove dummy departures? Are needed for visualisation in via

			// only add route to transitLine if it has stops
			if(stopSequenceForward.size() > 0)
				line.addRoute(routeForward);

			if(stopSequenceBackward.size() > 0)
				line.addRoute(routeBackward);

			log.info("routes added to line " + line.getId());
		}

		// add lines to schedule
		for(TransitLine transitLine : transitLinesDump.values()) {
			if(transitLine.getRoutes().size() > 0) {
				this.transitSchedule.addTransitLine(transitLine);
			}
		}

		// remove non used facilities to schedule
		PTMapperUtils.removeNonUsedStopFacilities(transitSchedule);
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

	@Deprecated
	public void run(String filenameOSMinput, String filenameMTSoutput) {
		this.transitSchedule = new TransitScheduleFactoryImpl().createTransitSchedule();
		this.factory = transitSchedule.getFactory();

		OSM2MTSHandler handler = new OSM2MTSHandler();

		OsmParser parser = new OsmParser();
		parser.addHandler(handler);
		parser.readFile(filenameOSMinput);

		convert();

		log.info("OSM file parsed. Writing schedule to file...");
		new TransitScheduleWriter(transitSchedule).writeFile(filenameMTSoutput);
	}

}