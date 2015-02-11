/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.utils.plans;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scoring.functions.CharyparNagelScoringUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.utils.LoadMyScenarios;
import playground.benjamin.scenarios.munich.analysis.filter.PersonFilter;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;

/**
 * This class checks the initial plans file and check 
 * <p> 1) How many persons do not have same first and last activity?
 * <p> 2) Out of them how many are from urban group (only for munich data) ?
 * <p> 3) Also writes the activity sequence of such inconsistent plans and their frequency.
 * <p> 4) How may activities have zero durations.
 * @author amit
 */
public class InitialPlansConsistencyCheck {
	public static final Logger log = Logger.getLogger(InitialPlansConsistencyCheck.class);
	private Scenario sc;
	private Map<Person, List<String>> person2ActivityType;
	private Map<Person, List<String>> person2Legs;
	private Map<UserGroup, Integer> userGroup2NumberOfPersons;
	private PersonFilter pf;
	private BufferedWriter writer;

	public InitialPlansConsistencyCheck(String initialPlans) {
		LoadMyScenarios.loadScenarioFromPlans(initialPlans);
	}

	public static void main(String[] args) {
		String initialPlansFile = "/Users/aagarwal/Desktop/ils4/agarwal/munich/input"
				+ "/mergedPopulation_All_1pct_scaledAndMode_workStartingTimePeakAllCommuter0800Var2h_gk4.xml.gz";
		String initialConfig = "/Users/aagarwal/Desktop/ils4/agarwal/munich/input/config_munich_1pct_baseCase.xml";
		String outputFile = "/Users/aagarwal/Desktop/ils4/agarwal/munich/output/1pct/";

		new InitialPlansConsistencyCheck(initialPlansFile).run(outputFile,initialConfig);
	}

	public void run(String outputFile, String configFile){
		initializePlansStatsWriter(outputFile);
		checkFor1stAndLastActivity();
		checkForZeroActivitDuration(outputFile);
		checkForActivityDurationLessThanZeroUtilityDuration(configFile, outputFile);
	}

	private void initializePlansStatsWriter (String outputFile){

		pf = new PersonFilter();

		getUserGrp2NumberOfPersons();
		getPersonId2ActivitiesAndLegs();

		writer = IOUtils.getBufferedWriter(outputFile+"analysis/plansConsistency_differentFirstAndLastActivities.txt");

		try {
			writer.write("UserGroup \t numberOfPersons \n");
			for(UserGroup ug : UserGroup.values()){
				writer.write(ug+"\t"+userGroup2NumberOfPersons.get(ug)+"\n");
			}
			writer.write("Total persons \t "+person2ActivityType.size()+"\n");
		} catch (Exception e) {
			throw new RuntimeException("Data is not written to file. Reason "+e);
		}
	}

	private void getPersonId2ActivitiesAndLegs(){

		person2ActivityType = new HashMap<Person, List<String>>();
		person2Legs = new HashMap<Person, List<String>>();

		for(Person p : sc.getPopulation().getPersons().values()){
			for(PlanElement pe : p.getSelectedPlan().getPlanElements()){
				if (pe instanceof Activity ) {
					List<String> acts = person2ActivityType.get(p);
					acts.add(((Activity) pe).getType());
				} else if (pe instanceof Leg ) {
					List<String> legs = person2Legs.get(p);
					legs.add(((Leg) pe).getMode());
				} else {
					throw new RuntimeException("Following plan elements is not included. "+pe.toString());
				}
			}
		}
	}

	private void getUserGrp2NumberOfPersons() {
		userGroup2NumberOfPersons = new HashMap<UserGroup, Integer>();

		for(UserGroup ug : UserGroup.values()){
			userGroup2NumberOfPersons.put(ug, 0);
		}

		for(Person p : sc.getPopulation().getPersons().values()){
			person2ActivityType.put(p, new ArrayList<String>());
			person2Legs.put(p, new ArrayList<String>());
			for(UserGroup ug : UserGroup.values()){
				if(pf.isPersonIdFromUserGroup(p.getId(), ug)) {
					int countSoFar = userGroup2NumberOfPersons.get(ug);
					userGroup2NumberOfPersons.put(ug, countSoFar+1);
					break;
				}
			}
		}
	}

