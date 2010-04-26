/* *********************************************************************** *
 * project: org.matsim.*
 * PersonLicenseModel.java
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
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;

import playground.balmermi.census2000v2.data.CAtts;

public class PopulationAddCustomAttributes {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(PopulationAddCustomAttributes.class);

	private final String infile;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PopulationAddCustomAttributes(final String infile) {
		log.info("    init " + this.getClass().getName() + " module...");
		this.infile = infile;
		log.info("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	public void run(Population pop) {
		log.info("    running " + this.getClass().getName() + " module...");
		try {
			FileReader fr = new FileReader(this.infile);
			BufferedReader br = new BufferedReader(fr);
			int line_cnt = 0;

			// Skip header
			String curr_line = br.readLine(); line_cnt++;
			curr_line = br.readLine(); line_cnt++;
			while ((curr_line = br.readLine()) != null) {
				String[] entries = curr_line.split("\t", -1);

				Integer nat = Integer.valueOf(entries[CAtts.I_HMAT]);
				Integer aperz = Integer.valueOf(entries[CAtts.I_APERZ]);
				Integer aperw = Integer.valueOf(entries[CAtts.I_APERW]);
				Integer wkata = Integer.valueOf(entries[CAtts.I_WKATA]);

				Person p = pop.getPersons().get(new IdImpl(entries[CAtts.I_PERSON_ID]));
				if (p == null) { p = pop.getPersons().get(new IdImpl(entries[CAtts.I_PARTNR])); }
				if (p != null) {
					p.getCustomAttributes().put(CAtts.P_HMAT,nat);
					p.getCustomAttributes().put(CAtts.P_APERZ,aperz);
					p.getCustomAttributes().put(CAtts.P_APERW,aperw);
					p.getCustomAttributes().put(CAtts.P_WKATA,wkata);
				}
				else { log.debug("      Line "+line_cnt+": neither pid="+entries[CAtts.I_PERSON_ID]+" nor pid="+entries[CAtts.I_PARTNR]+" found in population."); }

				// progress report
				if (line_cnt % 100000 == 0) { log.info("      Line " + line_cnt); }
				line_cnt++;
			}
			br.close();
			fr.close();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
		log.info("    done.");
	}
}
