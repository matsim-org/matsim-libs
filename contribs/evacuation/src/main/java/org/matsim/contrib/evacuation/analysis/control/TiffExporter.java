/* *********************************************************************** *
 * project: org.matsim.*
 * RoadClosuresEditor.java
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

package org.matsim.contrib.evacuation.analysis.control;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;

import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.imageio.GeoToolsWriteParams;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffWriteParams;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;

public class TiffExporter {

	public static boolean writeGEOTiff(Envelope env, String fileName, BufferedImage image) throws IOException {

		GeoTiffWriteParams writeParams = new GeoTiffWriteParams();
		writeParams = new GeoTiffWriteParams();
		writeParams.setCompressionMode(GeoTiffWriteParams.MODE_EXPLICIT);
		writeParams.setCompressionType("LZW");
		writeParams.setCompressionQuality(0.85F);
		writeParams.setTilingMode(GeoToolsWriteParams.MODE_EXPLICIT);
		writeParams.setTiling(512, 512);

		RenderedImage img = image;
		GridCoverageFactory fac = CoverageFactoryFinder.getGridCoverageFactory(null);
		GridCoverage2D coverage = fac.create("GeoTIFF", img, env, null, null, null);

		File file = new File(fileName);
		GeoTiffWriter writer = new GeoTiffWriter(file);

		final ParameterValueGroup params = new GeoTiffFormat().getWriteParameters();

		params.parameter(AbstractGridFormat.GEOTOOLS_WRITE_PARAMS.getName().toString()).setValue(writeParams);

		final GeneralParameterValue[] wps = (GeneralParameterValue[]) params.values().toArray(new GeneralParameterValue[1]);
		try {
			writer.write(coverage, wps);
			writer.dispose();
		} finally {
			try {
				writer.dispose();
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
		return true;

	}

}
