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

package playground.polettif.multiModalMap.plausibility;

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
import playground.polettif.multiModalMap.plausibility.log.*;
import playground.polettif.multiModalMap.tools.CsvTools;
import playground.polettif.multiModalMap.tools.NetworkTools;
import playground.polettif.multiModalMap.tools.ScheduleTools;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.*;

import static playground.polettif.multiModalMap.tools.CoordTools.getAzimuth;
import static playground.polettif.multiModalMap.tools.ScheduleTools.getLinkIds;

/**
 * Performs a plausibility check on the given schedule
 * and network.
 *
 * @author polettif
 */
public class PlausibilityCheck {

	public static final String CsvSeparator = ";";

	private Map<TransitLine, Map<TransitRoute, TreeSet<PlausibilityWarning>>> warningsSchedule = new HashMap<>();
	private Map<Id<Link>, TreeSet<PlausibilityWarning>> warningsLinks = new HashMap<>();

	private static final double PI = Math.PI;
	private static final double PI2 = 2*Math.PI;

	// todo mode dependent threshold
	private double uTurnAngleThreshold = 0.75*PI;

	private TransitSchedule schedule;
	private Network network;

	public PlausibilityCheck(TransitSchedule schedule, Network network) {
		this.schedule = schedule;
		this.network = network;
	}

	public static void main(final String[] args) {
		TransitSchedule schedule = ScheduleTools.loadTransitSchedule(args[0]);
		Network network = NetworkTools.loadNetwork(args[1]);

		PlausibilityCheck check = new PlausibilityCheck(schedule, network);
		check.run();

//		check.printResultByLink();
		check.writeCsvResultsBySchedule("C:/Users/polettif/Desktop/output/results_2016-05-10/hafas/plausibilityWarningsSchedule.csv");
		check.writeCsvResultsByLinkId("C:/Users/polettif/Desktop/output/results_2016-05-10/hafas/plausibilityWarningsLinkIds.csv");
		check.createResultShapeFile(args[2]);
	}

	public void run() {
		PlausibilityWarningAbstract.setNetwork(network);

		for(TransitLine transitLine : this.schedule.getTransitLines().values()) {
			for(TransitRoute transitRoute : transitLine.getRoutes().values()) {
				Iterator<TransitRouteStop> stopsIterator = transitRoute.getStops().iterator();

				List<Link> links = NetworkTools.getLinksFromIds(network, getLinkIds(transitRoute));
				Set<Node> nodesInRoute = new HashSet<>();

				TransitRouteStop previousStop = stopsIterator.next();
				TransitRouteStop nextStop = stopsIterator.next();
				double ttActual = 0;
				double departTime = previousStop.getDepartureOffset();

				for(int i=0; i<links.size()-2; i++) {
					Link linkFrom = links.get(i);
					Link linkTo = links.get(i+1);

					// travel time check
					ttActual += linkFrom.getLength() / linkFrom.getFreespeed();

					if(nextStop.getStopFacility().getLinkId().equals(linkTo.getId())) {
						double ttSchedule = nextStop.getArrivalOffset() - departTime;
						if(ttActual > ttSchedule) {
							PlausibilityWarning warning = new TravelTimeWarning(transitLine, transitRoute, previousStop, nextStop, ttActual, ttSchedule);
							getTreeSet(transitRoute, MapUtils.getMap(transitLine, this.warningsSchedule)).add(warning);

							for(Id<Link> linkId : warning.getLinkIds()){
								getTreeSet(linkId, warningsLinks).add(warning);
							}
						}
						// reset
						ttActual = 0;
						previousStop = nextStop;
						departTime = previousStop.getDepartureOffset();
						nextStop = stopsIterator.next();
					}

					// loopcheck
					if(!nodesInRoute.add(linkFrom.getToNode())) {
						PlausibilityWarning warning = new LoopWarning(transitLine, transitRoute, linkFrom, linkTo);
//						getTreeSet(transitRoute, MapUtils.getMap(transitLine, this.warningsSchedule)).add(warning);
					}

					// angle check (check if one link has length 0)
					double angleDiff = getAzimuthDiff(linkFrom, linkTo);
					if(Math.abs(angleDiff) > uTurnAngleThreshold && linkFrom.getLength() > 0 && linkTo.getLength() > 0 && angleDiff != PI) {
						PlausibilityWarning warning = new DirectionChangeWarning(transitLine, transitRoute, linkFrom, linkTo, angleDiff);
//						getTreeSet(transitRoute, MapUtils.getMap(transitLine, this.warningsSchedule)).add(warning);
					}
				}
			}
		}
	}

	public void printResultByTransitRoute() {
		for(Map.Entry<TransitLine, Map<TransitRoute, TreeSet<PlausibilityWarning>>> e : warningsSchedule.entrySet()) {
			p("\n#################################\n"+e.getKey().getId());
			for(Map.Entry<TransitRoute, TreeSet<PlausibilityWarning>> e2 : e.getValue().entrySet()) {
				p("\t" + e2.getKey().getId());
				for(PlausibilityWarning l : e2.getValue()) {
					System.out.println("\t"+l);
				}
			}
		}
	}

