/* *********************************************************************** *
 * project: org.matsim.*
 * PlansCreateFromNetwork.java
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

package playground.balmermi.mz;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.matsim.basic.v01.IdImpl;
import org.matsim.basic.v01.BasicPlanImpl.ActIterator;
import org.matsim.basic.v01.BasicPlanImpl.LegIterator;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.BasicLeg;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.PersonImpl;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.routes.CarRoute;
import org.matsim.population.routes.NodeCarRoute;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.geometry.CoordImpl;

public class PlansCreateFromMZ {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private static final String HOME = "h";
	private static final String WORK = "w";
	private static final String LEIS = "l";
	private static final String SHOP = "s";
	private static final String EDUC = "e";
	private static final String OTHR = "o";

	private static final String WALK = "walk";
	private static final String BIKE = "bike";
	private static final String CAR = "car";
	private static final String PT = "pt";
	private static final String RIDE = "ride";
	private static final String UNDF = "undef";

	private static final String MALE = "m";
	private static final String FEMALE = "f";

	private static final String YES = "yes";
	private static final String NO = "no";

	private final String inputfile;
	private final String outputfile;

	private final int dow_min;
	private final int dow_max;


	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PlansCreateFromMZ(final String inputfile, final String outputfile, final int dow_min, final int dow_max) {
		super();
		this.inputfile = inputfile;
		this.outputfile = outputfile;
		this.dow_min = dow_min;
		this.dow_max = dow_max;
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private final Set<Id> createPlans(final Population plans, final Map<Id,String> person_strings) throws Exception {
		Set<Id> coord_err_pids = new HashSet<Id>();

		Set<Id> pids_dow = new HashSet<Id>();

		for (Id pid : person_strings.keySet()) {
			String person_string = person_strings.get(pid);
			String person_string_new = "";

			String[] lines = person_string.split("\n", -1); // last line is always an empty line
			for (int l=0; l<lines.length-1; l++) {
				String[] entries = lines[l].split("\t", -1);
				// ID_PERSON  ID_TOUR  ID_TRIP  F58  S_X     S_Y     Z_X     Z_Y     F514  DURATION_1  DURATION_2
				// 5751       57511    5751101  540  507720  137360  493620  120600  560   20          20
				// 0          1        2        3    4       5       6       7       8     9           10

				// PURPOSE  TRIP_MODE  HHNR  ZIELPNR  WEGNR  WP                AGE  GENDER  LICENSE  DAY  TRIP_DISTANCE
				// 1        3          575   1        1      .422854535571358  30   0       1        5    21.9
				// 11       12         13    14       15     16                17   18      19       20   21

				// pid check
				if (!pid.toString().equals(entries[0].trim())) { Gbl.errorMsg("That must not happen!"); }

				// departure time (min => sec.)
				int departure = Integer.parseInt(entries[3].trim())*60;
				entries[3] = Integer.toString(departure);

				// start coordinate (round to hectare)
				Coord from = new CoordImpl(entries[4].trim(),entries[5].trim());
				from.setX(Math.round(from.getX()/100.0)*100);
				from.setY(Math.round(from.getY()/100.0)*100);
				entries[4] = Double.toString(from.getX());
				entries[5] = Double.toString(from.getY());

				// start coordinate (round to hectare)
				Coord to = new CoordImpl(entries[6].trim(),entries[7].trim());
				to.setX(Math.round(to.getX()/100.0)*100);
				to.setY(Math.round(to.getY()/100.0)*100);
				entries[6] = Double.toString(to.getX());
				entries[7] = Double.toString(to.getY());

				// departure time (min => sec.). A few arrival times are not given, setting to departure time (trav_time=0)
				int arrival = departure;
				if (!entries[8].trim().equals("")) { arrival = Integer.parseInt(entries[8].trim())*60; }
//				else { System.out.println("        pid="+pid+", tripid="+entries[2]+": no arival time for the trip given. Setting arrival := departue time."); }
				entries[8] = Integer.toString(arrival);

				// destination activity type
				int purpose = Integer.parseInt(entries[11].trim());
				String acttype = null;
				if (purpose == 1) { acttype = WORK; }
				else if (purpose == 2) { acttype = EDUC; }
				else if (purpose == 3) { acttype = SHOP; }
				else if (purpose == 4) { acttype = LEIS; }
				else if (purpose == 5) { acttype = LEIS; } // TODO: check
				else { Gbl.errorMsg("pid=" + pid + ": purpose=" + purpose + " not known!"); }

				// trip mode type
				int m = Integer.parseInt(entries[12].trim());
				BasicLeg.Mode mode = null;
				if (m == 1) { mode = BasicLeg.Mode.walk; }
				else if (m == 2) { mode = BasicLeg.Mode.bike; }
				else if (m == 3) { mode = BasicLeg.Mode.car; }
				else if (m == 4) { mode = BasicLeg.Mode.pt; }
				else if (m == 5) { mode = BasicLeg.Mode.ride; }
				else if (m == 6) { mode = BasicLeg.Mode.undefined; }
				else { Gbl.errorMsg("pid=" + pid + ": m=" + m + " not known!"); }

				// micro census person weight
				double weight = Double.parseDouble(entries[16].trim());

				// person age
				int age = Integer.parseInt(entries[17].trim());

				// person gender
				int g = Integer.parseInt(entries[18].trim());
				String gender = null;
				if (g == 0) { gender = FEMALE; }
				else if (g == 1) { gender = MALE; }
				else { Gbl.errorMsg("pid=" + pid + ": g=" + g + " not known!"); }

				// driving license
				int lic = Integer.parseInt(entries[19].trim());
				String licence = null;
				if (lic == 0) { licence = NO; }
				else if (lic == 1) { licence = YES; }
				else { Gbl.errorMsg("pid=" + pid + ": lic=" + lic + " not known!"); }

				// day of week
				int dow = Integer.parseInt(entries[20].trim());
				if ((dow < 1) || (dow > 7)) { Gbl.errorMsg("pid=" + pid + ": dow=" + dow + " not known!"); }
				if (!((this.dow_min<=dow) && (dow<=this.dow_max))) { pids_dow.add(pid); }

				// distance (km => m)
				double distance = Double.parseDouble(entries[21].trim())*1000.0;
				entries[21] = Double.toString(distance);

				// creating the line with the changed data
				String str = entries[0];
				for (int i=1; i<entries.length; i++) { str = str + "\t" + entries[i];  }
				str = str + "\n";
				person_string_new = person_string_new + str;

				// creating/getting the matsim person
				Person person = plans.getPerson(pid);
				if (person == null) {
					person = new PersonImpl(pid);
					plans.addPerson(person);
					person.setAge(age);
					person.setLicence(licence);
					person.setSex(gender);
				}

				// creating/getting plan
				Plan plan = person.getSelectedPlan();
				if (plan == null) {
					person.createPlan(true);
					plan = person.getSelectedPlan();
					plan.setScore(weight); // used plans score as a storage for the person weight of the MZ2005
				}

				// adding acts/legs
				if (plan.getActsLegs().size() != 0) { // already lines parsed and added
					Act from_act = (Act)plan.getActsLegs().get(plan.getActsLegs().size()-1);
					from_act.setEndTime(departure);
					from_act.setDuration(from_act.getEndTime()-from_act.getStartTime());
					Leg leg = plan.createLeg(mode);
					leg.setDepartureTime(departure);
					leg.setTravelTime(arrival-departure);
					leg.setArrivalTime(arrival);
					CarRoute route = new NodeCarRoute();
					leg.setRoute(route);
					route.setDist(distance);
					route.setTravelTime(leg.getTravelTime());
					Act act = plan.createAct(acttype,to);
					act.setStartTime(arrival);

					// coordinate consistency check
					if ((from_act.getCoord().getX() != from.getX()) || (from_act.getCoord().getY() != from.getY())) {
//						System.out.println("        pid=" + person.getId() + ": previous destination not equal to the current origin (dist=" + from_act.getCoord().calcDistance(from) + ")");
						coord_err_pids.add(pid);
					}
				}
				else {
					Act homeAct = plan.createAct(HOME,from);
					homeAct.setEndTime(departure);
					Leg leg = plan.createLeg(mode);
					leg.setDepartureTime(departure);
					leg.setTravelTime(arrival-departure);
					leg.setArrivalTime(arrival);
					CarRoute route = new NodeCarRoute(); 
					leg.setRoute(route);
					route.setDist(distance);
					route.setTravelTime(leg.getTravelTime());
					Act act = plan.createAct(acttype,to);
					act.setStartTime(arrival);
				}
			}
			// replacing the person string with the new data
			if (person_strings.put(pid,person_string_new) == null) { Gbl.errorMsg("That must not happen!"); }
		}

		System.out.println("        removing "+pids_dow.size()+" persons not part of the days = ["+this.dow_min+","+this.dow_max+"]...");
		this.removePlans(plans,person_strings,pids_dow);
		System.out.println("        done.");
		coord_err_pids.removeAll(pids_dow);

		return coord_err_pids;
	}

	//////////////////////////////////////////////////////////////////////

	private final void removePlans(final Population plans, final Map<Id,String> person_strings, final Set<Id> ids) {
		for (Id id : ids) {
			Person p = plans.getPersons().remove(id);
			if (p == null) { Gbl.errorMsg("pid="+id+": id not found in the plans DB!"); }
			String p_str = person_strings.remove(id);
			if (p_str == null) { Gbl.errorMsg("pid="+id+": id not found in the person_strings DB!"); }
		}
	}

	//////////////////////////////////////////////////////////////////////

	private final void setHomeLocations(final Population plans, final Map<Id,String> person_strings) {
		for (Person p : plans.getPersons().values()) {
			Plan plan = p.getSelectedPlan();
			Act home = plan.getFirstActivity();
			for (int i=2; i<plan.getActsLegs().size(); i=i+2) {
				Act act = (Act)plan.getActsLegs().get(i);
				if ((act.getCoord().getX() == home.getCoord().getX()) && (act.getCoord().getY() == home.getCoord().getY())) {
					if (!act.getType().equals(HOME)) {
						act.setType(HOME);
//						System.out.println("        pid=" + p.getId() + "; act_nr=" + (i/2) + ": set type to '"+HOME+"'");
					}
				}
			}
		}
	}

	//////////////////////////////////////////////////////////////////////

	private final Set<Id> identifyNonHomeBasedPlans(final Population plans, final Map<Id,String> person_strings) {
		Set<Id> ids = new HashSet<Id>();
		for (Person p : plans.getPersons().values()) {
			Plan plan = p.getSelectedPlan();
			Act last = (Act)plan.getActsLegs().get(plan.getActsLegs().size()-1);
			if (!last.getType().equals(HOME)) { ids.add(p.getId()); }
		}
		return ids;
	}

	//////////////////////////////////////////////////////////////////////

	private final Set<Id> identifyPlansWithTypeOther(final Population plans, final Map<Id,String> person_strings) {
		Set<Id> ids = new HashSet<Id>();
		for (Person p : plans.getPersons().values()) {
			Plan plan = p.getSelectedPlan();
			ActIterator act_it = plan.getIteratorAct();
			while (act_it.hasNext()) {
				Act act = (Act)act_it.next();
				if (act.getType().equals(OTHR)) { ids.add(p.getId()); }
			}
		}
		return ids;
	}

	//////////////////////////////////////////////////////////////////////

	private final Set<Id> identifyPlansModeTypeUndef(final Population plans, final Map<Id,String> person_strings) {
		Set<Id> ids = new HashSet<Id>();
		for (Person p : plans.getPersons().values()) {
			Plan plan = p.getSelectedPlan();
			LegIterator leg_it = plan.getIteratorLeg();
			while (leg_it.hasNext()) {
				Leg leg = (Leg)leg_it.next();
				if (leg.getMode().equals(UNDF)) { ids.add(p.getId()); }
			}
		}
		return ids;
	}

	//////////////////////////////////////////////////////////////////////

	private final Set<Id> identifyPlansWithRoundTrips(final Population plans, final Map<Id,String> person_strings) {
		Set<Id> ids = new HashSet<Id>();
		for (Person p : plans.getPersons().values()) {
			Plan plan = p.getSelectedPlan();
			ActIterator act_it = plan.getIteratorAct();
			Act prev_act = (Act)act_it.next();
			while (act_it.hasNext()) {
				Act curr_act = (Act)act_it.next();
				Coord prevc = prev_act.getCoord();
				Coord currc = curr_act.getCoord();
				if ((currc.getX()==prevc.getX())&& (currc.getY()==currc.getY())) { ids.add(p.getId()); }
				prev_act = curr_act;
			}
		}
		return ids;
	}

	//////////////////////////////////////////////////////////////////////

	private final void removingRoundTrips(final Population plans, final Map<Id,String> person_strings) {
		int cnt_sametypes = 0;
		int cnt_difftypes = 0;
		int cnt_p = 0;
		for (Person p : plans.getPersons().values()) {
			boolean has_changed = false;
			Plan plan = p.getSelectedPlan();
			Plan plan2 = new Plan(p);
			plan2.setSelected(true);
			plan2.setScore(plan.getScore());
			plan2.addAct((Act)plan.getActsLegs().get(0));

			for (int i=2; i<plan.getActsLegs().size(); i=i+2) {
				Act prev_act = (Act)plan.getActsLegs().get(i-2);
				Leg leg = (Leg)plan.getActsLegs().get(i-1);
				Act curr_act = (Act)plan.getActsLegs().get(i);
				Coord prevc = prev_act.getCoord();
				Coord currc = curr_act.getCoord();
				if ((currc.getX()==prevc.getX())&&(currc.getY()==prevc.getY())) {
					Act act2 = (Act)plan2.getActsLegs().get(plan2.getActsLegs().size()-1);
					act2.setEndTime(curr_act.getEndTime());
					act2.setDuration(act2.getEndTime()-act2.getStartTime());
//					System.out.println("        pid=" + p.getId() + ": merging act_nr="+((i-2)/2)+" with act_nr=" + (i/2) + ".");
					if (!curr_act.getType().equals(prev_act.getType())) {
						if (curr_act.getType().equals(HOME)) { act2.setType(HOME); }
						else if (curr_act.getType().equals(WORK)) {
							if (!act2.getType().equals(HOME)) { act2.setType(WORK); }
						}
						else if (curr_act.getType().equals(EDUC)) {
							if (!act2.getType().equals(HOME) && !act2.getType().equals(WORK)) { act2.setType(EDUC); }
						}
						else if (curr_act.getType().equals(SHOP)) {
							if (!act2.getType().equals(HOME) && !act2.getType().equals(WORK) && !act2.getType().equals(EDUC)) { act2.setType(SHOP); }
						}
						else if (curr_act.getType().equals(LEIS)) {
							if (!act2.getType().equals(HOME) && !act2.getType().equals(WORK) && !act2.getType().equals(EDUC) && !act2.getType().equals(SHOP)) { act2.setType(LEIS); }
						}
						else if (curr_act.getType().equals(OTHR)) {
							Gbl.errorMsg("pid="+p.getId()+", act_type="+OTHR+": Act type not allowed here!");
						}
						else { Gbl.errorMsg("That must not happen!"); }
						cnt_difftypes++;
					}
					else {
						cnt_sametypes++;
					}
					has_changed = true;
				}
				else {
					plan2.addLeg(leg);
					plan2.addAct(curr_act);
				}
			}
			if (has_changed) { cnt_p++; }
			p.getPlans().clear();
			p.addPlan(plan2);
			p.setSelectedPlan(plan2);

			// complete the last act with time info
			if (p.getSelectedPlan().getActsLegs().size() == 1) {
				Act act = (Act)p.getSelectedPlan().getActsLegs().get(0);
				act.setStartTime(0); act.setDuration(24*3600); act.setEndTime(24*3600);
			}
		}
		System.out.println("        # round trips removed (with same act types) = " + cnt_sametypes);
		System.out.println("        # round trips removed (with diff act types) = " + cnt_difftypes);
		System.out.println("        # persons with removed round trips = " + cnt_p);
	}

	//////////////////////////////////////////////////////////////////////

	private final Set<Id> identifyPlansNegDistance(final Population plans, final Map<Id,String> person_strings) {
		Set<Id> ids = new HashSet<Id>();
		for (Person p : plans.getPersons().values()) {
			Plan plan = p.getSelectedPlan();
			LegIterator leg_it = plan.getIteratorLeg();
			while (leg_it.hasNext()) {
				Leg leg = (Leg)leg_it.next();
				if (leg.getRoute().getDist() < 0) { ids.add(p.getId()); }
			}
		}
		return ids;
	}

	//////////////////////////////////////////////////////////////////////

	private final Set<Id> identifyPlansWithNegCoords(final Population plans, final Map<Id,String> person_strings) {
		Set<Id> ids = new HashSet<Id>();
		for (Person p : plans.getPersons().values()) {
			Plan plan = p.getSelectedPlan();
			ActIterator act_it = plan.getIteratorAct();
			while (act_it.hasNext()) {
				Act act = (Act)act_it.next();
				if ((act.getCoord().getX()<0) || (act.getCoord().getY()<0)) { ids.add(p.getId()); }
			}
		}
		return ids;
	}

	//////////////////////////////////////////////////////////////////////

	private final Set<Id> identifyPlansWithTooLongWalkTrips(final Population plans, final Map<Id,String> person_strings) {
		Set<Id> ids = new HashSet<Id>();
		for (Person p : plans.getPersons().values()) {
			Plan plan = p.getSelectedPlan();
			LegIterator leg_it = plan.getIteratorLeg();
			while (leg_it.hasNext()) {
				Leg leg = (Leg)leg_it.next();
				if ((leg.getMode().equals(BasicLeg.Mode.walk))&&(leg.getRoute().getDist()>10000.0)) {ids.add(p.getId()); }
			}
		}
		return ids;
	}

	//////////////////////////////////////////////////////////////////////

	private final Set<Id> identifyPlansInconsistentTimes(final Population plans, final Map<Id,String> person_strings) {
		Set<Id> ids = new HashSet<Id>();
		for (Person p : plans.getPersons().values()) {
			Plan plan = p.getSelectedPlan();
			for (int i=0; i<plan.getActsLegs().size()-2; i=i+2) {
				Act act = (Act)plan.getActsLegs().get(i);
				if (act.getEndTime()<act.getStartTime()) { ids.add(p.getId()); }
			}
		}
		return ids;
	}

	//////////////////////////////////////////////////////////////////////

	private final Set<Id> identifySingleActPlans(final Population plans, final Map<Id,String> person_strings) {
		Set<Id> ids = new HashSet<Id>();
		for (Person p : plans.getPersons().values()) {
			Plan plan = p.getSelectedPlan();
			if (plan.getActsLegs().size() <= 1) { ids.add(p.getId()); }
		}
		return ids;
	}

	//////////////////////////////////////////////////////////////////////
	// run method
	//////////////////////////////////////////////////////////////////////

	public void run(final Population plans) throws Exception {
		System.out.println("    running " + this.getClass().getName() + " module...");

		if (plans.getName() == null) { plans.setName("created by '" + this.getClass().getName() + "'"); }
		if (!plans.getPersons().isEmpty()) { Gbl.errorMsg("[plans=" + plans + " is not empty]"); }

		Map<Id,String> person_strings = new TreeMap<Id,String>();
		int id = Integer.MIN_VALUE;
		int prev_id = Integer.MIN_VALUE;
		Id prev_pid = new IdImpl(prev_id);
		int line_nr = 1;
		String person_string = "";

		System.out.println("      persing persons...");

		FileReader fr = new FileReader(this.inputfile);
		BufferedReader br = new BufferedReader(fr);
		String curr_line = br.readLine(); line_nr++; // Skip header
		while ((curr_line = br.readLine()) != null) {

			String[] entries = curr_line.split("\t", -1);
			// ID_PERSON  ID_TOUR  ID_TRIP  F58  S_X     S_Y     Z_X     Z_Y     F514  DURATION_1  DURATION_2
			// 5751       57511    5751101  540  507720  137360  493620  120600  560   20          20
			// 0          1        2        3    4       5       6       7       8     9           10

			// PURPOSE  TRIP_MODE  HHNR  ZIELPNR  WEGNR  WP                AGE  GENDER  LICENSE  DAY  TRIP_DISTANCE
			// 1        3          575   1        1      .422854535571358  30   0       1        5    21.9
			// 11       12         13    14       15     16                17   18      19       20   21

			id = Integer.parseInt(entries[0].trim());
			if (id == prev_id) { person_string = person_string + curr_line + "\n"; }
			else {
				if (prev_id != Integer.MIN_VALUE) {
					if (person_strings.put(prev_pid,person_string) != null) { Gbl.errorMsg("Person id="+prev_pid+" already parsed!"); }
				}
				person_string = curr_line + "\n";
				prev_id = id;
				prev_pid = new IdImpl(prev_id);
			}
			line_nr++;
		}
		if (person_strings.put(prev_pid,person_string) != null) { Gbl.errorMsg("Person id="+prev_pid+" already parsed!"); }
		br.close();
		fr.close();
		System.out.println("      done.");

		System.out.println("      # persons parsed = " + person_strings.size());
		System.out.println();

		//////////////////////////////////////////////////////////////////////

		System.out.println("      creating plans...");
		Set<Id> pids = this.createPlans(plans,person_strings);
		System.out.println("      done.");

		System.out.println("      # persons created = " + plans.getPersons().size());
		System.out.println("      # person_strings  = " + person_strings.size());
		System.out.println("      # persons with coord inconsistency = " + pids.size());

		System.out.println("      removing persons with coord inconsistency...");
		this.removePlans(plans,person_strings,pids);
		System.out.println("      done.");

		System.out.println("      # persons left        = " + plans.getPersons().size());
		System.out.println("      # person_strings left = " + person_strings.size());
		System.out.println();

		//////////////////////////////////////////////////////////////////////

		System.out.println("      setting home locations...");
		this.setHomeLocations(plans,person_strings);
		System.out.println("      done.");

		System.out.println("      # persons        = " + plans.getPersons().size());
		System.out.println("      # person_strings = " + person_strings.size());
		System.out.println();

		//////////////////////////////////////////////////////////////////////

		System.out.println("      identify plans with inconsistent times...");
		pids = this.identifyPlansInconsistentTimes(plans,person_strings);
		System.out.println("      done.");

		System.out.println("      # persons        = " + plans.getPersons().size());
		System.out.println("      # person_strings = " + person_strings.size());
		System.out.println("      # persons with plans with inconsistent times = " + pids.size());

		System.out.println("      removing persons with plans with inconsistent times...");
		this.removePlans(plans,person_strings,pids);
		System.out.println("      done.");

		System.out.println("      # persons left        = " + plans.getPersons().size());
		System.out.println("      # person_strings left = " + person_strings.size());
		System.out.println();

		//////////////////////////////////////////////////////////////////////

		System.out.println("      identify non home based day plans...");
		pids = this.identifyNonHomeBasedPlans(plans,person_strings);
		System.out.println("      done.");

		System.out.println("      # persons        = " + plans.getPersons().size());
		System.out.println("      # person_strings = " + person_strings.size());
		System.out.println("      # persons with non home based plans = " + pids.size());

		System.out.println("      removing persons with non home based plans...");
		this.removePlans(plans,person_strings,pids);
		System.out.println("      done.");

		System.out.println("      # persons left        = " + plans.getPersons().size());
		System.out.println("      # person_strings left = " + person_strings.size());
		System.out.println();

		//////////////////////////////////////////////////////////////////////

		System.out.println("      identify plans with act type '"+OTHR+"'...");
		pids = this.identifyPlansWithTypeOther(plans,person_strings);
		System.out.println("      done.");

		System.out.println("      # persons        = " + plans.getPersons().size());
		System.out.println("      # person_strings = " + person_strings.size());
		System.out.println("      # persons with plans with act type '"+OTHR+"' = " + pids.size());

		System.out.println("      removing persons with plans with act type '"+OTHR+"'...");
		this.removePlans(plans,person_strings,pids);
		System.out.println("      done.");

		System.out.println("      # persons left        = " + plans.getPersons().size());
		System.out.println("      # person_strings left = " + person_strings.size());
		System.out.println();

		//////////////////////////////////////////////////////////////////////

		System.out.println("      identify plans with mode '"+UNDF+"'...");
		pids = this.identifyPlansModeTypeUndef(plans,person_strings);
		System.out.println("      done.");

		System.out.println("      # persons        = " + plans.getPersons().size());
		System.out.println("      # person_strings = " + person_strings.size());
		System.out.println("      # persons with plans with mode '"+UNDF+"' = " + pids.size());

		System.out.println("      removing persons with plans with mode '"+UNDF+"'...");
		this.removePlans(plans,person_strings,pids);
		System.out.println("      done.");

		System.out.println("      # persons left        = " + plans.getPersons().size());
		System.out.println("      # person_strings left = " + person_strings.size());
		System.out.println();

		//////////////////////////////////////////////////////////////////////

		System.out.println("      identify plans round trips...");
		pids = this.identifyPlansWithRoundTrips(plans,person_strings);
		System.out.println("      done.");

		System.out.println("      # persons        = " + plans.getPersons().size());
		System.out.println("      # person_strings = " + person_strings.size());
		System.out.println("      # persons with plans with round trips = " + pids.size());

		System.out.println("      removing round trips...");
		this.removingRoundTrips(plans,person_strings);
		System.out.println("      done.");

		System.out.println("      # persons left        = " + plans.getPersons().size());
		System.out.println("      # person_strings left = " + person_strings.size());
		System.out.println();

		//////////////////////////////////////////////////////////////////////

		System.out.println("      identify plans neg. route distances...");
		pids = this.identifyPlansNegDistance(plans,person_strings);
		System.out.println("      done.");

		System.out.println("      # persons        = " + plans.getPersons().size());
		System.out.println("      # person_strings = " + person_strings.size());
		System.out.println("      # persons with plans neg. route distances = " + pids.size());

		System.out.println("      removing persons with plans neg. route distances...");
		this.removePlans(plans,person_strings,pids);
		System.out.println("      done.");

		System.out.println("      # persons left        = " + plans.getPersons().size());
		System.out.println("      # person_strings left = " + person_strings.size());
		System.out.println();

		//////////////////////////////////////////////////////////////////////

		System.out.println("      identify plans neg. act coords...");
		pids = this.identifyPlansWithNegCoords(plans,person_strings);
		System.out.println("      done.");

		System.out.println("      # persons        = " + plans.getPersons().size());
		System.out.println("      # person_strings = " + person_strings.size());
		System.out.println("      # persons with plans neg. act coords = " + pids.size());

		System.out.println("      removing persons with plans neg. act coords...");
		this.removePlans(plans,person_strings,pids);
		System.out.println("      done.");

		System.out.println("      # persons left        = " + plans.getPersons().size());
		System.out.println("      # person_strings left = " + person_strings.size());
		System.out.println();

		//////////////////////////////////////////////////////////////////////

		System.out.println("      identify plans with too long walk trips...");
		pids = this.identifyPlansWithTooLongWalkTrips(plans,person_strings);
		System.out.println("      done.");

		System.out.println("      # persons        = " + plans.getPersons().size());
		System.out.println("      # person_strings = " + person_strings.size());
		System.out.println("      # persons with plans with too long walk trips = " + pids.size());

		System.out.println("      removing persons with plans with too long walk trips...");
		this.removePlans(plans,person_strings,pids);
		System.out.println("      done.");

		System.out.println("      # persons left        = " + plans.getPersons().size());
		System.out.println("      # person_strings left = " + person_strings.size());
		System.out.println();

		//////////////////////////////////////////////////////////////////////

//		System.out.println("      identify single activity plans...");
//		pids = this.identifySingleActPlans(plans,person_strings);
//		System.out.println("      done.");
//
//		System.out.println("      # persons        = " + plans.getPersons().size());
//		System.out.println("      # person_strings = " + person_strings.size());
//		System.out.println("      # persons with single activity plans = " + pids.size());
//
//		System.out.println("      removing persons with single activity plans...");
//		this.removePlans(plans,person_strings,pids);
//		System.out.println("      done.");
//
//		System.out.println("      # persons left        = " + plans.getPersons().size());
//		System.out.println("      # person_strings left = " + person_strings.size());
//		System.out.println();

		//////////////////////////////////////////////////////////////////////

//		FileWriter fw = new FileWriter(outputfile);
//		BufferedWriter bw = new BufferedWriter(fw);
//		bw.write("ID_PERSON\tID_TOUR\tID_TRIP\tF58\tS_X\tS_Y\tZ_X\tZ_Y\tF514\tDURATION_1\tDURATION_2\tPURPOSE\tTRIP_MODE\tHHNR\tZIELPNR\tWEGNR\tWP\tAGE\tGENDER\tLICENSE\tDAY\tTRIP_DISTANCE\n");
//		bw.flush();
//		for (String str : person_strings.values()) { bw.write(str); }
//		bw.close();
//		fw.close();

		System.out.println("    done.");
	}
}
