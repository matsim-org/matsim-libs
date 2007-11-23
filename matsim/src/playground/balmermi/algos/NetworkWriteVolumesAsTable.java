/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkWriteVolumesAsTable.java
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

package playground.balmermi.algos;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Random;

import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.algorithms.NetworkAlgorithm;
import org.matsim.utils.identifiers.IdI;

public class NetworkWriteVolumesAsTable extends NetworkAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private FileWriter fw = null;
	private BufferedWriter out = null;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public NetworkWriteVolumesAsTable() {
		super();
		try {
			fw = new FileWriter("output/volumes.txt");
			out = new BufferedWriter(fw);
			out.write("station_id\tlink_id\tt_start\tt_end\tvolume\n");
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	//////////////////////////////////////////////////////////////////////
	// final method
	//////////////////////////////////////////////////////////////////////

	public final void close() {
		try {
			out.flush();
			out.close();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	public void run(NetworkLayer network) {
		System.out.println("    running " + this.getClass().getName() + " algorithm...");
		
		int capperiod = network.getCapacityPeriod();
		capperiod = capperiod / 3600;
		System.out.println("      capperiod = " + capperiod);

		try {
			Iterator<?> l_it = network.getLinks().iterator();
			while (l_it.hasNext()) {
				Link l = (Link)l_it.next();
				IdI l_id = l.getId();
				
				Random r = new Random();
				
				if (r.nextDouble() > 0.99) {
					for (int i=0; i<3600*24; i=i+30*60) {
						double cap_max = l.getCapacity()/capperiod/2;
						double cap = r.nextDouble() * cap_max;
						out.write("st" + l_id + "\t" + l_id + "\t" + i + "\t" + (i+30*60) + "\t" + cap + "\n");
						out.flush();
					}
				} 
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		System.out.println("    done.");
	}
}
