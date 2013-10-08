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
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;


public class ConvertThurgau2Plans {
	private static final String HOME = "home";
	private static final String WORK = "work";
	private static final String EDUCATION = "education";
	private static final String BUSINESS = "business";
	private static final String LEISURE = "leisure";
	private static final String SHOP = "shop";	
	
	private static final String MALE = "m";
	private static final String FEMALE = "f";
	
	private ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

	private String inputfileF3;
	private String inputfileF2;
	private String outputfile;
	
	private TreeMap<Id, PersonWeeks> personWeeks = new TreeMap<Id, PersonWeeks>();
		
	private final static Logger log = Logger.getLogger(ConvertThurgau2Plans.class);
	
	public static void main(final String[] args) {
		if (args.length != 3) {
			log.error("Provide correct number of arguments ...");
			System.exit(-1);
		}
		
		ConvertThurgau2Plans creator = new ConvertThurgau2Plans();
		creator.run(args[0], args[1], args[2]);
	}
			
	// ------------------------------------------------------------------------------------------------
	
	public void run(String inputfileF2, String inputfileF3, String outPath) {
		this.inputfileF2 = inputfileF2;
		this.inputfileF3 = inputfileF3;
		this.outputfile = outPath + "/plansThurgau.xml";
				
		try {
			this.convert(outPath);
		} catch (Exception e) {
			e.printStackTrace();
		}		
		log.info("Scenario creation finished \n ----------------------------------------------------");
	}

