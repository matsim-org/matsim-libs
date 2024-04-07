/* *********************************************************************** *
 * project: org.matsim.*
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
 * *********************************************************************** */

/**
 *
 */
package org.matsim.contrib.drt.analysis.zonal;

import one.util.streamex.EntryStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.misc.Counter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author jbischoff
 */
public class DrtGridUtils {

	static final Logger log = LogManager.getLogger(DrtGridUtils.class);

	public static Map<String, PreparedGeometry> createGridFromNetwork(Network network, double cellsize) {
		log.info("start creating grid from network");
		double[] boundingbox = NetworkUtils.getBoundingBox(network.getNodes().values());
		double minX = (Math.floor(boundingbox[0] / cellsize) * cellsize);
		double maxX = (Math.ceil(boundingbox[2] / cellsize) * cellsize);
		double minY = (Math.floor(boundingbox[1] / cellsize) * cellsize);
		double maxY = (Math.ceil(boundingbox[3] / cellsize) * cellsize);
		GeometryFactory gf = new GeometryFactory();
		PreparedGeometryFactory preparedGeometryFactory = new PreparedGeometryFactory();
		Map<String, PreparedGeometry> grid = new HashMap<>();
		int cell = 0;
		for (double lx = minX; lx < maxX; lx += cellsize) {
			for (double by = minY; by < maxY; by += cellsize) {
				cell++;
				Coordinate p1 = new Coordinate(lx, by);
				Coordinate p2 = new Coordinate(lx + cellsize, by);
				Coordinate p3 = new Coordinate(lx + cellsize, by + cellsize);
				Coordinate p4 = new Coordinate(lx, by + cellsize);
				Coordinate[] ca = { p1, p2, p3, p4, p1 };
				Polygon polygon = new Polygon(gf.createLinearRing(ca), null, gf);
				grid.put(cell + "", preparedGeometryFactory.create(polygon));
			}
		}
		log.info("finished creating grid from network");
		return grid;
	}

	/**
	 * Takes an existing grid and removes all zones that do not intersect the service area.
	 * Result may contain zones that are barely included in the service area. But as passengers may walk into the service area,
	 * it seems appropriate that the DrtZonalSystem, which is used for demand estimation, is larger than the service area.
	 * The {@code cellsize} indirectly determines, how much larger the DrtZonalSystem may get.
	 *
	 * @param grid a pre-computed grid of zones
	 * @param serviceAreaGeoms geometries that define the service area
	 * @return
	 */
	public static Map<String, PreparedGeometry> filterGridWithinServiceArea(Map<String, PreparedGeometry> grid, List<PreparedGeometry> serviceAreaGeoms) {
		log.info("total number of initial grid zones = " + grid.size());
		log.info("searching for grid zones within the drt service area...");
		Counter counter = new Counter("dealt with zone ");
		Map<String, PreparedGeometry> zonesWithinServiceArea = EntryStream.of(grid)
				.peekKeys(id -> counter.incCounter())
				.filterValues(cell -> serviceAreaGeoms.stream()
						.anyMatch(serviceArea -> serviceArea.intersects(cell.getGeometry())))
				.toMap();

		log.info("number of remaining grid zones = " + zonesWithinServiceArea.size());
		return zonesWithinServiceArea;
	}
}
