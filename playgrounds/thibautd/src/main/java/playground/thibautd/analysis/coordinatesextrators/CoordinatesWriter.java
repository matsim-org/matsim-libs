/* *********************************************************************** *
 * project: org.matsim.*
 * CoordinatesWriter.java
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
package playground.thibautd.analysis.coordinatesextrators;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

/**
 * @author thibautd
 */
public class CoordinatesWriter {
	public static void write(
			final List<Coord> coords,
			final String file) {
		BufferedWriter writer = IOUtils.getBufferedWriter( file );

		try {
			for (Coord coord : coords) {
				writer.write(coord.getX()+"\t"+coord.getY());
				writer.newLine();
			}

			writer.close();
		} catch (IOException e) {
			throw new UncheckedIOException( e );
		}
	}
}

