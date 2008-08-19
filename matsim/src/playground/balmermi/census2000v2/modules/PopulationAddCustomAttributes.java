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
import org.matsim.gbl.Gbl;
import org.matsim.population.Person;
import org.matsim.population.Population;

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
				// P_ZGDE  P_GEBAEUDE_ID  P_HHNR
				// 1       2              3

				// ZGDE  GEBAEUDE_ID  HHNR  PERSON_ID  PARTNR  HMAT
				// 1     2            3     5          12      26
				Person p1 = pop.getPerson(entries[CAtts.I_PERSON_ID]);
				Person p2 = pop.getPerson(entries[CAtts.I_PARTNR]);
				Person p = null;
				if ((p1 == null) && (p2 != null)) { p = p2; }
				else if ((p1 != null) && (p2 == null)) { p = p1; }
				else { Gbl.errorMsg("person id="+entries[CAtts.I_PERSON_ID]+": Something is Wrong!"); }
				
				if (p.getCustomAttributes().put(CAtts.P_HMAT,entries[CAtts.I_HMAT]) != null) {
					Gbl.errorMsg("line "+line_cnt+", pid="+p.getId()+": person does already have '"+CAtts.P_HMAT+"' assigned!");
				}
				
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
