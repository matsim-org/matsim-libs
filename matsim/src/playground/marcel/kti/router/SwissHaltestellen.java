/* *********************************************************************** *
 * project: org.matsim.*
 * SwissHaltestellenReader.java
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

package playground.marcel.kti.router;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.network.NetworkLayer;
import org.matsim.network.algorithms.CalcBoundingBox;
import org.matsim.utils.StringUtils;
import org.matsim.utils.collections.QuadTree;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.geometry.shared.Coord;
import org.matsim.utils.io.IOUtils;

public class SwissHaltestellen {

	private final QuadTree<CoordI> haltestellen;

	private static final Logger log = Logger.getLogger(SwissHaltestellen.class);

	public SwissHaltestellen(final NetworkLayer network) {
		CalcBoundingBox bbox = new CalcBoundingBox();
		bbox.run(network);
		this.haltestellen = new QuadTree<CoordI>(bbox.getMinX(), bbox.getMinY(), bbox.getMaxX(), bbox.getMaxY());
	}

	public void readFile(final String filename) throws FileNotFoundException, IOException {
		final BufferedReader reader = IOUtils.getBufferedReader(filename);
		String line = reader.readLine(); // header
		while ((line = reader.readLine()) != null) {
			String[] parts = StringUtils.explode(line, '\t');
			if (parts.length == 7) {
				Coord coord = new Coord(Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
				this.haltestellen.put(coord.getX(), coord.getY(), coord);
			} else {
				log.warn("Could not parse line: " + line);
			}
		}
	}

	public CoordI getClosestLocation(final CoordI coord) {
		return this.haltestellen.get(coord.getX(), coord.getY());
	}
}
