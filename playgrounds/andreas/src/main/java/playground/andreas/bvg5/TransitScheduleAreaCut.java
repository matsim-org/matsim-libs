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

package playground.andreas.bvg5;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * 
 * @author aneumann
 *
 */
public class TransitScheduleAreaCut {
	
	private final static Logger log = Logger.getLogger(TransitScheduleAreaCut.class);
	private Geometry area = null;
	private TransitSchedule transitSchedule;
	private HashMap<TransitLine, List<TransitRoute>> allStopsWithinTheArea;
	private HashMap<TransitLine, List<TransitRoute>> firstStopWithinTheArea;
	private HashMap<TransitLine, List<TransitRoute>> lastStopWithinTheArea;
	private HashMap<TransitLine, List<TransitRoute>> firstAndLastStopOutsideTheAreaButPassingThrough;
	private HashMap<TransitLine, List<TransitRoute>> firstAndLastStopInsideTheAreaButLineLeavesAtLeastOnce;
	private HashMap<TransitLine, List<TransitRoute>> nonClassified;
	
	public TransitScheduleAreaCut(TransitSchedule transitSchedule, String areaShape) {
		Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(areaShape);
		for (SimpleFeature simpleFeature : features) {
			if (this.area == null) {
				this.area = (Geometry) simpleFeature.getDefaultGeometry();
			} else {
				this.area = this.area.union((Geometry) simpleFeature.getDefaultGeometry());
			}
		}
		this.transitSchedule = transitSchedule;
	}

	public static TransitSchedule cutTransitSchedule(TransitSchedule transitSchedule, String areaShape) {
		log.info("Removing parts of transit routes within the given area: " + areaShape);
		TransitScheduleAreaCut cutter = new TransitScheduleAreaCut(transitSchedule, areaShape);
		cutter.classify();
		cutter.removeRoutesCompletelyWithinArea();
		cutter.processThroughPassingLines();
		cutter.processStartInAreaLines();
	
		log.info("...done");
		return cutter.transitSchedule;
	}

	private void processStartInAreaLines() {
		ArrayList<Id> routesProcessed = new ArrayList<Id>();
		for (Entry<TransitLine, List<TransitRoute>> lineEntry : this.firstStopWithinTheArea.entrySet()) {
			for (TransitRoute route : lineEntry.getValue()) {
				processStartRoute(route);
				routesProcessed.add(route.getId());
			}
		}
		log.info("Removed the end of the following " + routesProcessed.size() + " transit routes from schedule: " + routesProcessed);
	}

	private void processStartRoute(TransitRoute route) {
		TransitRouteStop lastRouteStopToKeep = null;
		List<TransitRouteStop> stopsToKeep = new LinkedList<TransitRouteStop>();
		
		for (TransitRouteStop stop : route.getStops()) {
			stopsToKeep.add(stop);
			if(this.coordInServiceArea(stop.getStopFacility().getCoord(), this.area)){
				// last stop to keep found
				lastRouteStopToKeep = stop;
				break;
			}
		}
		// There is no setter - need to deep copy the whole route
//		route.setStops(stopsToKeep);
		
		LinkedList<Id<Link>> linkIds = new LinkedList<Id<Link>>();
		Id endLinkId = null;
		for (Id linkId : route.getRoute().getLinkIds()) {
			linkIds.add(linkId);
			if (linkId.toString().equalsIgnoreCase(lastRouteStopToKeep.getStopFacility().getLinkId().toString())) {
				endLinkId = linkIds.pollLast();
				break;
			}
		}
		Id startLinkId = linkIds.pollFirst();
		route.getRoute().setLinkIds(startLinkId, linkIds, endLinkId);
	}

	private void processThroughPassingLines() {
		log.info("Cannot process the throughpassing lines due to missing implementation. Will leave the following " + this.firstAndLastStopOutsideTheAreaButPassingThrough.size() + " routes unmodified: " + this.firstAndLastStopOutsideTheAreaButPassingThrough);
	}

	private void removeRoutesCompletelyWithinArea() {
		ArrayList<Id> routesRemoved = new ArrayList<Id>();
		for (Entry<TransitLine, List<TransitRoute>> lineEntry : this.allStopsWithinTheArea.entrySet()) {
			for (TransitRoute route : lineEntry.getValue()) {
				lineEntry.getKey().removeRoute(route);
				routesRemoved.add(route.getId());
			}
		}
		log.info("Removed the following " + routesRemoved.size() + " transit routes from schedule: " + routesRemoved);
	}

