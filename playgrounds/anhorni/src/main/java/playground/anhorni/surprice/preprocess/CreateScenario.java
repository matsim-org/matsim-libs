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

package playground.anhorni.surprice.preprocess;

import org.apache.log4j.Logger;
import org.matsim.analysis.Bins;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.*;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import playground.anhorni.surprice.DayConverter;
import playground.anhorni.surprice.Surprice;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;
import java.util.Vector;


public class CreateScenario {	
	private ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());	
	private ConvertThurgau2Plans thurgauConverter = new ConvertThurgau2Plans();
	private TreeMap<Id, PersonHomeWork> personHWFacilities = new TreeMap<Id, PersonHomeWork>();
	private Random random = new Random(102830259L);
	private TreeMap<Id, PersonWeeks> personWeeksMZ = new TreeMap<Id, PersonWeeks>();
	private Config config;
	private int [] incomeCategoryFrequencies = new int[9];
	private ObjectAttributes incomes = new ObjectAttributes();
	private ObjectAttributes preferences = new ObjectAttributes();
		
	private final static Logger log = Logger.getLogger(ConvertThurgau2Plans.class);
		
	public static void main(final String[] args) {		
		if (args.length != 1) {
			log.error("Provide correct number of arguments ...");
			System.exit(-1);
		}		
		CreateScenario creator = new CreateScenario();
		creator.run(args[0]);
	}
	
	public void run(String configFile) {
		this.config = ConfigUtils.loadConfig(configFile);
		
		AdaptFacilities facilityAdapter = new AdaptFacilities();
		facilityAdapter.run(config.getModule("facilities").getValue("inputFacilitiesFile"), 
				config.findParam(Surprice.SURPRICE_PREPROCESS, "outPath") + "/facilities_incl_business.xml.gz", 
				config.getModule("network").getValue("inputNetworkFile"));
		
		// handle MZ ...........................................................
		this.readMZ(config.getModule("plans").getValue("inputPlansFile"), 
				config.getModule("network").getValue("inputNetworkFile"), 
				config.findParam(Surprice.SURPRICE_PREPROCESS, "outPath") + "/facilities_incl_business.xml.gz");
		
		this.storeHomeAndWork();
				
		// handle Thurgau ......................................................
		thurgauConverter.run(
				config.findParam(Surprice.SURPRICE_PREPROCESS, "infileF2"),
				config.findParam(Surprice.SURPRICE_PREPROCESS, "infileF3"),
				config.findParam(Surprice.SURPRICE_PREPROCESS, "outPath"));
		
		// merge ................................................................
		this.merge();
										
		this.createIncomes(config.findParam(Surprice.SURPRICE_PREPROCESS, "outPath"), 
				config.findParam(Surprice.SURPRICE_PREPROCESS, "mzIncomeFile"));
		
		this.writeWeek(config.findParam(Surprice.SURPRICE_PREPROCESS, "outPath"));
		
		this.createToll(config.findParam(Surprice.SURPRICE_PREPROCESS, "outPath"),
				Double.parseDouble(config.findParam(Surprice.SURPRICE_PREPROCESS, "tollRadius")));
		
		this.createPreferences();
	}
	
	private void readMZ(final String plansFilePath, final String networkFilePath, final String facilitiesFilePath) {
		new MatsimNetworkReader(scenario).readFile(networkFilePath);		
		new FacilitiesReaderMatsimV1(scenario).readFile(facilitiesFilePath);
		MatsimPopulationReader populationReader = new MatsimPopulationReader(this.scenario);
		populationReader.readFile(plansFilePath);
		
		log.info("Reading population, size: " + this.scenario.getPopulation().getPersons().size());
		
		this.sample();
	}
	
	private void sample() {
		
		double share = Double.parseDouble(config.getModule("surprice_preprocess").getValue("sample"));
		
		List<Person> persons = new Vector<Person>();
		
		for (Person person : this.scenario.getPopulation().getPersons().values()) {
			if (this.random.nextDouble() > (1.0 - share)) {
				persons.add(person);
			}
		}
		this.scenario.getPopulation().getPersons().clear();
		
		for (Person person : persons) {
			this.scenario.getPopulation().addPerson(person);
		}
		log.info("Scenario size: " + this.scenario.getPopulation().getPersons().size());
	}
	
	private void storeHomeAndWork() {
		int counter = 0;
		int nextMsg = 1;
		for (Person p : this.scenario.getPopulation().getPersons().values()) {			
			counter++;
			if (counter % nextMsg == 0) {
				nextMsg *= 2;
				log.info(" person # " + counter);
			}
			PlanImpl plan = (PlanImpl)p.getSelectedPlan();
			Id homeFacilityId = plan.getFirstActivity().getFacilityId();
			Id workFacilityId = this.getWorkFacilityId(plan);			
			this.personHWFacilities.put(p.getId(), new PersonHomeWork(p, homeFacilityId, workFacilityId));
		}
	}
	
	private Id getWorkFacilityId(PlanImpl plan) {
		Id workId = null;
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				ActivityImpl act = (ActivityImpl)pe;				
				if (act.getType().startsWith("w")) {
					workId = act.getFacilityId();
				}
			}
		}
		return workId;
	}
	
	private void merge() {
		// prepare data structures for probabilistic draw of chains
		// pweights + workers and non-workers
		TreeMap<Double, PersonWeeks> workersNormalized = new TreeMap<Double, PersonWeeks>(java.util.Collections.reverseOrder());
		TreeMap<Double, PersonWeeks> nonworkersNormalized = new TreeMap<Double, PersonWeeks>(java.util.Collections.reverseOrder());		
		this.prepareForDrawing(workersNormalized, nonworkersNormalized);
		
		PersonSetSecondaryLocation secLocationAssigner = new PersonSetSecondaryLocation(this.scenario.getActivityFacilities());
						
		// assign chain (acts + times) ----------------
		int counter = 0;
		int nextMsg = 1;
		for (Person p : this.scenario.getPopulation().getPersons().values()) {			
			counter++;
			if (counter % nextMsg == 0) {
				nextMsg *= 2;
				log.info(" person # " + counter);
			}			
			PersonWeeks personWeeksThurgau;
			if (this.personHWFacilities.get(p.getId()).getWorkFaciliyId() != null) {
				personWeeksThurgau = this.chooseWeek(workersNormalized);
			}
			else {
				personWeeksThurgau = this.chooseWeek(nonworkersNormalized);
			}
			PersonImpl person = (PersonImpl)p;			
			this.createPlansForPerson(person, personWeeksThurgau, secLocationAssigner);
		}				
	}
	
	private void prepareForDrawing(TreeMap<Double, PersonWeeks> workersNormalized, TreeMap<Double, PersonWeeks> nonworkersNormalized) {
		TreeMap<Id, PersonWeeks> workers = new TreeMap<Id, PersonWeeks>();
		TreeMap<Id, PersonWeeks> nonworkers = new TreeMap<Id, PersonWeeks>();		
		for (Id pid : thurgauConverter.getPersonWeeks().keySet()) {			
			if (thurgauConverter.getPersonWeeks().get(pid).isWorker()) {
				workers.put(pid, thurgauConverter.getPersonWeeks().get(pid));
			}
			else {
				nonworkers.put(pid, thurgauConverter.getPersonWeeks().get(pid));
			}
		}
		// normalize maps:
		this.normalizeMap(workersNormalized, workers);
		this.normalizeMap(nonworkersNormalized, nonworkers);
	}
		
	private void normalizeMap(TreeMap<Double, PersonWeeks> mapNormalized, TreeMap<Id, PersonWeeks> map) {		
		double sumScore = 0.0;
		
		double totalScore = this.getTotalScore(map);
		
		for (PersonWeeks personWeeks : map.values()) {
			double score = personWeeks.getPweight();
			sumScore += (score / totalScore);				
			mapNormalized.put(sumScore, personWeeks);	
		}
	}
	
	private double getTotalScore(TreeMap<Id, PersonWeeks> map) {
		double totalScore = 0.0;
		for (PersonWeeks personWeeks : map.values()) {				
			double score = personWeeks.getPweight();
			totalScore += score;
		}
		return totalScore;
	}

	private PersonWeeks chooseWeek(TreeMap<Double, PersonWeeks> map) {				
		// score 0 is included as random range = 0.0d (inclusive) to 1.0d (exclusive)		
		for (int i = 0; i < 10; i++) {
			random.nextDouble();
		}
		double randomScore = random.nextDouble();
				
		PersonWeeks personWeeks = map.get(map.firstKey());
		for (Entry<Double, PersonWeeks> entry : map.entrySet()) {
	        if (entry.getKey() > randomScore + 0.000000000000000001) {
	        	personWeeks = entry.getValue();
	        }
	    }		
		return personWeeks;
	}

	private void createPlansForPerson(PersonImpl person, PersonWeeks personWeeksThurgau, PersonSetSecondaryLocation secLocationAssigner) {	
		// only one week to begin with
		int week = 0;		
		for (int dow = 0; dow < 7; dow++) {
			person.getPlans().clear();
			Plan plan = personWeeksThurgau.getDay(dow, week);
			PersonImpl thurgauPerson = (PersonImpl)personWeeksThurgau.getPerson();
						
			thurgauPerson.addPlan(plan);
			thurgauPerson.setSelectedPlan(plan);
			Plan planNew = thurgauPerson.createCopyOfSelectedPlanAndMakeSelected();
			
			person.addPlan(planNew);
			person.setSelectedPlan(planNew);

			// TODO: smear times
			
			// assign home and work locations
			PersonHomeWork phw = this.personHWFacilities.get(person.getId());			
			for (PlanElement pe : planNew.getPlanElements()) {
				if (pe instanceof Activity) {
					ActivityImpl act = (ActivityImpl)pe;				
					if (act.getType().startsWith("w")) {
						act.setFacilityId(phw.getWorkFaciliyId());
						act.setCoord(this.scenario.getActivityFacilities().getFacilities().get(phw.getWorkFaciliyId()).getCoord());
						act.setLinkId(this.scenario.getActivityFacilities().getFacilities().get(phw.getWorkFaciliyId()).getLinkId());
					}
					else if (act.getType().startsWith("h")) {
						act.setFacilityId(phw.getHomeFacilityId());
						act.setCoord(this.scenario.getActivityFacilities().getFacilities().get(phw.getHomeFacilityId()).getCoord());
						act.setLinkId(this.scenario.getActivityFacilities().getFacilities().get(phw.getHomeFacilityId()).getLinkId());
					}					
				}
			}
			// assign shop, leisure, education and other according to Balmers neighborhood search
			secLocationAssigner.run(person);
			
			if (this.personWeeksMZ.get(person.getId()) == null) {
				this.personWeeksMZ.put(person.getId(), new PersonWeeks(person));
				this.personWeeksMZ.get(person.getId()).setCurrentWeek(week);
			}
			this.personWeeksMZ.get(person.getId()).addDay(dow, person.getSelectedPlan());
		}
	}
			
	private void createIncomes(String outPath, String mzIncomeFile) {
		TreeMap<Double, Integer> incomesNormalized = new TreeMap<Double, Integer>();
		try {
			this.readMZIncomes(mzIncomeFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.normalizeIncomes(incomesNormalized);
		this.drawIncomes(incomesNormalized);
		this.writeIncomes(outPath);
	}
	
	private void readMZIncomes(String mzIncomeFile) throws Exception {	
		FileReader fr = new FileReader(mzIncomeFile);
		BufferedReader br = new BufferedReader(fr);
		String curr_line = br.readLine(); // Skip header
		while ((curr_line = br.readLine()) != null) {
			String[] entrs = curr_line.split("\t", -1);			
			int income = Integer.parseInt(entrs[0].trim());
			int incomeIndex = income -1;
						
			if (income > 0) {
				this.incomeCategoryFrequencies[incomeIndex] = this.incomeCategoryFrequencies[incomeIndex] + 1;
			}
		}
		br.close();
	}
	
	private void normalizeIncomes(TreeMap<Double, Integer> mapNormalized) {
		double sum = 0.0;
		for (int i = 0; i < this.incomeCategoryFrequencies.length; i++) {
			sum += this.incomeCategoryFrequencies[i];
		}
		
		double previousKey = 0.0;
		for (int i = 0; i < this.incomeCategoryFrequencies.length; i++) {			
			double key = previousKey + (double)this.incomeCategoryFrequencies[i] / sum;
			previousKey = key;
			mapNormalized.put(key, i);
		}
	}
	
	private void drawIncomes(TreeMap<Double, Integer> incomesNormalized) {
		for (PersonWeeks personWeeks : personWeeksMZ.values()) {	
			double randomScore = random.nextDouble();
			double income = -99.0;
			for (Entry<Double, Integer> entry : incomesNormalized.entrySet()) {				
		        if (entry.getKey() > randomScore + 0.000000000000000001) {
		        	income = entry.getValue();
		        	break;	
		        }
		    }
			personWeeks.setIncome(income);
		}
	}
	
	private void writeIncomes(String outPath) {
		Bins incomeBins = new Bins(1, 9, "incomes");
				
		for (PersonWeeks personWeeks : personWeeksMZ.values()) {	
			double incomeNormalized =  personWeeks.getIncome() / 8.0;
			incomes.putAttribute(personWeeks.getPerson().getId().toString(), "income", incomeNormalized);
			incomeBins.addVal(incomeNormalized * 8.0, 1.0);
		}		
		log.info("Writing incomes to " + outPath + "/incomes.xml");
		ObjectAttributesXmlWriter attributesWriter = new ObjectAttributesXmlWriter(incomes);
		attributesWriter.writeFile(outPath + "/incomes.xml");
		
		incomeBins.plotBinnedDistribution(outPath + "/", "income * 8", "");	
	}
						
	private void writeWeek(String outPath) {
		new Analyzer().writeHeader(outPath);
		for (int dow = 0; dow < Surprice.days.size(); dow++) {
			int counter = 0;
			int nextMsg = 1;
			for (Person person : this.scenario.getPopulation().getPersons().values()) {			
				counter++;
				if (counter % nextMsg == 0) {
					nextMsg *= 2;
					log.info(" person # " + counter);
				}
				person.getPlans().clear();
				Plan plan = this.personWeeksMZ.get(person.getId()).getDay(dow, 0);
				person.addPlan(plan);
				((PersonImpl)person).setSelectedPlan(plan);				
			}
			new Analyzer().run(this.scenario.getPopulation(), outPath, Surprice.days.get(dow));
			
			this.createDesiresForPersons();
			
			this.adaptTimesForPersons();
								
			log.info("Writing population with plans ..." + outPath + "/" + DayConverter.getDayString(dow) + "/plans.xml.gz");
			new File(outPath + "/" + DayConverter.getDayString(dow) + "/").mkdirs();
			new PopulationWriter(
					this.scenario.getPopulation(), scenario.getNetwork()).writeFileV4(outPath + "/" + DayConverter.getDayString(dow) + "/plans.xml.gz");
		}	
	}
					
	private void createDesiresForPersons() {
		log.info("creating desires");
		for (Person person : this.scenario.getPopulation().getPersons().values()) {			
			if (((PersonImpl)person).getDesires() == null) ((PersonImpl)person).createDesires("");
			else ((PersonImpl)person).getDesires().getActivityDurations().clear();
			
			double wDur = 0.0; int wCount = 0;
			double sDur = 0.0; int sCount = 0;
			double lDur = 0.0; int lCount = 0;
			double eDur = 0.0; int eCount = 0;
			double bDur = 0.0; int bCount = 0;
			int hCount = 0;
						
			for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
				if (pe instanceof Activity) {
					ActivityImpl act = (ActivityImpl)pe;					
					if (act.getType().startsWith("w")) {
						wDur += (act.getEndTime() - act.getStartTime()); wCount++;
					} else if (act.getType().startsWith("s")) {
						sDur += (act.getEndTime() - act.getStartTime()); sCount++;
					} else if (act.getType().startsWith("l")) {
						lDur += (act.getEndTime() - act.getStartTime()); lCount++;
					} else if (act.getType().startsWith("e")) {
						eDur += (act.getEndTime() - act.getStartTime()); eCount++;
					} else if (act.getType().startsWith("b")) {
						bDur += (act.getEndTime() - act.getStartTime()); bCount++;
					} else if (act.getType().startsWith("h")) {
						hCount++;
					}
				}
			}
			if (wCount > 0) wDur = Math.max(1.0 * 3600.0, wDur / wCount);
			if (sCount > 0) sDur = Math.max(3.0 * 60.0, sDur / sCount);
			if (lCount > 0) lDur = Math.max(5.0 * 60.0, lDur / lCount);
			if (eCount > 0) eDur = Math.max(5.0 * 60.0, eDur / eCount);
			if (bCount > 0) bDur = Math.max(61.0, bDur / bCount);
			double hDur = Math.max(10.0, 24.0 * 3600.0 - wDur - sDur - lDur - eDur - bDur) / hCount;	
			
			if (wDur > 0.0) ((PersonImpl)person).getDesires().putActivityDuration("work", wDur);
			if (sDur > 0.0) ((PersonImpl)person).getDesires().putActivityDuration("shop", sDur);
			if (lDur > 0.0) ((PersonImpl)person).getDesires().putActivityDuration("leisure", lDur);
			if (eDur > 0.0) ((PersonImpl)person).getDesires().putActivityDuration("education", eDur);
			if (bDur > 0.0) ((PersonImpl)person).getDesires().putActivityDuration("business", bDur);
			if (hDur > 0.0) ((PersonImpl)person).getDesires().putActivityDuration("home", hDur);		
		}
	}
	
	private void adaptTimesForPersons() {		
		for (Person person : this.scenario.getPopulation().getPersons().values()) {	
			Plan plan = person.getSelectedPlan();
			Leg leg = null;
			Activity prevAct = null;
			double currentTime = 0.0;
			
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {
					ActivityImpl act = (ActivityImpl)pe;	
									
					
					if (leg != null) {
						//leg.setDepartureTime(currentTime);
						double tt = 2.0 * this.estimateTravelTime(leg, prevAct, act); // add factor 2 to not have a fictive best plan until the end!
						currentTime += tt;
						//leg.setTravelTime(tt);
						
					}
					act.setStartTime(currentTime);
					double desiredDuration = ((PersonImpl)plan.getPerson()).getDesires().getActivityDuration(act.getType());
					currentTime += desiredDuration;
									
					if (act != ((PlanImpl)plan).getLastActivity()) {
						act.setEndTime(currentTime);
						leg = ((PlanImpl)plan).getNextLeg(act);
						prevAct = act;
					}	
				}
			}
		}
	}
	
	private double estimateTravelTime(Leg leg, Activity fromAct, Activity toAct) {
		double dist = CoordUtils.calcDistance(fromAct.getCoord(), toAct.getCoord());
		double carSpeed = 10.0;
		return dist * 1.5 /carSpeed;		
		
//		PlansCalcRouteConfigGroup routeConfigGroup = scenario.getConfig().plansCalcRoute();
//		TeleportationLegRouter router = new TeleportationLegRouter(
//	            ((PopulationFactoryImpl) scenario.getPopulation().getFactory()).getModeRouteFactory(),
//	            routeConfigGroup.getTeleportedModeSpeeds().get(leg.getMode()),
//	            routeConfigGroup.getBeelineDistanceFactor());
//		
//		return router.routeLeg(person, leg, fromAct, toAct, depTime);
	}
	
	private void createToll(String outPath, double radius) {	
		
		NetworkImpl network = (NetworkImpl)this.scenario.getNetwork();
		
		for (ActivityFacility facility : this.scenario.getActivityFacilities().getFacilities().values()) {
			((ActivityFacilityImpl)facility).setLinkId(
					NetworkUtils.getNearestLink(network, facility.getCoord()).getId()
					);
		}
		
		CoordImpl bellevue = new CoordImpl(683518.0,246836.0);
		Zone tollZone =  new Zone("tollZone", bellevue, 2000.0); 
		
		for (ActivityFacility facility : this.scenario.getActivityFacilities().getFacilities().values()) {	
			if (bellevue.calcDistance(facility.getCoord()) < radius) {
				tollZone.addFacility(facility);
			}
		}
		
//		CreateToll tollCreator = new CreateToll();
//		tollCreator.createLinkTolling(
//				outPath, 
//				network,
//				tollZone,
//				6.0 * 3600.0,
//				20.0 * 3600.0,
//				1.0,
//				"link",
//				"ZH scenario"); 
		
		// String path, Zone tollZone, double startTime, double endTime, double amount, String type, String desc
		CreateToll tollCreator = new CreateToll();
		tollCreator.create(
				outPath, 
				tollZone,
				5.75 * 3600.0,
				8.5 * 3600.0,
				3.0,
				"area",
				"ZH scenario"); 
	}
	
	private void createPreferences() {
		// create marginal utility of money		
		log.info("creating preferences");
		for (Person person : this.scenario.getPopulation().getPersons().values()) {	
			//double offset = (0.5 - random.nextDouble()) * 0.5; // -0.25 .. 0.25
			//double nullOffset = 0.25;
			//double totalOffset = offset + nullOffset;
			//double dudm = (Double)(this.incomes.getAttribute(person.getId().toString(), "income")) + totalOffset;
			//double dudmNormalized = dudm / 1.5;
			double dudmNormalized = 1.1 - (Double)(this.incomes.getAttribute(person.getId().toString(), "income"));
			this.preferences.putAttribute(person.getId().toString(), "dudm", dudmNormalized);
		}
		this.writePreferences(config.findParam(Surprice.SURPRICE_PREPROCESS, "outPath"));
	}
	
	private void writePreferences(String outPath) {
		Bins preferencesBins = new Bins(1, 20, "preferences");
				
		for (Id id : this.scenario.getPopulation().getPersons().keySet()) {	
			preferencesBins.addVal((Double)this.preferences.getAttribute(id.toString(), "dudm") * 10.0, 1.0);
		}		
		log.info("Writing preferences to " + outPath + "/preferences.xml");
		ObjectAttributesXmlWriter attributesWriter = new ObjectAttributesXmlWriter(preferences);
		attributesWriter.writeFile(outPath + "/preferences.xml");
		
		preferencesBins.plotBinnedDistribution(outPath + "/", "preferences * 10", "");	
	}
}
