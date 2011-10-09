/* *********************************************************************** *
 * project: org.matsim.*
 * UCSBStopsParser.java
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
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;

/**
 * @author balmermi
 *
 */
public class UCSBStopsParser {

	private final static Logger log = Logger.getLogger(UCSBStopsParser.class);

	private final Random r = MatsimRandom.getRandom();
	private final Coord DEFAULT_COORD = new CoordImpl(-1.0,-1.0);

	private static final String HID = "HID";
	private static final String PID = "PID";
	private static final String O_ZONE_ID = "OZoneID";
	private static final String P_ACT_TYPE = "PActType";
	private static final String P_DEPTIME = "StartT";
	private static final Object ARRTIME = "ArriveT";
	private static final int TIME_OFFSET = 3*3600;
	private static final String MODE = "Mode";
	private static final String ACT_TYPE = "ActType";
	private static final String ZONE_ID = "ZoneID";
	private static final String ACTDUR = "Duration";

	public static final String ZONE = "zone";


	public UCSBStopsParser() {
	}
	
	private final String transformMode(int modeNo) {
		switch (modeNo) {
		case 0: return TransportMode.car;
		case 1: return TransportMode.ride;
		case 2: return TransportMode.walk;
		case 3: return TransportMode.pt;
		case 4: return TransportMode.ride; // driven by parent (for child)
		case 5: return TransportMode.ride; // driven by other (for child)
		case 6: return TransportMode.ride; // school bus (for child)
		case 7: return TransportMode.car;  // shared ride driver (drive by car with passenger)
		default:
			Gbl.errorMsg(new IllegalArgumentException("modeNo="+modeNo+" not allowed."));
			return null;
		}
	}
	
//	private final String transformActType(int actTypeNo) {
//		switch (actTypeNo) {
//		case 0: return "shop";
//		case 1: return "social";
//		case 2: return "other";
//		case 3: return "leisure_eat_out";
//		case 4: return "other_serve_passenger";
//		case 5: return "leisure_entertainment";
//		case 6: return "leisure_sports";
//		case 7: return "visit";
//		case 8: return "work_related";
//		case 9: return "other_maintainance";
//		case 10: return "dropoff_school";
//		case 11: return "pickup_school";
//		case 12: return "home_adult";
//		case 13: return "work_adult";
//		case 14: return "home_child";
//		case 15: return "education";
//		case 16: return "leisure_child";
//		case 17: return "work";
//		case 18: return "home";
//		case 19: return "education";
//		case 20: return "leisure_adult_withChild";
//		case 21: return "leisure_child_withParent";
//		default:
//			Gbl.errorMsg(new IllegalArgumentException("actTypeNo="+actTypeNo+" not allowed."));
//			return null;
//		}
//	}
	
	private final String transformActType(int actTypeNo) {
		switch (actTypeNo) {
		case 0: return "shop";
		case 1: return "other";
		case 2: return "other";
		case 3: return "leis";
		case 4: return "other";
		case 5: return "leis";
		case 6: return "leis";
		case 7: return "other";
		case 8: return "work";
		case 9: return "other";
		case 10: return "other";
		case 11: return "other";
		case 12: return "home";
		case 13: return "work";
		case 14: return "home";
		case 15: return "educ";
		case 16: return "leis";
		case 17: return "work";
		case 18: return "home";
		case 19: return "educ";
		case 20: return "leis";
		case 21: return "leis";
		default:
			Gbl.errorMsg(new IllegalArgumentException("actTypeNo="+actTypeNo+" not allowed."));
			return null;
		}
	}
	
	public final void parse(String cemdapStopsFile, Scenario scenario, ObjectAttributes personObjectAttributes, double fraction) {
		Population population = scenario.getPopulation();
		int line_cnt = 0;

		try {
			BufferedReader br = IOUtils.getBufferedReader(cemdapStopsFile);

			// header
			String curr_line = br.readLine(); line_cnt++;
			String[] heads = curr_line.split("\t", -1);
			Map<String,Integer> column = new LinkedHashMap<String,Integer>(heads.length);
			for (int i=0; i<heads.length; i++) { column.put(heads[i],i); }
			
			Id currPid = null;
			boolean storePerson = false;

			// data
			while ((curr_line = br.readLine()) != null) {
				String[] entries = curr_line.split("\t", -1);
				line_cnt++;
				
				if (line_cnt % 1000000 == 0) {
					log.info("line "+line_cnt+": "+population.getPersons().size()+" persons stored so far.");
					Gbl.printMemoryUsage();
				}
				
				// household id / person id
				Integer hid = new Double(entries[column.get(HID)]).intValue();
				Integer id = new Double(entries[column.get(PID)]).intValue();
				Id pid = scenario.createId(hid+"_"+id);
				
				if (!pid.equals(currPid)) {
					currPid = pid;
					if (r.nextDouble() < fraction) { storePerson = true; } else { storePerson = false; }
				}
				if (!storePerson) { continue; }
				
				Person person = population.getPersons().get(pid);
				if (person == null) {
					person = population.getFactory().createPerson(pid);
					population.addPerson(person);
					person.addPlan(population.getFactory().createPlan());
				}

				Plan plan = person.getSelectedPlan();
				int depTime = Integer.parseInt(entries[column.get(P_DEPTIME)])*60 + TIME_OFFSET;
				int arrTime = Integer.parseInt(entries[column.get(ARRTIME)])*60 + TIME_OFFSET;

				if (plan.getPlanElements().isEmpty()) {
					String zoneId = entries[column.get(O_ZONE_ID)].trim();
					personObjectAttributes.putAttribute(pid.toString(),ZONE+"0",zoneId);
					String actType = transformActType(new Double(entries[column.get(P_ACT_TYPE)]).intValue());
					Activity firstActivity = population.getFactory().createActivityFromCoord(actType,DEFAULT_COORD);
					firstActivity.setStartTime(0);
					firstActivity.setEndTime(depTime);
					firstActivity.setMaximumDuration(depTime);
					plan.addActivity(firstActivity);
				}
				
				String mode = transformMode(Integer.parseInt(entries[column.get(MODE)]));
				Leg leg = population.getFactory().createLeg(mode);
				leg.setDepartureTime(depTime);
				leg.setTravelTime(arrTime-depTime);
				plan.addLeg(leg);
				
				String zoneId = entries[column.get(ZONE_ID)].trim();
				int actIndex = plan.getPlanElements().size()/2;
				personObjectAttributes.putAttribute(pid.toString(),ZONE+actIndex,zoneId);
				String actType = transformActType(new Double(entries[column.get(ACT_TYPE)]).intValue());
				Activity activity = population.getFactory().createActivityFromCoord(actType,DEFAULT_COORD);
				int actDur = Integer.parseInt(entries[column.get(ACTDUR)])*60;
				activity.setStartTime(arrTime);
				activity.setEndTime(arrTime+actDur);
				activity.setMaximumDuration(actDur);
				plan.addActivity(activity);

			}
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
		log.info(line_cnt+" lines parsed.");
		log.info(population.getPersons().size()+" persons stored.");
	}
}
