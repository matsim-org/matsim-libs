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
import java.io.File;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.DirectPosition2D;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.geotools.referencing.CRS;

public class ElevationDataParser {

	private static GridCoverage2D grid;
	private static  Raster gridData;
		
	public double parseGeoTiff(double xCoord, double yCoord, boolean firsttime) throws Exception {
		if (firsttime) {
		initTif();}		
		return getValue(xCoord, yCoord);
	}

	private void initTif() throws Exception {
		
		//Where to download elevation Raw Data?
		// SRTM1:  http://earthexplorer.usgs.gov/ (login in required)
		// SRTM3:  http://srtm.csi.cgiar.org/SELECTION/inputCoord.asp
		// EU-DEM: http://data.eox.at/eudem
		
		
		//berlin SRTM3
//		File tiffFile = new File(
//				"../../../shared-svn/studies/countries/de/berlin-bike/networkRawData/elevation_berlin/srtm3/srtm_39_02.tif");
		
	    //berlin EU-DEM
		File tiffFile = new File(
				"../../../shared-svn/studies/countries/de/berlin-bike/networkRawData/elevation_berlin/BerlinEUDEM.tif");
		
//		//stuttgart EUDEM
//		File tiffFile = new File(
//				"../../../shared-svn/studies/countries/de/berlin-bike/networkRawData/elevation_stuttgart/stuttgartEUDEM.tif");		
//		
//		//stuttgart SRTM3
//		File tiffFile = new File(
//				"../../../shared-svn/studies/countries/de/berlin-bike/networkRawData/elevation_stuttgart/srtm_38_03.tif");
//		
//		//brasilia SRTM3
//		File tiffFile = new File(
//				"../../../shared-svn/studies/countries/de/berlin-bike/networkRawData/elevation_brasilia/srtm_27_16.tif");
		
		
		GeoTiffReader reader = new GeoTiffReader(tiffFile);

		grid = reader.read(null);
		gridData = grid.getRenderedImage().getData();

	}

	private double getValue(double x, double y) throws Exception {

		GridGeometry2D gg = grid.getGridGeometry();

		
		//convert the the projection used in the MATSim Berlin scenario (DHDN / 3-degree Gauss-Kruger zone 4) to one used in the elevation data (Geotiff, WGS84) 
		//new
		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:31468", true); // DHDN / 3-degree Gauss-Kruger zone 4
		CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:4326", true);  //WGS84

		MathTransform mathTransform = CRS.findMathTransform(sourceCRS, targetCRS, true);

		DirectPosition transformedCoords = mathTransform.transform(new DirectPosition2D(x, y), null);


		DirectPosition2D posWorld = new DirectPosition2D(transformedCoords); 
		GridCoordinates2D posGrid = gg.worldToGrid(posWorld);

		// envelope is the size in the target projection
		double[] pixel = new double[1];
		double[] data = gridData.getPixel(posGrid.x, posGrid.y, pixel);
		return data[0];
	}
}