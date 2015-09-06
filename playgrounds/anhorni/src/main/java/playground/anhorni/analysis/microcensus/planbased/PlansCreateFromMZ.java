/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.anhorni.analysis.microcensus.planbased;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

public class PlansCreateFromMZ {

	private static final String HOME = "h";
	private static final String WORK = "w";
	private static final String LEIS = "l";
	private static final String SHOP = "s";
	private static final String SHOP_GROCERY = "sg";
	private static final String SHOP_NONGROCERY = "sng";
	
	private static final String EDUC = "e";
	private static final String OTHR = "o";
	private static final String UNDF = "undefined";

	private final int dow_min;
	private final int dow_max;

	private final static Logger log = Logger.getLogger(PlansCreateFromMZ.class);
	
	public PlansCreateFromMZ(final int dow_min, final int dow_max) {
		super();
		this.dow_min = dow_min;
		this.dow_max = dow_max;
	}
	
	public void run(final Population population, String indir) {
		Population population2005 = ((ScenarioImpl) ScenarioUtils.createScenario(
				ConfigUtils.createConfig())).getPopulation();
		Population population2010 = ((ScenarioImpl) ScenarioUtils.createScenario(
				ConfigUtils.createConfig())).getPopulation();
				
		try {
			this.readPersonStrings(population2005, indir + "/2005Wegeinland.dat", "2005");
			this.readPersonStrings(population2010, indir + "/2010Wegeinland.dat", "2010");
										
			this.readPersonAttributes(indir + "/2005Zielpersonen.dat", "2005", population2005);
			this.readPersonAttributes(indir + "/2010Zielpersonen.dat", "2010", population2010);
			
			this.readHHAttributes(indir + "/2005Haushalte.dat", "2005", population2005);
			this.readHHAttributes(indir + "/2010Haushalte.dat", "2010", population2010);
			
			for (Person p : population2005.getPersons().values()) {
				population.addPerson(p);
			}
			for (Person p : population2010.getPersons().values()) {
				population.addPerson(p);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void readPersonStrings(final Population population, String inputFile, String year) throws Exception {
		log.info("    running " + this.getClass().getName() + " module...");

		if (population.getName() == null) { population.setName("created by '" + this.getClass().getName() + "'"); }
		if (!population.getPersons().isEmpty()) { throw new RuntimeException("[plans=" + population + " is not empty]"); }

		Map<Id<Person>,String> person_strings = new TreeMap<Id<Person>,String>();
		int id = Integer.MIN_VALUE;
		int prev_id = Integer.MIN_VALUE;
		Id<Person> prev_pid = Id.create(prev_id, Person.class);
		String person_string = "";

		log.info("      " + year + ": parsing persons...");

		FileReader fr = new FileReader(inputFile);
		BufferedReader br = new BufferedReader(fr);
		String curr_line = br.readLine();  // Skip header
		while ((curr_line = br.readLine()) != null) {

			String[] entries = curr_line.split("\t", -1);
			id = Integer.parseInt(year) * 1000000 + Integer.parseInt(entries[0].trim() + entries[1].trim());
			if (id == prev_id) {
				person_string = person_string + curr_line + "\n"; 
			}
			else {
				if (prev_id != Integer.MIN_VALUE) {
					if (person_strings.put(prev_pid,person_string) != null) { throw new RuntimeException("Person id="+prev_pid+" already parsed!"); }
				}
				person_string = curr_line + "\n";
				prev_id = id;
				prev_pid = Id.create(prev_id, Person.class);
			}
		}
		if (person_strings.put(prev_pid, person_string) != null) {
			throw new RuntimeException("Person id="+prev_pid+" already parsed!");
		}
		br.close();
		fr.close();
		log.info("      done.");

		log.info("      # persons parsed = " + person_strings.size());
		
		Set<Id<Person>> pids;
		if (year.equals("2005")) {
			pids = this.createPlans2005(population, person_strings);
		}
		else {
			pids = this.createPlans2010(population, person_strings);
		}
		log.info("      # persons created = " + population.getPersons().size());
		log.info("      # person_strings  = " + person_strings.size());
		log.info("      # persons with coord inconsistency = \t" + pids.size());
		this.processPlans(population, person_strings, pids);
	}
	
	private final Set<Id<Person>> createPlans2005(final Population plans, final Map<Id<Person>,String> person_strings) throws Exception {
		Set<Id<Person>> coord_err_pids = new HashSet<Id<Person>>();
		Set<Id<Person>> pids_dow = new HashSet<>();
		for (Id<Person> pid : person_strings.keySet()) {
			String person_string = person_strings.get(pid);
			String person_string_new = "";
			String[] lines = person_string.split("\n", -1); // last line is always an empty line
			for (int l = 0; l < lines.length-1; l++) {
				String[] entries = lines[l].split("\t", -1);
				// HHNR	ZIELPNR	WP	WEGNR	F58	F514	W_X	W_Y	W_BFS	S_X	S_Y	S_BFS	S_PLZ	S_ORT	PSEUDO	
				// 0	1		2	3		4	5		6	7	8		9	10	11		12		13		14
				// Z_X	Z_Y	Z_BFS	Z_PLZ	Z_ORT	F5202	F521	F521a	dauer1	w_etappen	wmittel	wmittela	
				// 15	16	17		18		19		20		21		22		23		24			25		26
				// wzweck2	wzweck1	dauer2	w_dist_obj2	ausnr	ausmittel
				// 27		28		29		30			31		32

				// pid check
				if (!pid.toString().equals(Integer.toString(2005 * 1000000 + Integer.parseInt(entries[0].trim() + entries[1].trim())))) {
					throw new RuntimeException("That must not happen!");
				}

				// departure time (min => sec.)
				int departure = Integer.parseInt(entries[4].trim())*60;
				entries[4] = Integer.toString(departure);

				Coord from = new Coord(Double.parseDouble(entries[9].trim()), Double.parseDouble(entries[10].trim()));
				entries[9] = Double.toString(from.getX());
				entries[10] = Double.toString(from.getY());
				int fromPLZ = -99;
				if (!entries[12].trim().isEmpty()) {
					fromPLZ = Integer.parseInt(entries[12].trim());
				}
				Coord to = new Coord(Double.parseDouble(entries[15].trim()), Double.parseDouble(entries[16].trim()));
				entries[15] = Double.toString(to.getX());
				entries[16] = Double.toString(to.getY());
				int toPLZ = -99;
				if (!entries[18].trim().isEmpty()) {
					toPLZ = Integer.parseInt(entries[18].trim());
				}

				// departure time (min => sec.). A few arrival times are not given, setting to departure time (trav_time=0)
				int arrival = departure;
				if (!entries[5].trim().equals("")) { 
					arrival = Integer.parseInt(entries[5].trim())*60; 
				}
				entries[5] = Integer.toString(arrival);

				// destination activity type ---------------------
				int purpose = Integer.parseInt(entries[28].trim());
				String acttype = null;
				if (purpose == -99) { acttype = UNDF; }
				else if (purpose == 1) { acttype = OTHR; }
				else if (purpose == 2) { acttype = WORK; }
				else if (purpose == 3) { acttype = EDUC; }
				else if (purpose == 4) { acttype = SHOP; }
				else if (purpose == 5) { acttype = OTHR; }
				else if (purpose == 6) { acttype = OTHR; }
				else if (purpose == 7) { acttype = OTHR; }
				else if (purpose == 8) { acttype = LEIS; }
				else if (purpose == 9) { acttype = OTHR; }
				else if (purpose == 10) { acttype = OTHR; }
				else if (purpose == 11) { acttype = OTHR; }
				else if (purpose == 12) { acttype = OTHR; }
				else { 
					throw new RuntimeException("pid=" + pid + ": purpose=" + purpose + " not known!"); 
				}

				// trip mode type ---------------------------------
				int m = Integer.parseInt(entries[25].trim());
				String mode = null;
				if (m == 1) { mode = "undefined"; }
				else if (m == 2) { mode = TransportMode.pt; }
				else if (m == 3) { mode = TransportMode.pt; }
				else if (m == 4) { mode = TransportMode.pt; }
				else if (m == 5) { mode = TransportMode.pt; }
				else if (m == 5) { mode = TransportMode.pt; }
				else if (m == 6) { mode = TransportMode.pt; }
				else if (m == 7) { mode = TransportMode.pt; }
				else if (m == 8) { mode = TransportMode.ride; }
				else if (m == 9) { mode = TransportMode.car; }
				else if (m == 10) { mode = TransportMode.car; }
				else if (m == 11) { mode = TransportMode.ride; }
				else if (m == 12) { mode = TransportMode.car; }
				else if (m == 13) { mode = TransportMode.car; }
				else if (m == 14) { mode = TransportMode.bike; }
				else if (m == 15) { mode = TransportMode.walk; }
				else if (m == 16) { mode = "undefined"; }
				else if (m == 17) { mode = "undefined"; }
				else { 
					throw new RuntimeException("pid=" + pid + ": m=" + m + " not known!");
				}

				// micro census person weight
				double weight = Double.parseDouble(entries[2].trim());
				
				// shopping purpose ----------------------------------------------
				double shoppingPurpose = Integer.parseInt(entries[21].trim());
				if (purpose == 4) {
					if (shoppingPurpose == 1) {
						acttype = SHOP_GROCERY;
					}
					else {
						acttype = SHOP_NONGROCERY;
					}
				}

				// creating the line with the changed data ------------------------
				String str = entries[0];
				for (int i=1; i<entries.length; i++) { 
					str = str + "\t" + entries[i]; 
				}
				str = str + "\n";
				person_string_new = person_string_new + str;

				// creating/getting the matsim person
				MZPerson person = (MZPerson) plans.getPersons().get(pid);
				if (person == null) {
					person = new MZPerson(pid);
					plans.addPerson(person);
				}
				
				person.setWeight(weight);

				// creating/getting plan
				Plan plan = person.getSelectedPlan();
				if (plan == null) {
					person.createAndAddPlan(true);
					plan = person.getSelectedPlan();
					plan.setScore(weight); // used plans score as a storage for the person weight of the MZ2005
				}

				// adding acts/legs
				if (plan.getPlanElements().size() != 0) { // already lines parsed and added
					MZActivityImpl from_act = (MZActivityImpl)plan.getPlanElements().get(plan.getPlanElements().size()-1);
					from_act.setEndTime(departure);
					from_act.setPlz(fromPLZ);
					LegImpl leg = ((PlanImpl) plan).createAndAddLeg(mode);
					leg.setDepartureTime(departure);
					leg.setTravelTime(arrival-departure);
					leg.setArrivalTime(arrival);
					NetworkRoute route = new LinkNetworkRouteImpl(null, null);
					leg.setRoute(route);
					route.setTravelTime(leg.getTravelTime());
					MZActivityImpl act = new MZActivityImpl(acttype,to);
					plan.addActivity(act);
					act.setPlz(toPLZ);
					act.setStartTime(arrival);

					// coordinate consistency check
					if ((from_act.getCoord().getX() != from.getX()) || (from_act.getCoord().getY() != from.getY())) {
						coord_err_pids.add(pid);
					}
				}
				else {
					MZActivityImpl homeAct = new MZActivityImpl(HOME,from);
					plan.addActivity(homeAct);
					homeAct.setEndTime(departure);
					homeAct.setPlz(fromPLZ);
					LegImpl leg = ((PlanImpl) plan).createAndAddLeg(mode);
					leg.setDepartureTime(departure);
					leg.setTravelTime(arrival-departure);
					leg.setArrivalTime(arrival);
					NetworkRoute route = new LinkNetworkRouteImpl(null, null);
					leg.setRoute(route);
					route.setTravelTime(leg.getTravelTime());
					MZActivityImpl act = new MZActivityImpl(acttype,to);
					plan.addActivity(act);
					act.setPlz(toPLZ);
					act.setStartTime(arrival);
				}
			}
			// replacing the person string with the new data
			if (person_strings.put(pid, person_string_new) == null) { throw new RuntimeException("That must not happen!"); }
		}
		log.info("        removing "+pids_dow.size()+" persons not part of the days = ["+this.dow_min+","+this.dow_max+"]...");
		this.removePlans(plans,person_strings,pids_dow);
		log.info("        done.");
		coord_err_pids.removeAll(pids_dow);

		return coord_err_pids;
	}
	
	private final Set<Id<Person>> createPlans2010(final Population plans, final Map<Id<Person>,String> person_strings) throws Exception {
		Set<Id<Person>> coord_err_pids = new HashSet<>();

		Set<Id<Person>> pids_dow = new HashSet<>();

		for (Id<Person> pid : person_strings.keySet()) {
			String person_string = person_strings.get(pid);
			String person_string_new = "";

			String[] lines = person_string.split("\n", -1); // last line is always an empty line
			for (int l=0; l<lines.length-1; l++) {
				String[] entries = lines[l].split("\t", -1);
				// HHNR	ZIELPNR	WP	WEGNR	f51100	f51400	pseudo	W_X	W_Y	w_x_CH1903	w_y_CH1903	W_BFS	S_X	S_Y	
				// 0	1		2	3		4		5		6		7	8	9			10			11		12	13
				// S_X_CH1903	S_Y_CH1903	S_BFS	S_PLZ	S_Ort	Z_X	Z_Y	Z_X_CH1903	Z_Y_CH1903	Z_BFS	Z_PLZ	Z_Ort	
				// 14			15			16		17		18		19	20	21			22			23		24		25
				// f51700	f51800a	f51800b	f51800c	f51800d	f51900	dauer1	w_etappen	w_dist_subj	w_luftlin	w_rdist	wmittel	wmittela	
				// 26		27		28		29		30		31		32		33			34			35			36		37		38		
				// wzweck1	wzweck2	dauer2	w_dist_obj2	ausnr
				// 39		40		41		42			43

				// pid check
				if (!pid.toString().equals(Integer.toString(2010 * 1000000 + Integer.parseInt(entries[0].trim() + entries[1].trim())))) {
					throw new RuntimeException("That must not happen!");
				}

				// departure time (min => sec.)
				int departure = Integer.parseInt(entries[4].trim())*60;
				entries[4] = Integer.toString(departure);

				Coord from = new Coord(Double.parseDouble(entries[14].trim()), Double.parseDouble(entries[15].trim()));
				entries[14] = Double.toString(from.getX());
				entries[15] = Double.toString(from.getY());
				int fromPLZ = -99;
				if (!entries[17].trim().isEmpty()) {
					fromPLZ = Integer.parseInt(entries[17].trim());
				}

				Coord to = new Coord(Double.parseDouble(entries[21].trim()), Double.parseDouble(entries[22].trim()));
				entries[21] = Double.toString(to.getX());
				entries[22] = Double.toString(to.getY());
				int toPLZ = -99;
				if (!entries[24].trim().isEmpty()) {
					toPLZ = Integer.parseInt(entries[24].trim());
				}

				// departure time (min => sec.). A few arrival times are not given, setting to departure time (trav_time=0)
				int arrival = departure;
				if (!entries[5].trim().equals("")) { 
					arrival = Integer.parseInt(entries[5].trim())*60; 
				}
				entries[5] = Integer.toString(arrival);

				// destination activity type ---------------------
				int purpose = Integer.parseInt(entries[39].trim());
				String acttype = null;
				if (purpose == -99) { acttype = UNDF; }
				else if (purpose == 1) { acttype = OTHR; }
				else if (purpose == 2) { acttype = WORK; }
				else if (purpose == 3) { acttype = EDUC; }
				else if (purpose == 4) { acttype = SHOP; }
				else if (purpose == 5) { acttype = OTHR; }
				else if (purpose == 6) { acttype = OTHR; }
				else if (purpose == 7) { acttype = OTHR; }
				else if (purpose == 8) { acttype = LEIS; }
				else if (purpose == 9) { acttype = OTHR; }
				else if (purpose == 10) { acttype = OTHR; }
				else if (purpose == 11) { acttype = OTHR; }
				else if (purpose == 12) { acttype = OTHR; }
				else if (purpose == 13) { acttype = OTHR; }
				else { 
					throw new RuntimeException("pid=" + pid + ": purpose=" + purpose + " not known!"); 
				}

				// trip mode type ---------------------------------
				int m = Integer.parseInt(entries[37].trim());
				String mode = null;
				if (m == 1) { mode = "undefined"; }
				else if (m == 2) { mode = TransportMode.pt; }
				else if (m == 3) { mode = TransportMode.pt; }
				else if (m == 4) { mode = TransportMode.pt; }
				else if (m == 5) { mode = TransportMode.pt; }
				else if (m == 5) { mode = TransportMode.pt; }
				else if (m == 6) { mode = TransportMode.pt; }
				else if (m == 7) { mode = TransportMode.pt; }
				else if (m == 8) { mode = TransportMode.ride; }
				else if (m == 9) { mode = TransportMode.car; }
				else if (m == 10) { mode = TransportMode.car; }
				else if (m == 11) { mode = TransportMode.ride; }
				else if (m == 12) { mode = TransportMode.car; }
				else if (m == 13) { mode = TransportMode.car; }
				else if (m == 14) { mode = TransportMode.bike; }
				else if (m == 15) { mode = TransportMode.walk; }
				else if (m == 16) { mode = "undefined"; }
				else if (m == 17) { mode = "undefined"; }
				else if (m == -99) { mode = "undefined"; }
				else { 
					throw new RuntimeException("pid=" + pid + ": m=" + m + " not known!");
				}
				// micro census person weight
				double weight = Double.parseDouble(entries[2].trim());
				
				// shopping purpose ----------------------------------------------
				double shoppingPurpose = Integer.parseInt(entries[27].trim());
				if (purpose == 4) {
					if (shoppingPurpose == 1) {
						acttype = SHOP_GROCERY;
					}
					else {
						acttype = SHOP_NONGROCERY;
					}
				}
				// creating the line with the changed data ------------------------
				String str = entries[0];
				for (int i=1; i<entries.length; i++) {
					str = str + "\t" + entries[i];  
				}
				str = str + "\n";
				person_string_new = person_string_new + str;

				// creating/getting the matsim person
				MZPerson person = (MZPerson) plans.getPersons().get(pid);
				if (person == null) {
					person = new MZPerson(pid);
					plans.addPerson(person);
				}
				
				person.setWeight(weight);

				// creating/getting plan
				Plan plan = person.getSelectedPlan();
				if (plan == null) {
					person.createAndAddPlan(true);
					plan = person.getSelectedPlan();
					plan.setScore(weight); // used plans score as a storage for the person weight of the MZ2005
				}

				// adding acts/legs
				if (plan.getPlanElements().size() != 0) { // already lines parsed and added
					MZActivityImpl from_act = (MZActivityImpl)plan.getPlanElements().get(plan.getPlanElements().size()-1);
					from_act.setEndTime(departure);
					from_act.setPlz(fromPLZ);
					LegImpl leg = ((PlanImpl) plan).createAndAddLeg(mode);
					leg.setDepartureTime(departure);
					leg.setTravelTime(arrival-departure);
					leg.setArrivalTime(arrival);
					NetworkRoute route = new LinkNetworkRouteImpl(null, null);
					leg.setRoute(route);
					route.setTravelTime(leg.getTravelTime());
					MZActivityImpl act = new MZActivityImpl(acttype,to);
					plan.addActivity(act);
					act.setPlz(toPLZ);
					act.setStartTime(arrival);

					// coordinate consistency check
					if ((from_act.getCoord().getX() != from.getX()) || (from_act.getCoord().getY() != from.getY())) {
						coord_err_pids.add(pid);
					}
				}
				else {
					MZActivityImpl homeAct = new MZActivityImpl(HOME,from);
					plan.addActivity(homeAct);
					homeAct.setEndTime(departure);
					homeAct.setPlz(fromPLZ);
					LegImpl leg = ((PlanImpl) plan).createAndAddLeg(mode);
					leg.setDepartureTime(departure);
					leg.setTravelTime(arrival-departure);
					leg.setArrivalTime(arrival);
					NetworkRoute route = new LinkNetworkRouteImpl(null, null);
					leg.setRoute(route);
					route.setTravelTime(leg.getTravelTime());
					MZActivityImpl act = new MZActivityImpl(acttype,to);
					plan.addActivity(act);
					act.setPlz(toPLZ);
					act.setStartTime(arrival);
				}
			}
			// replacing the person string with the new data
			if (person_strings.put(pid, person_string_new) == null) { 
				throw new RuntimeException("That must not happen!");
			}
		}
		log.info("        removing "+pids_dow.size()+" persons not part of the days = ["+this.dow_min+","+this.dow_max+"]...");
		this.removePlans(plans, person_strings,pids_dow);
		log.info("        done.");
		coord_err_pids.removeAll(pids_dow);

		return coord_err_pids;
	}
		
	private void readPersonAttributes(String inputFile, String year, Population population) throws IOException {
		FileReader fr = new FileReader(inputFile);
		BufferedReader br = new BufferedReader(fr);
		String curr_line = br.readLine();  // Skip header
		int id = Integer.MIN_VALUE;
		while ((curr_line = br.readLine()) != null) {
			String[] entries = curr_line.split("\t", -1);
			id = Integer.parseInt(year) * 1000000 + Integer.parseInt(entries[0].trim() + entries[1].trim());			
			int day = Integer.parseInt(entries[3].trim());
			int age = Integer.parseInt(entries[4].trim());
			int hh =  Integer.parseInt(year) * 1000000 + Integer.parseInt(entries[0].trim());
			String sex = ""; // 1=m 2=f
			int s = Integer.parseInt(entries[5].trim());
			if (s == 2) {
				sex = "f";
			}
			else if (s == 1) {
				sex = "m";
			}
			MZPerson person = (MZPerson) population.getPersons().get(Id.create(id, Person.class));
			if (person != null) {
				person.setAge(age);
				person.setDay(day);
				person.setHh(hh);
				person.setSex(sex);
			}
		}
	}
	
	private void readHHAttributes(String inputFile, String year, Population population) throws IOException {
		TreeMap<Id, Household> households = new TreeMap<Id, Household>();
		FileReader fr = new FileReader(inputFile);
		BufferedReader br = new BufferedReader(fr);
		String curr_line = br.readLine();  // Skip header
		int id = Integer.MIN_VALUE;
		while ((curr_line = br.readLine()) != null) {
			String[] entries = curr_line.split("\t", -1);
			id = Integer.parseInt(year) * 1000000 + Integer.parseInt(entries[0].trim());
			
			Household hh = new Household(id);			
			int hhSize = Integer.parseInt(entries[1].trim());
			int hhIncome = Integer.parseInt(entries[2].trim());
			hh.setHhIncome(hhIncome);
			hh.setSize(hhSize);
			households.put(Id.create(id, Household.class), hh);			
		}
		
		for (Person p:population.getPersons().values()) {
			MZPerson person = (MZPerson)p;
			Household household = households.get(Id.create(person.getHh(), Household.class));
			
			if (household == null) {
				log.info(person.getHh());
				log.info(households.keySet().toString());
			}			
			person.setHhSize(household.getSize());
			person.setHhIncome(household.getHhIncome());		
		}		
	}
			
	private void processPlans(final Population plans, final Map<Id<Person>,String> person_strings, Set<Id<Person>> pids) throws Exception {
		log.info("      removing persons with coord inconsistency...");
		this.removePlans(plans, person_strings, pids);
		log.info("      done.");
		log.info("-------------------------------------------------------------");
		log.info("      # persons left        = " + plans.getPersons().size());

		//////////////////////////////////////////////////////////////////////

		log.info("      setting home locations...");
		this.setHomeLocations(plans,person_strings);
		log.info("      done.");

		log.info("      # persons        = " + plans.getPersons().size());

		//////////////////////////////////////////////////////////////////////

		log.info("      identify plans with inconsistent times...");
		pids = this.identifyPlansInconsistentTimes(plans,person_strings);
		log.info("      done.");

		log.info("      # persons        = " + plans.getPersons().size());
		log.info("      # person_strings = " + person_strings.size());
		log.info("      # persons with plans with inconsistent times = \t" + pids.size());

		log.info("      removing persons with plans with inconsistent times...");
		this.removePlans(plans,person_strings,pids);
		log.info("      done.");
		log.info("-------------------------------------------------------------");
		log.info("      # persons left        = " + plans.getPersons().size());

		//////////////////////////////////////////////////////////////////////

		log.info("      identify non home based day plans...");
		pids = this.identifyNonHomeBasedPlans(plans,person_strings);
		log.info("      done.");

		log.info("      # persons        = " + plans.getPersons().size());
		log.info("      # person_strings = " + person_strings.size());
		log.info("      # persons with non home based plans = \t" + pids.size());

		log.info("      removing persons with non home based plans...");
		this.removePlans(plans,person_strings,pids);
		log.info("      done.");
		log.info("-------------------------------------------------------------");
		log.info("      # persons left        = " + plans.getPersons().size());

		//////////////////////////////////////////////////////////////////////

		log.info("      identify plans with act type '"+OTHR+"'...");
		pids = this.identifyPlansWithTypeOther(plans,person_strings);
		log.info("      done.");

		log.info("      # persons        = " + plans.getPersons().size());
		log.info("      # person_strings = " + person_strings.size());
		log.info("      # persons with plans with act type '"+OTHR+"' = \t" + pids.size());

		log.info("      removing persons with plans with act type '"+OTHR+"'...");
		this.removePlans(plans,person_strings,pids);
		log.info("      done.");
		log.info("-------------------------------------------------------------");
		log.info("      # persons left        = " + plans.getPersons().size());

		//////////////////////////////////////////////////////////////////////

		log.info("      identify plans with mode '"+UNDF+"'...");
		pids = this.identifyPlansModeTypeUndef(plans,person_strings);
		log.info("      done.");

		log.info("      # persons        = " + plans.getPersons().size());
		log.info("      # person_strings = " + person_strings.size());
		log.info("      # persons with plans with mode '"+UNDF+"' = \t" + pids.size());

		log.info("      removing persons with plans with mode '"+UNDF+"'...");
		this.removePlans(plans,person_strings,pids);
		log.info("      done.");
		log.info("-------------------------------------------------------------");
		log.info("      # persons left        = " + plans.getPersons().size());

		//////////////////////////////////////////////////////////////////////

//		log.info("      identify plans round trips...");
//		pids = this.identifyPlansWithRoundTrips(plans,person_strings);
//		log.info("      done.");
//
//		log.info("      # persons        = " + plans.getPersons().size());
//		log.info("      # person_strings = " + person_strings.size());
//		log.info("      # persons with plans with round trips = \t" + pids.size());
//
//		log.info("      removing round trips...");
//		this.removingRoundTrips(plans,person_strings);
//		log.info("      done.");
//		log.info("-------------------------------------------------------------");
//		log.info("      # persons left        = " + plans.getPersons().size());

		//////////////////////////////////////////////////////////////////////

		log.info("      identify plans neg. route distances...");
		pids = this.identifyPlansNegDistance(plans,person_strings);
		log.info("      done.");

		log.info("      # persons        = " + plans.getPersons().size());
		log.info("      # person_strings = " + person_strings.size());
		log.info("      # persons with plans neg. route distances = \t" + pids.size());

		log.info("      removing persons with plans neg. route distances...");
		this.removePlans(plans,person_strings,pids);
		log.info("      done.");
		log.info("-------------------------------------------------------------");
		log.info("      # persons left        = " + plans.getPersons().size());

		//////////////////////////////////////////////////////////////////////

		log.info("      identify plans neg. act coords...");
		pids = this.identifyPlansWithNegCoords(plans,person_strings);
		log.info("      done.");

		log.info("      # persons        = " + plans.getPersons().size());
		log.info("      # person_strings = " + person_strings.size());
		log.info("      # persons with plans neg. act coords = \t" + pids.size());

		log.info("      removing persons with plans neg. act coords...");
		this.removePlans(plans,person_strings,pids);
		log.info("      done.");
		log.info("-------------------------------------------------------------");
		log.info("      # persons left        = " + plans.getPersons().size());

		//////////////////////////////////////////////////////////////////////

		log.info("      identify plans with too long walk trips...");
		pids = this.identifyPlansWithTooLongWalkTrips(plans,person_strings);
		log.info("      done.");

		log.info("      # persons        = " + plans.getPersons().size());
		log.info("      # person_strings = " + person_strings.size());
		log.info("      # persons with plans with too long walk trips = \t" + pids.size());

		log.info("      removing persons with plans with too long walk trips...");
		this.removePlans(plans,person_strings,pids);
		log.info("      done.");

		log.info("-------------------------------------------------------------");
		log.info("      # persons left        = " + plans.getPersons().size());

		//////////////////////////////////////////////////////////////////////

		log.info("      identify single activity plans...");
		pids = this.identifySingleActPlans(plans,person_strings);
		log.info("      done.");

		log.info("      # persons        = " + plans.getPersons().size());
		log.info("      # person_strings = " + person_strings.size());
		log.info("      # persons with single activity plans = \t" + pids.size());

		log.info("      removing persons with single activity plans...");
		this.removePlans(plans,person_strings,pids);
		log.info("      done.");
		log.info("-------------------------------------------------------------");
		log.info("      # persons left        = " + plans.getPersons().size());

		//////////////////////////////////////////////////////////////////////
		
		log.info("removing routes");
		this.removeRoutes(plans);

		log.info("    done.");
	}

	//////////////////////////////////////////////////////////////////////

	private final void removePlans(final Population plans, final Map<Id<Person>,String> person_strings, final Set<Id<Person>> ids) {
		for (Id<Person> id : ids) {
			Person p = plans.getPersons().remove(id);
			if (p == null) { throw new RuntimeException("pid="+id+": id not found in the plans DB!"); }
			String p_str = person_strings.remove(id);
			if (p_str == null) { throw new RuntimeException("pid="+id+": id not found in the person_strings DB!"); }
		}
	}

	//////////////////////////////////////////////////////////////////////

	private final void setHomeLocations(final Population plans, final Map<Id<Person>,String> person_strings) {
		for (Person p : plans.getPersons().values()) {
			Plan plan = p.getSelectedPlan();
			Activity home = ((PlanImpl) plan).getFirstActivity();
			for (int i=2; i<plan.getPlanElements().size(); i=i+2) {
				Activity act = (ActivityImpl)plan.getPlanElements().get(i);
				if ((act.getCoord().getX() == home.getCoord().getX()) && (act.getCoord().getY() == home.getCoord().getY())) {
					if (!act.getType().equals(HOME)) {
						act.setType(HOME);
//						log.info("        pid=" + p.getId() + "; act_nr=" + (i/2) + ": set type to '"+HOME+"'");
					}
				}
			}
		}
	}

	//////////////////////////////////////////////////////////////////////

	private final Set<Id<Person>> identifyNonHomeBasedPlans(final Population plans, final Map<Id<Person>,String> person_strings) {
		Set<Id<Person>> ids = new HashSet<>();
		for (Person p : plans.getPersons().values()) {
			Plan plan = p.getSelectedPlan();
			ActivityImpl last = (ActivityImpl)plan.getPlanElements().get(plan.getPlanElements().size()-1);
			if (!last.getType().equals(HOME)) { ids.add(p.getId()); }
		}
		return ids;
	}

	//////////////////////////////////////////////////////////////////////

	private final Set<Id<Person>> identifyPlansWithTypeOther(final Population plans, final Map<Id<Person>,String> person_strings) {
		Set<Id<Person>> ids = new HashSet<>();
		for (Person p : plans.getPersons().values()) {
			Plan plan = p.getSelectedPlan();
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {
					Activity act = (Activity) pe;
					if (act.getType().equals(OTHR)) { ids.add(p.getId()); }
				}
			}
		}
		return ids;
	}

	//////////////////////////////////////////////////////////////////////

	private final Set<Id<Person>> identifyPlansModeTypeUndef(final Population plans, final Map<Id<Person>,String> person_strings) {
		Set<Id<Person>> ids = new HashSet<>();
		for (Person p : plans.getPersons().values()) {
			Plan plan = p.getSelectedPlan();
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Leg) {
					Leg leg = (Leg) pe;
					if (leg.getMode().equals(UNDF)) { ids.add(p.getId()); }
				}
			}
		}
		return ids;
	}

