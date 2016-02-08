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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.ActivityOptionImpl;

import playground.balmermi.census2000v2.data.CAtts;
import playground.balmermi.world.Layer;
import playground.balmermi.world.Zone;

public class FacilitiesCreateBuildingsFromCensus2000 {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(FacilitiesCreateBuildingsFromCensus2000.class);

	private final String infile;
	private final Layer municipalities;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public FacilitiesCreateBuildingsFromCensus2000(final String infile, final Layer municipalities) {
		super();
		log.info("    init " + this.getClass().getName() + " module...");
		this.infile = infile;
		this.municipalities = municipalities;
		log.info("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private final int getMinFacilityId(final ActivityFacilities facilities) {
		int min_id = Integer.MAX_VALUE;
		for (Id id : facilities.getFacilities().keySet()) {
			int f_id = Integer.parseInt(id.toString());
			if (f_id < min_id) { min_id = f_id; }
		}
		return min_id;
	}

	//////////////////////////////////////////////////////////////////////
	// run method
	//////////////////////////////////////////////////////////////////////

	public void run(final ActivityFacilities facilities) {
		log.info("    running " + this.getClass().getName() + " module...");
		log.info("      # facilities = " + facilities.getFacilities().size());

		int min_id_given = this.getMinFacilityId(facilities);
		log.info("      min_f_id = " + min_id_given);

		try {
			FileReader fr = new FileReader(this.infile);
			BufferedReader br = new BufferedReader(fr);
			int line_cnt = 0;
			int home_fac_cnt = 0;
			double max_home_cap = 1;
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
				Id<Zone> zone_id = Id.create(entries[CAtts.I_ZGDE], Zone.class);
				BasicLocation zone = this.municipalities.getLocation(zone_id);
				if (zone == null) { throw new RuntimeException("Line "+line_cnt+": Zone id="+zone_id+" does not exist!"); }

				// check for facility id
				Id<ActivityFacility> f_id = Id.create(entries[CAtts.I_GEBAEUDE_ID], ActivityFacility.class);
				if (Integer.parseInt(f_id.toString()) >= min_id_given) { throw new RuntimeException("Line "+line_cnt+": f_id="+f_id+" must be less then min_id="+min_id_given+"!"); }

				// home facility creation
				Coord coord = new Coord(Double.parseDouble(entries[CAtts.I_XACH]), Double.parseDouble(entries[CAtts.I_YACH]));
				ActivityFacilityImpl f = (ActivityFacilityImpl) facilities.getFacilities().get(f_id);
				if (f == null) {
					// create new home facility id
					f = (ActivityFacilityImpl) facilities.getFactory().createActivityFacility(f_id, coord);
					facilities.addActivityFacility(f);
					ActivityOptionImpl act = f.createAndAddActivityOption(CAtts.ACT_HOME);
					act.setCapacity(1);

					// store some info
					home_fac_cnt++;
					int id = Integer.parseInt(f.getId().toString());
					if (id < min_f_id) { min_f_id = id; }
					if (id > max_f_id) { max_f_id = id; }
				}
				else {
					// check for coordinate consistency of existing home facility
					if ((coord.getX() != f.getCoord().getX()) || coord.getY() != f.getCoord().getY()) {
						throw new RuntimeException("Line "+line_cnt+": facility id="+f_id+" already exists and has another coordinate!");
					}

					// add 1 to capacity
					ActivityOptionImpl act = (ActivityOptionImpl) f.getActivityOptions().get(CAtts.ACT_HOME);
					act.setCapacity(act.getCapacity()+1);

					// store some info
					if (act.getCapacity()>max_home_cap) { max_home_cap = act.getCapacity(); }
				}

				// progress report
				if (line_cnt % 100000 == 0) {
					log.info("    Line " + line_cnt + ": # facilities = " + facilities.getFacilities().size());
				}
				line_cnt++;
			}
			br.close();
			fr.close();
			log.info("    "+home_fac_cnt+" home facilities with capcacities from 1 to "+max_home_cap+" created!");
			log.info("    min home facility id="+min_f_id);
			log.info("    max home facility id="+max_f_id);
			log.info("      # facilities = " + facilities.getFacilities().size());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		log.info("    done.");
	}
}
