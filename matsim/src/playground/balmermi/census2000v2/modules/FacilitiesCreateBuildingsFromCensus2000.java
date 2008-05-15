/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesSetCapacity.java
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

package playground.balmermi.census2000v2.modules;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.facilities.Activity;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.geometry.shared.Coord;
import org.matsim.world.Layer;
import org.matsim.world.Location;

/**
 * <p>
 * <b>MATSim-FUSION Module</b>
 * </p>
 *
 * <p>
 * For each given activity of each given facility, the capacity will be set to 1
 * if no capacity is defined.
 * </p>
 * <p>
 * Log messages:<br>
 * for each <code>home</code> and <code>work</code> activity which the
 * capacity is set to 1, one log line will be written.
 * No log lines are written for other activity types.
 * </p>
 *
 * @author Michael Balmer
 */
public class FacilitiesCreateBuildingsFromCensus2000 {

	private static final String HOME = "home";
	private final String infile;
	private final Layer municipalities;

	public FacilitiesCreateBuildingsFromCensus2000(final String infile, final Layer municipalities) {
		super();
		System.out.println("    init " + this.getClass().getName() + " module...");
		this.infile = infile;
		this.municipalities = municipalities;
		System.out.println("    done.");
	}

	public void run(Facilities facilities) {
		System.out.println("    running " + this.getClass().getName() + " module...");

		if (!facilities.getFacilities().isEmpty()) { Gbl.errorMsg("Facilities DB is not empty!"); }
		try {
			FileReader fr = new FileReader(this.infile);
			BufferedReader br = new BufferedReader(fr);
			int line_cnt = 0;
			int max_home_cap = 1;
			int min_f_id = Integer.MAX_VALUE;
			int max_f_id = Integer.MIN_VALUE;

			// Skip header
			String curr_line = br.readLine(); line_cnt++;
			curr_line = br.readLine(); line_cnt++;
			while ((curr_line = br.readLine()) != null) {
				String[] entries = curr_line.split("\t", -1);
				// ZGDE  GEBAEUDE_ID  ...  XACH  YACH
				// 1     2                 170   171
				
				// check for existing municipality
				Id zone_id = new IdImpl(entries[1]);
				Location zone = this.municipalities.getLocation(zone_id);
				if (zone == null) { Gbl.errorMsg("Line "+line_cnt+": Zone id="+zone_id+" does not exist!"); }

				// home facility creation
				Id f_id = new IdImpl(entries[2]);
				CoordI coord = new Coord(entries[170],entries[171]);
				Facility f = facilities.getFacilities().get(f_id);
				if (f == null) {
					// create new home facility id
					f = facilities.createFacility(f_id,coord);
					Activity act = f.createActivity(HOME);
					act.setCapacity(1);
					
					// store some info
					int id = Integer.parseInt(f.getId().toString());
					if (id < min_f_id) { min_f_id = id; }
					if (id > max_f_id) { max_f_id = id; }
				}
				else {
					// check for coordinate consistency of existing home facility
					if ((coord.getX() != f.getCenter().getX()) || coord.getY() != f.getCenter().getY()) {
						Gbl.errorMsg("Line "+line_cnt+": facility id="+f_id+" already exists and has another coordinate!");
					}
					
					// add 1 to capacity
					Activity act = f.getActivity(HOME);
					act.setCapacity(act.getCapacity()+1);
					
					// store some info
					if (act.getCapacity()>max_home_cap) { max_home_cap = act.getCapacity(); }
				}
				
				// progress report
				if (line_cnt % 100000 == 0) {
					System.out.println("    Line " + line_cnt + ": # facilities = " + facilities.getFacilities().size());
				}
				line_cnt++;
			}
			br.close();
			fr.close();
			System.out.println("    "+facilities.getFacilities().size()+" home facilities with capcacities from 1 to "+max_home_cap+" created!");
			System.out.println("    min facility id="+min_f_id);
			System.out.println("    max facility id="+max_f_id);
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
		System.out.println("    done.");
	}
}
