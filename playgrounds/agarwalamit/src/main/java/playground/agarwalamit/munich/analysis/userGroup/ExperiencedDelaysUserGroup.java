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
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.analysis.congestion.ExperiencedDelayAnalyzer;
import playground.agarwalamit.munich.utils.MunichPersonFilter;
import playground.agarwalamit.munich.utils.MunichPersonFilter.MunichUserGroup;
import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * A class to get absolute experienced delays and delays costs per user group.
 * @author amit
 */
public class ExperiencedDelaysUserGroup {

	private  double vttsCar ;

	public ExperiencedDelaysUserGroup(String outputDir) {
		this.outputDir = outputDir;
	}

	private int lastIteration;
	public static final Logger logger = Logger.getLogger(ExperiencedDelaysUserGroup.class);
	private final String outputDir;

	private SortedMap<MunichUserGroup, Double> userGroupToDelays;
	private Map<Double, Map<Id<Person>, Double>> time2linkIdDelays;
	private Scenario scenario;
	private final MunichPersonFilter pf = new MunichPersonFilter();

	public static void main(String[] args) throws IOException {
		String outputDir = "../../../repos/runs-svn/detEval/emissionCongestionInternalization/output/1pct/run10/policies/";/*"./output/run2/";*/
		String [] runCases = {"bau","ei","ci","eci","10ei"};

		new ExperiencedDelaysUserGroup(outputDir).run(runCases);
	}

	private void init(final String runCase){
		this.time2linkIdDelays = new HashMap<>();
		this.userGroupToDelays  = new TreeMap<>();
		for (MunichUserGroup ug:MunichUserGroup.values()) {
			this.userGroupToDelays.put(ug, 0.0);
		}
		
		this.scenario = LoadMyScenarios.loadScenarioFromOutputDir(outputDir+runCase);
		
		this.lastIteration = scenario.getConfig().controler().getLastIteration();

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

	public void run(final String [] runCases){
		for(String runCase:runCases){
			init(runCase);
			String eventFile = this.outputDir+runCase+"/ITERS/it."+this.lastIteration+"/"+this.lastIteration+".events.xml.gz";//"/events.xml";//
			ExperiencedDelayAnalyzer personAnalyzer = new ExperiencedDelayAnalyzer(eventFile, this.scenario, 1);
			personAnalyzer.run();
			personAnalyzer.checkTotalDelayUsingAlternativeMethod();
			this.time2linkIdDelays = personAnalyzer.getTimeBin2AffectedPersonId2Delay();

			getTotalDelayPerUserGroup(this.time2linkIdDelays);
			writeTotalDelaysPerUserGroup(this.outputDir+runCase+"/analysis/userGrpExperiencedDelays.txt");
		}
	}

	private void writeTotalDelaysPerUserGroup(final String outputFile){
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFile);
		try{
			writer.write("userGroup \t delaySeconds \t delaysMoney \n");
			for(MunichUserGroup ug:this.userGroupToDelays.keySet()){
				writer.write(ug+"\t"+this.userGroupToDelays.get(ug)+"\t"+this.userGroupToDelays.get(ug)*this.vttsCar+"\n");
			}
			writer.close();
		} catch (Exception e){
			throw new RuntimeException("Data is not written in the file. Reason - "+e);
		}
		logger.info("Finished Writing data to file "+outputFile);
	}

	private void getTotalDelayPerUserGroup(final Map<Double, Map<Id<Person>, Double>> delaysPerPersonPerTimeBin){
		for(double d:delaysPerPersonPerTimeBin.keySet()){
			for(Id<Person> personId : delaysPerPersonPerTimeBin.get(d).keySet()){
				MunichUserGroup ug = pf.getMunichUserGroupFromPersonId(personId);
				double delaySoFar = this.userGroupToDelays.get(ug);
				double newDelays = delaySoFar+delaysPerPersonPerTimeBin.get(d).get(personId);
				this.userGroupToDelays.put(ug, newDelays);
			}
		}
	}
}