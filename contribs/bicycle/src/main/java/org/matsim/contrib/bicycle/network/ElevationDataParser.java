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

import org.geotools.api.referencing.operation.TransformException;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.InvalidGridGeometryException;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.Position2D;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

/**
 * @author smetzler, dziemke
 */
public class ElevationDataParser {

	private final GridCoverage2D grid;
	private final Raster gridData;
	private final CoordinateTransformation ct;


	// TODO: convert this to a JUnit test once a small test DEM (or a stable reference TIFF
	// path via system property) is available. The Berlin sample points below have known
	// approximate elevations from Sonny's DTM Germany 50m v3b and could serve as fixtures.
	public static void main(String[] args) {
		// Data sources:
		// https://sonny.4lima.de/
		// for the future see also https://github.com/graphhopper/graphhopper/pull/3287
		// deprecated:
		// SRTM1:  http://earthexplorer.usgs.gov/ (login in required)
		// SRTM3:  http://srtm.csi.cgiar.org/SELECTION/inputCoord.asp
		// EU-DEM: http://data.eox.at/eudem


		// get GEOtiffs from Sonny https://sonny.4lima.de/ (CC BY 4.0)
		// -> choose either
		// DTM 20m  ~1.1 GB Germany "EPSG:32632"
		// DTM 50m  ~0.3 GB Germany "EPSG:32632"
		String tiffFile = "C:/Users/metz_so/Workspace/data/elevation/DTM Germany 50m v3b by Sonny.tif";


		String scenarioCRS = "EPSG:4326"; // WGS84 as the coorinates to test below are stated like this
		String demCRS = "EPSG:32632";     // UTM 32N - native CRS of Sonny's German DTM

		ElevationDataParser elevationDataParser = new ElevationDataParser(tiffFile, scenarioCRS, demCRS);

		// Reference values from Sonny DTM 50m v3b (run on 2026-05-08), in meters above sea level.
		System.out.println("Teufelsberg:            " + elevationDataParser.getElevation(13.2407, 52.4971));    // ~112.4
		System.out.println("Tempelhofer Feld:       " + elevationDataParser.getElevation(13.3989, 52.4755));    //  ~45.7
		System.out.println("Müggelsee:              " + elevationDataParser.getElevation(13.6354, 52.4334));    //  ~32.3
		System.out.println("Müggelberg:             " + elevationDataParser.getElevation(13.64048, 52.41594));   //  ~94.5
		System.out.println("Alexanderplatz:         " + elevationDataParser.getElevation(13.40993, 52.52191));   //  ~36.6
		System.out.println("Kreuzberg (Berg):       " + elevationDataParser.getElevation(13.379491, 52.487610));  //  ~57.8
		System.out.println("Herrmannplatz:          " + elevationDataParser.getElevation(13.422301, 52.486477));  //  ~36.8
		System.out.println("U-Bahnhof Boddinstraße: " + elevationDataParser.getElevation(13.423210, 52.480278));  //  ~52.3
	}


	public ElevationDataParser(String tiffFile, String scenarioCRS, String demCRS) {
		this.ct = TransformationFactory.getCoordinateTransformation(scenarioCRS, demCRS);
		try {
			GeoTiffReader reader = new GeoTiffReader(tiffFile);
			this.grid = reader.read(null);
			this.gridData = grid.getRenderedImage().getData();
		} catch (IOException e) {
			// DataSourceException extends IOException, ein catch reicht
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
