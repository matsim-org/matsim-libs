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

package playground.meisterk.kti.router;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.algorithms.CalcBoundingBox;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.StringUtils;

public class SwissHaltestellen {

	private final QuadTree<SwissHaltestelle> haltestellen;

	private static final Logger log = Logger.getLogger(SwissHaltestellen.class);

	public SwissHaltestellen(final Network network) {
		CalcBoundingBox bbox = new CalcBoundingBox();
		bbox.run(network);
		this.haltestellen = new QuadTree<SwissHaltestelle>(bbox.getMinX(), bbox.getMinY(), bbox.getMaxX(), bbox.getMaxY());
	}

	public void readFile(final String filename) throws FileNotFoundException, IOException {
		final BufferedReader reader = IOUtils.getBufferedReader(filename);
		String line = reader.readLine(); // header
		while ((line = reader.readLine()) != null) {
			String[] parts = StringUtils.explode(line, '\t');
			if (parts.length == 7) {
				CoordImpl coord = new CoordImpl(Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
				SwissHaltestelle swissStop = new SwissHaltestelle(new IdImpl(parts[0]), coord);
				this.haltestellen.put(coord.getX(), coord.getY(), swissStop);
			} else {
				log.warn("Could not parse line: " + line);
			}
		}
	}

	public SwissHaltestelle getClosestLocation(final Coord coord) {
		return this.haltestellen.get(coord.getX(), coord.getY());
	}
	
	public SwissHaltestelle getHaltestelle(Id id) {
		
		SwissHaltestelle swissStop = null;
		
		Iterator<SwissHaltestelle> it = haltestellen.values().iterator();
		boolean notFound = true;
		while (notFound && it.hasNext()) {
			swissStop = it.next();
			if (id.equals(swissStop.getId())) {
				notFound = false;
			}
		}
		
		return swissStop;
		
	}
	
}
