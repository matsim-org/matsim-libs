/* *********************************************************************** *
 * project: org.matsim.*
 * GridCenterReader.java
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

package playground.yu.utils.io;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;

public class GridCenterReader {
	private SimpleReader reader;
	private Set<Coord> centers;

	public Set<Coord> getCenters() {
		return centers;
	}

	public GridCenterReader(String filename) {
		this.reader = new SimpleReader(filename);
		this.centers = new HashSet<Coord>();
	}

	public void readFile() {
		String line = this.reader.readLine();
		line = this.reader.readLine();
		while (line != null) {

			String[] words = line.split("\t");
			double x = Double.parseDouble(words[0]);
			double y = Double.parseDouble(words[1]);
			Coord coord = new CoordImpl(x, y);
			this.centers.add(coord);
			line = this.reader.readLine();
		}
		this.reader.close();
	}

	public static void main(String[] args) {
		String filename = "../integration-demandCalibration/test/DestinationUtilOffset2/1000.destUtiloffset.10.grid.log";
		GridCenterReader reader = new GridCenterReader(filename);
		reader.readFile();
		System.out.println("centers :\t" + reader.getCenters());
		System.out.println("centers size :\t" + reader.getCenters().size());
	}
}
