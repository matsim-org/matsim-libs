/* *********************************************************************** *
 * project: org.matsim.*
 * EmissionsPerPersonAnalysis.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.contrib.emissions.utils.EmissionUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.munich.utils.MunichPersonFilter;
import playground.agarwalamit.munich.utils.MunichPersonFilter.MunichUserGroup;
import playground.agarwalamit.utils.LoadMyScenarios;
import playground.vsp.airPollution.flatEmissions.EmissionCostFactors;
import playground.vsp.analysis.modules.emissionsAnalyzer.EmissionsAnalyzer;

/**
 * A class to get emissions and emissions cost for each user group.
 * @author amit
 *
 */
public class EmissionsPerPersonPerUserGroup {

	public static final Logger LOG = Logger.getLogger(EmissionsPerPersonPerUserGroup.class);
	private int lastIteration;
	private final String outputDir;
	private SortedMap<MunichUserGroup, SortedMap<String, Double>> userGroupToEmissions;
	private Scenario scenario;
	private Map<Id<Person>, SortedMap<String, Double>> emissionsPerPerson;
	private final MunichPersonFilter pf = new MunichPersonFilter();
	
	public EmissionsPerPersonPerUserGroup(String outputDir) {
		this.outputDir = outputDir;
	}

	public static void main(String[] args) {
		String outputDir = "../../../../repos/runs-svn/detEval/emissionCongestionInternalization/otherRuns/output/1pct/run10/policies/backcasting/exposure/";
		String [] runCases =  {"ExI","5ExI","10ExI","15ExI","20ExI","25ExI"};
		
		EmissionsPerPersonPerUserGroup eppa = new EmissionsPerPersonPerUserGroup(outputDir);
		eppa.run(runCases);
	}
	
	private void init(final String runCase){
		
		this.scenario = LoadMyScenarios.loadScenarioFromOutputDir(this.outputDir+runCase);
		this.lastIteration = this.scenario.getConfig().controler().getLastIteration();
		
		this.userGroupToEmissions = new TreeMap<>();
		this.emissionsPerPerson = new HashMap<>();

		for(MunichUserGroup ug:MunichUserGroup.values()){
			SortedMap<String, Double> pollutantToValue = new TreeMap<>();
			for(WarmPollutant wm:WarmPollutant.values()) {//because ('warmPollutants' U 'coldPollutants') = 'warmPollutants'
				pollutantToValue.put(wm.toString(), 0.0);
			}
			this.userGroupToEmissions.put(ug, pollutantToValue);
		}
	}

	public void run(final String [] runCases) {
		for(String runCase:runCases){
			init(runCase);
			
			String emissionEventFile = this.outputDir+runCase+"/ITERS/it."+this.lastIteration+"/"+this.lastIteration+".emission.events.xml.gz";//"/events.xml";//
			EmissionsAnalyzer ema = new EmissionsAnalyzer(emissionEventFile);
			ema.init((MutableScenario) this.scenario);
			ema.preProcessData();
			ema.postProcessData();

			EmissionUtils emu = new EmissionUtils();
			Map<Id<Person>, SortedMap<String, Double>> totalEmissions = ema.getPerson2totalEmissions();
			emissionsPerPerson = emu.setNonCalculatedEmissionsForPopulation(scenario.getPopulation(), totalEmissions);

			getTotalEmissionsPerUserGroup(this.emissionsPerPerson);
			writeTotalEmissionsPerUserGroup(this.outputDir+runCase+"/analysis/userGrpEmissions.txt");
			writeTotalEmissionsCostsPerUserGroup(this.outputDir+runCase+"/analysis/userGrpEmissionsCosts.txt");
		}
	}

	private void writeTotalEmissionsCostsPerUserGroup(final String outputFile){
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFile);
		try{
			writer.write("userGroup \t");
			for(EmissionCostFactors ecf:EmissionCostFactors.values()){
				writer.write(ecf.toString()+"\t");
			}
			writer.write("total \n");
			for(MunichUserGroup ug:this.userGroupToEmissions.keySet()){
				double totalEmissionCost =0. ;
				writer.write(ug+"\t");
				for(EmissionCostFactors ecf:EmissionCostFactors.values()){
					double ec = this.userGroupToEmissions.get(ug).get(ecf.toString()) * ecf.getCostFactor();
					writer.write(ec+"\t");
					totalEmissionCost += ec;
				}
				writer.write(totalEmissionCost+"\n");
			}
			writer.close();
		} catch (Exception e){
			throw new RuntimeException("Data is not written in the file. Reason - "+e);
		}
		LOG.info("Finished Writing data to file "+outputFile);		
	}

	private void writeTotalEmissionsPerUserGroup(final String outputFile) {

		BufferedWriter writer = IOUtils.getBufferedWriter(outputFile);
		try{
			writer.write("userGroup \t");
			for(String str:this.userGroupToEmissions.get(MunichUserGroup.Urban).keySet()){
				writer.write(str+"\t");
			}
			writer.newLine();
			for(MunichUserGroup ug:this.userGroupToEmissions.keySet()){
				writer.write(ug+"\t");
				for(String str:this.userGroupToEmissions.get(ug).keySet()){
					writer.write(this.userGroupToEmissions.get(ug).get(str)+"\t");
				}
				writer.newLine();
			}
			writer.close();
		} catch (Exception e){
			throw new RuntimeException("Data is not written in the file. Reason - "+e);
		}
		LOG.info("Finished Writing files to file "+outputFile);		
	}

	private void getTotalEmissionsPerUserGroup(
			Map<Id<Person>, SortedMap<String, Double>> emissionsPerPerson) {
		for(Id<Person> personId: scenario.getPopulation().getPersons().keySet()){
			MunichUserGroup ug = pf.getMunichUserGroupFromPersonId(personId);
			SortedMap<String, Double> emissionsNewValue = new TreeMap<>();
			for(String str: emissionsPerPerson.get(personId).keySet()){
				double emissionSoFar = this.userGroupToEmissions.get(ug).get(str);
				double emissionNewValue = emissionSoFar+emissionsPerPerson.get(personId).get(str);
				emissionsNewValue.put(str, emissionNewValue);
			}
			this.userGroupToEmissions.put(ug, emissionsNewValue);
		}
	}
}
