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
package playground.agarwalamit.analysis;

import java.io.BufferedWriter;
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.utils.LoadMyScenarios;
import playground.vsp.airPollution.flatEmissions.EmissionCostFactors;
import playground.vsp.analysis.modules.emissionsAnalyzer.EmissionsAnalyzer;
import playground.vsp.congestion.handlers.CongestionHandlerImplV3;

/**
 * @author amit
 */
public class VerifyResults {
	private static final Logger LOG = Logger.getLogger(VerifyResults.class);

	private static final double MARGIN_UTIL_MONEY=0.0789942;//*/0.062; //(for SiouxFalls =0.062 and for Munich =0.0789942);
	private static final double MARGIN_UTL_PERF_SEC=0.96/3600;
	private static final double MARGIN_UTIL_TRAVEL_CAR=-0.0/3600;
	private static final double MARGIN_UTIL_TRAVEL_TIME = MARGIN_UTIL_TRAVEL_CAR+MARGIN_UTL_PERF_SEC;
	private static final double VTTS_CAR = MARGIN_UTIL_TRAVEL_TIME/MARGIN_UTIL_MONEY;

	private static final String RUN_DIR = "/Users/amit/Documents/repos/runs-svn/detEval/emissionCongestionInternalization/output/1pct/run9/";
	private static final String [] RUN_NR =  {"baseCaseCtd","ei","ci","eci"};

    private static final boolean CONSIDER_CO2 = true;
	private static final double EMISSION_COST_FACTOR = 1.0;

	public static void main(String[] args) {

		for(int i=0;i<RUN_NR.length;i++){
			String inputConfigFile = RUN_DIR+RUN_NR[i]+"/output_config.xml";
			String networkFile = RUN_DIR+RUN_NR[i]+"/output_network.xml.gz";
			
			int lastItenation = LoadMyScenarios.getLastIteration(inputConfigFile);
			String emissionsEventsFile = RUN_DIR+RUN_NR[i]+"/ITERS/it."+lastItenation+"/"+lastItenation+".emission.events.xml.gz";
			String plansFile = RUN_DIR+RUN_NR[i]+"/output_plans.xml.gz";
            Scenario scenario = LoadMyScenarios.loadScenarioFromPlansAndNetwork(plansFile, networkFile);
			//			scenario = ScenarioUtils.loadScenario(config);
			String eventsFile=RUN_DIR+RUN_NR[i]+"/ITERS/it."+lastItenation+"/"+lastItenation+".events.xml.gz";

			calculateEmissionCosts(emissionsEventsFile, scenario,RUN_NR[i]);
			calculateDelaysCosts(eventsFile, scenario,RUN_NR[i]);
			calculateUserBenefits(scenario, RUN_NR[i]);
		}
		Logger.getLogger(VerifyResults.class).info("Writing files is finsished.");
	}

	private static void calculateEmissionCosts(final String emissionsEventsFile, final Scenario scenario, final String runNr){
		EmissionsAnalyzer analyzer	= new EmissionsAnalyzer(emissionsEventsFile);
		analyzer.init((MutableScenario) scenario);
		analyzer.preProcessData();
		analyzer.postProcessData();
		SortedMap<String, Double> totalEmissions = analyzer.getTotalEmissions();
		double totalEmissionCost =0;
		BufferedWriter writer = IOUtils.getBufferedWriter(RUN_DIR+runNr+"/analysis/verifyTotalEmissionCost.txt");
		try {
//			for(String str:totalEmissions.keySet()){
			for(EmissionCostFactors ecf:EmissionCostFactors.values()) {
				String str = ecf.toString();
				if(str.equals("CO2_TOTAL") && !CONSIDER_CO2){
					// do not include CO2_TOTAL costs.
				} else {
					double emissionsCosts = ecf.getCostFactor() * totalEmissions.get(str);
					totalEmissionCost += emissionsCosts;
					writer.write(str+" emissions in gm  are = "+"\t"+totalEmissions.get(str).toString()+"\t"+". Total NOX emission cost is "+emissionsCosts);
					writer.newLine();
				}
			}
			writer.write("Emission cost factor is "+"\t"+EMISSION_COST_FACTOR+"\t"+"and total cost of emissions is "+"\t"+EMISSION_COST_FACTOR*totalEmissionCost);
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: "+ e);
		}
	}

	public static void calculateDelaysCosts(final String eventsFile, final Scenario scenario, final String runNr){
		EventsManager em = EventsUtils.createEventsManager();
		CongestionHandlerImplV3 congestionHandler = new CongestionHandlerImplV3(em, scenario);
		MatsimEventsReader eventsReader = new MatsimEventsReader(em);
		em.addHandler(congestionHandler);
		eventsReader.readFile(eventsFile);
		BufferedWriter writer = IOUtils.getBufferedWriter(RUN_DIR+runNr+"/analysis/verifyTotalDelayCost.txt");
		try{
			writer.write("Total delays in sec are \t"+congestionHandler.getTotalDelay()+"\t"+"and total payment due to delays is \t"+VTTS_CAR*congestionHandler.getTotalDelay());
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: "+ e);
		}
	}

	public static void calculateUserBenefits(final Scenario scenario, final String runNr){
		Population population = scenario.getPopulation();
		double totalUtils=0;
		for(Person p : population.getPersons().values()){
			double personScore = p.getSelectedPlan().getScore();
			if(personScore<0) {
				LOG.warn("Utility for person "+p.getId()+" is negative and this ignoring this in user benefit callculation.");
				personScore=0;
			}
			totalUtils+=personScore;
		}
		BufferedWriter writer = IOUtils.getBufferedWriter(RUN_DIR+runNr+"/analysis/verifyUserBenefits.txt");
		try{
			writer.write("Total user Benefits in utils are \t"+totalUtils+"\t"+"and total user benefits in monetary units is \t"+totalUtils/MARGIN_UTIL_MONEY);
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: "+ e);
		}
	}
}