	//////////////////////////////////////////////////////////////////////

	private final Set<Id<Person>> identifyPlansWithRoundTrips(final Population plans, final Map<Id<Person>,String> person_strings) {
		Set<Id<Person>> ids = new HashSet<>();
		for (Person p : plans.getPersons().values()) {
			Plan plan = p.getSelectedPlan();
			Activity prevAct = null;
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {
					Activity act = (Activity) pe;
					if (prevAct != null) {
						Coord prevc = prevAct.getCoord();
						Coord currc = act.getCoord();
						if ((currc.getX()==prevc.getX())&& (currc.getY()==currc.getY())) { ids.add(p.getId()); }
					}
					prevAct = act;
				}
			}
		}
		return ids;
	}

	//////////////////////////////////////////////////////////////////////

	private final void removingRoundTrips(final Population plans, final Map<Id<Person>,String> person_strings) {
		int cnt_sametypes = 0;
		int cnt_difftypes = 0;
		int cnt_p = 0;
		for (Person p : plans.getPersons().values()) {
			boolean has_changed = false;
			Plan plan = p.getSelectedPlan();
			PlanImpl plan2 = new org.matsim.core.population.PlanImpl(p);
			
			//produces exception
			//plan2.setSelected(true);
			plan2.setScore(plan.getScore());
			plan2.addActivity((ActivityImpl)plan.getPlanElements().get(0));

			for (int i = 2; i < plan.getPlanElements().size(); i = i + 2) {
				ActivityImpl prev_act = (ActivityImpl)plan.getPlanElements().get(i-2);
				LegImpl leg = (LegImpl)plan.getPlanElements().get(i-1);
				ActivityImpl curr_act = (ActivityImpl)plan.getPlanElements().get(i);
				Coord prevc = prev_act.getCoord();
				Coord currc = curr_act.getCoord();
				
				if ((currc.getX() == prevc.getX()) && (currc.getY() == prevc.getY())) {
					ActivityImpl act2 = (ActivityImpl)plan2.getPlanElements().get(plan2.getPlanElements().size()-1);
					act2.setEndTime(curr_act.getEndTime());
					
					if (!curr_act.getType().equals(prev_act.getType())) {
						if (curr_act.getType().equals(HOME)) { 
							act2.setType(HOME);
						}
						else if (curr_act.getType().equals(WORK)) {
							if (!act2.getType().equals(HOME)) {
								act2.setType(WORK);
							}
						}
						else if (curr_act.getType().equals(EDUC)) {
							if (!act2.getType().equals(HOME) && !act2.getType().equals(WORK)) {
								act2.setType(EDUC);
							}
						}
						else if (curr_act.getType().equals(SHOP) || 
								curr_act.getType().equals(SHOP_GROCERY) || 
								curr_act.getType().equals(SHOP_NONGROCERY)) {
							if (!act2.getType().equals(HOME) && 
									!act2.getType().equals(WORK) && 
									!act2.getType().equals(EDUC)) { 
								act2.setType(SHOP);
							}
						}
						else if (curr_act.getType().equals(LEIS)) {
							if (!act2.getType().equals(HOME) && !act2.getType().equals(WORK) && !act2.getType().equals(EDUC) && !act2.getType().equals(SHOP)) {
								act2.setType(LEIS);
							}
						}
						else if (curr_act.getType().equals(OTHR)) {
							throw new RuntimeException("pid="+p.getId()+", act_type="+OTHR+": Act type not allowed here!");
						}
						else { throw new RuntimeException("That must not happen!"); }
						cnt_difftypes++;
					}
					else {
						cnt_sametypes++;
					}
					has_changed = true;
				}
				else {
					plan2.addLeg(leg);
					plan2.addActivity(curr_act);
				}
			}
			if (has_changed) { cnt_p++; }
			p.getPlans().clear();
			p.addPlan(plan2);
			p.setSelectedPlan(plan2);

			// complete the last act with time info
			if (p.getSelectedPlan().getPlanElements().size() == 1) {
				ActivityImpl act = (ActivityImpl)p.getSelectedPlan().getPlanElements().get(0);
				act.setStartTime(0); //act.setDuration(24*3600); 
				act.setEndTime(24*3600);
			}
		}
		log.info("        # round trips removed (with same act types) = " + cnt_sametypes);
		log.info("        # round trips removed (with diff act types) = " + cnt_difftypes);
		log.info("        # persons with removed round trips = " + cnt_p);
	}

	//////////////////////////////////////////////////////////////////////

	private final Set<Id<Person>> identifyPlansNegDistance(final Population plans, final Map<Id<Person>,String> person_strings) {
		Set<Id<Person>> ids = new HashSet<>();
		for (Person p : plans.getPersons().values()) {
			Plan plan = p.getSelectedPlan();
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Leg) {
					Leg leg = (Leg) pe;
					if (leg.getRoute().getDistance() < 0) { ids.add(p.getId()); }
				}
			}
		}
		return ids;
	}

	//////////////////////////////////////////////////////////////////////

	private final Set<Id<Person>> identifyPlansWithNegCoords(final Population plans, final Map<Id<Person>,String> person_strings) {
		Set<Id<Person>> ids = new HashSet<>();
		for (Person p : plans.getPersons().values()) {
			Plan plan = p.getSelectedPlan();
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof ActivityImpl) {
					ActivityImpl act = (ActivityImpl) pe;
					if ((act.getCoord().getX()<0) || (act.getCoord().getY()<0)) { ids.add(p.getId()); }
				}
			}
		}
		return ids;
	}

	//////////////////////////////////////////////////////////////////////

	private final Set<Id<Person>> identifyPlansWithTooLongWalkTrips(final Population plans, final Map<Id<Person>,String> person_strings) {
		Set<Id<Person>> ids = new HashSet<>();
		for (Person p : plans.getPersons().values()) {
			Plan plan = p.getSelectedPlan();
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Leg) {
					Leg leg = (Leg) pe;
					if ((leg.getMode().equals(TransportMode.walk))&&(leg.getRoute().getDistance()>10000.0)) {ids.add(p.getId()); }
				}
			}
		}
		return ids;
	}

	//////////////////////////////////////////////////////////////////////

	private final Set<Id<Person>> identifyPlansInconsistentTimes(final Population plans, final Map<Id<Person>,String> person_strings) {
		Set<Id<Person>> ids = new HashSet<>();
		for (Person p : plans.getPersons().values()) {
			Plan plan = p.getSelectedPlan();
			for (int i=0; i<plan.getPlanElements().size()-2; i=i+2) {
				ActivityImpl act = (ActivityImpl)plan.getPlanElements().get(i);
				if (act.getEndTime()<act.getStartTime()) { ids.add(p.getId()); }
			}
		}
		return ids;
	}

	//////////////////////////////////////////////////////////////////////

	private final Set<Id<Person>> identifySingleActPlans(final Population plans, final Map<Id<Person>,String> person_strings) {
		Set<Id<Person>> ids = new HashSet<>();
		for (Person p : plans.getPersons().values()) {
			Plan plan = p.getSelectedPlan();
			if (plan.getPlanElements().size() <= 1) { ids.add(p.getId()); }
		}
		return ids;
	}
	
	//////////////////////////////////////////////////////////////////////
	//remove routes again
	private final void removeRoutes(final Population plans) {
		for (Person p : plans.getPersons().values()) {
			Plan plan = p.getSelectedPlan();
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Leg) {
					((Leg)pe).setRoute(null);
				}
			}
		}
	}
}
