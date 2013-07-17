/* *********************************************************************** *
 * project: org.matsim.*
 * SpatialGridTableWriter.java
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
package org.matsim.contrib.accessibility.utils.io.writer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.matsim.contrib.accessibility.gis.SpatialGrid;


/**
 * @author illenberger
 * @author thomas
 */
public class SpatialGridTableWriter {
	
	public static final String separator = "\t";

	public void write(SpatialGrid grid, String file) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		
//		for(double x = grid.getXmin(); x <= grid.getXmax(); x += grid.getResolution()) {
//			writer.write(SpatialGridTableWriter.separator);
//			writer.write(String.valueOf(x));
//		}
//		writer.newLine();
		
		for(double y = grid.getYmin(); y <= grid.getYmax() ; y += grid.getResolution()) {
//			writer.write(String.valueOf(y));
			for(double x = grid.getXmin(); x <= grid.getXmax(); x += grid.getResolution()) {
				writer.write(SpatialGridTableWriter.separator);
				Double val = grid.getValue(x, y);
				if(!Double.isNaN(val))
					writer.write(String.valueOf(val));
				else
					writer.write("NaN");
			}
			writer.newLine();
		}
		writer.flush();
		writer.close();
	}
}
