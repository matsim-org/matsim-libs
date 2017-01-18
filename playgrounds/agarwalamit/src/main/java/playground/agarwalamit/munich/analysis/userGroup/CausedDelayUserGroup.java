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
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.congestion.CausedDelayAnalyzer;
import playground.agarwalamit.analysis.congestion.CrossMarginalCongestionEventsWriter;
import playground.agarwalamit.munich.utils.MunichPersonFilter;
import playground.agarwalamit.munich.utils.MunichPersonFilter.MunichUserGroup;
import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * @author amit
 */

public class CausedDelayUserGroup {

	private final String outputDir;
	private  double vttsCar ;
	private SortedMap<MunichUserGroup, Double> userGroupToDelays;
	private Map<Id<Person>, Double> personId2CausingDelay;
	private final MunichPersonFilter pf = new MunichPersonFilter();

	public CausedDelayUserGroup(final String outputDir) {
		this.outputDir = outputDir;
	}
	
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

	private void run(final String runCase, final String eventsFile){
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
			MunichUserGroup ug = pf.getMunichUserGroupFromPersonId(p);
			double delaySoFar = this.userGroupToDelays.get(ug);
			this.userGroupToDelays.put(ug, delaySoFar+this.personId2CausingDelay.get(p));
		}

		writeTotalDelaysPerUserGroup(this.outputDir+runCase+"/analysis/userGrpCausedDelays.txt");
	}

	private void writeTotalDelaysPerUserGroup(final String outputFile){
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFile);
		try{
			writer.write("userGroup \t causedDelaySeconds \t causedDelaysMoney \n");
			for(MunichUserGroup ug:this.userGroupToDelays.keySet()){
				writer.write(ug+"\t"+this.userGroupToDelays.get(ug)+"\t"+this.userGroupToDelays.get(ug)*this.vttsCar+"\n");
			}
			writer.close();
		} catch (Exception e){
			throw new RuntimeException("Data is not written in the file. Reason - "+e);
		}
	}

	private void init(final String runCase){
		this.userGroupToDelays  = new TreeMap<>();
		this.personId2CausingDelay = new HashMap<>();

		for (MunichUserGroup ug : MunichUserGroup.values()) {
			this.userGroupToDelays.put(ug, 0.0);
		}

		Scenario scenario = LoadMyScenarios.loadScenarioFromOutputDir(outputDir + runCase);

		double marginalUtlMoney = scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney();
		double marginalUtlPerformingSec = scenario.getConfig().planCalcScore().getPerforming_utils_hr() / 3600;
		double marginalUtlTravelingCarSec = scenario.getConfig()
				.planCalcScore()
				.getModes()
				.get(TransportMode.car)
				.getMarginalUtilityOfTraveling() / 3600;
		double marginalUtlOfTravelTime = marginalUtlTravelingCarSec + marginalUtlPerformingSec;
		this.vttsCar = marginalUtlOfTravelTime / marginalUtlMoney;
	}
}
