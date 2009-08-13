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

package playground.balmermi.datapuls.modules;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.ActivityFacilities;
import org.matsim.core.facilities.ActivityFacility;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.knowledges.Knowledge;
import org.matsim.knowledges.Knowledges;

public class PlansCreateFromDataPuls {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(PlansCreateFromDataPuls.class);
	private final String infile;
	private final ActivityFacilities facilities;
	private final Knowledges kn;
	private final Random random = MatsimRandom.getRandom();

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PlansCreateFromDataPuls(String infile, ActivityFacilities facilities, Knowledges kn) {
		log.info("init " + this.getClass().getName() + " module...");
		this.infile = infile;
		this.facilities = facilities;
		this.kn = kn;
		random.nextInt();
		log.info("done.");
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////
	
	private final void parse(Population population) {
		int line_cnt = 0;
		try {
			FileReader fr = new FileReader(infile);
			BufferedReader br = new BufferedReader(fr);

			String curr_line = null;
			while ((curr_line = br.readLine()) != null) {
				// CDBID  VORNAME  NACHNAME  ZUNAME  STRASSE  HNR  PLZ  ORT  ADRZUSATZ  JR  UMZDAT  SPRACHE  SEX  HAUSHALT  TELEFON  MOBILE  NO_PUB  RT_KOORD  HOCH_KOORD  LINKID
				// 0      1        2         3       4        5    6    7    8          9   10      11       12   13        14       15      16      17        18          19
				String[] entries = curr_line.split("\t", -1);

				Id id = new IdImpl(entries[0].trim());
				int age = 2008-Integer.parseInt(entries[9].trim());
				if (age < 0) { age = 0; }
				int gender = Integer.parseInt(entries[12].trim());
				if (gender == 0) { gender = random.nextInt(2)+1; }
				String sex = null;
				if (gender == 1) { sex = "m"; }
				else if (gender == 2) { sex = "f"; }
				else { throw new RuntimeException("line "+line_cnt+": gender is not 0,1 or 2."); }
				Id fid = new IdImpl(entries[19].trim());
				ActivityFacility af = facilities.getFacilities().get(fid);
				if (af == null) { throw new RuntimeException("line "+line_cnt+": fid="+fid+" not found in facilities."); }
				if (af.getActivityOptions().size() != 1) { throw new RuntimeException("line "+line_cnt+": fid="+fid+" must have only one activity option."); }
				ActivityOption a = af.getActivityOption("home");
				if (a == null) { throw new RuntimeException("line "+line_cnt+": fid="+fid+" does not contain 'home'."); }
				
				PersonImpl p = (PersonImpl)population.getPopulationBuilder().createPerson(id);
				Knowledge k = kn.getKnowledgesByPersonId().get(p.getId());
				if (k != null) { throw new RuntimeException("pid="+p.getId()+": knowledge already exist."); }
				k = kn.getBuilder().createKnowledge(p.getId(),null);
				kn.getKnowledgesByPersonId().put(p.getId(),k);
				k.addActivity(a,true);
				population.addPerson(p);
				p.setAge(age);
				p.setSex(sex);
				
				// progress report
				if (line_cnt % 100000 == 0) { log.info("  line " + line_cnt); }
				line_cnt++;
			}
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	//////////////////////////////////////////////////////////////////////
	// run method
	//////////////////////////////////////////////////////////////////////

	public void run(final PopulationImpl plans) {
		log.info("running " + this.getClass().getName() + " module...");
		parse(plans);
		log.info("done.");
	}
}
