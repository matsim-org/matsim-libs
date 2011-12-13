/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.tnicolai.matsim4opus.utils.io.writer;

import java.io.BufferedWriter;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

import playground.tnicolai.matsim4opus.gis.SpatialGrid;
import playground.tnicolai.matsim4opus.utils.helperObjects.SquareLayer;

public class SpatialGrid2AccessibilityCSVWriter {

	private static final Logger log = Logger.getLogger(SpatialGrid2AccessibilityCSVWriter.class);

	public static void write(final SpatialGrid<SquareLayer> grid, final String file){

		BufferedWriter writer = IOUtils.getBufferedWriter(file);
		log.info("... NOT done!");

		assert(grid!=null);




	}

}
