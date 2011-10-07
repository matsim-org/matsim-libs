/* *********************************************************************** *
 * project: org.matsim.*
 * CEMDAPParser.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.ucsb.demand;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author balmermi
 *
 */
public class CEMDAPParser {

	private final static Logger log = Logger.getLogger(CEMDAPParser.class);
	private static final String HID = "HID";
	private static final Object PID = "PID";
	private static final Object O_ZONE_ID = "OZoneID";
	private static final Object P_ACT_TYPE = "PActType";

	public CEMDAPParser() {
	}
	
	private final String transformActType(int actTypeNo) {
		switch (actTypeNo) {
		case 0: return "shop";
		case 1: return "leisure";
		case 2: return "other_personal";
		case 3: return "leisure_eat";
		case 4: return "other_serve_passenger";
		case 6: return "home";
		case 7: return "work";
		case 8: return "work_related";
		case 10: return "drop_off_school";
		case 11: return "pick_up_school";
		case 12: return "other_joint_withChild";
		case 13: return "home";
		case 14: return "educ_school";
		case 15: return "other_forChild";
		case 16: return "other_joint_withAdult";
		case 17: return "work";
		case 18: return "home";
		case 19: return "educ_school";
		default:
			Gbl.errorMsg(new IllegalArgumentException("actTypeNo="+actTypeNo+" not allowed."));
			return null;
		}
	}
	
	public final void parse(String cemdapStopsFile, Scenario scenario) {
		log.info("parsing "+cemdapStopsFile+" file...");
		Population population = scenario.getPopulation();
		int line_cnt = 0;

		try {
			BufferedReader br = IOUtils.getBufferedReader(cemdapStopsFile);

			// header
			String curr_line = br.readLine(); line_cnt++;
			String[] heads = curr_line.split("\t", -1);
			Map<String,Integer> column = new LinkedHashMap<String,Integer>(heads.length);
			for (int i=0; i<heads.length; i++) { column.put(heads[i],i); }
			log.info("columns of input file: "+cemdapStopsFile+" ...");
			for (String head : column.keySet()) { log.info(column.get(head)+":"+head); }
			log.info("done.");

			// data
			while ((curr_line = br.readLine()) != null) {
				String[] entries = curr_line.split("\t", -1);
				line_cnt++;
				
				// household id / person id
				Integer hid = new Double(entries[column.get(HID)]).intValue();
				Integer id = new Double(entries[column.get(PID)]).intValue();
				Id pid = scenario.createId(hid+"_"+id);
				
				Person person = population.getPersons().get(pid);
				if (person == null) {
					person = population.getFactory().createPerson(pid);
					population.addPerson(person);
					person.addPlan(population.getFactory().createPlan());
				}
				Plan plan = person.getSelectedPlan();
				if (plan.getPlanElements().isEmpty()) {
					// TODO balmermi: replace zone id with real coordinte
					long zoneId = new Double(entries[column.get(O_ZONE_ID)]).longValue();
					Coord coord = scenario.createCoord(zoneId,zoneId);
					String actType = transformActType(new Double(entries[column.get(P_ACT_TYPE)]).intValue());
					Activity firstActivity = population.getFactory().createActivityFromCoord(actType, coord);
					plan.addActivity(firstActivity);
				}

			}
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
		log.info(line_cnt+" lines parsed");
		log.info("done.");
	}
}
