/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.anhorni.surprice.preprocess.rwscenario;

import java.io.BufferedReader;
import java.io.FileReader;
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
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.geometry.CoordImpl;

public class ConvertThurgau2Plans {

	private static final String HOME = "h";
	private static final String WORK = "w";
	private static final String LEIS = "l";
	private static final String SHOP = "s";	
	private static final String EDUC = "e";
	private static final String OTHR = "o";

	private static final String MALE = "m";
	private static final String FEMALE = "f";

	private static final String YES = "yes";
	private static final String NO = "no";

	private final String inputfile;
	private final String outputfile;

	private final int dow_min;
	private final int dow_max;
	
	private final static Logger log = Logger.getLogger(ConvertThurgau2Plans.class);

	public ConvertThurgau2Plans(final String inputfile, final String outputfile, final int dow_min, final int dow_max) {
		super();
		this.inputfile = inputfile;
		this.outputfile = outputfile;
		this.dow_min = dow_min;
		this.dow_max = dow_max;
	}

	private final Set<Id> createPlans(final Population plans, final Map<Id,String> person_strings) throws Exception {
		Set<Id> coord_err_pids = new HashSet<Id>();

		Set<Id> pids_dow = new HashSet<Id>();

		for (Id pid : person_strings.keySet()) {
			String person_string = person_strings.get(pid);
			String person_string_new = "";

			String[] lines = person_string.split("\n", -1); // last line is always an empty line
			for (int l=0; l<lines.length-1; l++) {
				String[] entries = lines[l].split("\t", -1);

				// pid check
				if (!pid.toString().equals(entries[0].trim())) { Gbl.errorMsg("That must not happen!"); }

				// departure time (min => sec.)
				int departure = Integer.parseInt(entries[3].trim()) * 60;
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
				entries[8] = Integer.toString(arrival);

				// destination activity type ---------------------
				int purpose = Integer.parseInt(entries[11].trim());
				String acttype = null;
				if (purpose == 1) { acttype = WORK; }
				else if (purpose == 2) { acttype = EDUC; }
				else if (purpose == 3) { acttype = SHOP; }
				else if (purpose == 4) { acttype = LEIS; }
				else if (purpose == 5) { acttype = LEIS; }
				else if (purpose == 3) { acttype = SHOP; }
				else { Gbl.errorMsg("pid=" + pid + ": purpose=" + purpose + " not known!"); }

				// trip mode type ---------------------------------
				int m = Integer.parseInt(entries[12].trim());
				String mode = null;
				if (m == 1) { mode = TransportMode.walk; }
				else if (m == 2) { mode = TransportMode.bike; }
				else if (m == 3) { mode = TransportMode.car; }
				else if (m == 4) { mode = TransportMode.pt; }
				else if (m == 5) { mode = TransportMode.ride;}
				else if (m == 6) { mode = "undefined"; }
				else { Gbl.errorMsg("pid=" + pid + ": m=" + m + " not known!"); }

				// micro census person weight
				double weight = Double.parseDouble(entries[16].trim());

				// person age
				int age = Integer.parseInt(entries[17].trim());

				// person gender
				int g = Integer.parseInt(entries[18].trim());
				String gender = null;
				if (g == 0) {
					gender = FEMALE;
				}
				else if (g == 1) {
					gender = MALE;
				}
				else { 
					Gbl.errorMsg("pid=" + pid + ": g=" + g + " not known!");
				}
				// day of week
				int dow = Integer.parseInt(entries[20].trim());
				if ((dow < 1) || (dow > 7)) { Gbl.errorMsg("pid=" + pid + ": dow=" + dow + " not known!"); }
				if (!((this.dow_min<=dow) && (dow<=this.dow_max))) { pids_dow.add(pid); }

				// distance (km => m)
				double distance = Double.parseDouble(entries[21].trim())*1000.0;
				entries[21] = Double.toString(distance);
				
				// creating the line with the changed data ------------------------
				String str = entries[0];
				for (int i=1; i<entries.length; i++) { str = str + "\t" + entries[i];  }
				str = str + "\n";
				person_string_new = person_string_new + str;

				// creating/getting the MATSim person
				PersonImpl person = (PersonImpl) plans.getPersons().get(pid);
				if (person == null) {
					person = new PersonImpl(pid);
					plans.addPerson(person);
					person.setAge(age);
					person.setSex(gender);
				}

				// creating/getting plan
				Plan plan = person.getSelectedPlan();
				if (plan == null) {
					person.createAndAddPlan(true);
					plan = person.getSelectedPlan();
					plan.setScore(weight); // used plans score as a storage for the person weight of the MZ2000
				}

				// adding acts/legs
				if (plan.getPlanElements().size() != 0) { // already lines parsed and added
					ActivityImpl from_act = (ActivityImpl)plan.getPlanElements().get(plan.getPlanElements().size()-1);
					from_act.setEndTime(departure);
					//from_act.setDuration(from_act.getEndTime()-from_act.getStartTime());
					LegImpl leg = ((PlanImpl) plan).createAndAddLeg(mode);
					leg.setDepartureTime(departure);
					leg.setTravelTime(arrival-departure);
					leg.setArrivalTime(arrival);
					NetworkRoute route = new LinkNetworkRouteImpl(null, null);
					leg.setRoute(route);
					route.setDistance(distance);
					route.setTravelTime(leg.getTravelTime());
					ActivityImpl act = ((PlanImpl) plan).createAndAddActivity(acttype,to);
					act.setStartTime(arrival);

					// coordinate consistency check
					if ((from_act.getCoord().getX() != from.getX()) || (from_act.getCoord().getY() != from.getY())) {
						coord_err_pids.add(pid);
					}
				}
				else {
					ActivityImpl homeAct = ((PlanImpl) plan).createAndAddActivity(HOME,from);
					homeAct.setEndTime(departure);
					LegImpl leg = ((PlanImpl) plan).createAndAddLeg(mode);
					leg.setDepartureTime(departure);
					leg.setTravelTime(arrival-departure);
					leg.setArrivalTime(arrival);
					NetworkRoute route = new LinkNetworkRouteImpl(null, null);
					leg.setRoute(route);
					route.setDistance(distance);
					route.setTravelTime(leg.getTravelTime());
					ActivityImpl act = ((PlanImpl) plan).createAndAddActivity(acttype,to);
					act.setStartTime(arrival);
				}
			}
			// replacing the person string with the new data
			if (person_strings.put(pid,person_string_new) == null) { Gbl.errorMsg("That must not happen!"); }
		}

		log.info("        removing "+pids_dow.size()+" persons not part of the days = ["+this.dow_min+","+this.dow_max+"]...");
		this.removePlans(plans,person_strings,pids_dow);
		log.info("        done.");
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
			Activity home = ((PlanImpl) plan).getFirstActivity();
			for (int i=2; i<plan.getPlanElements().size(); i=i+2) {
				Activity act = (ActivityImpl)plan.getPlanElements().get(i);
				if ((act.getCoord().getX() == home.getCoord().getX()) && (act.getCoord().getY() == home.getCoord().getY())) {
					if (!act.getType().equals(HOME)) {
						act.setType(HOME);
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
			ActivityImpl last = (ActivityImpl)plan.getPlanElements().get(plan.getPlanElements().size()-1);
			if (!last.getType().equals(HOME)) { ids.add(p.getId()); }
		}
		return ids;
	}

	//////////////////////////////////////////////////////////////////////

	private final Set<Id> identifyPlansWithTypeOther(final Population plans, final Map<Id,String> person_strings) {
		Set<Id> ids = new HashSet<Id>();
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

	private final Set<Id> identifyPlansModeTypeUndef(final Population plans, final Map<Id,String> person_strings) {
		Set<Id> ids = new HashSet<Id>();
		for (Person p : plans.getPersons().values()) {
			Plan plan = p.getSelectedPlan();
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Leg) {
					Leg leg = (Leg) pe;
					if (leg.getMode().equals("undefined")) { ids.add(p.getId()); }
				}
			}
		}
		return ids;
	}

	//////////////////////////////////////////////////////////////////////

	private final Set<Id> identifyPlansWithRoundTrips(final Population plans, final Map<Id,String> person_strings) {
		Set<Id> ids = new HashSet<Id>();
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

	private final void removingRoundTrips(final Population plans, final Map<Id,String> person_strings) {
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

			for (int i=2; i<plan.getPlanElements().size(); i=i+2) {
				ActivityImpl prev_act = (ActivityImpl)plan.getPlanElements().get(i-2);
				LegImpl leg = (LegImpl)plan.getPlanElements().get(i-1);
				ActivityImpl curr_act = (ActivityImpl)plan.getPlanElements().get(i);
				Coord prevc = prev_act.getCoord();
				Coord currc = curr_act.getCoord();
				if ((currc.getX()==prevc.getX())&&(currc.getY()==prevc.getY())) {
					ActivityImpl act2 = (ActivityImpl)plan2.getPlanElements().get(plan2.getPlanElements().size()-1);
					act2.setEndTime(curr_act.getEndTime());
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
					plan2.addActivity(curr_act);
				}
			}
			if (has_changed) { cnt_p++; }
			p.getPlans().clear();
			p.addPlan(plan2);
			((PersonImpl) p).setSelectedPlan(plan2);

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

	private final Set<Id> identifyPlansNegDistance(final Population plans, final Map<Id,String> person_strings) {
		Set<Id> ids = new HashSet<Id>();
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

	private final Set<Id> identifyPlansWithNegCoords(final Population plans, final Map<Id,String> person_strings) {
		Set<Id> ids = new HashSet<Id>();
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

	private final Set<Id> identifyPlansWithTooLongWalkTrips(final Population plans, final Map<Id,String> person_strings) {
		Set<Id> ids = new HashSet<Id>();
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

	private final Set<Id> identifyPlansInconsistentTimes(final Population plans, final Map<Id,String> person_strings) {
		Set<Id> ids = new HashSet<Id>();
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

	private final Set<Id> identifySingleActPlans(final Population plans, final Map<Id,String> person_strings) {
		Set<Id> ids = new HashSet<Id>();
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

	//////////////////////////////////////////////////////////////////////
	// run method
	//////////////////////////////////////////////////////////////////////

	public void run(final Population plans) throws Exception {
		log.info("    running " + this.getClass().getName() + " module...");

		if (plans.getName() == null) { plans.setName("created by '" + this.getClass().getName() + "'"); }
		if (!plans.getPersons().isEmpty()) { Gbl.errorMsg("[plans=" + plans + " is not empty]"); }

		Map<Id,String> person_strings = new TreeMap<Id,String>();
		int id = Integer.MIN_VALUE;
		int prev_id = Integer.MIN_VALUE;
		Id prev_pid = new IdImpl(prev_id);
		String person_string = "";
		log.info("      parsing persons...");

		FileReader fr = new FileReader(this.inputfile);
		BufferedReader br = new BufferedReader(fr);
		String curr_line = br.readLine(); // Skip header
		while ((curr_line = br.readLine()) != null) {

			String[] entries = curr_line.split("\t", -1);

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
		}
		if (person_strings.put(prev_pid,person_string) != null) {
			Gbl.errorMsg("Person id="+prev_pid+" already parsed!");
		}
		br.close();
		fr.close();
		log.info("      done.");
		log.info("      # persons parsed = " + person_strings.size());

		//////////////////////////////////////////////////////////////////////

		log.info("      creating plans...");
		Set<Id> pids = this.createPlans(plans,person_strings);
		log.info("      done.");
		log.info("      # persons created = " + plans.getPersons().size());
		log.info("      # person_strings  = " + person_strings.size());
		log.info("      # persons with coord inconsistency = \t" + pids.size());
		log.info("      removing persons with coord inconsistency...");
		this.removePlans(plans,person_strings,pids);
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

		log.info("      identify plans with act type '"+ OTHR +"'...");
		pids = this.identifyPlansWithTypeOther(plans,person_strings);
		log.info("      done.");
		log.info("      # persons        = " + plans.getPersons().size());
		log.info("      # person_strings = " + person_strings.size());
		log.info("      # persons with plans with act type '"+ OTHR +"' = \t" + pids.size());
		log.info("      removing persons with plans with act type '"+ OTHR +"'...");
		this.removePlans(plans,person_strings,pids);
		log.info("      done.");
		log.info("-------------------------------------------------------------");
		log.info("      # persons left        = " + plans.getPersons().size());

		//////////////////////////////////////////////////////////////////////

		log.info("      identify plans with mode undefined ...");
		pids = this.identifyPlansModeTypeUndef(plans,person_strings);
		log.info("      done.");
		log.info("      # persons        = " + plans.getPersons().size());
		log.info("      # person_strings = " + person_strings.size());
		log.info("      # persons with plans with mode undefined = \t" + pids.size());
		log.info("      removing persons with plans with mode undefined ...");
		this.removePlans(plans,person_strings,pids);
		log.info("      done.");
		log.info("-------------------------------------------------------------");
		log.info("      # persons left        = " + plans.getPersons().size());

		//////////////////////////////////////////////////////////////////////

		log.info("      identify plans round trips...");
		pids = this.identifyPlansWithRoundTrips(plans,person_strings);
		log.info("      done.");
		log.info("      # persons        = " + plans.getPersons().size());
		log.info("      # person_strings = " + person_strings.size());
		log.info("      # persons with plans with round trips = \t" + pids.size());
		log.info("      removing round trips...");
		this.removingRoundTrips(plans,person_strings);
		log.info("      done.");
		log.info("-------------------------------------------------------------");
		log.info("      # persons left        = " + plans.getPersons().size());

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
}