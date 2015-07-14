/* *********************************************************************** *
 * project: org.matsim.*
 * UCSBTransitScheduleCreator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.ucsb.transit;

import com.vividsolutions.jts.geom.Coordinate;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkWriteAsTable;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.*;
import org.opengis.feature.simple.SimpleFeature;

import java.io.BufferedReader;
import java.util.*;

/**
 * @author balmermi
 *
 */
public class UCSBTransitScheduleCreator {
	
	private final static Logger log = Logger.getLogger(UCSBTransitScheduleCreator.class);

	private static final String ILIN_NAME = "ilin";
	private static final String DIST_NAME = "dist";
	private static final String TRIP_NAME = "trip";
	private static final String TIME_NAME = "timexsec";
	private static final String DESC_NAME = "loc";
	private static final String MODE_NAME = "mode";
	
	private static final String FAC_ID_PREFIX = "stop-";
	private static final String PSEUDO_ID_PREFIX = "pn-";
	
	private static final int MAX_SPEED = 80; // feet per sec
	private static final double DEFAULT_CAP = 999999.0; // cars per hour
	private static final double DEFAULT_LANES = 1.0;
	
	private static final double FEET2METER = 0.3048;

	private final Scenario scenario;
	private final String ptTimesFile;
	private final String ptStopsFile;
	
	private final Map<Integer,PtLine> ptLines = new HashMap<Integer,PtLine>();
	
	public UCSBTransitScheduleCreator(Scenario scenario, String ptTimesFile, String ptStopsFile) {
		this.scenario = scenario;
		this.ptTimesFile = ptTimesFile;
		this.ptStopsFile = ptStopsFile;
	}
	
