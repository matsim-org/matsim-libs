/* *********************************************************************** *
 * project: org.matsim.*
 * PlansFilterArea.java
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

import org.apache.log4j.Logger;
import org.matsim.gbl.Gbl;
import org.matsim.population.Person;
import org.matsim.population.Population;

public class PlansWriteCustomAttributes {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(PlansWriteCustomAttributes.class);

	private final String outfile;
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PlansWriteCustomAttributes(String outfile) {
		super();
		log.info("    init " + this.getClass().getName() + " module...");
		this.outfile = outfile;
		log.info("    done.");
		Gbl.errorMsg("TODO: THIS DOES NOT WORK YET!");
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// run method
	//////////////////////////////////////////////////////////////////////

	public void run(final Population plans) {
		log.info("    running " + this.getClass().getName() + " algorithm...");
		
		Gbl.errorMsg("TODO: THIS DOES NOT WORK YET!");

		int cols = plans.getPersons().values().iterator().next().getCustomAttributes().size();
		
		try {
			FileWriter fw = new FileWriter(outfile);
			BufferedWriter out = new BufferedWriter(fw);
			out.write("p_id");
			for (String key : plans.getPersons().values().iterator().next().getCustomAttributes().keySet()) { out.write("\t"+key); }
			out.write("\n");
			out.flush();
			for (Person p : plans.getPersons().values()) {
				out.write(p.getId().toString());
				if (p.getCustomAttributes().size() != cols) { Gbl.errorMsg("pid="+p.getId()+": THAT SCHOULD NOT HAPPEN!"); }
				for (Object o : p.getCustomAttributes().values()) { out.write("\t"+o.toString()); }
				out.write("\n");
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
