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
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacilityImpl;

import playground.balmermi.census2000.data.Municipalities;
import playground.balmermi.census2000.data.Municipality;
import playground.balmermi.census2000v2.data.CAtts;
import playground.balmermi.census2000v2.data.Household;
import playground.balmermi.census2000v2.data.Households;
import playground.balmermi.world.Layer;
import playground.balmermi.world.Zone;

public class HouseholdsCreateFromCensus2000 {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(HouseholdsCreateFromCensus2000.class);

	private final String infile;
	private final ActivityFacilitiesImpl facilities;
	private final Municipalities municipalities;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public HouseholdsCreateFromCensus2000(final String infile, final ActivityFacilitiesImpl facilities, final Municipalities municipalities) {
		super();
		log.info("    init " + this.getClass().getName() + " module...");
		this.infile = infile;
		this.facilities = facilities;
		this.municipalities = municipalities;
		log.info("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// private method
	//////////////////////////////////////////////////////////////////////

	private final void setAttributes(Household hh, String[] entries, int line_cnt) {
		int hhtpz = Integer.parseInt(entries[CAtts.I_HHTPZ]);
		if (!hh.setHHTPZ(hhtpz)) { throw new RuntimeException("Line "+line_cnt+": Household id="+hh.getId()+" something is wrong!");  }
		int hhtpw = Integer.parseInt(entries[CAtts.I_HHTPW]);
		if (!hh.setHHTPW(hhtpw)) { throw new RuntimeException("Line "+line_cnt+": Household id="+hh.getId()+" something is wrong!");  }
	}

	//////////////////////////////////////////////////////////////////////
	// run method
	//////////////////////////////////////////////////////////////////////

	public void run(Households households, Layer municipalityLayer) {
		log.info("    running " + this.getClass().getName() + " module...");

		try {
			FileReader fr = new FileReader(this.infile);
			BufferedReader br = new BufferedReader(fr);
			int line_cnt = 0;
			int min_hh_id = Integer.MAX_VALUE;
			int max_hh_id = Integer.MIN_VALUE;

			// Skip header
			String curr_line = br.readLine(); line_cnt++;
			curr_line = br.readLine(); line_cnt++;
			while ((curr_line = br.readLine()) != null) {
				String[] entries = curr_line.split("\t", -1);
				// P_ZGDE  P_GEBAEUDE_ID  P_HHNR
				// 1       2              3

				// check for existing zone
				Id<Zone> zone_id = Id.create(entries[CAtts.I_ZGDE], Zone.class);
				BasicLocation zone = municipalityLayer.getLocation(zone_id);
				if (zone == null) { throw new RuntimeException("Line "+line_cnt+": Zone id="+zone_id+" does not exist!"); }

				// check for existing facility
				Id<ActivityFacility> f_id = Id.create(entries[CAtts.I_GEBAEUDE_ID], ActivityFacility.class);
				ActivityFacilityImpl f = (ActivityFacilityImpl) this.facilities.getFacilities().get(f_id);
				if (f == null) { throw new RuntimeException("Line "+line_cnt+": Facility id="+f_id+" does not exist!"); }
				if (f.getActivityOptions().get(CAtts.ACT_HOME) == null) { throw new RuntimeException("Line "+line_cnt+": Facility id="+f_id+" exists but does not have 'home' activity type assigned!"); }

				// check for existing municipality
				Municipality muni = this.municipalities.getMunicipality(Integer.parseInt(zone_id.toString()));
				if (muni == null) { throw new RuntimeException("Line "+line_cnt+": Municipality id="+zone_id+" does not exist!"); }

				// household creation
				Id<Household> hh_id = Id.create(entries[CAtts.I_HHNR], Household.class);
				Household hh = households.getHousehold(hh_id);
				if (hh == null) {
					// create new household
					hh = new Household(hh_id,muni,f);
					households.addHH(hh);

					// set attributes
					this.setAttributes(hh,entries,line_cnt);

					// store some info
					int id = Integer.parseInt(hh.getId().toString());
					if (id < min_hh_id) { min_hh_id = id; }
					if (id > max_hh_id) { max_hh_id = id; }
				}
				else {
					// check for muni and facility consistency of existing household
					if ((!hh.getMunicipality().equals(muni)) || (!hh.getFacility().equals(f))) {
						throw new RuntimeException("Line "+line_cnt+": municipality id="+muni.getId()+" or facility id="+f_id+" are different to the existing household!");
					}
					// set attributes
					this.setAttributes(hh,entries,line_cnt);
				}

				// progress report
				if (line_cnt % 100000 == 0) {
					log.info("    Line " + line_cnt + ": # households = " + households.getHouseholds().size());
				}
				line_cnt++;
			}
			br.close();
			fr.close();
			log.info("    "+households.getHouseholds().size()+" households created!");
			log.info("    min household id="+min_hh_id);
			log.info("    max household id="+max_hh_id);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		log.info("    done.");
	}
}
