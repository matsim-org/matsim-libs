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

package playground.polettif.publicTransitMapping.plausibility;

import com.vividsolutions.jts.geom.Coordinate;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.opengis.feature.simple.SimpleFeature;
import playground.polettif.publicTransitMapping.plausibility.log.*;
import playground.polettif.publicTransitMapping.tools.CsvTools;
import playground.polettif.publicTransitMapping.tools.MiscUtils;
import playground.polettif.publicTransitMapping.tools.NetworkTools;
import playground.polettif.publicTransitMapping.tools.ScheduleTools;
import playground.polettif.publicTransitMapping.tools.shp.Schedule2ShapeFileWriter;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.*;

import static playground.polettif.publicTransitMapping.tools.CoordTools.getAzimuth;
import static playground.polettif.publicTransitMapping.tools.ScheduleTools.getLinkIds;

/**
 * Performs a plausibility check on the given schedule
 * and network.
 *
 * @author polettif
 */
public class PlausibilityCheck {

	protected static Logger log = Logger.getLogger(PlausibilityCheck.class);

	public static final String CsvSeparator = ";";

	private static final double PI = Math.PI;
	private static final double PI2 = 2 * Math.PI;

	public static final String TRAVEL_TIME_WARNING = "TravelTimeWarning";
	public static final String LOOP_WARNING = "LoopWarning";
	public static final String DIRECTION_CHANGE_WARNING = "DirectionChangeWarning";

	private Set<PlausibilityWarning> allWarnings = new HashSet<>();
	private Map<TransitLine, Map<TransitRoute, Set<PlausibilityWarning>>> warningsSchedule = new HashMap<>();
	private Map<List<Id<Link>>, Set<PlausibilityWarning>> warningsLinkIds = new HashMap<>();
	private Map<Id<Link>, Set<PlausibilityWarning>> warningsLinks = new HashMap<>();

	private Map<String, Double> thresholds;

	private TransitSchedule schedule;
	private Network network;

	public PlausibilityCheck(TransitSchedule schedule, Network network) {
		this.schedule = schedule;
		this.network = network;

		this.thresholds = new HashMap<>();
		this.thresholds.put("bus", 0.8 * PI);
		this.thresholds.put("rail", 0.2 * PI);
	}

	/**
	 * @param args schedule file, network file, output folder
	 */
	public static void main(final String[] args) {
		TransitSchedule schedule = ScheduleTools.loadTransitSchedule(args[0]);
		Network network = NetworkTools.loadNetwork(args[1]);

		PlausibilityCheck check = new PlausibilityCheck(schedule, network);
		check.run();

		check.writeCsv(args[2] + "allPlausibilityWarnings.csv");
		check.writeResultShapeFiles(args[2]);

		Schedule2ShapeFileWriter schedule2shp = new Schedule2ShapeFileWriter(schedule, network);
		schedule2shp.routes2Polylines(args[2]+"Schedule_TransitRoutes.shp");
		schedule2shp.stopFacilities2Shapes(args[2]+"Schedule_StopFacilities.shp", args[2]+"Schedule_StopFacilities_refLinks.shp");
	}

