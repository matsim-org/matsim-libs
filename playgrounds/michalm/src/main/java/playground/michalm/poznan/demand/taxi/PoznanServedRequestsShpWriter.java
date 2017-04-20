/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.michalm.poznan.demand.taxi;

import java.util.*;

import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.*;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class PoznanServedRequestsShpWriter {
	private final Iterable<PoznanServedRequest> servedRequests;
	private final String coordinateSystem;

	public PoznanServedRequestsShpWriter(Iterable<PoznanServedRequest> servedRequests, String coordinateSystem) {
		this.servedRequests = servedRequests;
		this.coordinateSystem = coordinateSystem;
	}

	@SuppressWarnings("deprecation")
	public void write(String originsShpFile, String destinationsShpFile) {
		CoordinateReferenceSystem crs = MGC.getCRS(coordinateSystem);

		PointFeatureFactory pointFactory = new PointFeatureFactory.Builder().//
				addAttribute("ID", String.class).//
				addAttribute("month", Integer.class).//
				addAttribute("month_day", Integer.class).//
				addAttribute("week_day", Integer.class).//
				addAttribute("hour", Integer.class).//
				addAttribute("taxi_id", String.class).//
				setCrs(crs).setName("point").create();

		List<SimpleFeature> origins = new ArrayList<>();
		List<SimpleFeature> destinations = new ArrayList<>();
		for (PoznanServedRequest r : servedRequests) {
			String id = r.id + "";

			Object[] attrs = new Object[6];
			attrs[0] = id;
			attrs[1] = r.assigned.getMonth();
			attrs[2] = r.assigned.getDate();
			attrs[3] = r.assigned.getDay();
			attrs[4] = r.assigned.getHours();
			attrs[5] = r.taxiId + "";

			origins.add(pointFactory.createPoint(r.from, attrs, id));
			destinations.add(pointFactory.createPoint(r.to, attrs, id));
		}

		ShapeFileWriter.writeGeometries(origins, originsShpFile);
		ShapeFileWriter.writeGeometries(destinations, destinationsShpFile);
	}

	public static void main(String[] args) {
		Iterable<PoznanServedRequest> requests = PoznanServedRequests.readRequests(2);
		requests = PoznanServedRequests.filterRequestsWithinAgglomeration(requests);

		String shpPath = "d:/PP-rad/taxi/poznan-supply/zlecenia_obsluzone/GIS/";
		String originsShpFile = shpPath + "origins_2014_02.shp";
		String destinationsShpFile = shpPath + "destinations_2014_02.shp";

		new PoznanServedRequestsShpWriter(requests, TransformationFactory.WGS84_UTM33N).write(originsShpFile,
				destinationsShpFile);
	}
}