	public void printResultByLink() {
		for(Map.Entry<Id<Link>, TreeSet<PlausibilityWarning>> e : warningsLinks.entrySet()) {
			System.out.println("Link " + e.getKey().toString());
			Set<Tuple<Object, Object>> stopPairs = new HashSet<>();
			for(PlausibilityWarning warning : e.getValue()) {
				if(stopPairs.add(warning.getPair())) {
//						System.out.println(warning.linkMessage());
				}
			}
		}
	}

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
		for(Map.Entry<TransitLine, Map<TransitRoute, TreeSet<PlausibilityWarning>>> e : warningsSchedule.entrySet()) {
			for(Map.Entry<TransitRoute, TreeSet<PlausibilityWarning>> e2 : e.getValue().entrySet()) {
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

	public void writeCsvResultsByLinkId(String outputFile) {
		List<String> csvLines = new ArrayList<>();
		for(Map.Entry<Id<Link>, TreeSet<PlausibilityWarning>> e : warningsLinks.entrySet()) {
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

	public void createResultShapeFile(String outFile) {
		Collection<SimpleFeature> features = new ArrayList<>();
		Map<List<Id<Link>>, SimpleFeature> uniqueFeatures = new HashMap<>();

		PolylineFeatureFactory ff = new PolylineFeatureFactory.Builder()
				.setName("PlausibilityWarnings")
				.setCrs(MGC.getCRS("EPSG:2056"))
				.addAttribute("type", String.class)
				.addAttribute("line", String.class)
				.addAttribute("route", String.class)
				.addAttribute("from", String.class)
				.addAttribute("to", String.class)
				.addAttribute("diff", String.class)
				.addAttribute("exp", String.class)
				.addAttribute("act", String.class)
				.create();
/*
		for(Map<TransitRoute, TreeSet<PlausibilityWarning>> e : warningsSchedule.values()) {
			for(TreeSet<PlausibilityWarning> e2 : e.values()) {
				for(PlausibilityWarning warning : e2) {
					if(uniqueFeatures.containsKey(warning.getCoordinates())) {
						SimpleFeature f = uniqueFeatures.get(warning.getLinkIds());
						Set<String> routesAttr = CollectionUtils.stringToSet((String) f.getAttribute("routes"));
						routesAttr.add(warning.getTransitRoute().getId().toString());
						f.setAttribute("routes", CollectionUtils.setToString(routesAttr));
					} else {
						SimpleFeature f = ff.createPolyline(warning.getCoordinates());
						f.setAttribute("type", 	warning.getType());
						f.setAttribute("line", 	warning.getTransitLine().getId());
						f.setAttribute("route", warning.getTransitRoute().getId());
						f.setAttribute("from", 	warning.getFromId());
						f.setAttribute("to", 	warning.getToId());
						f.setAttribute("diff", 	warning.getDifference());
						f.setAttribute("exp", 	warning.getExpected());
						f.setAttribute("act", 	warning.getActual());
						features.add(f);
						uniqueFeatures.put(warning.getLinkIds(), f);
					}
				}
			}
		}
*/
		// one feature for every warningsSchedule
		for(Map<TransitRoute, TreeSet<PlausibilityWarning>> e : warningsSchedule.values()) {
			for(TreeSet<PlausibilityWarning> e2 : e.values()) {
				for(PlausibilityWarning logMessage : e2) {
					SimpleFeature f = ff.createPolyline(logMessage.getCoordinates());
					f.setAttribute("type", 	logMessage.getType());
					f.setAttribute("line", 	logMessage.getTransitLine().getId());
					f.setAttribute("route",	logMessage.getTransitRoute().getId());
					f.setAttribute("from", 	logMessage.getFromId());
					f.setAttribute("to", 	logMessage.getToId());
					f.setAttribute("diff", 	logMessage.getDifference());
					f.setAttribute("exp", 	logMessage.getExpected());
					f.setAttribute("act", 	logMessage.getActual());
					features.add(f);
				}
			}
		}

		ShapeFileWriter.writeGeometries(features, outFile);
//		ShapeFileWriter.writeGeometries(uniqueFeatures.values(), outFile);
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

	private double getAzimuthDiff(Link link1, Link link2) {
		Coord c1 = link1.getFromNode().getCoord();
		Coord c2 = link2.getToNode().getCoord();

		if(c1.equals(c2)) {
			return PI;
		}

		double az1 = getAzimuth(c1, link1.getToNode().getCoord());
		double az2 = getAzimuth(link2.getFromNode().getCoord(), c2);
		double diff = Math.abs(az2-az1);

		if(diff > PI) {
			diff = PI2-diff;
		}

		return (diff > PI2 ? diff-PI2 : diff);
	}

	private void p(Object s) {
		System.out.println(s);
	}

	public static <K,V> TreeSet<V> getTreeSet(
			final K key,
			final Map<K, TreeSet<V>> map) {
		TreeSet<V> coll = map.get( key );

		if ( coll == null ) {
			coll = new TreeSet<>();
			map.put( key , coll );
		}

		return coll;
	}
}