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
 * @author smetzler, dziemke
 */
public class ElevationDataParser {

	private static GridCoverage2D grid;
	private static Raster gridData;
	private CoordinateTransformation ct;
	
	public static void main(String[] args) {
		// Data sources:
		// SRTM1:  http://earthexplorer.usgs.gov/ (login in required)
		// SRTM3:  http://srtm.csi.cgiar.org/SELECTION/inputCoord.asp
		// EU-DEM: http://data.eox.at/eudem
		
		String tiffFile = "../../../shared-svn/studies/countries/de/berlin-bike/networkRawData/elevation_berlin/BerlinEUDEM.tif"; // Berlin EU-DEM
		// String tiffFile = "../../../shared-svn/studies/countries/de/berlin-bike/networkRawData/elevation_berlin/srtm3/srtm_39_02.tif"; // Berlin SRTM3
		// String tiffFile = "../../../shared-svn/studies/countries/de/berlin-bike/networkRawData/elevation_stuttgart/stuttgartEUDEM.tif"; // Stuttgart EU-DEM
		// String tiffFile = "../../../shared-svn/studies/countries/de/berlin-bike/networkRawData/elevation_stuttgart/srtm_38_03.tif"; // Stuttgart SRTM3		
		// String tiffFile = "../../../shared-svn/studies/countries/de/berlin-bike/networkRawData/elevation_brasilia/srtm_27_16.tif"; // Brasilia SRTM3
		
		String scenarioCRS = "EPSG:4326"; // WGS84 as the coorinates to test below are stated like this
		
		ElevationDataParser elevationDataParser = new ElevationDataParser(tiffFile, scenarioCRS);
		
		System.out.println("Teufelsberg: " + elevationDataParser.getElevation(13.2407, 52.4971));
		System.out.println("Tempelhofer Feld: " + elevationDataParser.getElevation(13.3989, 52.4755));
		System.out.println("Müggelsee: " + elevationDataParser.getElevation(13.6354, 52.4334));
		System.out.println("Müggelberg: " + elevationDataParser.getElevation(13.64048, 52.41594));
		System.out.println("Alexanderplatz: " + elevationDataParser.getElevation(13.40993, 52.52191));
		System.out.println("Kreuzberg (Berg): " + elevationDataParser.getElevation(13.379491, 52.487610));
		System.out.println("Herrmannplatz: " + elevationDataParser.getElevation(13.422301,52.486477));
		System.out.println("U-Bahnhof Boddinstraße: " + elevationDataParser.getElevation(13.423210,52.480278));
	}
	
	
	public ElevationDataParser(String tiffFile, String scenarioCRS) {
		this.ct = TransformationFactory.getCoordinateTransformation(scenarioCRS, "EPSG:4326");
		
		GeoTiffReader reader = null;
		try {
			reader = new GeoTiffReader(tiffFile);
		} catch (DataSourceException e) {
			e.printStackTrace();
		}

		try {
			grid = reader.read(null);
		} catch (IOException e) {
			e.printStackTrace();
		}
		gridData = grid.getRenderedImage().getData();
	}
	
	
	public double getElevation(double x, double y) {
		return getElevation(CoordUtils.createCoord(x, y));
	}
	
	
	public double getElevation(Coord coord) {
		GridGeometry2D gg = grid.getGridGeometry();
		
		Coord transformedCoord = ct.transform(coord);
		
		GridCoordinates2D posGrid = null;
		try {
			posGrid = gg.worldToGrid(new Position2D(transformedCoord.getX(), transformedCoord.getY()));
		} catch (InvalidGridGeometryException e) {
			e.printStackTrace();
		} catch (TransformException e) {
			e.printStackTrace();
		}

		double[] pixel = new double[1];
		double[] data = gridData.getPixel(posGrid.x, posGrid.y, pixel);
		return data[0];
	}
}