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
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

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

	private final String inputfileF3;
	private final String inputfileF2;
	private final String outputfile;
	
	private TreeMap<Id, PersonWeeks> personWeeks = new TreeMap<Id, PersonWeeks>();
	
	private final static Logger log = Logger.getLogger(ConvertThurgau2Plans.class);
	
	public static void main(final String[] args) {		
		if (args.length != 0) {
			log.error("Provide correct number of arguments ...");
			System.exit(-1);
		}
		
		// hard-coded for the moment
		String inputF2 = "C:/l/studies/surprice/thurgau_2003_public_F2.dat";
		String inputF3 = "C:/l/studies/surprice/thurgau_2003_public_F3.dat";
		String output = "C:/l/studies/surprice/plansThurgau.xml";
		
		//ConvertThurgau2Plans scenarioCreator = new ConvertThurgau2Plans(args[0], args[1], args[2]);	
		ConvertThurgau2Plans scenarioCreator = new ConvertThurgau2Plans(inputF2, inputF3, output);
		
		try {
			scenarioCreator.run();
		} catch (Exception e) {
			e.printStackTrace();
		}
		log.info("Scenario creation finished \n ----------------------------------------------------");
	}
	
	// ------------------------------------------------------------------------------------------------

	public ConvertThurgau2Plans(final String inputfileF2, final String inputfileF3, final String outputfile) {
		this.inputfileF2 = inputfileF2;
		this.inputfileF3 = inputfileF3;
		this.outputfile = outputfile;
	}
	
	// ------------------------------------------------------------------------------------------------

	private void addPlans(final Map<Id, String> person_strings) {		
		for (Id pid : person_strings.keySet()) {
			String person_string = person_strings.get(pid);
			
			int dow_prev = -1;
			Id pid_prev = null;
			int week_prev = -1;
			
			String[] lines = person_string.split("\n", -1); // last line is always an empty line
			for (int l = 0; l < lines.length-1; l++) {
				
				String[] entrs = lines[l].split("\t", -1);

				int dow = Integer.parseInt(entrs[22].trim());
				if ((dow < 1) || (dow > 7)) {
					Gbl.errorMsg("pid=" + pid + ": dow=" + dow + " not known!"); 
				}
				
				// add new plan if day of week changes or person changes
				if (dow != dow_prev || !pid.equals(pid_prev)) {
					Plan plan = this.addPlan(pid, entrs, dow);
					int week = Integer.parseInt(entrs[21].trim());
					
					if (week != week_prev) {
						if (this.personWeeks.get(pid) == null) {
							this.personWeeks.put(pid, new PersonWeeks());
						}
						this.personWeeks.get(pid).increaseWeek();
					}
					this.personWeeks.get(pid).addDay(dow, plan);
					week_prev = week;
				}
				this.addTripsAndActs(pid, entrs);
				
				pid_prev = pid;
				dow_prev = dow;
			}
		}
	}
	
	private void addTripsAndActs(Id pid, String[] entrs) {
		PersonImpl person = (PersonImpl) this.scenario.getPopulation().getPersons().get(pid);
		Plan plan = person.getSelectedPlan();
		
		// departure time (min => sec.)
		String dp[] = (entrs[6].trim()).split(":", -1);
		int departure = Integer.parseInt(dp[0].trim()) * 3600 + Integer.parseInt(dp[1].trim()) * 60;
		
		// arrival time (min => sec.)
		String ar[] = (entrs[7].trim()).split(":", -1);
		int arrival = Integer.parseInt(ar[0].trim()) * 3600 + Integer.parseInt(ar[1].trim()) * 60;
			

		// destination activity type -------------------------------------------------
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
		
		// trip mode type --------------------------------------------------------------				
		if (entrs[29].trim().equals("")) {
			Gbl.errorMsg("no mode for pid = " + pid + " trip id = " + entrs[0].trim() + " " + entrs[30].trim());
		}				
		int m = Integer.parseInt(entrs[29].trim());
		String mode = null;
		if (m == 0) 	 { mode = "undefined"; }	// Unkown
		else if (m == 1) { mode = TransportMode.pt; }	// Rail
		else if (m == 2) { mode = TransportMode.pt; }	// Bus
		else if (m == 3) { mode = TransportMode.car; }	// Car driver
		else if (m == 4) { mode = TransportMode.ride; }	// Car passenger
		else if (m == 5) { mode = TransportMode.car; }	// Motorcycle
		else if (m == 6) { mode = TransportMode.bike; }	// Cycle
		else if (m == 7) { mode = TransportMode.walk; }	// Walking
		else if (m == 8) { mode = "undefined"; }		// Other
		else {
			Gbl.errorMsg("pid = " + pid + ": m = " + m + " not known!");
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
	
	private Plan addPlan(Id pid, String[] entrs, int dow) {
		// creating/getting plan	
		PersonImpl person = (PersonImpl) this.scenario.getPopulation().getPersons().get(pid);
		person.createAndAddPlan(true);
		Plan plan = person.getSelectedPlan();
				
		// micro census 2000 person weight
		double pweight = Double.parseDouble(entrs[89].trim());
		
		// micro census 2000 trip weight
		// double tweight = Double.parseDouble(entrs[90].trim());
		
		plan.setScore(pweight + dow * 100); // used plans score as a storage for the person weight of the MZ2000
		return plan;
	}

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

		FileReader fr = new FileReader(this.inputfileF3);
		BufferedReader br = new BufferedReader(fr);
		String curr_line = br.readLine(); // Skip header
		while ((curr_line = br.readLine()) != null) {
			String[] entrs = curr_line.split("\t", -1);

			id = Integer.parseInt(entrs[2].trim());
			if (id == prev_id) {
				person_string = person_string + curr_line + "\n";
			}
			else {
				if (prev_id != Integer.MIN_VALUE) {
					if (person_strings.put(prev_pid,person_string) != null) {
						Gbl.errorMsg("Person id="+prev_pid+" already parsed!");
					}
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

		this.createPersons();
		
		this.addPlans(person_strings);
		
		this.clean(person_strings);
				
		this.write();
	}
	
	private void createPersons() throws Exception {
		
		Population population = this.scenario.getPopulation();
		
		FileReader fr = new FileReader(this.inputfileF2);
		BufferedReader br = new BufferedReader(fr);
		String curr_line = br.readLine(); // Skip header
		while ((curr_line = br.readLine()) != null) {
			String[] entrs = curr_line.split("\t", -1);

			Id id = new IdImpl(Integer.parseInt(entrs[0].trim()));
			
			// person age
			int age = Integer.parseInt(entrs[4].trim());

			// person gender
			int g = Integer.parseInt(entrs[3].trim());
			String gender = null;
			if (g == 0) {
				gender = FEMALE;
			}
			else if (g == 1) {
				gender = MALE;
			}
			else { 
				Gbl.errorMsg("pid=" + id + ": g=" + g + " not known!");
			}			
			PersonImpl person = new PersonImpl(id);
			person.setAge(age);
			person.setSex(gender);
			population.addPerson(person);
		}
	}
	
	private void write() {
		log.info("Writing population with plans ...");
		new PopulationWriter(this.scenario.getPopulation(), scenario.getNetwork()).write(this.outputfile);
		
	}
	
	private void clean(Map<Id,String> person_strings) {
		
		Population population = this.scenario.getPopulation();
		
		log.info("      creating population...");
		Set<Id> pids = null;

		//////////////////////////////////////////////////////////////////////

		log.info("      identify plans with inconsistent times...");
		pids = this.identifyPlansInconsistentTimes(population);
		log.info("      done.");
		log.info("      # persons        = " + population.getPersons().size());
		log.info("      # person_strings = " + person_strings.size());
		log.info("      # persons with plans with inconsistent times = \t" + pids.size());
		log.info("      removing persons with plans with inconsistent times...");
		this.removePersons(pids);
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
		this.removePersons(pids);
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
		this.removePersons(pids);
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
		this.removePersons(pids);
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
		this.removePersons(pids);
		log.info("      done.");
		log.info("-------------------------------------------------------------");
		log.info("      # persons left        = " + population.getPersons().size());

		//////////////////////////////////////////////////////////////////////		
		log.info("removing routes");
		this.removeRoutes(population);
		log.info("    done.");
	}
	
	//////////////////////////////////////////////////////////////////////

	private final void removePersons(final Set<Id> ids) {
		
		Population population = this.scenario.getPopulation();
		
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

	private final Set<Id> identifyPlansInconsistentTimes(final Population plans) {
		Set<Id> ids = new HashSet<Id>();
		for (Person p : plans.getPersons().values()) {
			Plan plan = p.getSelectedPlan();
			for (int i=0; i<plan.getPlanElements().size()-2; i=i+2) {
				ActivityImpl act = (ActivityImpl)plan.getPlanElements().get(i);
				if (act.getEndTime() < act.getStartTime()) {
					ids.add(p.getId());
				}
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
}