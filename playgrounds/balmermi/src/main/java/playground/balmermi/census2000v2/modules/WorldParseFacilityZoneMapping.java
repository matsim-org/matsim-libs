/* *********************************************************************** *
 * project: org.matsim.*
 * WorldBottom2TopCompletion.java
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
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.world.Layer;
import org.matsim.world.World;

import playground.balmermi.census2000.data.Municipalities;

public class WorldParseFacilityZoneMapping {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(WorldWriteFacilityZoneMapping.class);

	private final String infile;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public WorldParseFacilityZoneMapping(String infile) {
		super();
		log.info("    init " + this.getClass().getName() + " module...");
		this.infile = infile;
		log.info("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	public void run(World world) {
		log.info("    running " + this.getClass().getName() + " module...");
		Layer fl = world.getLayer(ActivityFacilitiesImpl.LAYER_TYPE);
		Layer zl = world.getLayer(Municipalities.MUNICIPALITY);
		int line_cnt = 0;
		try {
			FileReader fr = new FileReader(this.infile);
			BufferedReader br = new BufferedReader(fr);

			// Skip header
			String curr_line = br.readLine(); line_cnt++;
			while ((curr_line = br.readLine()) != null) {
				String[] entries = curr_line.split("\t", -1);
				// f_id  z_id
				// 0     1
				Id fid = new IdImpl(entries[0]);
				Id zid = new IdImpl(entries[1]);
				world.addMapping(fl.getLocation(fid),zl.getLocation(zid));
				line_cnt++;
			}
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
		log.info("    done.");
	}
}