	/**
	 * Performs the plausibility check on the schedule
	 */
	public void run() {
		AbstractPlausibilityWarning.setNetwork(network);

		for(TransitLine transitLine : this.schedule.getTransitLines().values()) {
			for(TransitRoute transitRoute : transitLine.getRoutes().values()) {

				Double directionChangeThreshold = thresholds.get(transitRoute.getTransportMode());

				Iterator<TransitRouteStop> stopsIterator = transitRoute.getStops().iterator();

				List<Link> links = NetworkTools.getLinksFromIds(network, getLinkIds(transitRoute));
				Map<Node, Tuple<Link, Link>> nodesInRoute = new HashMap<>();
				Set<List<Id<Link>>> loops = new HashSet<>();

				TransitRouteStop previousStop = stopsIterator.next();
				TransitRouteStop nextStop = stopsIterator.next();
				double ttActual = 0;
				double departTime = previousStop.getDepartureOffset();

				for(int i = 0; i < links.size() - 2; i++) {
					Link linkFrom = links.get(i);
					Link linkTo = links.get(i + 1);

					// travel time check
					ttActual += linkFrom.getLength() / linkFrom.getFreespeed();
					if(nextStop.getStopFacility().getLinkId().equals(linkTo.getId())) {
						double ttSchedule = nextStop.getArrivalOffset() - departTime;
						if(ttActual > ttSchedule) {
							PlausibilityWarning warning = new TravelTimeWarning(transitLine, transitRoute, previousStop, nextStop, ttActual, ttSchedule);
							addWarningToContainers(warning);
						}
						// reset
						ttActual = 0;
						previousStop = nextStop;
						departTime = previousStop.getDepartureOffset();
						nextStop = stopsIterator.next();
					}

					// loopcheck
					Tuple<Link, Link> tuple = nodesInRoute.put(linkFrom.getToNode(), new Tuple<>(linkFrom, linkTo));
					if(tuple != null && !linkFrom.equals(tuple.getSecond())) {
						loops.add(ScheduleTools.getLoopSubRouteLinkIds(transitRoute, tuple.getSecond().getId(), linkFrom.getId()));
					}

					// angle check (check if one link has length 0)
					if(directionChangeThreshold != null) {
						double angleDiff = getAzimuthDiff(linkFrom, linkTo);
						if(Math.abs(angleDiff) > directionChangeThreshold && linkFrom.getLength() > 0 && linkTo.getLength() > 0 && angleDiff != PI) {
							PlausibilityWarning warning = new DirectionChangeWarning(transitLine, transitRoute, linkFrom, linkTo, directionChangeThreshold, angleDiff);
							addWarningToContainers(warning);
						}
					}
				}

				// get "loop" that are part of a bigger loop
				Set<List<Id<Link>>> subsetLoops = new HashSet<>();
				for(List<Id<Link>> loop1 : loops) {
					for(List<Id<Link>> loop2 : loops) {
						if(!loop1.equals(loop2) && MiscUtils.listIsSubset(loop1, loop2)) {
							subsetLoops.add(loop1);
						}
					}
				}
				// add LoopWarning
				for(List<Id<Link>> loop : loops) {
					if(!subsetLoops.contains(loop)) {
						PlausibilityWarning warning = new LoopWarning(transitLine, transitRoute, loop);
						addWarningToContainers(warning);
					}
				}
			}
		}
	}

