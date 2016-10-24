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
package playground.agarwalamit.analysis.userBenefits;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.utils.LoadMyScenarios;
import playground.vsp.analysis.modules.monetaryTransferPayments.MonetaryPaymentsAnalyzer;
import playground.vsp.analysis.modules.userBenefits.WelfareMeasure;

/**
 * @author amit
 */
public class UserBenefitsAndTotalWelfare {

	public static final Logger LOGGER = Logger.getLogger(UserBenefitsAndTotalWelfare.class);
	private final String outputDir;
	private static final WelfareMeasure WM = WelfareMeasure.SELECTED;
	private MutableScenario sc;
	
	public UserBenefitsAndTotalWelfare(final String outputDir) {
		this.outputDir = outputDir;
	}

	public static void main(String[] args) {
		String clusterPathDesktop = "/Users/amit/Documents/repos/runs-svn/detEval/emissionCongestionInternalization/output/1pct/run9/";
		String [] runCases =  {"baseCaseCtd","ei","ci","eci","ei_10"};
		
		 new UserBenefitsAndTotalWelfare(clusterPathDesktop).runAndWrite(runCases);
	}

	public void runAndWrite(final String [] runCases){

		double [] userBenefitsMoney = new double [runCases.length];
		double [] monetaryPayments = new double [runCases.length];
		double [] excludedToll = new double [runCases.length];
		
		for(int i=0; i< runCases.length;i++){
			loadScenario(runCases[i]);
			userBenefitsMoney[i] = getAllUserBenefits(runCases[i], WM);
			double tollInfo [] = getMonetaryPayment(runCases[i], false);
			monetaryPayments[i] = tollInfo[0];
			excludedToll[i] = tollInfo[1];
		}
		
		String fileName = outputDir+"/analysis/absoluteUserBenefits"+WM+".txt";
		BufferedWriter writer = IOUtils.getBufferedWriter(fileName);
		try {
			writer.write("runCase \t userBenefits_money \t tollPayments \t excludedTollIfAny \n");
			for(int i=0; i< runCases.length;i++){
				writer.write(runCases[i]+"\t"+userBenefitsMoney[i]+"\t"+monetaryPayments[i]+"\t"+excludedToll[i]+"\n");
			}
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException("Data is not written into a File. Reason : "+e);
		}
		LOGGER.info("Data is written to "+fileName);
	}

	private void loadScenario(final String runCase) {
		String runPath = outputDir + runCase;
		String configFile = runPath+"/output_config.xml";
		String plansFile = outputDir+runCase+"/output_plans.xml.gz";
		Scenario scenario = LoadMyScenarios.loadScenarioFromPlansAndConfig(plansFile, configFile);
		sc = (MutableScenario) scenario;
	}

	public double getAllUserBenefits(final String runCase, final WelfareMeasure welfareMeasure){
		MyUserBenefitsAnalyzer userBenefitsAnalyzer = new MyUserBenefitsAnalyzer();
		userBenefitsAnalyzer.init(sc, welfareMeasure, false);
		userBenefitsAnalyzer.preProcessData();
		userBenefitsAnalyzer.postProcessData();
		userBenefitsAnalyzer.writeResults(outputDir+runCase+"/analysis/");
		return userBenefitsAnalyzer.getTotalUserWelfareMoney();
	}

	/**
	 * @param runCase desired scenario
	 * @param considerAllPersonsInSumOfTolls persons having negative score will be excluded from sum to tolls
	 * @return returns total toll excluding persons with negative score (currently person will be excluded if score is negative)
	 */
	public double [] getMonetaryPayment(final String runCase, final boolean considerAllPersonsInSumOfTolls){
		double totalToll =0;
		double excludedToll =0;

		MonetaryPaymentsAnalyzer paymentsAnalyzer = new MonetaryPaymentsAnalyzer();
		paymentsAnalyzer.init(sc);
		paymentsAnalyzer.preProcessData();

		EventsManager events = EventsUtils.createEventsManager();
		List<EventHandler> handler = paymentsAnalyzer.getEventHandler();

		for(EventHandler eh : handler){
			events.addHandler(eh);
		}

		int lastIteration = sc.getConfig().controler().getLastIteration();
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(outputDir+runCase+"/ITERS/it."+lastIteration+"/"+lastIteration+".events.xml.gz");

		paymentsAnalyzer.postProcessData();
		paymentsAnalyzer.writeResults(outputDir+runCase+"/analysis/");

		Map<Id<Person>, Double> personId2amount = paymentsAnalyzer.getPersonId2amount();

		if(! considerAllPersonsInSumOfTolls){
			for (Id<Person> personId : personId2amount.keySet()){
				if(isPersonIncluded(personId)) totalToll += personId2amount.get(personId);
				else {
					excludedToll += personId2amount.get(personId);
				}
			}
		} else totalToll = paymentsAnalyzer.getAllUsersAmount();

		if(!considerAllPersonsInSumOfTolls) LOGGER.warn("Person having negative score in their executed plans are excluded in the calculation."
				+ " \n and thus toll payments which is excluded is " +excludedToll);
		return new double [] {totalToll, excludedToll};
	}

	private boolean isPersonIncluded(final Id<Person> personId){
		Id<Person> id = Id.createPersonId(personId.toString());
		double score = sc.getPopulation().getPersons().get(id).getSelectedPlan().getScore();
        return score >= 0;
	}
}