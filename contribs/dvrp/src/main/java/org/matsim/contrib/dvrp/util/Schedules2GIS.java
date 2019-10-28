/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.locationtech.jts.geom.Coordinate;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.path.VrpPath;
import org.matsim.contrib.dvrp.schedule.DriveTask;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;

public class Schedules2GIS {
	private final Iterable<? extends DvrpVehicle> vehicles;
	private final PolylineFeatureFactory factory;

	public Schedules2GIS(Iterable<? extends DvrpVehicle> vehicles, String coordSystem) {
		this.vehicles = vehicles;

		factory = new PolylineFeatureFactory.Builder().
				setCrs(MGC.getCRS(coordSystem)).
				setName("vrp_route").
				addAttribute("VEH_ID", Integer.class).
				addAttribute("VEH_NAME", String.class).
				addAttribute("ROUTE_ID", Integer.class).
				addAttribute("ARC_IDX", Integer.class).
				create();
	}

	public void write(String vrpOutDir) {
		new File(vrpOutDir).mkdir();
		String file = vrpOutDir + "\\route_";

		for (DvrpVehicle v : vehicles) {
			Stream<DriveTask> drives = Schedules.driveTasks(v.getSchedule());
			Collection<SimpleFeature> features = new ArrayList<>();

			drives.forEach(drive -> {
				Coordinate[] coords = createLineString(drive);
				if (coords != null) {
					features.add(this.factory.createPolyline(coords,
							new Object[] { v.getId(), v.getId(), drive.getTaskIdx() }, null));
				}
			});

			if (!features.isEmpty()) {
				ShapeFileWriter.writeGeometries(features, file + v.getId() + ".shp");
			}
		}
	}

	private Coordinate[] createLineString(DriveTask driveTask) {
		VrpPath path = driveTask.getPath();

		if (path.getLinkCount() == 1) {
			return null;
		}

		List<Coordinate> coordList = new ArrayList<>();

		for (Link l : path) {
			Coord c = l.getToNode().getCoord();
			coordList.add(new Coordinate(c.getX(), c.getY()));
		}

		return coordList.toArray(new Coordinate[coordList.size()]);
	}
}
