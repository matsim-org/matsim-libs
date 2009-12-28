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
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;

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
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// run method
	//////////////////////////////////////////////////////////////////////

	public void run(final Population plans) {
		log.info("    running " + this.getClass().getName() + " module...");
		
		Set<String> keys = new HashSet<String>();
		
		for (Person p : plans.getPersons().values()) {
			if (p.getCustomAttributes().size() > keys.size()) { keys = p.getCustomAttributes().keySet(); }
		}
		
		try {
			FileWriter fw = new FileWriter(outfile);
			BufferedWriter out = new BufferedWriter(fw);
			out.write("p_id");
			for (String key : keys) { out.write("\t"+key); }
			out.write("\n");
			out.flush();
			for (Person p : plans.getPersons().values()) {
				out.write(p.getId().toString());
				for (String key : keys) { out.write("\t"+p.getCustomAttributes().get(key)); }
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
