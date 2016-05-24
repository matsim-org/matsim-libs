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

import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;
import playground.polettif.publicTransitMapping.gtfs.lib.GTFSRoute;
import playground.polettif.publicTransitMapping.gtfs.lib.Shape;
import playground.polettif.publicTransitMapping.gtfs.lib.Trip;

import java.util.*;

/**
 * Provides tools to convert MATSim or gtfs data
 * to shapefiles.
 *
 * @author polettif
 */
public class ShapeFileTools {

	public static void writeGtfsTripsToFile(Map<String, GTFSRoute> gtfsRoutes, Set<String> serviceIds, String outputCoordinateSystem, String outFile) {
		Collection<SimpleFeature> features = new ArrayList<>();

		PolylineFeatureFactory ff = new PolylineFeatureFactory.Builder()
				.setName("gtfs_shapes")
				.setCrs(MGC.getCRS(outputCoordinateSystem))
				.addAttribute("id", String.class)
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
						f.setAttribute("id", shape.getId());
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
