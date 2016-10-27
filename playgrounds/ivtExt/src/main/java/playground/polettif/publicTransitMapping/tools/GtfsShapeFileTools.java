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

package playground.polettif.publicTransitMapping.tools;

import com.vividsolutions.jts.geom.Coordinate;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;
import playground.polettif.publicTransitMapping.gtfs.GtfsConverter;
import playground.polettif.publicTransitMapping.gtfs.lib.GTFSRoute;
import playground.polettif.publicTransitMapping.gtfs.lib.Shape;
import playground.polettif.publicTransitMapping.gtfs.lib.Trip;

import java.util.*;

/**
 * Provides tools to convert GTFS data
 * to ESRI shapefiles. Is called within {@link GtfsConverter}
 *
 * @author polettif
 */
@Deprecated
public class GtfsShapeFileTools {

	/**
	 * Converts a list of link ids to an array of coordinates for shp features
	 */
	public static Coordinate[] linkIdList2Coordinates(Network network, List<Id<Link>> linkIdList) {
		List<Coordinate> coordList = new ArrayList<>();
		for(Id<Link> linkId : linkIdList) {
			if(network.getLinks().containsKey(linkId)) {
				coordList.add(MGC.coord2Coordinate(network.getLinks().get(linkId).getFromNode().getCoord()));
			} else {
				throw new IllegalArgumentException("Link " + linkId + " not found in network");
			}
		}
		coordList.add(MGC.coord2Coordinate(network.getLinks().get(linkIdList.get(linkIdList.size() - 1)).getToNode().getCoord()));
		Coordinate[] coordinates = new Coordinate[coordList.size()];
		return coordList.toArray(coordinates);
	}

	public static void writeGtfsTripsToFile(Map<String, GTFSRoute> gtfsRoutes, Set<String> serviceIds, String outputCoordinateSystem, String outFile) {
		Collection<SimpleFeature> features = new ArrayList<>();

		PolylineFeatureFactory ff = new PolylineFeatureFactory.Builder()
				.setName("gtfs_shapes")
				.setCrs(MGC.getCRS(outputCoordinateSystem))
				.addAttribute("shape_id", String.class)
				.addAttribute("trip_id", String.class)
				.addAttribute("trip_name", String.class)
				.addAttribute("route_id", String.class)
				.addAttribute("route_name", String.class)
				.create();


		for(GTFSRoute gtfsRoute : gtfsRoutes.values()) {
			for(Trip trip : gtfsRoute.getTrips().values()) {
				boolean useTrip = false;
				if(serviceIds != null) {
					for(String serviceId : serviceIds) {
						if(trip.getServiceId().equals(serviceId)) {
							useTrip = true;
							break;
						}
					}
				} else {
					useTrip = true;
				}

				if(useTrip) {
					Shape shape = trip.getShape();
					if(shape != null) {
						SimpleFeature f = ff.createPolyline(shape.getCoordinates());
						f.setAttribute("shape_id", shape.getId());
						f.setAttribute("trip_id", trip.getId());
						f.setAttribute("trip_name", trip.getName());
						f.setAttribute("route_id", gtfsRoute.getRouteId());
						f.setAttribute("route_name", gtfsRoute.getShortName());
						features.add(f);
					}
				}
			}
		}
		ShapeFileWriter.writeGeometries(features, outFile);
	}
}
