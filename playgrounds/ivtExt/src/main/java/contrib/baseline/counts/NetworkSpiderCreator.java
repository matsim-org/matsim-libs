/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package contrib.baseline.counts;

import com.vividsolutions.jts.geom.Coordinate;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.vehicles.Vehicle;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.util.*;

/**
 * Creates network spiders for a given set of anchor links.
 *
 * @author boescpa
 */
public class NetworkSpiderCreator implements LinkEnterEventHandler, VehicleAbortsEventHandler, VehicleLeavesTrafficEventHandler, VehicleEntersTrafficEventHandler {
	private final Set<String> anchorLinks;
	private final List<TripTrace> fullSpiderWeb = new LinkedList<>();
	private final Map<Id<Vehicle>, TripTrace> activeTraces = new HashMap<>();

	public NetworkSpiderCreator(Collection<String> anchorLinks) {
		this.anchorLinks = new HashSet<>(anchorLinks.size());
		this.anchorLinks.addAll(anchorLinks);
		this.reset(0);
	}

	public Map<String, Map<String, Double>> createAllRelativeSpiders() {
		Map<String, Map<String, Double>> allSpiders = new HashMap<>();
		for (String linkId : anchorLinks) {
			allSpiders.put(linkId, createRelativeSpider(linkId));
		}
		return allSpiders;
	}

	public Map<String, Double> createRelativeSpider(String anchorLink) {
		// Create relative spider:
		//		idea: link with highest count should be anchor link (because every trace considered here has to pass this link),
		//			ergo can highest count be used as base to calculate relSpider (<-> anchor link has 1 (that is 100%))
		// 			and every other link has also 100% or lower...
		Map<String, Integer> absSpider = createAbsoluteSpider(anchorLink);
		Map<String, Double> relSpider = new HashMap<>();
		double maxValue = (double)Collections.max(absSpider.values());
		for (String link : absSpider.keySet()) {
			relSpider.put(link, absSpider.get(link)/maxValue);
		}
		return relSpider;
	}

	public Map<String, Integer> createAbsoluteSpider(String anchorLink) {
		if (!anchorLinks.contains(anchorLink)) {
			throw new IllegalArgumentException("Provided link not in anchor links.");
		}
		// Create absolute spider:
		Map<String, Integer> absSpider = new HashMap<>();
		for (TripTrace trace : fullSpiderWeb) {
			if (trace.contains(anchorLink)) {
				for (String link : trace.getTraceLinks()) {
					if (absSpider.keySet().contains(link)) {
						absSpider.put(link, absSpider.get(link) + 1);
					} else {
						absSpider.put(link, 1);
					}
				}
			}
		}
		return absSpider;
	}

	@Override
	public void reset(int iteration) {
		fullSpiderWeb.clear();
		activeTraces.clear();
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Id<Vehicle> vehicleId = event.getVehicleId();
		assureActiveTrace(vehicleId);
		activeTraces.get(vehicleId).addLink(event.getLinkId());
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		Id<Vehicle> vehicleId = event.getVehicleId();
		assureActiveTrace(vehicleId);
		activeTraces.get(vehicleId).addLink(event.getLinkId());
	}

	private void assureActiveTrace(Id<Vehicle> vehicleId) {
		if (!activeTraces.keySet().contains(vehicleId)) {
			activeTraces.put(vehicleId, new TripTrace());
		}
	}

	@Override
	public void handleEvent(VehicleAbortsEvent event) {
		this.endVehicleTrace(event.getVehicleId());
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		this.endVehicleTrace(event.getVehicleId());
	}

	private void endVehicleTrace(Id<Vehicle> vehicleId) {
		if (activeTraces.keySet().contains(vehicleId) && activeTraces.get(vehicleId).lengthOfTrace > 0) {
			TripTrace trace = activeTraces.get(vehicleId);
			for (String linkId : anchorLinks) {
				if (trace.contains(linkId)) {
					fullSpiderWeb.add(trace);
					break;
				}
			}
		}
		activeTraces.remove(vehicleId);
	}

	private class TripTrace {
		private final String DELIMITER = ";";
		private String trace = "";
		private int lengthOfTrace = 0;

		void addLink(Id<Link> linkId) {
			trace += linkId.toString() + DELIMITER;
			lengthOfTrace++;
		}

		boolean contains(String link) {
			return trace.contains(link);
		}

		String[] getTraceLinks() {
			return trace.split(DELIMITER);
		}
	}

	public void createSpiderSHP(String anchorLink, Network network, String pathToOutputFolder, String coordSystem) {
		Map<String, Integer> absSpider = createAbsoluteSpider(anchorLink);
		Map<String, Double> relSpider = createRelativeSpider(anchorLink);

		// create shp-factory
		CoordinateReferenceSystem crs = MGC.getCRS(coordSystem);
		Collection<SimpleFeature> features = new ArrayList<>();
		PolylineFeatureFactory linkFactory = new PolylineFeatureFactory.Builder().
				setCrs(crs).
				setName("link").
				addAttribute("ID", String.class).
				addAttribute("fromID", String.class).
				addAttribute("toID", String.class).
				addAttribute("length", Double.class).
				addAttribute("capacity", Double.class).
				addAttribute("freespeed", Double.class).
				addAttribute("modes", String.class).
				addAttribute("relSpider", Double.class).
				addAttribute("absSpider", Double.class).
				create();

		// transform network
		for (Link link : network.getLinks().values()) {
			Coordinate fromNodeCoordinate = new Coordinate(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY());
			Coordinate toNodeCoordinate = new Coordinate(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY());
			Coordinate linkCoordinate = new Coordinate(link.getCoord().getX(), link.getCoord().getY());
			SimpleFeature ft = linkFactory.createPolyline(
					new Coordinate[]{fromNodeCoordinate, linkCoordinate, toNodeCoordinate},
					new Object[]{link.getId().toString(),
							link.getFromNode().getId().toString(),
							link.getToNode().getId().toString(),
							link.getLength(),
							link.getCapacity(),
							link.getFreespeed(),
							link.getAllowedModes().toString(),
							relSpider.get(link.getId().toString()),
							absSpider.get(link.getId().toString())},
					null);
			features.add(ft);
		}
		ShapeFileWriter.writeGeometries(features, pathToOutputFolder + "network_links.shp");
	}
}
