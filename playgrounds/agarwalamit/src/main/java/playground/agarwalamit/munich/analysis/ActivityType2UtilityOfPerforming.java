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
package playground.agarwalamit.munich.analysis;

import java.io.BufferedWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.activity.ActivityType2ActDurationsAnalyzer;
import playground.agarwalamit.analysis.activity.ActivityType2DurationHandler;
import playground.benjamin.scenarios.munich.analysis.filter.PersonFilter;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;

/**
 * A class to get utility of performing for each activity type assuming
 * repetition of same activities as different leg.
 * @author amit
 */
public class ActivityType2UtilityOfPerforming {

	/**
	 * @param outputDir
	 * Use this to get distribution for whole population
	 */
	public ActivityType2UtilityOfPerforming(String outputDir) {
		this.outputDir = outputDir;
		this.sortPersons = false;
		this.userGroup = "";
	}

	/**
	 * @param outputDir
	 * @param userGroup for which distribution is required
	 */
	public ActivityType2UtilityOfPerforming(String outputDir, UserGroup userGroup) {
		this.outputDir = outputDir;
		this.sortPersons = true;
		this.userGroup = userGroup.toString();
		ActivityType2DurationHandler.log.warn("Result will consider only persons from "+this.userGroup+" sub population group.");
	}

	private ActivityType2ActDurationsAnalyzer actDurAnalyzer;
	private Map<Id<Person>, Map<String, List<Double>>> personId2ActDurations;
	private String outputDir;
	private boolean sortPersons;
	private String userGroup;
	private double marginalUtil_performing;
	private SortedMap<String, Double> act2TypDur;
	private SortedMap<String, Double> act2Priority;
	private Map<Id<Person>,Map<String, Double>> personId2Act2UtilPerfor;
	private int wrnCnt =0;

	public static void main(String[] args) {
		String outputDir = "/Users/amit/Documents/repos/runs-svn/detEval/emissionCongestionInternalization/output/1pct/run9/";
		String [] runCases = { "baseCaseCtd","ei","ci","eci"};
		new ActivityType2UtilityOfPerforming(outputDir,UserGroup.URBAN).run(runCases);
	}

	public void run(String [] runCases){
		for(String runCase:runCases){
			init(runCase);
			writeActType2UtilPerforming(outputDir+runCase+"/analysis/");
		}
	}

	public void init(String runCase){
		actDurAnalyzer = new ActivityType2ActDurationsAnalyzer(outputDir+runCase);
		actDurAnalyzer.preProcessData();
		actDurAnalyzer.postProcessData();
		personId2ActDurations = actDurAnalyzer.getPersonId2ActivityType2ActivityDurations();

		String outputConfig = outputDir+runCase+"/output_config.xml";
		storeActType2TypicalDuration(outputConfig);
	}

	/**
	 * stores activity type to typical activity duration to file. 
	 */
	private void storeActType2TypicalDuration(String configFile){
		Config config = new Config();
		config.addCoreModules();
		MatsimConfigReader reader = new MatsimConfigReader(config);
		reader.readFile(configFile);

		marginalUtil_performing = config.planCalcScore().getPerforming_utils_hr()/3600;
		act2TypDur = new TreeMap<>();
		act2Priority = new TreeMap<>();

		for (String actTyp :config.planCalcScore().getActivityTypes()){
			act2TypDur.put(actTyp, config.planCalcScore().getActivityParams(actTyp).getTypicalDuration());
			act2Priority.put(actTyp, config.planCalcScore().getActivityParams(actTyp).getPriority());
		}
	}

	public void writeActType2UtilPerforming(String outputFolder) {
		String fileName = outputFolder+"/"+userGroup+"actTyp2TotalUtilityOfPerforming.txt";
		SortedMap<String, Double> act2UtilPerform = getActType2TotalUtilOfPerforming();

		BufferedWriter writer = IOUtils.getBufferedWriter(fileName);
		try {
			writer.write("ActType \t totalUtilPerforming \n");
			double sum =0;
			for(String act : act2UtilPerform.keySet()){
				double util = act2UtilPerform.get(act);
				writer.write(act+"\t"+util+"\n");
				sum +=util;
			}
			writer.write("TotalUtilPerforming \t "+sum+"\n");
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written. Reason - " + e);
		}
		ActivityType2DurationHandler.log.info("Data is written to file "+fileName);
	}


	/**
	 * @return activity type to total utility of performing
	 */
	public SortedMap<String, Double> getActType2TotalUtilOfPerforming(){
		Map<Id<Person>,Map<String, Double>> personId2Act2UtilPerfor = getPersonId2UtilityOfPerforming();
		SortedMap<String, Double> act2TotalUtilPerforming = new TreeMap<>();

		for(String actType : act2TypDur.keySet()){
			double sum=0;
			for(Id<Person> id : personId2Act2UtilPerfor.keySet()){
				if(personId2Act2UtilPerfor.get(id).containsKey(actType)){
					double util = personId2Act2UtilPerfor.get(id).get(actType);
					sum +=util;
				}
			}
			act2TotalUtilPerforming.put(actType, sum);
		}
		return act2TotalUtilPerforming;
	}