	public void writeCsv(String outputFile) {
		List<String> csvLines = new ArrayList<>();
		csvLines.add(AbstractPlausibilityWarning.CSV_HEADER);
		for(PlausibilityWarning w : allWarnings) {
			csvLines.add(w.getCsvLine());
		}
		try {
			log.info("Writing warnings to csv file " +outputFile +" ...");
			CsvTools.writeToFile(csvLines, outputFile);
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	public void writeResultShapeFiles(String outputPath) {
		log.info("Writing warnings shapefiles in folder " + outputPath + " ...");

		Collection<SimpleFeature> traveltTimeWarningsFeatures = new ArrayList<>();
		Collection<SimpleFeature> loopWarningsFeatures = new ArrayList<>();
		Collection<SimpleFeature> directionChangeWarningsFeatures = new ArrayList<>();

		PolylineFeatureFactory travelTimeWarningsFF = new PolylineFeatureFactory.Builder()
				.setName("TravelTimeWarnings")
				.setCrs(MGC.getCRS("EPSG:2056"))
				.addAttribute("warningIds", String.class)
				.addAttribute("routeIds", String.class)
				.addAttribute("linkIds", String.class)
				.addAttribute("diff", Double.class)
				.addAttribute("expected", Double.class)
				.addAttribute("actual", Double.class)
				.create();

		PolylineFeatureFactory loopWarningsFF = new PolylineFeatureFactory.Builder()
				.setName("LoopWarnings")
				.setCrs(MGC.getCRS("EPSG:2056"))
				.addAttribute("warningIds", String.class)
				.addAttribute("routeIds", String.class)
				.addAttribute("linkIds", String.class)
				.create();

		PolylineFeatureFactory directionChangeWarnings = new PolylineFeatureFactory.Builder()
				.setName("DirectionChangeWarnings")
				.setCrs(MGC.getCRS("EPSG:2056"))
				.addAttribute("warningIds", String.class)
				.addAttribute("routeIds", String.class)
				.addAttribute("linkIds", String.class)
				.addAttribute("diff [rad]", String.class)
				.addAttribute("diff [gon]", String.class)
				.create();

		// route through all linkIdLists
		for(Map.Entry<List<Id<Link>>, Set<PlausibilityWarning>> e : warningsLinkIds.entrySet()) {
			boolean createLoopFeature = false;
			boolean createTravelTimeFeature = false;
			boolean createDirectionChangeFeature = false;

			double diff = -1, ttExpected = -1, ttActual = -1, azDiff = 0.0;

			Set<Id<PlausibilityWarning>> warningIds = new HashSet<>();
			Set<String> routeIds = new HashSet<>();

			for(PlausibilityWarning w : e.getValue()) {
				// Travel Time Warnings
				if(w instanceof TravelTimeWarning) {
					createTravelTimeFeature = true;
					if(w.getDifference() > diff) {
						diff = w.getDifference();
					}
					if(w.getExpected() > ttExpected) {
						ttExpected = w.getExpected();
					}
					if(w.getActual() > ttActual) {
						ttActual = w.getActual();
					}
					warningIds.add(w.getId());
					routeIds.add(w.getTransitLine().getId() + ":" + w.getTransitRoute().getId());
				}

				// Direction Change Warnings
				if(w instanceof DirectionChangeWarning) {
					createDirectionChangeFeature = true;
					warningIds.add(w.getId());
					routeIds.add(w.getTransitLine().getId() + ":" + w.getTransitRoute().getId());
					azDiff = w.getDifference();
				}

				// Loop Warnings
				if(w instanceof LoopWarning) {
					createLoopFeature = true;
					warningIds.add(w.getId());
					routeIds.add(w.getTransitLine().getId() + ":" + w.getTransitRoute().getId());
				}
			}

			// Travel Time Warnings
			if(createTravelTimeFeature) {
				SimpleFeature f = travelTimeWarningsFF.createPolyline(linkIdList2Coordinates(e.getKey()));
				f.setAttribute("warningIds", CollectionUtils.idSetToString(warningIds));
				f.setAttribute("routeIds", CollectionUtils.setToString(routeIds));
				f.setAttribute("linkIds", CollectionUtils.idSetToString(new HashSet<>(e.getKey())));
				f.setAttribute("diff", diff);
				f.setAttribute("expected", ttExpected);
				f.setAttribute("actual", ttActual);
				traveltTimeWarningsFeatures.add(f);
			}

			// Direction Change Warning
			if(createDirectionChangeFeature) {
				SimpleFeature f = directionChangeWarnings.createPolyline(linkIdList2Coordinates(e.getKey()));
				f.setAttribute("warningIds", CollectionUtils.idSetToString(warningIds));
				f.setAttribute("routeIds", CollectionUtils.setToString(routeIds));
				f.setAttribute("linkIds", CollectionUtils.idSetToString(new HashSet<>(e.getKey())));
				f.setAttribute("diff [rad]", azDiff);
				f.setAttribute("diff [gon]", 200*azDiff/Math.PI);
				directionChangeWarningsFeatures.add(f);
			}

			// Loop Warnings
			if(createLoopFeature) {
				SimpleFeature f = loopWarningsFF.createPolyline(linkIdList2Coordinates(e.getKey()));
				f.setAttribute("warningIds", CollectionUtils.idSetToString(warningIds));
				f.setAttribute("routeIds", CollectionUtils.setToString(routeIds));
				f.setAttribute("linkIds", CollectionUtils.idSetToString(new HashSet<>(e.getKey())));
				loopWarningsFeatures.add(f);
			}
		}

		ShapeFileWriter.writeGeometries(traveltTimeWarningsFeatures, outputPath + "WarningsTravelTime.shp");
		ShapeFileWriter.writeGeometries(directionChangeWarningsFeatures, outputPath + "WarningsDirectionChange.shp");
		ShapeFileWriter.writeGeometries(loopWarningsFeatures, outputPath + "WarningsLoop.shp");
	}

	/**
	 * Adds a warning object to the different data containers.
	 */
	private void addWarningToContainers(PlausibilityWarning warning) {
		allWarnings.add(warning);
		MapUtils.getSet(warning.getTransitRoute(), MapUtils.getMap(warning.getTransitLine(), this.warningsSchedule)).add(warning);
		MapUtils.getSet(warning.getLinkIds(), warningsLinkIds).add(warning);

		for(Id<Link> linkId : warning.getLinkIds()) {
			MapUtils.getSet(linkId, warningsLinks).add(warning);
		}
	}

	/**
	 * Transforms a list of link ids to an array of coordinates for shp features
	 *
	 * @return
	 */
	public Coordinate[] linkIdList2Coordinates(List<Id<Link>> linkIdList) {
		List<Coordinate> coordList = new ArrayList<>();
		for(Id<Link> linkId : linkIdList) {
			coordList.add(MGC.coord2Coordinate(network.getLinks().get(linkId).getFromNode().getCoord()));
		}
		coordList.add(MGC.coord2Coordinate(network.getLinks().get(linkIdList.get(linkIdList.size() - 1)).getToNode().getCoord()));
		Coordinate[] coordinates = new Coordinate[coordList.size()];
		coordList.toArray(coordinates);
		return coordinates;
	}

	/**
	 * calculates the azimuth difference of two links
	 *
	 * @param link1
	 * @param link2
	 * @return the difference in [rad]
	 */
	private static double getAzimuthDiff(Link link1, Link link2) {
		Coord c1 = link1.getFromNode().getCoord();
		Coord c2 = link2.getToNode().getCoord();

		if(c1.equals(c2)) {
			return PI;
		}

		double az1 = getAzimuth(c1, link1.getToNode().getCoord());
		double az2 = getAzimuth(link2.getFromNode().getCoord(), c2);
		double diff = Math.abs(az2 - az1);

		return (diff > PI ? PI2 - diff : diff);
	}


	private void p(Object s) {
		System.out.println(s);
	}



	@Deprecated
	public void writeCsvResultsBySchedule(String outputFile) {
		List<String> csvLines = new ArrayList<>();
		csvLines.add("WarningType" + PlausibilityCheck.CsvSeparator +
				"TransitLine" + PlausibilityCheck.CsvSeparator +
				"TransitRoute" + PlausibilityCheck.CsvSeparator +
				"fromId" + PlausibilityCheck.CsvSeparator +
				"toId" + PlausibilityCheck.CsvSeparator +
				"diff" + PlausibilityCheck.CsvSeparator +
				"expected" + PlausibilityCheck.CsvSeparator +
				"actual");
		for(Map.Entry<TransitLine, Map<TransitRoute, Set<PlausibilityWarning>>> e : warningsSchedule.entrySet()) {
			for(Map.Entry<TransitRoute, Set<PlausibilityWarning>> e2 : e.getValue().entrySet()) {
				for(PlausibilityWarning warning : e2.getValue()) {
					csvLines.add(warning.getCsvLine());
				}
			}
		}
		try {
			CsvTools.writeToFile(csvLines, outputFile);
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	@Deprecated
	public void writeCsvResultsByLinkId(String outputFile) {
		List<String> csvLines = new ArrayList<>();
		for(Map.Entry<Id<Link>, Set<PlausibilityWarning>> e : warningsLinks.entrySet()) {
			for(PlausibilityWarning warning : e.getValue()) {
				csvLines.addAll(warning.getCsvLineForEachLink());
			}
		}

		try {
			CsvTools.writeToFile(csvLines, outputFile);
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}


	public void printSummary() {
		for(TransitLine transitLine : this.schedule.getTransitLines().values()) {
			for(TransitRoute transitRoute : transitLine.getRoutes().values()) {
				System.out.println(transitRoute.getId());
				System.out.println("    tt:   " + TravelTimeWarning.routeStat.get(transitRoute));
				System.out.println("    loop: " + LoopWarning.routeStat.get(transitRoute));
				System.out.println("    dir:  " + DirectionChangeWarning.routeStat.get(transitRoute));
			}
		}
	}

	public void printLineSummary() {
		for(TransitLine transitLine : this.schedule.getTransitLines().values()) {
			System.out.println(transitLine.getId());
			System.out.println("    tt:   " + TravelTimeWarning.lineStat.get(transitLine));
			System.out.println("    loop: " + LoopWarning.lineStat.get(transitLine));
			System.out.println("    dir:  " + DirectionChangeWarning.lineStat.get(transitLine));
		}
	}

	@Deprecated
	public void printResultByTransitRoute() {
		for(Map.Entry<TransitLine, Map<TransitRoute, Set<PlausibilityWarning>>> e : warningsSchedule.entrySet()) {
			p("\n#################################\n" + e.getKey().getId());
			for(Map.Entry<TransitRoute, Set<PlausibilityWarning>> e2 : e.getValue().entrySet()) {
				p("\t" + e2.getKey().getId());
				for(PlausibilityWarning l : e2.getValue()) {
					System.out.println("\t" + l);
				}
			}
		}
	}

	@Deprecated
	public void printResultByLink() {
		for(Map.Entry<Id<Link>, Set<PlausibilityWarning>> e : warningsLinks.entrySet()) {
			System.out.println("Link " + e.getKey().toString());
			Set<Tuple<Object, Object>> stopPairs = new HashSet<>();
			for(PlausibilityWarning warning : e.getValue()) {
				if(stopPairs.add(warning.getPair())) {
					System.out.println(warning);
				}
			}
		}
	}
}