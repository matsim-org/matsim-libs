/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityLocationUtilOffset2QGIS.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.yu.integration.cadyts.demandCalibration.withCarCounts.utils.qgis;

import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.yu.integration.cadyts.demandCalibration.withCarCounts.experiments.actLocUtilOffset.AreaUtilityOffsets;
import playground.yu.integration.cadyts.demandCalibration.withCarCounts.experiments.actLocUtilOffset.Grid2Graph4Distribution;
import playground.yu.utils.qgis.X2QGIS;

public class ActivityLocationUtilOffset2QGIS4Distribution implements X2QGIS {

	private CoordinateReferenceSystem crs = null;
	private Grid2Graph4Distribution g2g = null;

	public ActivityLocationUtilOffset2QGIS4Distribution(Scenario scenario,
			String coordRefSys, double gridSideLength_m,
			Map<Coord, AreaUtilityOffsets> gridUtilOffsetMap) {
		crs = MGC.getCRS(coordRefSys);
		g2g = new Grid2Graph4Distribution(crs, 1000d/* [m] */,
				gridUtilOffsetMap);
	}

	/**
	 * @param ShapeFilename
	 *            where the shapefile will be saved
	 */
	public void writeShapeFile(final String ShapeFilename) {
		ShapeFileWriter.writeGeometries(g2g.getFeatures(), ShapeFilename);
	}

	/**
	 * transfers additional parameters to Graph
	 * 
	 * @param paraName
	 * @param clazz
	 * @param parameters
	 */
	public void addParameter(final String paraName, final Class<?> clazz,
			final Map<Id<Link>, ?> parameters) {
		g2g.addParameter(paraName, clazz, parameters);
	}
}
