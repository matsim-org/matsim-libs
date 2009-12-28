/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesAddDataPulsBuildings.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.balmermi.datapuls.modules;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.utils.geometry.CoordImpl;

public class FacilitiesAddDataPulsBuildings {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(FacilitiesAddDataPulsBuildings.class);
	private final String infile;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public FacilitiesAddDataPulsBuildings(String infile) {
		super();
		log.info("init " + this.getClass().getName() + " module...");
		this.infile = infile;
		log.info("done.");
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private final void parse(ActivityFacilitiesImpl facilities) {
		log.info("  parsing "+infile+", create facilities and add them to the given ones...");
		log.info("    number of facilities: " + facilities.getFacilities().size());
		int line_cnt = 0;
		try {
			FileReader fr = new FileReader(infile);
			BufferedReader br = new BufferedReader(fr);

			String curr_line = null;
			while ((curr_line = br.readLine()) != null) {
				// BFS_GDE_NR  KANTON  RT_KOORD  HOCH_KOORD  ANZPRS  ANZHH  ANZKND  ANZAUSL  ANTAUSL  KKKL  KKKT  HHKLS1  HHKLS2  HHKLS3  HHKLS4  ALTKLS1  ALTKLS2  ALTKLS3  ALTKLS4  ALTKLS5  ALTER  ANTERWPRS  ANTERWLOS  GTYP  DKBAUPER  MPPBAUPER  RPBAUPER1  RPBAUPER2  RPBAUPER3  RPBAUPER4  RPBAUPER5  RPBAUPER6  RPBAUPER7  RPBAUPER8  MPPRENPER  RPRENPER0  RPRENPER5  RPRENPER6  RPRENPER7  LINKID
				// 6002        VS      642300    128800      8       2      3       0        .0000    1     53    0       0       0       2       3        2        2        0        1        33     .5070      .0540      2     6         3          .0000      .0769      .1538      .2308      .1538      .3846      .0000      .0000      6          .6154      .0000      .0769      .3077      91436663
				// 0           1       2         3           4       5      6       7        8        9     10    11      12      13      14      15       16       17       18       19       20     21         22         23    24        25         26         27         28         29         30         31         32         33         34         35         36         37         38         39
				String[] entries = curr_line.split("\t", -1);

				Coord coord = new CoordImpl(entries[2].trim(),entries[3].trim());
				double cap = Double.parseDouble(entries[4].trim());
				if (cap < 1.0) { cap = 1.0; }
				Id id = new IdImpl(entries[39].trim());
				
				ActivityFacilityImpl af = facilities.createFacility(id,coord);
				ActivityOptionImpl ao = af.createActivityOption("home");
				ao.setCapacity(cap);
				
				// progress report
				if (line_cnt % 100000 == 0) { log.info("    line " + line_cnt); }
				line_cnt++;
			}
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage());
		}
		log.info("    => number of facilities: " + facilities.getFacilities().size());
		log.info("  done.");
	}

	
	
	//////////////////////////////////////////////////////////////////////
	// run method
	//////////////////////////////////////////////////////////////////////

	public void run(final ActivityFacilitiesImpl facilities) {
		log.info("running " + this.getClass().getName() + " module...");
		parse(facilities);
		log.info("done.");
	}
}
