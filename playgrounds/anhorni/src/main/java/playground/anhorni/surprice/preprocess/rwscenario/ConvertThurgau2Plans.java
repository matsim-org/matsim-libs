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
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
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
	
	private ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

	private final String inputfile;
	//private final String outputfile;
	
	private final static Logger log = Logger.getLogger(ConvertThurgau2Plans.class);
	
	public static void main(final String[] args) {		
		if (args.length != 1) {
			log.error("Provide correct number of arguments ...");
			System.exit(-1);
		}
		
		ConvertThurgau2Plans scenarioCreator = new ConvertThurgau2Plans(args[0]);	
		
		try {
			scenarioCreator.run();
		} catch (Exception e) {
			e.printStackTrace();
		}
		log.info("Scenario creation finished \n ----------------------------------------------------");
	}
	
	// ------------------------------------------------------------------------------------------------

	public ConvertThurgau2Plans(final String inputfile) {
		this.inputfile = inputfile;
	}
	
	// ------------------------------------------------------------------------------------------------

	private void createPopulationAndPlans(final Population population, final Map<Id, String> person_strings) throws Exception {
		for (Id pid : person_strings.keySet()) {
			String person_string = person_strings.get(pid);

			String[] lines = person_string.split("\n", -1); // last line is always an empty line
			for (int l = 0; l < lines.length-1; l++) {
				
				String[] entrs = lines[l].split(",", -1);

				// pid check
				if (!pid.toString().equals(entrs[2].trim())) {
					Gbl.errorMsg("That must not happen!");
				}

				// departure time (min => sec.)
				String dp[] = (entrs[6].trim()).split(",", -1);
				int departure = Integer.parseInt(dp[0].trim()) * 3600 + Integer.parseInt(dp[1].trim()) * 60;
				
				// arrival time (min => sec.)
				String ar[] = (entrs[7].trim()).split(",", -1);
				int arrival = Integer.parseInt(ar[0].trim()) * 3600 + Integer.parseInt(ar[1].trim()) * 60;
				

				// destination activity type ---------------------
				int purpose = Integer.parseInt(entrs[24].trim());
				String acttype = null;
				if 		(purpose == 1) { acttype = OTHR; }	// Pick up/Drop off
				else if (purpose == 2) { acttype = OTHR; }	// Private business
				else if (purpose == 3) { acttype = WORK; }	// Work related
				else if (purpose == 4) { acttype = EDUC; }	// School
				else if (purpose == 5) { acttype = WORK; }	// Work
				else if (purpose == 6) { acttype = SHOP; }	// Shopping daily
				else if (purpose == 7) { acttype = SHOP; }	// Shopping long-term
				else if (purpose == 8) { acttype = LEIS; }	// Leisure
				else if (purpose == 9) { acttype = OTHR; }	// Other
				else if (purpose == 10) { acttype = HOME; }	// Home
				else { Gbl.errorMsg("pid=" + pid + ": purpose=" + purpose + " not known!"); }
				
				// trip mode type ---------------------------------
				int m = Integer.parseInt(entrs[29].trim());
				String mode = null;
				if (m == 0) 	 { mode = "undefined"; }	// Unkown
				else if (m == 1) { mode = TransportMode.pt; }	// Rail
				else if (m == 2) { mode = TransportMode.pt; }	// Bus
				else if (m == 3) { mode = TransportMode.car; }	// Car driver
				else if (m == 4) { mode = TransportMode.ride; }	// Car passenger
				else if (m == 5) { mode = TransportMode.car; }	// Motorcycle
				else if (m == 6) { mode = TransportMode.bike; }	// Cycle
				else if (m == 6) { mode = TransportMode.walk; }	// Walking
				else if (m == 6) { mode = "undefined"; }		// Other
				else { Gbl.errorMsg("pid=" + pid + ": m=" + m + " not known!");
				}

				// micro census 2000 person weight
				double pweight = Double.parseDouble(entrs[89].trim());
				
				// micro census 2000 trip weight
				double tweight = Double.parseDouble(entrs[90].trim());

// get this info from 2nd file
//				// person age
//				int age = Integer.parseInt(entries[17].trim());
//
//				// person gender
//				int g = Integer.parseInt(entries[18].trim());
//				String gender = null;
//				if (g == 0) {
//					gender = FEMALE;
//				}
//				else if (g == 1) {
//					gender = MALE;
//				}
//				else { 
//					Gbl.errorMsg("pid=" + pid + ": g=" + g + " not known!");
//				}
				
				// day of week
				int dow = Integer.parseInt(entrs[22].trim());
				if ((dow < 1) || (dow > 7)) {
					Gbl.errorMsg("pid=" + pid + ": dow=" + dow + " not known!"); 
				}
				
				// creating/getting the MATSim person
				PersonImpl person = (PersonImpl) population.getPersons().get(pid);
				if (person == null) {
					person = new PersonImpl(pid);
					population.addPerson(person);
// get this info from 2nd file
//					person.setAge(age);
//					person.setSex(gender);
				}

				// creating/getting plan
				Plan plan = person.getSelectedPlan();
				if (plan == null) {
					person.createAndAddPlan(true);
					plan = person.getSelectedPlan();
					plan.setScore(pweight); // used plans score as a storage for the person weight of the MZ2000
				}

				// adding acts/legs
				if (plan.getPlanElements().size() != 0) { // already lines parsed and added
					ActivityImpl from_act = (ActivityImpl)plan.getPlanElements().get(plan.getPlanElements().size()-1);
					from_act.setEndTime(departure);
					LegImpl leg = ((PlanImpl) plan).createAndAddLeg(mode);
					leg.setDepartureTime(departure);
					leg.setTravelTime(arrival-departure);
					leg.setArrivalTime(arrival);
					ActivityImpl act = ((PlanImpl) plan).createAndAddActivity(acttype);
					act.setStartTime(arrival);
				}
				else {
					ActivityImpl homeAct = ((PlanImpl) plan).createAndAddActivity(HOME);
					homeAct.setEndTime(departure);
					LegImpl leg = ((PlanImpl) plan).createAndAddLeg(mode);
					leg.setDepartureTime(departure);
					leg.setTravelTime(arrival-departure);
					leg.setArrivalTime(arrival);
					ActivityImpl act = ((PlanImpl) plan).createAndAddActivity(acttype);
					act.setStartTime(arrival);
				}
			}
		}
	}

	//////////////////////////////////////////////////////////////////////

	private final void removePersons(final Population population, final Set<Id> ids) {
		for (Id id : ids) {
			Person p = population.getPersons().remove(id);
			if (p == null) {
				Gbl.errorMsg("pid="+id+": id not found in the plans DB!");
			}
		}
	}

	//////////////////////////////////////////////////////////////////////

	private final Set<Id> identifyNonHomeBasedPlans(final Population plans) {
		Set<Id> ids = new HashSet<Id>();
		for (Person p : plans.getPersons().values()) {
			Plan plan = p.getSelectedPlan();
			ActivityImpl last = (ActivityImpl)plan.getPlanElements().get(plan.getPlanElements().size()-1);
			if (!last.getType().equals(HOME)) { ids.add(p.getId()); }
		}
		return ids;
	}

	//////////////////////////////////////////////////////////////////////

	private final Set<Id> identifyPlansWithTypeOther(final Population plans) {
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

	private final Set<Id> identifyPlansModeTypeUndef(final Population plans) {
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

	private final Set<Id> identifyPlansWithRoundTrips(final Population plans) {
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

	private final void removingRoundTrips(final Population plans) {
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

	private final Set<Id> identifyPlansNegDistance(final Population plans) {
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

	private final Set<Id> identifyPlansWithNegCoords(final Population plans) {
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

	private final Set<Id> identifyPlansWithTooLongWalkTrips(final Population plans) {
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

	private final Set<Id> identifyPlansInconsistentTimes(final Population plans) {
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

	private final Set<Id> identifySingleActPlans(final Population plans) {
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

	public void run() throws Exception {
		
		Population population = this.scenario.getPopulation();
		
		if (population.getName() == null) {
			population.setName("created by '" + this.getClass().getName() + "'");
		}
		if (!population.getPersons().isEmpty()) {
			Gbl.errorMsg("[population=" + population + " is not empty]");
		}

		Map<Id,String> person_strings = new TreeMap<Id, String>();
		int id = Integer.MIN_VALUE;
		int prev_id = Integer.MIN_VALUE;
		Id prev_pid = new IdImpl(prev_id);
		String person_string = "";
		log.info("      parsing persons...");

		FileReader fr = new FileReader(this.inputfile);
		BufferedReader br = new BufferedReader(fr);
		String curr_line = br.readLine(); // Skip header
		while ((curr_line = br.readLine()) != null) {
			String[] entrs = curr_line.split("\t", -1);

			id = Integer.parseInt(entrs[2].trim());
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

		this.clean(population, person_strings);
	}
	
	private void clean(Population population, Map<Id,String> person_strings) {
		log.info("      creating population...");
		Set<Id> pids = null;
		try {
			this.createPopulationAndPlans(population, person_strings);
		} catch (Exception e) {
			e.printStackTrace();
		}

		//////////////////////////////////////////////////////////////////////

		log.info("      identify plans with inconsistent times...");
		pids = this.identifyPlansInconsistentTimes(population);
		log.info("      done.");
		log.info("      # persons        = " + population.getPersons().size());
		log.info("      # person_strings = " + person_strings.size());
		log.info("      # persons with planspopulation inconsistent times = \t" + pids.size());
		log.info("      removing persons with plans with inconsistent times...");
		this.removePersons(population, pids);
		log.info("      done.");
		log.info("-------------------------------------------------------------");
		log.info("      # persons left        = " + population.getPersons().size());

		//////////////////////////////////////////////////////////////////////

		log.info("      identify non home based day plans...");
		pids = this.identifyNonHomeBasedPlans(population);
		log.info("      done.");
		log.info("      # persons        = " + population.getPersons().size());
		log.info("      # person_strings = " + person_strings.size());
		log.info("      # persons with non home based plans = \t" + pids.size());
		log.info("      removing persons with non home based plans...");
		this.removePersons(population, pids);
		log.info("      done.");
		log.info("-------------------------------------------------------------");
		log.info("      # persons left        = " + population.getPersons().size());

		//////////////////////////////////////////////////////////////////////

		log.info("      identify plans with act type '"+ OTHR +"'...");
		pids = this.identifyPlansWithTypeOther(population);
		log.info("      done.");
		log.info("      # persons        = " + population.getPersons().size());
		log.info("      # person_strings = " + person_strings.size());
		log.info("      # persons with plans with act type '"+ OTHR +"' = \t" + pids.size());
		log.info("      removing persons with plans with act type '"+ OTHR +"'...");
		this.removePersons(population, pids);
		log.info("      done.");
		log.info("-------------------------------------------------------------");
		log.info("      # persons left        = " + population.getPersons().size());

		//////////////////////////////////////////////////////////////////////

		log.info("      identify plans with mode undefined ...");
		pids = this.identifyPlansModeTypeUndef(population);
		log.info("      done.");
		log.info("      # persons        = " + population.getPersons().size());
		log.info("      # person_strings = " + person_strings.size());
		log.info("      # persons with plans with mode undefined = \t" + pids.size());
		log.info("      removing persons with plans with mode undefined ...");
		this.removePersons(population, pids);
		log.info("      done.");
		log.info("-------------------------------------------------------------");
		log.info("      # persons left        = " + population.getPersons().size());

		//////////////////////////////////////////////////////////////////////

		log.info("      identify plans round trips...");
		pids = this.identifyPlansWithRoundTrips(population);
		log.info("      done.");
		log.info("      # persons        = " + population.getPersons().size());
		log.info("      # person_strings = " + person_strings.size());
		log.info("      # persons with plans with round trips = \t" + pids.size());
		log.info("      removing round trips...");
		this.removingRoundTrips(population);
		log.info("      done.");
		log.info("-------------------------------------------------------------");
		log.info("      # persons left        = " + population.getPersons().size());

		//////////////////////////////////////////////////////////////////////

		log.info("      identify plans neg. route distances...");
		pids = this.identifyPlansNegDistance(population);
		log.info("      done.");
		log.info("      # persons        = " + population.getPersons().size());
		log.info("      # person_strings = " + person_strings.size());
		log.info("      # persons with plans neg. route distances = \t" + pids.size());
		log.info("      removing persons with plans neg. route distances...");
		this.removePersons(population, pids);
		log.info("      done.");
		log.info("-------------------------------------------------------------");
		log.info("      # persons left        = " + population.getPersons().size());

		//////////////////////////////////////////////////////////////////////

		log.info("      identify plans neg. act coords...");
		pids = this.identifyPlansWithNegCoords(population);
		log.info("      done.");
		log.info("      # persons        = " + population.getPersons().size());
		log.info("      # person_strings = " + person_strings.size());
		log.info("      # persons with plans neg. act coords = \t" + pids.size());
		log.info("      removing persons with plans neg. act coords...");
		this.removePersons(population, pids);
		log.info("      done.");
		log.info("-------------------------------------------------------------");
		log.info("      # persons left        = " + population.getPersons().size());

		//////////////////////////////////////////////////////////////////////

		log.info("      identify plans with too long walk trips...");
		pids = this.identifyPlansWithTooLongWalkTrips(population);
		log.info("      done.");
		log.info("      # persons        = " + population.getPersons().size());
		log.info("      # person_strings = " + person_strings.size());
		log.info("      # persons with plans with too long walk trips = \t" + pids.size());
		log.info("      removing persons with plans with too long walk trips...");
		this.removePersons(population, pids);
		log.info("      done.");
		log.info("-------------------------------------------------------------");
		log.info("      # persons left        = " + population.getPersons().size());

		//////////////////////////////////////////////////////////////////////

		log.info("      identify single activity plans...");
		pids = this.identifySingleActPlans(population);
		log.info("      done.");
		log.info("      # persons        = " + population.getPersons().size());
		log.info("      # person_strings = " + person_strings.size());
		log.info("      # persons with single activity plans = \t" + pids.size());
		log.info("      removing persons with single activity plans...");
		this.removePersons(population, pids);
		log.info("      done.");
		log.info("-------------------------------------------------------------");
		log.info("      # persons left        = " + population.getPersons().size());

		//////////////////////////////////////////////////////////////////////		
		log.info("removing routes");
		this.removeRoutes(population);
		log.info("    done.");
	}
}