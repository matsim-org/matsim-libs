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
package playground.agarwalamit.siouxFalls;

import java.io.BufferedWriter;
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.ikaddoura.internalizationCar.MarginalCongestionHandlerImplV3;
import playground.vsp.analysis.modules.emissionsAnalyzer.EmissionsAnalyzer;

/**
 * @author amit
 */
public class VerifyResults {
	/*Values taken from IMPACT (Maibach et al.(2008))*/
	private static final Logger log = Logger.getLogger(VerifyResults.class);
	private static final double EURO_PER_GRAMM_NOX = 9600. / (1000. * 1000.);
	private static final double EURO_PER_GRAMM_NMVOC = 1700. / (1000. * 1000.);
	private static  final double EURO_PER_GRAMM_SO2 = 11000. / (1000. * 1000.);
	private static  final double EURO_PER_GRAMM_PM2_5_EXHAUST = 384500. / (1000. * 1000.);
	private static final double EURO_PER_GRAMM_CO2 = 70. / (1000. * 1000.);

	private static final double marginal_Utl_money=0.0789942;//0.062 //(for SiouxFalls =0.062 and for Munich =0.0789942);
	private static final double marginal_Utl_performing_sec=0.96/3600;
	private static final double marginal_Utl_traveling_car_sec=-0.0/3600;
	private static final double marginalUtlOfTravelTime = marginal_Utl_traveling_car_sec+marginal_Utl_performing_sec;
	private static final double vtts_car = marginalUtlOfTravelTime/marginal_Utl_money;

	private final  static String runDir = "/Users/aagarwal/Desktop/ils4/agarwal/munich/output/1pct/";
	private final static String networkFile = "/Users/aagarwal/Desktop/ils4/agarwal/munich/input/network-86-85-87-84_simplifiedWithStrongLinkMerge---withLanes.xml";
	private final static String inputConfigFile = "/Users/aagarwal/Desktop/ils4/agarwal/munich/input/config_munich_1pct_baseCaseCtd.xml";

	private  final static String [] runNr = {"baseCaseCtd","ei","ci","eci"};//{"201","202", "203","204"};//{"1","2","3","4"};

	private  static Scenario scenario ;

	private static final boolean considerCO2Costs=true;
	private static final double emissionCostFacotr=1.0;

	public static void main(String[] args) {

		Config config = ConfigUtils.loadConfig(inputConfigFile);
		config.network().setInputFile(networkFile);

		for(int i=0;i<runNr.length;i++){

			int lastItenation = (int) getLastIteration(config);
			String emissionsEventsFile = runDir+runNr[i]+"/ITERS/it."+lastItenation+"/"+lastItenation+".emission.events.xml.gz";
			String plansFile = runDir+runNr[i]+"/ITERS/it."+lastItenation+"/"+lastItenation+".plans.xml.gz";
			config.plans().setInputFile(plansFile);
			scenario = ScenarioUtils.loadScenario(config);
			String eventsFile=runDir+runNr[i]+"/ITERS/it."+lastItenation+"/"+lastItenation+".events.xml.gz";

//			calculateEmissionCosts(emissionsEventsFile, scenario,runNr[i]);
//			calculateDelaysCosts(eventsFile,scenario,runNr[i]);
			calculateUserBenefits(scenario, runNr[i]);
		}
		Logger.getLogger(VerifyResults.class).info("Writing files is finsished.");
	}

