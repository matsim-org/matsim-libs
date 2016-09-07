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

package contrib.publicTransitMapping.tools;

import com.vividsolutions.jts.geom.Coordinate;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.pt.transitSchedule.api.*;
import org.opengis.feature.simple.SimpleFeature;

import java.util.*;

/**
 * Converts a MATSim Transit Schedule to a ESRI shape file
 *
 * @author polettif
 */
public class ScheduleShapeFileWriter {

	private static final Logger log = Logger.getLogger(ScheduleShapeFileWriter.class);

	private final TransitSchedule schedule;
	private final Network network;
	private final String crs;

	private Map<TransitStopFacility, Set<Id<TransitRoute>>> routesOnStopFacility = new HashMap<>();

	public ScheduleShapeFileWriter(final TransitSchedule schedule, final Network network, String crs) {
		this.schedule = schedule;
		this.network = network;
		this.crs = crs;
	}

	/**
	 * Converts the given schedule based on the given network
	 * to GIS shape files.
	 *
	 * @param args	[0] input schedule
	 *              [1] input network
	 *              [2] coordinate reference system (EPSG=*)
	 *              [3] output folder
	 */
	public static void main(final String[] args) {
		if(args.length == 3) {
			run(args[0], args[1], args[2], args[3]);
		}
	}

	/**
	 * Converts the given schedule based on the given network
	 * to GIS shape files.
	 *
	 * @param scheduleFile	input schedule
	 * @param networkFile   input network
	 * @param outputFolder  output folder
	 */
	public static void run(String scheduleFile, String networkFile, String crs, String outputFolder) {
		TransitSchedule schedule = ScheduleTools.readTransitSchedule(scheduleFile);
		Network network = NetworkTools.readNetwork(networkFile);

		ScheduleShapeFileWriter s2s = new ScheduleShapeFileWriter(schedule, network, crs);

		s2s.routes2Polylines(outputFolder+"transitRoutes.shp");
		s2s.stopFacilities2Shapes(outputFolder+"stopFacilities.shp", "refLinks.shp");
	}

	public static void run(TransitSchedule schedule, Network network, String crs, String outputFolder) {
		ScheduleShapeFileWriter s2s = new ScheduleShapeFileWriter(schedule, network, crs);

		s2s.routes2Polylines(outputFolder+"transitRoutes.shp");
		s2s.stopFacilities2Shapes(outputFolder+"stopFacilities.shp", outputFolder+"refLinks.shp");
	}

		public void stopFacilities2Shapes(String pointOutputFile, String lineOutputFile) {
		Collection<SimpleFeature> lineFeatures = new ArrayList<>();
		Collection<SimpleFeature> pointFeatures = new ArrayList<>();

		PointFeatureFactory pointFeatureFactory = new PointFeatureFactory.Builder()
				.setName("StopFacilities")
				.setCrs(MGC.getCRS(crs))
				.addAttribute("id", String.class)
				.addAttribute("name", String.class)
				.addAttribute("linkId", String.class)
				.addAttribute("postAreaId", String.class)
				.addAttribute("isBlocking", Boolean.class)
				.addAttribute("routes", String.class)
				.create();

		PolylineFeatureFactory polylineFeatureFactory = new PolylineFeatureFactory.Builder()
				.setName("StopFacilities")
				.setCrs(MGC.getCRS(crs))
				.addAttribute("id", String.class)
				.addAttribute("name", String.class)
				.addAttribute("linkId", String.class)
				.addAttribute("postAreaId", String.class)
				.addAttribute("isBlocking", Boolean.class)
				.addAttribute("routes", String.class)
				.create();

		for(TransitStopFacility stopFacility : schedule.getFacilities().values()) {
			Link refLink = network.getLinks().get(stopFacility.getLinkId());

			Coordinate[] coordinates = new Coordinate[2];
			try {
				coordinates[0] = MGC.coord2Coordinate(refLink.getFromNode().getCoord());
			} catch (Exception e) {
				e.printStackTrace();
			}
			coordinates[1] = MGC.coord2Coordinate(refLink.getToNode().getCoord());

			SimpleFeature pf = pointFeatureFactory.createPoint(MGC.coord2Coordinate(stopFacility.getCoord()));
			pf.setAttribute("id", stopFacility.getId().toString());
			pf.setAttribute("name", stopFacility.getName());
			pf.setAttribute("linkId", stopFacility.getLinkId().toString());
			pf.setAttribute("postAreaId", stopFacility.getStopPostAreaId());
			pf.setAttribute("isBlocking", stopFacility.getIsBlockingLane());
			if(routesOnStopFacility.get(stopFacility) != null) pf.setAttribute("routes", CollectionUtils.idSetToString(routesOnStopFacility.get(stopFacility)));
			pointFeatures.add(pf);

			SimpleFeature lf = polylineFeatureFactory.createPolyline(coordinates);
			lf.setAttribute("id", stopFacility.getId().toString());
			lf.setAttribute("name", stopFacility.getName());
			lf.setAttribute("linkId", stopFacility.getLinkId().toString());
			lf.setAttribute("postAreaId", stopFacility.getStopPostAreaId());
			lf.setAttribute("isBlocking", stopFacility.getIsBlockingLane());
			if(routesOnStopFacility.get(stopFacility) != null) pf.setAttribute("routes", CollectionUtils.idSetToString(routesOnStopFacility.get(stopFacility)));
			lineFeatures.add(lf);
		}

		ShapeFileWriter.writeGeometries(pointFeatures, pointOutputFile);
		ShapeFileWriter.writeGeometries(lineFeatures, lineOutputFile);
	}


