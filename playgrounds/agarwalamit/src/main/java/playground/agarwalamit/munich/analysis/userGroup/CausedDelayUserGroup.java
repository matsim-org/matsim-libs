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
package playground.agarwalamit.munich.analysis.userGroup;

import java.io.BufferedWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.congestion.CausedDelayAnalyzer;
import playground.agarwalamit.analysis.congestion.CrossMarginalCongestionEventsWriter;
import playground.agarwalamit.utils.LoadMyScenarios;
import playground.benjamin.scenarios.munich.analysis.filter.PersonFilter;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;

/**
 * @author amit
 */

public class CausedDelayUserGroup {

	public CausedDelayUserGroup(String outputDir) {
		this.outputDir = outputDir;
	}

	private String outputDir;
	private  double marginal_Utl_money;
	private  double marginal_Utl_performing_sec;
	private  double marginal_Utl_traveling_car_sec;
	private  double marginalUtlOfTravelTime ;
	private  double vtts_car ;
	private SortedMap<UserGroup, Double> userGroupToDelays;
	private Map<Id<Person>, Double> personId2CausingDelay;
	private Scenario scenario;

	public static void main(String[] args) {
		String outputDir = "../../../repos/runs-svn/detEval/emissionCongestionInternalization/output/1pct/run10/policies/";/*"./output/run2/";*/
		String [] runCases = {"ci","eci"};

		for (String runCase : runCases){
			int lastIteration = LoadMyScenarios.getLastIteration(outputDir+runCase+"/output_config.xml.gz");
			String eventFile = outputDir+runCase+"/ITERS/it."+lastIteration+"/"+lastIteration+".events.xml.gz";//"/events.xml";//
			new CausedDelayUserGroup(outputDir).run(runCase,eventFile);
		}

		// for bau and ei there is no congestion events. Writing them externally.
		runCases = new String [] {"bau","ei","10ei"};
		String congestionImpl = "implV3";

		for(String runCase : runCases){
			Scenario runCaseSc = LoadMyScenarios.loadScenarioFromOutputDir(outputDir+runCase);
			new CrossMarginalCongestionEventsWriter(runCaseSc).readAndWrite(congestionImpl);

			// now read this events file.
			int lastIt = runCaseSc.getConfig().controler().getLastIteration();
			String eventFileBAU = runCaseSc.getConfig().controler().getOutputDirectory()+"/ITERS/it."+lastIt+"/"+lastIt+".events_"+congestionImpl+".xml.gz";
			new CausedDelayUserGroup(outputDir).run(runCase, eventFileBAU);
		}
	}

	private void run(String runCase, String eventsFile){
		init(runCase);
		
		Scenario sc = LoadMyScenarios.loadScenarioFromOutputDir(outputDir+runCase+"/output_config.xml");
		
		CausedDelayAnalyzer analyzer = new CausedDelayAnalyzer(eventsFile,sc,1);
		analyzer.run();
		Map<Double, Map<Id<Person>, Double>> timeBin2ersonId2CausingDelay = analyzer.getTimeBin2CausingPersonId2Delay();
		
		for(double d:timeBin2ersonId2CausingDelay.keySet()){
			for (Id<Person> personId : timeBin2ersonId2CausingDelay.get(d).keySet()){
				this.personId2CausingDelay.put(personId, this.personId2CausingDelay.get(personId) + timeBin2ersonId2CausingDelay.get(d).get(personId) );
			}
		}

		for(Id<Person> p : personId2CausingDelay.keySet()){
			UserGroup ug = getUserGrpFromPersonId(p);
			double delaySoFar = this.userGroupToDelays.get(ug);
			this.userGroupToDelays.put(ug, delaySoFar+this.personId2CausingDelay.get(p));
		}

		writeTotalDelaysPerUserGroup(this.outputDir+runCase+"/analysis/userGrpCausedDelays.txt");
	}

	private void writeTotalDelaysPerUserGroup(String outputFile){
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFile);
		try{
			writer.write("userGroup \t causedDelaySeconds \t causedDelaysMoney \n");
			for(UserGroup ug:this.userGroupToDelays.keySet()){
				writer.write(ug+"\t"+this.userGroupToDelays.get(ug)+"\t"+this.userGroupToDelays.get(ug)*this.vtts_car+"\n");
			}
			writer.close();
		} catch (Exception e){
			throw new RuntimeException("Data is not written in the file. Reason - "+e);
		}
	}

	private void init(String runCase){
		this.userGroupToDelays  = new TreeMap<UserGroup, Double>();
		this.personId2CausingDelay = new HashMap<Id<Person>, Double>();

		for (UserGroup ug:UserGroup.values()) {
			this.userGroupToDelays.put(ug, 0.0);
		}

		this.scenario = LoadMyScenarios.loadScenarioFromOutputDir(outputDir+runCase);

		this.marginal_Utl_money = scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney();
		this.marginal_Utl_performing_sec = scenario.getConfig().planCalcScore().getPerforming_utils_hr()/3600;
		this.marginal_Utl_traveling_car_sec = scenario.getConfig().planCalcScore().getTraveling_utils_hr()/3600;
		this.marginalUtlOfTravelTime = this.marginal_Utl_traveling_car_sec + this.marginal_Utl_performing_sec;
		this.vtts_car = this.marginalUtlOfTravelTime / this.marginal_Utl_money;
	}

	private UserGroup getUserGrpFromPersonId(Id<Person> personId){
		PersonFilter pf = new PersonFilter();
		UserGroup outUG = UserGroup.URBAN;
		for(UserGroup ug : UserGroup.values()){
			if(pf.isPersonIdFromUserGroup(personId, ug)) {
				outUG =ug;
				break;
			}
		}
		return outUG;
	}
}
