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

package playground.polettif.publicTransitMapping.tools.shp;

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
import playground.polettif.publicTransitMapping.tools.NetworkTools;
import playground.polettif.publicTransitMapping.tools.ScheduleTools;

import java.util.*;

/**
 * Converts a MATSim Transit Schedule to a GIS shape file
 *
 * @author polettif
 */
public class Schedule2ShapeFileWriter {

	private static final Logger log = Logger.getLogger(Schedule2ShapeFileWriter.class);

	private final TransitSchedule schedule;
	private final Network network;

	private Map<TransitStopFacility, Set<Id<TransitRoute>>> routesOnStopFacility = new HashMap<>();

	public Schedule2ShapeFileWriter(final TransitSchedule schedule, final Network network) {
		this.schedule = schedule;
		this.network = network;
	}

	public static void main(final String[] arg) {
		String[] args = new String[5];
		args[0] = "E:/output/PublicTransitMapping/uri_schedule.xml";
		args[1] = "E:/output/PublicTransitMapping/uri_network.xml";
		args[2] = "E:/output/shp/transitLines.shp";
		args[3] = "E:/output/shp/stopFacilities.shp";
		args[4] = "E:/output/shp/refLinks.shp";
		TransitSchedule schedule = ScheduleTools.loadTransitSchedule(args[0]);
		Network network = NetworkTools.loadNetwork(args[1]);

		Schedule2ShapeFileWriter s2s = new Schedule2ShapeFileWriter(schedule, network);

		s2s.routes2Polylines(args[2]);
		s2s.stopFacilities2Shapes(args[3], args[4]);
	}

	public void stopFacilities2Shapes(String pointOutputFile, String lineOutputFile) {
		Collection<SimpleFeature> lineFeatures = new ArrayList<>();
		Collection<SimpleFeature> pointFeatures = new ArrayList<>();

		PointFeatureFactory pointFeatureFactory = new PointFeatureFactory.Builder()
				.setName("StopFacilities")
				.setCrs(MGC.getCRS("EPSG:2056"))
				.addAttribute("id", String.class)
				.addAttribute("name", String.class)
				.addAttribute("linkId", String.class)
				.addAttribute("postAreaId", String.class)
				.addAttribute("isBlocking", Boolean.class)
				.addAttribute("routes", String.class)
				.create();

		PolylineFeatureFactory polylineFeatureFactory = new PolylineFeatureFactory.Builder()
				.setName("StopFacilities")
				.setCrs(MGC.getCRS("EPSG:2056"))
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
			coordinates[0] = MGC.coord2Coordinate(refLink.getFromNode().getCoord());
			coordinates[1] = MGC.coord2Coordinate(refLink.getToNode().getCoord());

			SimpleFeature pf = pointFeatureFactory.createPoint(MGC.coord2Coordinate(stopFacility.getCoord()));
			pf.setAttribute("id", stopFacility.getId().toString());
			pf.setAttribute("name", stopFacility.getName());
			pf.setAttribute("linkId", stopFacility.getLinkId().toString());
			pf.setAttribute("postAreaId", stopFacility.getStopPostAreaId());
			pf.setAttribute("isBlocking", stopFacility.getIsBlockingLane());
			pf.setAttribute("routes", CollectionUtils.idSetToString(routesOnStopFacility.get(stopFacility)));
			pointFeatures.add(pf);

			SimpleFeature lf = polylineFeatureFactory.createPolyline(coordinates);
			lf.setAttribute("id", stopFacility.getId().toString());
			lf.setAttribute("name", stopFacility.getName());
			lf.setAttribute("linkId", stopFacility.getLinkId().toString());
			lf.setAttribute("postAreaId", stopFacility.getStopPostAreaId());
			lf.setAttribute("isBlocking", stopFacility.getIsBlockingLane());
			pf.setAttribute("routes", CollectionUtils.idSetToString(routesOnStopFacility.get(stopFacility)));
			lineFeatures.add(lf);
		}

		ShapeFileWriter.writeGeometries(pointFeatures, pointOutputFile);
		ShapeFileWriter.writeGeometries(lineFeatures, lineOutputFile);
	}


	public void routes2Polylines(String outFile) {
		Collection<SimpleFeature> features = new ArrayList<>();

		PolylineFeatureFactory ff = new PolylineFeatureFactory.Builder()
				.setName("TransitRoutes")
				.setCrs(MGC.getCRS("EPSG:2056"))
				.addAttribute("line", String.class)
				.addAttribute("route", String.class)
				.addAttribute("mode", String.class)
				.create();

		for(TransitLine transitLine : schedule.getTransitLines().values()) {
			for(TransitRoute transitRoute : transitLine.getRoutes().values()) {

				for(TransitRouteStop stop : transitRoute.getStops()) {
					MapUtils.getSet(stop.getStopFacility(), routesOnStopFacility).add(transitRoute.getId());
				}

				Coordinate[] coordinates = getCoordinatesFromRoute(transitRoute);

				if(coordinates == null) {
					log.error("No links found for route " + transitRoute.getId() + " on line " + transitLine.getId());
				} else {
					SimpleFeature f = ff.createPolyline(coordinates);
					f.setAttribute("line", transitLine.getId().toString());
					f.setAttribute("route", transitRoute.getId().toString());
					f.setAttribute("mode", transitRoute.getTransportMode());
					features.add(f);
				}
			}
		}

		ShapeFileWriter.writeGeometries(features, outFile);
	}

	private Coordinate[] getCoordinatesFromRoute(TransitRoute transitRoute) {
		List<Coordinate> coordList = new ArrayList<>();
		List<Id<Link>> linkList = ScheduleTools.getLinkIds(transitRoute);

		for(Id<Link> linkId : linkList) {
			if(network.getLinks().containsKey(linkId)) {
				coordList.add(MGC.coord2Coordinate(network.getLinks().get(linkId).getFromNode().getCoord()));
			} else {
				log.warn("Link " + linkId + " not found in network");
				return null;
			}
		}
		coordList.add(MGC.coord2Coordinate(network.getLinks().get(linkList.get(linkList.size()-1)).getToNode().getCoord()));
		Coordinate[] returnArray = new Coordinate[coordList.size()];

		return coordList.toArray(returnArray);
	}
}