	public void routes2Polylines(String outFile) {
		Collection<SimpleFeature> features = new ArrayList<>();

		PolylineFeatureFactory ff = new PolylineFeatureFactory.Builder()
				.setName("TransitRoutes")
				.setCrs(MGC.getCRS(crs))
				.addAttribute("line", String.class)
				.addAttribute("route", String.class)
				.addAttribute("mode", String.class)
				.addAttribute("simLength", Double.class)
				.create();

		for(TransitLine transitLine : schedule.getTransitLines().values()) {
			for(TransitRoute transitRoute : transitLine.getRoutes().values()) {

				for(TransitRouteStop stop : transitRoute.getStops()) {
					MapUtils.getSet(stop.getStopFacility(), routesOnStopFacility).add(transitRoute.getId());
				}

				Coordinate[] coordinates = getCoordinatesFromRoute(transitRoute);

				if(coordinates == null) {
					log.warn("No links found for route " + transitRoute.getId() + " on line " + transitLine.getId());
				} else {
					SimpleFeature f = ff.createPolyline(coordinates);
					f.setAttribute("line", transitLine.getId().toString());
					f.setAttribute("route", transitRoute.getId().toString());
					f.setAttribute("mode", transitRoute.getTransportMode());
					f.setAttribute("simLength", getRouteLength(transitRoute));
					features.add(f);
				}
			}
		}

		ShapeFileWriter.writeGeometries(features, outFile);
	}

	private double getRouteLength(TransitRoute transitRoute) {
		double length = 0;
		for(Link l : NetworkTools.getLinksFromIds(network, ScheduleTools.getTransitRouteLinkIds(transitRoute))) {
			length += l.getLength();
		}
		return length;
	}

	private Coordinate[] getCoordinatesFromRoute(TransitRoute transitRoute) {
		List<Coordinate> coordList = new ArrayList<>();
		List<Id<Link>> linkIds = ScheduleTools.getTransitRouteLinkIds(transitRoute);

		if(transitRoute.getId().toString().equals("80.T0.31-650-P-j16-1.6.H")) {
			log.debug("break");
		}

		if(linkIds.size() > 0) {
			for(Id<Link> linkId : linkIds) {
				if(network.getLinks().containsKey(linkId)) {
					coordList.add(MGC.coord2Coordinate(network.getLinks().get(linkId).getFromNode().getCoord()));
				} else {
					throw new IllegalArgumentException("Link " + linkId + " not found in network");
				}
			}
			coordList.add(MGC.coord2Coordinate(network.getLinks().get(linkIds.get(linkIds.size() - 1)).getToNode().getCoord()));
			Coordinate[] coordinates = new Coordinate[coordList.size()];
			return coordList.toArray(coordinates);
		}
		return null;
	}

}