	private void classify() {
		this.allStopsWithinTheArea = new HashMap<TransitLine, List<TransitRoute>>();
		this.firstStopWithinTheArea = new HashMap<TransitLine, List<TransitRoute>>();
		this.lastStopWithinTheArea = new HashMap<TransitLine, List<TransitRoute>>();
		this.firstAndLastStopOutsideTheAreaButPassingThrough = new HashMap<TransitLine, List<TransitRoute>>();
		this.firstAndLastStopInsideTheAreaButLineLeavesAtLeastOnce = new HashMap<TransitLine, List<TransitRoute>>();
		this.nonClassified = new HashMap<TransitLine, List<TransitRoute>>();
		
		// Classify routes
		for (TransitLine line : transitSchedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				Boolean startStopWithinArea = null;
				Boolean endStopWithinArea = null;
				Boolean anyIntermediateStopWithinArea = false;
				Boolean anyIntermediateStopOutsideTheArea = false;
				for (TransitRouteStop stop : route.getStops()) {
					boolean stopWithinArea = this.coordInServiceArea(stop.getStopFacility().getCoord(), this.area);
					if (startStopWithinArea == null) {
						startStopWithinArea = stopWithinArea;
					} else {
						if (stopWithinArea) {
							anyIntermediateStopWithinArea = true;
						} else {
							anyIntermediateStopOutsideTheArea = true;
						}
					}

					endStopWithinArea = stopWithinArea;
				}

				// There are five common types of transit line
				if (startStopWithinArea && endStopWithinArea) {
					if (anyIntermediateStopOutsideTheArea) {
						// start and end within area but lines leaves the area before returning
						if (firstAndLastStopInsideTheAreaButLineLeavesAtLeastOnce.get(line) == null) {
							firstAndLastStopInsideTheAreaButLineLeavesAtLeastOnce.put(line, new LinkedList<TransitRoute>());
						}
						firstAndLastStopInsideTheAreaButLineLeavesAtLeastOnce.get(line).add(route);
					} else {
						// all stops are wihtin the area
						if (allStopsWithinTheArea.get(line) == null) {
							allStopsWithinTheArea.put(line, new LinkedList<TransitRoute>());
						}
						allStopsWithinTheArea.get(line).add(route);
					}
				} else if (!startStopWithinArea && !endStopWithinArea && anyIntermediateStopWithinArea) {
					// line starts and ends outside the area but passes through
					if (firstAndLastStopOutsideTheAreaButPassingThrough.get(line) == null) {
						firstAndLastStopOutsideTheAreaButPassingThrough.put(line, new LinkedList<TransitRoute>());
					}
					firstAndLastStopOutsideTheAreaButPassingThrough.get(line).add(route);
				} else if (startStopWithinArea && !endStopWithinArea) {
					// starting within area and leaving
					if (firstStopWithinTheArea.get(line) == null) {
						firstStopWithinTheArea.put(line, new LinkedList<TransitRoute>());
					}
					firstStopWithinTheArea.get(line).add(route);
				} else if (!startStopWithinArea && endStopWithinArea) {
					// route starts outside the area but end within the area
					if (lastStopWithinTheArea.get(line) == null) {
						lastStopWithinTheArea.put(line, new LinkedList<TransitRoute>());
					}
					lastStopWithinTheArea.get(line).add(route);
				} else {
					if (nonClassified.get(line) == null) {
						nonClassified.put(line, new LinkedList<TransitRoute>());
					}
					nonClassified.get(line).add(route);
				}
			}
		}
	}

	private boolean coordInServiceArea(Coord coord, Geometry geometry) {
		Point p = new GeometryFactory().createPoint(MGC.coord2Coordinate(coord));
		if(geometry.contains(p)){
			return true;
		}
		return false;
	}
	
	
	public static void main(String[] args) {
		String shape = "F:/temp/scenarioArea.shp";
		String netFile = "F:/temp/network.final.xml.gz";
		String scheduleFile = "F:/temp/remainingSchedule.xml.gz";
		
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		sc.getConfig().network().setInputFile(netFile);
		ScenarioUtils.loadScenario(sc);
		sc.getConfig().scenario().setUseTransit(true);
		TransitScheduleReader scheduleReader = new TransitScheduleReader(sc);
		scheduleReader.readFile(scheduleFile);
		
		TransitSchedule schedule = TransitScheduleAreaCut.cutTransitSchedule(sc.getTransitSchedule(), shape);
		
		TransitScheduleWriter writer = new TransitScheduleWriter(schedule);
		writer.writeFile("F:/temp/out.xml.gz");
	}
}