	/**
	 * If t_actual becomes less than t_0 (duration at which util_perf =0) than util_perf is negative. Reporting such instances.
	 */
	private void checkForActivityDurationLessThanZeroUtilityDuration(String configFile, String outputFile){
		
		log.info("Consistency check for zero activity duration.");
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFile+"analysis/negativeUtil_perfActivities.txt");
		int zeroUtilDurCount =0;
		SortedMap<String, Double> actType2ZeroUtilDuration = getZeroUtilDuration(configFile);
		try {
			writer.write("Person \t activity \t startTime \t endTime \t zeroUtilDuration \n");
			for(Person p : sc.getPopulation().getPersons().values()){
				for(PlanElement pe : p.getSelectedPlan().getPlanElements()){
					if (pe instanceof Activity ) {
						double dur = ((Activity)pe).getEndTime() - ((Activity)pe).getStartTime();
						double zeroUtilDur = actType2ZeroUtilDuration.get(((Activity)pe).getType());
						if(dur<=zeroUtilDur){
							if(zeroUtilDurCount<1){
								log.warn("Activity duration of person "+p.toString()+" for activity "+
										((Activity)pe).getType()+" is "+dur+". Utility of performing is zero at (=zero utility duration)"+zeroUtilDur+" sec. Any duration less than this will result in lesser score.");
								log.warn(Gbl.ONLYONCE);
							}
							zeroUtilDurCount ++;
							writer.write(p.getId()+"\t"+((Activity)pe).getType()+"\t"+((Activity)pe).getStartTime()+
									"\t"+((Activity)pe).getEndTime()+"\t"+zeroUtilDur+"\n");

						}
					} 
				}
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException(
					"Data is not written. Reason - " + e);
		}
		if (zeroUtilDurCount>0) log.warn("There are "+zeroUtilDurCount+" instances where person have activity duration equal to or less than zero utility duration. Check for written file for detailed discription.");
	}
	
	private SortedMap<String, Double> getZeroUtilDuration(String configFile){
		Config config = new Config();
		config.addCoreModules();
		MatsimConfigReader reader = new MatsimConfigReader(config);
		reader.readFile(configFile);
		
		PlanCalcScoreConfigGroup params = config.planCalcScore();
		
		SortedMap<String, Double> actType2ZeroUtilDuration = new TreeMap<>();
		for(String actType : params.getActivityTypes()){
			double priority = params.getActivityParams(actType).getPriority();
			double typicalDuration_s = params.getActivityParams(actType).getTypicalDuration();
			double zeroUtilDur_sec = CharyparNagelScoringUtils.computeZeroUtilityDuration(priority, typicalDuration_s);
			actType2ZeroUtilDuration.put(actType, zeroUtilDur_sec);
		}
		return actType2ZeroUtilDuration;
	}
	
	private void checkForZeroActivitDuration(String outputFile){
		log.info("Consistency check for zero activity duration.");
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFile+"analysis/zeroActivityDurationPersons.txt");
		int zeroDurCount =0;
		try {
			writer.write("Person \t activity \t startTime \t endTime \n");
			for(Person p : sc.getPopulation().getPersons().values()){
				for(PlanElement pe : p.getSelectedPlan().getPlanElements()){
					if (pe instanceof Activity ) {
						double dur = ((Activity)pe).getEndTime() - ((Activity)pe).getStartTime();
						if(dur==0){
							if(zeroDurCount<1){
								log.warn("Activity duration of person "+p.toString()+" for activity "+
										((Activity)pe).getType()+" is zero, it may result in higher utility loss.");
								log.warn(Gbl.ONLYONCE);
							}
							zeroDurCount ++;
							writer.write(p.getId()+"\t"+((Activity)pe).getType()+"\t"+((Activity)pe).getStartTime()+
									"\t"+((Activity)pe).getEndTime()+"\n");

						}
					} 
				}
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException(
					"Data is not written. Reason - " + e);
		}
		log.warn("There are "+zeroDurCount+" instances where person have activity duration zero. Check for written file for detailed discription.");
	}

	private void checkFor1stAndLastActivity(){
		log.info("Consistency check for equality of first and last activity in a plan.");
		SortedMap<String, Integer> actSeq2Count = new TreeMap<>();
		int warnCount =0;
		int urbanPersons =0;
		for(Person p:person2ActivityType.keySet()){
			List<String> acts = person2ActivityType.get(p);
			if(!acts.get(0).equals(acts.get(acts.size()-1))){
				warnCount++;
				if(pf.isPersonFromMID(p.getId())) urbanPersons++;

				if(actSeq2Count.containsKey(acts.toString())){
					int countSoFar = actSeq2Count.get(acts.toString());
					actSeq2Count.put(acts.toString(), countSoFar+1);
				}
				else {
					actSeq2Count.put(acts.toString(),1);
				}
			}
		}
		try {
			writer.write("Number of persons not having first and last activity same \t "+warnCount+"\n");
			writer.write("Number of such persons from Urban population \t "+urbanPersons+"\n");

			writer.write("\n \n \n");

			writer.write("act Sequence \t count \n");
			for(String str : actSeq2Count.keySet()){
				writer.write(str+"\t"+actSeq2Count.get(str)+"\n");
			}

			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written to file. Reason "+e);
		}
		log.warn(warnCount+" number of persons do not have first and last activites same."
				+ "Out of them "+urbanPersons+" persons belong to urban user group."
				+ "\n The total number of person in populatino are "+person2ActivityType.size());
	}
}
