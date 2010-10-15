/* *********************************************************************** *
 * project: org.matsim.*
 * Importer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.gregor.pedvis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.core.basic.v01.IdImpl;

import com.jmatio.io.MatFileReader;
import com.jmatio.types.MLArray;
import com.jmatio.types.MLCell;
import com.jmatio.types.MLDouble;
import com.vividsolutions.jts.geom.Coordinate;

/**
 * @author laemmel
 * 
 */
public class Importer {

	private static final File file = new File("/home/laemmel/arbeit/dfg/90grad_01.mat");

	private final List<Ped> peds = new ArrayList<Ped>();

	private int count = 0;

	private final List<Double> timeSteps = new ArrayList<Double>(350);

	public void read() throws IOException {
		MatFileReader reader = new MatFileReader();
		Map<String, MLArray> map = reader.read(file);
		MLArray redA = map.get("pedXYr");
		MLArray greenA = map.get("pedXYg");

		for (int i = 0; i < 350; i++) {
			this.timeSteps.add(0.);
		}

		extractPeds("red", redA);
		extractPeds("green", greenA);

	}

	/**
	 * @param string
	 * @param redA
	 */
	private void extractPeds(String string, MLArray redA) {
		MLCell redC = (MLCell) redA;
		int[] dims = redC.getDimensions();

		double maxTime = 0;

		for (int i = 0; i < dims[1]; i++) {
			Ped p = new Ped();
			p.id = new IdImpl(this.count++);
			p.color = string;
			p.depart = -1;
			MLArray redCC = redC.get(0, i);
			MLDouble d = (MLDouble) redCC;
			double[][] a = d.getArray();
			// for all time steps;

			for (int j = 0; j < 350; j++) {
				double time = a[j][2];
				if (!Double.isNaN(time)) {
					if (time > maxTime) {
						maxTime = time;
						System.out.println("time:" + time);
					}
					p.arrived = time;
					this.timeSteps.set(j, time);
					if (p.depart == -1) {
						p.depart = time;
					}
					Coordinate coord = new Coordinate(a[j][0] / 1000, a[j][1] / 1000, 0);
					p.coords.put(time, coord);
				}

			}
			this.peds.add(p);
		}

	}

	public static void main(String[] args) throws FileNotFoundException, IOException {
		MatFileReader reader = new MatFileReader();
		Map<String, MLArray> map = reader.read(file);
		System.out.println(map.size());
		for (Entry<String, MLArray> e : map.entrySet()) {
			System.out.println(e);
			MLArray v = e.getValue();
			if (v.isCell()) {
				MLCell c = (MLCell) v;
				MLArray cc = c.get(0, 0);
				if (cc.isDouble()) {
					MLDouble d = (MLDouble) cc;
					System.out.println(d);
					double[][] a = d.getArray();
					for (int i = 0; i < d.getDimensions()[0]; i++) {
						System.out.print(a[i][1] + "  ");
						for (int j = 0; j < d.getDimensions()[1]; j++) {

						}
						// System.out.println();
					}
				}
				System.out.println(cc);
			}
			int[] dim = v.getDimensions();
			System.out.println(dim.length);

		}
	}

	/**
	 * @return
	 */
	public List<Ped> getPeds() {

		return this.peds;
	}

	/**
	 * @return
	 */
	public List<Double> getTimeSteps() {
		return this.timeSteps;
	}

}