	private final void createScheduleVehicleAndNetwork() {
		log.info("creating schedule and network from pt data...");

		TransitSchedule schedule = scenario.getTransitSchedule();
		TransitScheduleFactory factory = schedule.getFactory();

		Network network = scenario.getNetwork();
		
		Vehicles vehicles = ((ScenarioImpl)scenario).getTransitVehicles();
		VehicleType defaultVehicleType = vehicles.getFactory().createVehicleType(Id.create(1, VehicleType.class));
		vehicles.addVehicleType(defaultVehicleType);
		defaultVehicleType.setDescription("generic default");
		defaultVehicleType.setLength(7.5);
		defaultVehicleType.setMaximumVelocity(1.0);
		defaultVehicleType.setWidth(1.0);
		defaultVehicleType.setAccessTime(0.01);
		defaultVehicleType.setEgressTime(0.01);
		defaultVehicleType.setDoorOperationMode(VehicleType.DoorOperationMode.serial);
		VehicleCapacity vehicleCapacity = vehicles.getFactory().createVehicleCapacity();
		vehicleCapacity.setSeats(1000);
		vehicleCapacity.setStandingRoom(1000);
		defaultVehicleType.setCapacity(vehicleCapacity);
		
		for (PtLine ptLine : ptLines.values()) {
			TransitLine transitLine = factory.createTransitLine(Id.create(ptLine.ilin, TransitLine.class));
			schedule.addTransitLine(transitLine);
			
			int ptRouteNr = 0;
			for (PtRoute ptRoute : ptLine.ptRoutes) {
				ptRouteNr++;
				List<TransitRouteStop> transitRouteStops = new ArrayList<TransitRouteStop>();
				List<Id<Link>> linkIds = new ArrayList<Id<Link>>();
				Link startLink = null; Link endLink = null;
				PtPoint prevPtPoint = null; Node prevNode = null;
				for (PtPoint ptPoint : ptRoute.ptPoints.values()) {
					
					Id<TransitStopFacility> facilityId = Id.create(FAC_ID_PREFIX+ptLine.ilin+"-"+ptRouteNr+"-"+ptPoint.dist, TransitStopFacility.class);
					if (!schedule.getFacilities().containsKey(facilityId)) {
						TransitStopFacility facility = factory.createTransitStopFacility(facilityId,new CoordImpl(ptPoint.coord),true);
						schedule.addStopFacility(facility);
					}
					
					TransitRouteStop transitRouteStop = factory.createTransitRouteStop(schedule.getFacilities().get(facilityId), ptPoint.timeOffset, ptPoint.timeOffset);
					transitRouteStop.setAwaitDepartureTime(true);
					transitRouteStops.add(transitRouteStop);
					
					Node node = network.getFactory().createNode(Id.create(ptLine.ilin+"-"+ptRouteNr+"-"+ptPoint.dist, Node.class), new CoordImpl(ptPoint.coord));
					network.addNode(node);
					Link link = null;
					if (prevNode == null) { // first ptPoint
						prevNode = network.getFactory().createNode(Id.create(PSEUDO_ID_PREFIX+ptLine.ilin+"-"+ptRouteNr+"-"+ptPoint.dist, Node.class), new CoordImpl(ptPoint.coord));
						network.addNode(prevNode);
						link = network.getFactory().createLink(Id.create(prevNode.getId().toString()+"-"+ptPoint.dist, Link.class),prevNode,node);
						network.addLink(link);
						Set<String> modes = new HashSet<String>(2); modes.add(TransportMode.pt); modes.add(ptRoute.modeType);
						link.setAllowedModes(modes);
						link.setCapacity(DEFAULT_CAP);
						link.setFreespeed(100.0); // m/s
						link.setLength(50.0); // meter
						link.setNumberOfLanes(DEFAULT_LANES);
						startLink = link;
					}
					else {
						link = network.getFactory().createLink(Id.create(prevNode.getId().toString()+"-"+ptPoint.dist, Link.class),prevNode,node);
						network.addLink(link);
						Set<String> modes = new HashSet<String>(2); modes.add(TransportMode.pt); modes.add(ptRoute.modeType);
						link.setAllowedModes(modes);
						link.setCapacity(DEFAULT_CAP);
						int segmentDistance = (ptPoint.dist-prevPtPoint.dist)*100; // feet
						int segmentTime = ptPoint.timeOffset-prevPtPoint.timeOffset; // sec
						link.setFreespeed(Math.ceil(FEET2METER*(segmentDistance)/(segmentTime))); // m/s
						link.setLength(FEET2METER*(segmentDistance));
						link.setNumberOfLanes(DEFAULT_LANES);
						endLink = link;
						linkIds.add(link.getId());
					}
					schedule.getFacilities().get(facilityId).setLinkId(link.getId());
					schedule.getFacilities().get(facilityId).setName(ptPoint.desc);

					prevNode = node;
					prevPtPoint = ptPoint;
				}
				linkIds.remove(linkIds.size()-1); // remove the end link
				NetworkRoute networkRoute = new LinkNetworkRouteImpl(startLink.getId(),endLink.getId());
				networkRoute.setLinkIds(startLink.getId(),linkIds,endLink.getId());
				TransitRoute transitRoute = factory.createTransitRoute(Id.create(transitLine.getId()+"."+ptRouteNr, TransitRoute.class), networkRoute, transitRouteStops,ptRoute.modeType);
				transitLine.addRoute(transitRoute);
				
				int depNr = 0;
				for (Integer depTime : ptRoute.depTimes) {
					depNr++;
					Departure departure = factory.createDeparture(Id.create(transitRoute.getId().toString()+"."+depNr, Departure.class),depTime);
					departure.setVehicleId(Id.create(departure.getId(), Vehicle.class));
					Vehicle vehicle = vehicles.getFactory().createVehicle(Id.create(departure.getId(), Vehicle.class),defaultVehicleType);
					vehicles.addVehicle(vehicle);
					transitRoute.addDeparture(departure);
				}
			}
		}
		log.info(schedule.getFacilities().size()+" transit stop facilities created.");
		log.info(schedule.getTransitLines().size()+" transit lines created.");
		int routeCnt = 0; int depCnt = 0; int stopCnt = 0;
		for (TransitLine transitLine : schedule.getTransitLines().values()) {
			routeCnt += transitLine.getRoutes().size();
			for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
				depCnt += transitRoute.getDepartures().size();
				stopCnt += transitRoute.getStops().size();
			}
		}
		log.info(routeCnt+" transit routes created.");
		log.info(depCnt+" departures created.");
		log.info(stopCnt+" transit route stops created.");
		log.info(network.getNodes().size()+" network nodes created.");
		log.info(network.getLinks().size()+" network links created.");
		log.info(vehicles.getVehicles().size()+" transit vehicles created.");
		log.info("done. (creating)");
	}
	
	private final void parseDataFromPtStopsFile() {
		log.info("parsing data from UCSB pt stops shape file...");
		int fCnt = 0;
		int ptStopsIgnoredCnt = 0;
		Set<Integer> ilinIgnored = new TreeSet<Integer>();
		for (SimpleFeature f : ShapeFileReader.getAllFeatures(ptStopsFile)) {
			fCnt++;

			// data
			int ilinId = Integer.parseInt(f.getAttribute(ILIN_NAME).toString().trim());
			int distId = Integer.parseInt(f.getAttribute(DIST_NAME).toString().trim());
			
			Coordinate c = new Coordinate((f.getBounds().getMinX() + f.getBounds().getMaxX())/2.0, (f.getBounds().getMinY() + f.getBounds().getMaxY())/2.0);
			String desc = f.getAttribute(DESC_NAME).toString().trim();
			String ptModeType = f.getAttribute(MODE_NAME).toString().trim();
			if (!(ptModeType.equals("1CR") || ptModeType.equals("2LR") ||
			      ptModeType.equals("3EX") || ptModeType.equals("4RB") ||
			      ptModeType.equals("5LB") || ptModeType.equals("6TW"))) {
				throw new RuntimeException("fCnt "+fCnt+": ptType="+ptModeType+" is neither 1CR, 2LR, 3EX, 4RB, 5LB nor 6TW!");
			}

			PtLine ptLine = ptLines.get(ilinId);
			if (ptLine == null) { ptStopsIgnoredCnt++; ilinIgnored.add(ilinId); continue; }
			for (PtRoute ptRoute : ptLine.ptRoutes) {
				if ((ptRoute.modeType != null) && (!ptRoute.modeType.equals(ptModeType))) {
					throw new RuntimeException("fCnt "+fCnt+"; ilin="+ptLine.ilin+": route contains modeType="+ptRoute.modeType+" that does not fit with ptModeType="+ptModeType+".");
				}
				ptRoute.modeType = ptModeType;
				PtPoint ptPoint = ptRoute.ptPoints.get(distId);
				if (ptPoint != null) {
					ptPoint.coord = new CoordImpl(c.x,c.y);
					ptPoint.desc = desc;
				}
			}
		}
		log.info("fCnt = "+fCnt);
		log.info("ptStopsIgnoredCnt = "+ptStopsIgnoredCnt);
		log.info("ilinIgnored = "+ilinIgnored.size());
		log.info("done. (parsing)");
		
		checkPtData();
	}
	
	private final void checkPtData() {
		log.info("checking pt data...");
		for (PtLine ptLine : ptLines.values()) {
			int ptRouteNr = 0;
			for (PtRoute ptRoute : ptLine.ptRoutes) {
				ptRouteNr++;
				if (ptRoute.modeType == null) { log.warn("ilin="+ptLine.ilin+"; ptRouteNr="+ptRouteNr+": no mode type assigned."); }
				for (PtPoint ptPoint : ptRoute.ptPoints.values()) {
					if (ptPoint.coord == null) { log.warn("ilin="+ptLine.ilin+"; ptRouteNr="+ptRouteNr+"; dist="+ptPoint.dist+": no coord assigned."); }
				}
			}
		}
		log.info("done. (checking)");
	}
	
	private final void parseDataFromPtTimesFile() {
		log.info("parsing data from UCSB pt times file...");
		int lineCnt = 0;
		try {
			BufferedReader br = IOUtils.getBufferedReader(ptTimesFile);

			// header
			String curr_line = br.readLine(); lineCnt++;
			String[] heads = curr_line.split("\t", -1);
			Map<String,Integer> column = new LinkedHashMap<String, Integer>(heads.length);
			for (int i=0; i<heads.length; i++) { column.put(heads[i],i); }
			
			// data
			int prevIlin = -1; int prevTrip = -1; int prevDist = -1;
			PtLine currPtLine = null;
			PtRoute currPtRoute = null;
			PtPoint currPtPoint = null;
			boolean isRouteStartBlock = true;
			while ((curr_line = br.readLine()) != null) {
				lineCnt++;
				String[] entries = curr_line.split("\t", -1);
				int ilin = new Integer(entries[column.get(ILIN_NAME)]).intValue();
				int trip = new Integer(entries[column.get(TRIP_NAME)]).intValue();
				int dist = new Integer(entries[column.get(DIST_NAME)]).intValue();
				int time = new Integer(entries[column.get(TIME_NAME)]).intValue();
				
				// store time data
				if (ilin != prevIlin) { // new line starts
					isRouteStartBlock = true;
					currPtLine = new PtLine(ilin);
					ptLines.put(ilin,currPtLine);
					currPtRoute = new PtRoute();
					currPtLine.ptRoutes.add(currPtRoute);
					currPtRoute.depTimes.add(time);
					currPtPoint = new PtPoint(dist,0);
					currPtRoute.ptPoints.put(dist,currPtPoint);
				} else if ((trip != prevTrip) || (dist < prevDist)) { // new route series starts
					isRouteStartBlock = true;
					currPtRoute = new PtRoute();
					currPtLine.ptRoutes.add(currPtRoute);
					currPtRoute.depTimes.add(time);
					currPtPoint = new PtPoint(dist,0);
					currPtRoute.ptPoints.put(dist,currPtPoint);
				} else if (dist != prevDist) { // time block of the next (not the first) pt point starts
					isRouteStartBlock = false;
					int prevTimeOffset = currPtPoint.timeOffset;
					int currTimeOffset = time-currPtRoute.depTimes.iterator().next();
					if (prevTimeOffset < currTimeOffset) { // add only if the time offset fits
						int segmentDistance = (dist-currPtPoint.dist)*100; // feet
						int segmentSpeed = segmentDistance/(currTimeOffset-prevTimeOffset); // feet per sec
						if (segmentSpeed < MAX_SPEED) {
							currPtPoint = new PtPoint(dist,currTimeOffset);
							currPtRoute.ptPoints.put(dist,new PtPoint(dist,time-currPtRoute.depTimes.iterator().next()));
						}
					}
				}
				else { // sill at the same time block of a pt point
					if (isRouteStartBlock) { // store dep times of first pt point of the route
						currPtRoute.depTimes.add(time);
					}

					if (currPtPoint.dist != dist) { // the previous entries of the same block had points with wrong times or speeds and were not added
						int prevTimeOffset = currPtPoint.timeOffset;
						int currTimeOffset = time-currPtRoute.depTimes.iterator().next();
						if (prevTimeOffset < currTimeOffset) { // add only if the time offset fits
							int segmentDistance = (dist-currPtPoint.dist)*100; // feet
							int segmentSpeed = segmentDistance/(currTimeOffset-prevTimeOffset); // feet per sec
							if (segmentSpeed < MAX_SPEED) {
								currPtPoint = new PtPoint(dist,currTimeOffset);
								currPtRoute.ptPoints.put(dist,new PtPoint(dist,time-currPtRoute.depTimes.iterator().next()));
							}
						}
					}
				}
				prevDist = dist;
				prevTrip = trip;
				prevIlin = ilin;
			}
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		log.info(lineCnt+" lines parsed.");
		log.info(ptLines.size() + " pt lines stored.");
		log.info("done. (creating)");

		printSummary();

		log.info("merging pt route data...");
		mergePtRouteData();
		log.info("done. (merging)");

		printSummary();
	}
	
	private final void printSummary() {
		log.info("printing stored data...");
		int lineCnt = 0; int routeCnt = 0; int tripCnt = 0;
		int minSpeed = Integer.MAX_VALUE;
		int maxSpeed = Integer.MIN_VALUE;
		for (PtLine ptLine : ptLines.values()) {
			lineCnt++;
			for (PtRoute ptRoute : ptLine.ptRoutes) {
				routeCnt++;
				tripCnt += ptRoute.depTimes.size();
				PtPoint firstPtPoint = ptRoute.ptPoints.values().iterator().next();
				for (PtPoint ptPoint : ptRoute.ptPoints.values()) {
					if (!ptPoint.equals(firstPtPoint)) {
						int distance = (ptPoint.dist-firstPtPoint.dist)*100; // feet
						if (ptPoint.timeOffset != 0) {
							int speed = Math.round(distance/ptPoint.timeOffset); // feet per sec
							if (speed < minSpeed) { minSpeed = speed; }
							if (speed > maxSpeed) { maxSpeed = speed; }
						}
					}
				}
			}
		}
		log.info("lineCnt  = "+lineCnt);
		log.info("routeCnt = "+routeCnt);
		log.info("tripCnt  = "+tripCnt);
		log.info("minSpeed  = "+minSpeed);
		log.info("maxSpeed  = "+maxSpeed);
		log.info("done. (printing)");
	}
	
	private final void printData() {
		log.info("printing stored data...");
		int lineCnt = 0; int routeCnt = 0; int tripCnt = 0;
		int minSpeed = Integer.MAX_VALUE;
		int maxSpeed = Integer.MIN_VALUE;
		for (PtLine ptLine : ptLines.values()) {
			printLineData(ptLine);
			lineCnt++;
			for (PtRoute ptRoute : ptLine.ptRoutes) {
				routeCnt++;
				tripCnt += ptRoute.depTimes.size();
				PtPoint firstPtPoint = ptRoute.ptPoints.values().iterator().next();
				for (PtPoint ptPoint : ptRoute.ptPoints.values()) {
					if (!ptPoint.equals(firstPtPoint)) {
						int distance = (ptPoint.dist-firstPtPoint.dist)*100; // feet
						if (ptPoint.timeOffset != 0) {
							int speed = Math.round(distance/ptPoint.timeOffset); // feet per sec
							if (speed < minSpeed) { minSpeed = speed; }
							if (speed > maxSpeed) { maxSpeed = speed; }
						}
					}
				}
			}
		}
		log.info("lineCnt  = "+lineCnt);
		log.info("routeCnt = "+routeCnt);
		log.info("tripCnt  = "+tripCnt);
		log.info("minSpeed  = "+minSpeed);
		log.info("maxSpeed  = "+maxSpeed);
		log.info("done. (printing)");
	}
	
	private final void mergePtRouteData() {
		Map<Integer,PtLine> newPtLines = new HashMap<Integer,PtLine>();
		for (PtLine ptLine : ptLines.values()) {
			PtLine newPtLine = new PtLine(ptLine.ilin);
			newPtLines.put(ptLine.ilin,newPtLine);
			for (PtRoute ptRoute : ptLine.ptRoutes) {
				PtRoute newPtRoute = null;
				for (PtRoute route : newPtLine.ptRoutes) {
					if (ptRoute.ptPoints.keySet().equals(route.ptPoints.keySet())) {
						newPtRoute = route;
						break;
					}
				}
				if (newPtRoute == null) { newPtLine.ptRoutes.add(ptRoute); }
				else { newPtRoute.depTimes.addAll(ptRoute.depTimes); }
			}
		}
		ptLines.clear();
		ptLines.putAll(newPtLines);
	}
	
	private final void printLineData(PtLine ptLine) {
		log.info("Line ilin="+ptLine.ilin+":");
		for (PtRoute ptRoute : ptLine.ptRoutes) {
			String timeStr = "  times:";
			for (Integer time : ptRoute.depTimes) { timeStr = timeStr + " " + time; }
			log.info(timeStr);
			String pointStr = "  points:";
			PtPoint firstPtPoint = ptRoute.ptPoints.values().iterator().next();
			for (PtPoint ptPoint : ptRoute.ptPoints.values()) {
				if (ptPoint.timeOffset < 0) { log.info("time offset warning: (d"+ptPoint.dist+"|t"+ptPoint.timeOffset+")"); }
				int speed = 0;
				if (ptPoint.timeOffset > 0) { speed = Math.round((ptPoint.dist-firstPtPoint.dist)*100/ptPoint.timeOffset); }
				pointStr = pointStr + " (d" + ptPoint.dist + "|t"+ptPoint.timeOffset+"|s"+speed+")";
			}
			log.info(pointStr);
		}
	}

	static class PtLine {
		final int ilin;
		List<PtRoute> ptRoutes = new ArrayList<PtRoute>();
		PtLine(int ilin) { this.ilin = ilin; }
	}
	
	static class PtRoute {
		String modeType = null;
		Set<Integer> depTimes = new TreeSet<Integer>();
		Map<Integer,PtPoint> ptPoints = new TreeMap<Integer,PtPoint>();
	}
	
	static class PtPoint {
		final int dist;
		final int timeOffset;
		CoordImpl coord = null;
		String desc = null;
		PtPoint(int dist, int timeOffset) { this.dist = dist; this.timeOffset = timeOffset; }
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().transit().setUseTransit(true);
		scenario.getConfig().scenario().setUseVehicles(true);
		String ptTimesFile = "D:/balmermi/documents/eclipse/input/raw/america/usa/losAngeles/UCSB/0000/transit/preparation/times/times_tuesday_sorted.txt";
		String ptStopsFile = "D:/balmermi/documents/eclipse/input/raw/america/usa/losAngeles/UCSB/0000/transit/preparation/stops/stops_UTM_Zone_11N.shp";
		String outBase = "../../../output/ucsb/";
		UCSBTransitScheduleCreator scheduleCreator = new UCSBTransitScheduleCreator(scenario, ptTimesFile, ptStopsFile);
		scheduleCreator.parseDataFromPtTimesFile();
		scheduleCreator.parseDataFromPtStopsFile();
		scheduleCreator.createScheduleVehicleAndNetwork();
		new NetworkWriter(scenario.getNetwork()).write(outBase+"network.xml.gz");
		new NetworkWriteAsTable(outBase).run(scenario.getNetwork());
		new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile(outBase+"transitSchedule.xml.gz");
		new VehicleWriterV1(((ScenarioImpl)scenario).getTransitVehicles()).writeFile(outBase+"transitVehicles.xml.gz");
	}
}
