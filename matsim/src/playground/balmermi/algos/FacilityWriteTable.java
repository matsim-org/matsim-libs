/* *********************************************************************** *
 * project: org.matsim.*
 * FacilityWriteTable.java
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

import org.matsim.facilities.Activity;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.facilities.algorithms.FacilitiesAlgorithm;
import org.matsim.world.Location;

public class FacilityWriteTable extends FacilitiesAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private FileWriter fw = null;
	private BufferedWriter out = null;
	private String act_type;
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public FacilityWriteTable(String act_type) {
		super();
		this.act_type = act_type;
		try {
			fw = new FileWriter("output/facilities_" + act_type + ".txt");
			out = new BufferedWriter(fw);
			out.write("fId\tx\ty\tcap\n");
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

	public void run(Facilities facilities) {
		System.out.println("    running " + this.getClass().getName() + " algorithm...");

		try {
			Iterator<Location> f_it = facilities.getLocations().values().iterator();
			while (f_it.hasNext()) {
				Facility f = (Facility)f_it.next();
				Iterator<Activity> a_it = f.getActivities().values().iterator();
				while (a_it.hasNext()) {
					Activity a = a_it.next();
					if (a.getType().equals(this.act_type)) {
						out.write(f.getId() + "\t" + f.getCenter().getX() + "\t" + f.getCenter().getY() + "\t" + a.getCapacity() + "\n");
					}
				}
				out.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		System.out.println("    done.");
	}
}
