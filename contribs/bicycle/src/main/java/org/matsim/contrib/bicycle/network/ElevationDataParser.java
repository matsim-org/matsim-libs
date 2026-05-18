/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.contrib.bicycle.network;

import java.awt.image.Raster;
import java.io.IOException;

import org.geotools.api.data.DataSourceException;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.InvalidGridGeometryException;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.Position2D;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

/**
 * Reads a DEM GeoTIFF via GeoTools and samples elevation at arbitrary world
 * coordinates, transforming them from the scenario CRS into the DEM CRS as
 * needed. Sampling is nearest-neighbor.
 *
 * <p>DEM sources we know about:
 * <ul>
 *   <li>Sonny's DTMs — https://sonny.4lima.de/ (CC BY 4.0). LiDAR-based,
 *       much better quality than SRTM. Germany available as 20 m (~1.1 GB) or
 *       50 m (~300 MB), both in {@code EPSG:32632}.</li>
 *   <li>For future reference, see also GraphHopper PR #3287
 *       (https://github.com/graphhopper/graphhopper/pull/3287) which switched
 *       to Sonny.</li>
 * </ul>
 *
 * <p>Deprecated alternatives that we have used historically:
 * <ul>
 *   <li>SRTM1 — http://earthexplorer.usgs.gov/ (login required)</li>
 *   <li>SRTM3 — http://srtm.csi.cgiar.org/SELECTION/inputCoord.asp</li>
 *   <li>EU-DEM — http://data.eox.at/eudem</li>
 * </ul>
 *
 * @author smetzler, dziemke
 */
public class ElevationDataParser {

	private final GridCoverage2D grid;
	private final Raster gridData;
	private final CoordinateTransformation ct;


	public ElevationDataParser(String tiffFile, String scenarioCRS, String demCRS) {
		this.ct = TransformationFactory.getCoordinateTransformation(scenarioCRS, demCRS);
		try {
			GeoTiffReader reader = new GeoTiffReader(tiffFile);
			this.grid = reader.read(null);
			this.gridData = grid.getRenderedImage().getData();
		} catch (IOException e) {
			// DataSourceException extends IOException, one catch is enough.
			throw new RuntimeException("Failed to read DEM from " + tiffFile, e);
		}
	}


	public double getElevation(double x, double y) {
		return getElevation(CoordUtils.createCoord(x, y));
	}


	public double getElevation(Coord coord) {
		Coord transformed = ct.transform(coord);
		try {
			GridCoordinates2D posGrid = grid.getGridGeometry()
				.worldToGrid(new Position2D(transformed.getX(), transformed.getY()));
			double[] pixel = new double[1];
			return gridData.getPixel(posGrid.x, posGrid.y, pixel)[0];
		} catch (TransformException | InvalidGridGeometryException e) {
			throw new RuntimeException("Failed to read elevation at " + coord, e);
		}
	}
}