	private void addPlans(final Map<Id, String> person_strings) {		
		for (Id pid : person_strings.keySet()) {
			String person_string = person_strings.get(pid);	
						
			int dow_prev = -1;
			Id pid_prev = null;			
			String[] lines = person_string.split("\n", -1); // last line is always an empty line
			for (int l = 0; l < lines.length-1; l++) {				
				String[] entrs = lines[l].split("\t", -1);
				int dow = Integer.parseInt(entrs[22].trim()) - 1;
				
				// add new plan if day of week changes or person changes
				if (dow != dow_prev || !pid.equals(pid_prev)) {
					this.addPlan(pid, entrs, dow);
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
		int departure = Integer.parseInt(dp[0].trim()) * 3600 + Integer.parseInt(dp[1].trim()) * 60 + 5; // add 5 seconds to not have div by 0
		
		// arrival time (min => sec.)
		String ar[] = (entrs[7].trim()).split(":", -1);
		int arrival = Integer.parseInt(ar[0].trim()) * 3600 + Integer.parseInt(ar[1].trim()) * 60;
			

		// destination activity type -------------------------------------------------
		int purpose = Integer.parseInt(entrs[24].trim());
		String acttype = null;
		if 		(purpose == 1) { acttype = LEISURE; }	// Pick up/Drop off
		else if (purpose == 2) { acttype = LEISURE; }	// Private business
		else if (purpose == 3) { acttype = BUSINESS; }	// Work related
		else if (purpose == 4) { acttype = EDUCATION; }	// School
		else if (purpose == 5) { acttype = WORK; }		// Work
		else if (purpose == 6) { acttype = SHOP; }		// Shopping daily
		else if (purpose == 7) { acttype = SHOP; }		// Shopping long-term
		else if (purpose == 8) { acttype = LEISURE; }	// Leisure
		else if (purpose == 9) { acttype = LEISURE; }	// Other
		else if (purpose == 10) { acttype = HOME; }		// Home
		else { throw new RuntimeException("pid=" + pid + ": purpose=" + purpose + " not known!"); }
		
		// trip mode type --------------------------------------------------------------				
		if (entrs[29].trim().equals("")) {
			throw new RuntimeException("no mode for pid = " + pid + " trip id = " + entrs[0].trim() + " " + entrs[30].trim());
		}				
		int m = Integer.parseInt(entrs[29].trim());
		String mode = null;
		if (m == 0) 	 { mode = TransportMode.car; }		// Unkown
		else if (m == 1) { mode = TransportMode.pt; }	// Rail
		else if (m == 2) { mode = TransportMode.pt; }	// Bus
		else if (m == 3) { mode = TransportMode.car; }	// Car driver
		else if (m == 4) { mode = TransportMode.car;}	// Car passenger
		else if (m == 5) { mode = TransportMode.car; }	// Motorcycle
		else if (m == 6) { mode = TransportMode.bike; }	// Cycle
		else if (m == 7) { mode = TransportMode.walk; }	// Walking
		else if (m == 8) { mode = TransportMode.car; }			// Other
		else {
			throw new RuntimeException("pid = " + pid + ": m = " + m + " not known!");
		}
		
		// STARTING ALL PLANS WITH CAR NOW DUE TO INCONSISTENCIES! -> needs to be captured by replanning
		mode = TransportMode.car;
	
		// adding acts/legs
		if (plan.getPlanElements().size() != 0) { // already lines parsed and added
			ActivityImpl from_act = (ActivityImpl)plan.getPlanElements().get(plan.getPlanElements().size()-1);
			from_act.setEndTime(departure);
			((PlanImpl) plan).createAndAddLeg(mode);
			ActivityImpl act = ((PlanImpl) plan).createAndAddActivity(acttype);
			act.setStartTime(arrival);			
		}
		else {
			ActivityImpl homeAct = ((PlanImpl) plan).createAndAddActivity(HOME);
			homeAct.setEndTime(departure);
			((PlanImpl) plan).createAndAddLeg(mode);
			ActivityImpl act = ((PlanImpl) plan).createAndAddActivity(acttype);
			act.setStartTime(arrival);
		}
	}
	
	private Plan addPlan(Id pid, String[] entrs, int dow) {
		// creating/getting plan	
		PersonImpl person = (PersonImpl) this.scenario.getPopulation().getPersons().get(pid);
		person.createAndAddPlan(true);
		Plan plan = person.getSelectedPlan();
						
		plan.setScore(dow * 1.0); // used plans score as a storage for the person weight of the MZ2000
		return plan;
	}

	public void convert(String outPath) throws Exception {
		
		Population population = this.scenario.getPopulation();
		
		if (population.getName() == null) {
			population.setName("created by '" + this.getClass().getName() + "'");
		}
		if (!population.getPersons().isEmpty()) {
			throw new RuntimeException("[population=" + population + " is not empty]");
		}
		Map<Id,String> person_strings = new TreeMap<Id, String>();
		int id = Integer.MIN_VALUE;
		int prev_id = Integer.MIN_VALUE;
		Id prev_pid = new IdImpl(prev_id);
		String person_string = "";
		log.info("      parsing persons...");

		FileReader fr = new FileReader(this.inputfileF3);
		BufferedReader br = new BufferedReader(fr);
		String[] entrs = null;
		String curr_line = br.readLine(); // Skip header
		while ((curr_line = br.readLine()) != null) {
			entrs = curr_line.split("\t", -1);
			
			id = Integer.parseInt(entrs[2].trim());
						
			if (id == prev_id) {
				person_string = person_string + curr_line + "\n";
			}
			else {
				if (prev_id != Integer.MIN_VALUE) {
					if (person_strings.put(prev_pid, person_string) != null) {
						throw new RuntimeException("Person id=" + prev_pid + " already parsed!");
					}
				}
				person_string = curr_line + "\n";
				prev_id = id;
				prev_pid = new IdImpl(prev_id);
			}
		}
		if (person_strings.put(prev_pid, person_string) != null) {
			throw new RuntimeException("Person id=" + prev_pid + " already parsed!");
		}
		br.close();
		fr.close();
		log.info("      done.");
		log.info("      # persons parsed = " + person_strings.size());

		this.createPersons(outPath);
		
		this.addPlans(person_strings);
		
		this.clean(person_strings);
		
		this.createPersonWeeks(entrs);
			
		for (Person person : this.scenario.getPopulation().getPersons().values()) {
			this.personWeeks.get(person.getId()).removeIncompleteWeeks();
		}
		this.removeIncompletePersons();
				
		this.write();
	}
	
	private void createPersonWeeks(String[] entrs) {
		int counter = 0;
		int nextMsg = 1;
		for (Person person : this.scenario.getPopulation().getPersons().values()) {
			counter++;
			if (counter % nextMsg == 0) {
				nextMsg *= 2;
				log.info(" person # " + counter);
			}					
			Id pid = person.getId();
			this.personWeeks.put(pid, new PersonWeeks(this.scenario.getPopulation().getPersons().get(pid)));
			
			// micro census 2000 person weight
			double pweight = Double.parseDouble(entrs[89].trim());						
			// micro census 2000 trip weight
			// double tweight = Double.parseDouble(entrs[90].trim());
			this.personWeeks.get(pid).setPweight(pweight);
			
			double prev_score = 1000.0;
			for (Plan plan : person.getPlans()) {				
				if (plan.getScore() <= prev_score) {
					this.personWeeks.get(pid).increaseWeek();	
				}
				prev_score = plan.getScore();
				this.personWeeks.get(pid).addDay((int) Math.floor(plan.getScore()), plan);
			}		
			this.personWeeks.get(pid).setIsWorker();
		}
	}
	
	private void createPersons(String outPath) throws Exception {
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
				throw new RuntimeException("pid=" + id + ": g=" + g + " not known!");
			}
//			String houseHoldIncomeString = entrs[95].trim();
//			int householdIncome = -99;
//			if (!houseHoldIncomeString.equals("")) {
//				householdIncome = Integer.parseInt(houseHoldIncomeString);
//			}			
			PersonImpl person = new PersonImpl(id);
			person.setAge(age);
			person.setSex(gender);
			population.addPerson(person);
		}
	}
		
	private void write() {
		log.info("Writing population with plans ...");
		log.info("Number of persons: " + this.scenario.getPopulation().getPersons().size());
		new PopulationWriter(this.scenario.getPopulation(), scenario.getNetwork()).write(this.outputfile);
	}
	
	private void clean(Map<Id,String> person_strings) {
		
		Population population = this.scenario.getPopulation();

		log.info("      remove plans with inconsistent times...");
		this.removePlansInconsistentTimes(population);

		log.info("      remove non home based day plans...");
		this.removeNonHomeBasedPlans(population);

		// keep them
		// log.info("      remove plans with act type '"+ OTHR +"'...");
		// this.removePlansWithTypeOther(population);

		log.info("      remove plans with mode undefined ...");
		this.removePlansModeTypeUndef(population);
	
		log.info("removing routes");
		this.removeRoutes(population);
		log.info("    done.");
	}
	
	private void removeIncompletePersons() {
		Set<Id> removePersons = new HashSet<Id>();
		Population population = this.scenario.getPopulation();
		
		int originalSize = population.getPersons().size();
		
		for (Person p : population.getPersons().values()) {
			if (!this.personWeeks.get(p.getId()).hasCompleteWeek()) {
				removePersons.add(p.getId());
			}
		}
		for (Id id : removePersons) {
			population.getPersons().remove(id);
			this.personWeeks.remove(id);
		}
		log.info("removing incomplete persons: " + population.getPersons().size() + " of " + originalSize + " persons left.");
	}
	
	//////////////////////////////////////////////////////////////////////

	private final void removeNonHomeBasedPlans(final Population population) {
		int removeCnt = 0;
		Set<Plan> removePlans = new HashSet<Plan>();
		for (Person p : population.getPersons().values()) {
			for (Plan plan : p.getPlans()) {
				ActivityImpl last = (ActivityImpl)plan.getPlanElements().get(plan.getPlanElements().size()-1);
				if (!last.getType().equals(HOME)) {
					removePlans.add(plan);
					removeCnt++;
				}
			}
			p.getPlans().removeAll(removePlans);
		}
		log.info("removed " + removeCnt + " plans");
	}

	//////////////////////////////////////////////////////////////////////

	private final void removePlansWithTypeOther(final Population plans) {
		int removeCnt = 0;
		Set<Plan> removePlans = new HashSet<Plan>();
		for (Person p : plans.getPersons().values()) {
			for (Plan plan : p.getPlans()) {
				for (PlanElement pe : plan.getPlanElements()) {
					if (pe instanceof Activity) {
						Activity act = (Activity) pe;
						if (act.getType().startsWith(LEISURE)) {
							removePlans.add(plan);
							removeCnt++;
						}
					}
				}
			}
			p.getPlans().removeAll(removePlans);
		}	
		log.info("removed " + removeCnt + " plans");
	}

	//////////////////////////////////////////////////////////////////////

	private final void removePlansModeTypeUndef(final Population plans) {
		int removeCnt = 0;
		Set<Plan> removePlans = new HashSet<Plan>();
		for (Person p : plans.getPersons().values()) {
			for (Plan plan : p.getPlans()) {
				for (PlanElement pe : plan.getPlanElements()) {
					if (pe instanceof Leg) {
						Leg leg = (Leg) pe;
						if (leg.getMode().equals("undefined")) {
							removePlans.add(plan);
							removeCnt++;
						}
					}
				}
			}
			p.getPlans().removeAll(removePlans);
		}
		log.info("removed " + removeCnt + " plans");
	}

	//////////////////////////////////////////////////////////////////////

	private final void removePlansInconsistentTimes(final Population plans) {
		int removeCnt = 0;
		Set<Plan> removePlans = new HashSet<Plan>();
		for (Person p : plans.getPersons().values()) {
			for (Plan plan : p.getPlans()) {
				for (int i=0; i<plan.getPlanElements().size()-2; i=i+2) {
					ActivityImpl act = (ActivityImpl)plan.getPlanElements().get(i);
					if (act.getEndTime() < act.getStartTime()) {
						removePlans.add(plan);
						removeCnt++;
					}
				}
			}
			p.getPlans().removeAll(removePlans);
		}
		log.info("removed " + removeCnt + " plans");
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

	public TreeMap<Id, PersonWeeks> getPersonWeeks() {
		return personWeeks;
	}
}