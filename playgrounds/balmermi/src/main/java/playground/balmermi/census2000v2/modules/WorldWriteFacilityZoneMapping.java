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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.world.MappedLocation;
import org.matsim.world.World;

public class WorldWriteFacilityZoneMapping {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(WorldWriteFacilityZoneMapping.class);

	private final String outfile;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public WorldWriteFacilityZoneMapping(String outfile) {
		super();
		log.info("    init " + this.getClass().getName() + " module...");
		this.outfile = outfile;
		log.info("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	public void run(World world) {
		log.info("    running " + this.getClass().getName() + " module...");
		try {
			FileWriter fw = new FileWriter(outfile);
			BufferedWriter out = new BufferedWriter(fw);
			out.write("f_id\tz_id\n");
			out.flush();
			for (MappedLocation f : world.getLayer(ActivityFacilitiesImpl.LAYER_TYPE).getLocations().values()) {
				if (f.getUpMapping().size() == 0) {
					Collection<ActivityOptionImpl> acts = ((ActivityFacilityImpl)f).getActivityOptions().values();
					if (acts.size() != 1) { Gbl.errorMsg("f_id="+f.getId()+": That must never happen!"); }
					else if (!acts.iterator().next().getType().equals("tta")) { Gbl.errorMsg("f_id="+f.getId()+": That must never happen either!"); }
					else { log.info("      f_id="+f.getId()+" has no zone mapping (outside CH, act_type='tta')"); }
				}
				else if (f.getUpMapping().size() != 1) {
					Gbl.errorMsg("f_id="+f.getId()+": There must be exactly one zone mapping!");
				}
				else {
					out.write(f.getId()+"\t"+f.getUpMapping().firstKey()+"\n");
				}
			}
			out.close();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		log.info("    done.");
	}
}