	private static void calculateEmissionCosts(String emissionsEventsFile, Scenario scenario, String runNr){
		EmissionsAnalyzer analyzer	= new EmissionsAnalyzer(emissionsEventsFile);
		analyzer.init((ScenarioImpl) scenario);
		analyzer.preProcessData();
		analyzer.postProcessData();
		SortedMap<String, Double> totalEmissions = analyzer.getTotalEmissions();
		double totalEmissionCost =0;
		BufferedWriter writer = IOUtils.getBufferedWriter(runDir+runNr+"/analysis/verifyTotalEmissionCost.txt");
		try {
			for(String str:totalEmissions.keySet()){
				if(str.equals("NOX")) {
					double noxCosts = totalEmissions.get(str) * EURO_PER_GRAMM_NOX;
					writer.write("NOX emissions in gm  are = "+"\t"+totalEmissions.get(str).toString()+"\t"+". Total NOX emission cost is "+noxCosts);
					totalEmissionCost += noxCosts;
				} else if(str.equals("NMHC")) {
					double nmhcCosts =totalEmissions.get(str) * EURO_PER_GRAMM_NMVOC;
					writer.write("NMHC emissions in gm  are = "+"\t"+totalEmissions.get(str).toString()+"\t"+". Total NMHC emission cost is "+nmhcCosts);
					totalEmissionCost += nmhcCosts;
				} else if(str.equals("SO2")) {
					double so2Costs = totalEmissions.get(str) * EURO_PER_GRAMM_SO2;
					writer.write("SO2 emissions in gm  are = "+"\t"+totalEmissions.get(str).toString()+"\t"+". Total SO2 emission cost is "+so2Costs);
					totalEmissionCost += so2Costs;
				} else if(str.equals("PM")) {
					double pmCosts = totalEmissions.get(str) * EURO_PER_GRAMM_PM2_5_EXHAUST;
					writer.write("PM emissions in gm  are = "+"\t"+totalEmissions.get(str).toString()+"\t"+". Total PM emission cost is "+pmCosts);
					totalEmissionCost += pmCosts;
				} else if(str.equals("CO2_TOTAL")){
					if(considerCO2Costs) {
						double co2Costs = totalEmissions.get(str) * EURO_PER_GRAMM_CO2;
						writer.write("CO2 emissions in gm  are = "+"\t"+totalEmissions.get(str).toString()+"\t"+". Total CO2 emission cost is "+co2Costs);
						totalEmissionCost += co2Costs;
					} else ; //do nothing
				}
				else ; //do nothing
				writer.newLine();
			}
			writer.write("Emission cost factor is "+"\t"+emissionCostFacotr+"\t"+"and total cost of emissions is "+"\t"+emissionCostFacotr*totalEmissionCost);
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: "+ e);
		}
	}

	public static void calculateDelaysCosts(String eventsFile,Scenario scenario, String runNr){
		EventsManager em = EventsUtils.createEventsManager();
		MarginalCongestionHandlerImplV3 congestionHandler = new MarginalCongestionHandlerImplV3(em, (ScenarioImpl) scenario);
		MatsimEventsReader eventsReader = new MatsimEventsReader(em);
		em.addHandler(congestionHandler);
		eventsReader.readFile(eventsFile);
		BufferedWriter writer = IOUtils.getBufferedWriter(runDir+runNr+"/analysis/verifyTotalDelayCost.txt");
		try{
			writer.write("Total delays in sec are \t"+congestionHandler.getTotalDelay()+"\t"+"and total payment due to delays is \t"+vtts_car*congestionHandler.getTotalDelay());
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: "+ e);
		}
	}

	public static void calculateUserBenefits(Scenario scenario, String runNr){
		Population population = scenario.getPopulation();
		double totalUtils=0;
		for(Person p : population.getPersons().values()){
			double personScore = p.getSelectedPlan().getScore();
			if(personScore<0) {
				log.warn("Utility for person "+p.getId()+" is negative and this ignoring this in user benefit callculation.");
				personScore=0;
			}
			totalUtils+=personScore;
		}
		BufferedWriter writer = IOUtils.getBufferedWriter(runDir+runNr+"/analysis/verifyUserBenefits.txt");
		try{
			writer.write("Total user Benefits in utils are \t"+totalUtils+"\t"+"and total user benefits in monetary units is \t"+totalUtils/marginal_Utl_money);
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: "+ e);
		}
	}

	private static double getLastIteration (Config config){
		return config.controler().getLastIteration();
	}
}