	/**
	 * @return person to total utility of performing
	 */
	public Map<Id<Person>, Double> getPerson2TotalUtilityOfPerforming(){
		Map<Id<Person>,Map<String, Double>> personId2Act2UtilPerfor = getPersonId2UtilityOfPerforming();
		Map<Id<Person>, Double> person2TotalUtilPerforming = new HashMap<>();

		for(Id<Person> id : personId2Act2UtilPerfor.keySet()){
			double sum=0;
			for(String act:personId2Act2UtilPerfor.get(id).keySet()){
				double util = personId2Act2UtilPerfor.get(id).get(act);
				sum += util;
			}
			person2TotalUtilPerforming.put(id, sum);
		}
		return person2TotalUtilPerforming;
	}

	/**
	 * @return person to activity type 2 utility of performing.
	 */
	public Map<Id<Person>, Map<String, Double>> getPersonId2UtilityOfPerforming(){
		personId2Act2UtilPerfor = new HashMap<>();
		PersonFilter pf = new PersonFilter();
		for(Id<Person> id :personId2ActDurations.keySet()){
			if(sortPersons ){
				if(pf.isPersonIdFromUserGroup(id, UserGroup.valueOf(userGroup))){
					storeData(id);
				}
			} else {
				storeData(id);
			}
		}
		return personId2Act2UtilPerfor;
	}

	private void storeData(Id<Person> id) {
		Map<String, Double> act2UtilPerform = new HashMap<>();
		for (String act :personId2ActDurations.get(id).keySet()){
			double sum =0;
			double typDur = act2TypDur.get(act);
			double actPriority = act2Priority.get(act);
			for(double dur:personId2ActDurations.get(id).get(act)){
				if(dur!=0){
					double util = getUtilityOfPerforming(typDur, dur, actPriority);
					sum = sum+util;
				}
			}
			act2UtilPerform.put(act, sum);
		}
		personId2Act2UtilPerfor.put(id, act2UtilPerform);
	}

	public Map<Id<Person>, Map<String, List<Double>>> getPersonId2ActivityType2ActivityDurations(){
		return this.personId2ActDurations;
	}

	/**
	 * @param typDuration
	 * @param actualDuration
	 * @return util_performing; 
	 */
	private double getUtilityOfPerforming(double typicalDuration_s, double duration_s, double priority){
		double utilPerf;
//		double zeroUtilDuration_s = CharyparNagelScoringUtils.computeZeroUtilityDuration_s(priority, typicalDuration_s);
		double zeroUtilDuration_s = -1 ;
		Logger.getLogger( this.getClass() ).fatal("If you want to mirror what matsim does, you need to do something else, since matsim "
				+ "keeps changing. kai, apr'15");
		System.exit(-1);

		if ( duration_s >= zeroUtilDuration_s ) {
			 utilPerf = marginalUtil_performing * typicalDuration_s
					* Math.log((duration_s/zeroUtilDuration_s));
		} else {
			if ( wrnCnt < 1 ) {
				wrnCnt++ ;
				ActivityType2DurationHandler.log.warn("encountering duration < zeroUtilityDuration; the logic for this was changed around mid-nov 2013.") ;
				ActivityType2DurationHandler.log.warn( "your final score thus will be different from earlier runs; set usingOldScoringBelowZeroUtilityDuration to true if you "
						+ "absolutely need the old version.  See https://matsim.atlassian.net/browse/MATSIM-191." );
				ActivityType2DurationHandler.log.warn( Gbl.ONLYONCE ) ;
			}
			
			// below zeroUtilityDuration, we linearly extend the slope ...:
			double slopeAtZeroUtility =marginalUtil_performing* typicalDuration_s / ( zeroUtilDuration_s ) ;
			if ( slopeAtZeroUtility < 0. ) {
				// (beta_perf might be = 0)
				System.err.println("beta_perf: " + marginalUtil_performing);
				System.err.println("typicalDuration: " + typicalDuration_s );
				System.err.println( "zero utl duration in sec: " + zeroUtilDuration_s );
				throw new RuntimeException( "slope at zero utility < 0.; this should not happen ...");
			}
			double durationUnderrun = zeroUtilDuration_s - duration_s ;
			if ( durationUnderrun < 0. ) {
				throw new RuntimeException( "durationUnderrun < 0; this should not happen ...") ;
			}
			utilPerf = - slopeAtZeroUtility * durationUnderrun ;
		}
		return utilPerf;
	}

}
