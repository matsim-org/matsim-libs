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
package playground.agarwalamit.siouxFalls.userBenefits;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.vsp.analysis.modules.monetaryTransferPayments.MonetaryPaymentsAnalyzer;
import playground.vsp.analysis.modules.userBenefits.WelfareMeasure;

/**
 * @author amit after ikaddoura and benjamin
 */
public class UserBenefitsAndTotalWelfare {
	private final Logger logger = Logger.getLogger(UserBenefitsAndTotalWelfare.class);

	private static String clusterPathDesktop = "/Users/aagarwal/Desktop/ils4/agarwal/munich/";
	private final static String networkFile = "/Users/aagarwal/Desktop/ils4/agarwal/munich/input/network-86-85-87-84_simplifiedWithStrongLinkMerge---withLanes.xml";
	private final static String inputConfigFile = "/Users/aagarwal/Desktop/ils4/agarwal/munich/input/config_munich_1pct_baseCaseCtd.xml";

	private static String [] runNumbers = new String [] {"baseCaseCtd", "ei", "ci", "eci"};
	private static String [] runCases = new String [] {"baseCase","onlyEmission", "onlyCongestion", "both"};
	private final static WelfareMeasure welfareMeasure = WelfareMeasure.SELECTED;
	private static final String outputFolder = "/output/1pct/";
	
	public static void main(String[] args) {
		
		List<String> absoluteDataToWrite = new ArrayList<String>();
		List<String> relativeDataToWrite = new ArrayList<String>();	


		double allUserLogSums [] = new double [runNumbers.length];
		double monetaryPayments[] = new double [runNumbers.length];
		
		absoluteDataToWrite.add("runCase \t user benefits \t tollPayments");
		for(int i=0; i< runNumbers.length;i++){
			allUserLogSums[i] = getAllUserBenefits(runNumbers[i],welfareMeasure)/Math.pow(10, 4);
			monetaryPayments[i] = getMonetaryPayment(runNumbers[i]);
			absoluteDataToWrite.add(runCases[i]+"\t"+getAllUserBenefits(runNumbers[i],welfareMeasure)+"\t"+monetaryPayments[i]);
		}
		writeStrings(clusterPathDesktop+outputFolder+"/analysis/r/rAbsoluteUserBenefits"+welfareMeasure+".txt", absoluteDataToWrite);

		String [] xLabel = {"only Emissions", "only Congestion", "Both"};
		double [] relativeUserLogSum = { Math.pow(10, 4)*(allUserLogSums[1]-allUserLogSums[0]),
				Math.pow(10, 4)*(allUserLogSums[2]-allUserLogSums[0]), Math.pow(10, 4)*(allUserLogSums[3]-allUserLogSums[0])};
		double [] relativeTollPayments = {(monetaryPayments[1]-monetaryPayments[0]),
				(monetaryPayments[2]-monetaryPayments[0]),(monetaryPayments[3]-monetaryPayments[0])};

		double [] sumOfTwo = new double [xLabel.length];
		relativeDataToWrite.add("runCase \t changeInAllUserLogSum \t changeInTollPayments \t changeInUserBenefits");
		for(int j=0; j< xLabel.length;j++){
			sumOfTwo[j] = relativeUserLogSum [j]+Math.abs(relativeTollPayments[j]);//toll payments are already negative thus they will be positive for the system
			relativeDataToWrite.add(runCases[j+1]+"\t"+relativeUserLogSum[j]+"\t"+String.valueOf(-1*relativeTollPayments[j])+"\t"+sumOfTwo[j]); 
		}
		writeStrings(clusterPathDesktop+outputFolder+"/analysis/r/rChangeInSystemWelfare"+welfareMeasure+".txt", relativeDataToWrite);
	}

	private static ScenarioImpl loadScenario(String runNumber) {
		Config config = ConfigUtils.loadConfig(inputConfigFile);
		config.network().setInputFile(networkFile);
		int lastIteration = (int) config.controler().getLastIteration();
//		config.network().setInputFile(clusterPathDesktop+"/input/SiouxFalls_networkWithRoadType.xml.gz");
		config.plans().setInputFile(clusterPathDesktop+outputFolder+runNumber+"/ITERS/it."+lastIteration+"/"+lastIteration+".plans.xml.gz");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		return (ScenarioImpl) scenario;
	}

	public static double getAllUserBenefits(String runNumber, WelfareMeasure welfareMeasure){
		
		
		ScenarioImpl scenarioImpl = loadScenario(runNumber);
		UserBenefitsAnalyzerAA userBenefitsAnalyzer = new UserBenefitsAnalyzerAA();
		userBenefitsAnalyzer.init(scenarioImpl, welfareMeasure);
		userBenefitsAnalyzer.preProcessData();
		//		userBenefitsAnalyzer.postProcessData();
		userBenefitsAnalyzer.writeResults(clusterPathDesktop+outputFolder+runNumber+"/analysis/");
		return userBenefitsAnalyzer.getAllUsersLogSum();
	}

	public static double getMonetaryPayment(String runNumber){
		ScenarioImpl scenarioImpl = loadScenario(runNumber);

		MonetaryPaymentsAnalyzer paymentsAnalyzer = new MonetaryPaymentsAnalyzer();
		paymentsAnalyzer.init(scenarioImpl);
		paymentsAnalyzer.preProcessData();

		EventsManager events = EventsUtils.createEventsManager();
		List<EventHandler> handler = paymentsAnalyzer.getEventHandler();

		for(EventHandler eh : handler){
			events.addHandler(eh);
		}
	
		int lastIteration = (int) scenarioImpl.getConfig().controler().getLastIteration();
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(clusterPathDesktop+outputFolder+runNumber+"/ITERS/it."+lastIteration+"/"+lastIteration+".events.xml.gz");

		paymentsAnalyzer.postProcessData();
		paymentsAnalyzer.writeResults(clusterPathDesktop+outputFolder+runNumber+"/analysis/");
		return paymentsAnalyzer.getAllUsersAmount();
	}

	private static void writeStrings(String outputLocationAndFileName, List<String> listOfStringToWriteInFile){
		BufferedWriter writer = IOUtils.getBufferedWriter(outputLocationAndFileName);
		for(String str : listOfStringToWriteInFile){
			try {
				writer.write(str+"\n");
			} catch (IOException e) {
				throw new RuntimeException("Data is not written into a File. Reason : "+e);
			}
		}
		try {
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException("Data is not written into a File. Reason : "+e);
		}
	}
}
