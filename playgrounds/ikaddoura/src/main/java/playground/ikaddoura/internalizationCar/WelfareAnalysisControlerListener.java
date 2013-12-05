/* *********************************************************************** *
 * project: org.matsim.*
 * MyControlerListener.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

/**
 * 
 */

package playground.ikaddoura.internalizationCar;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;

import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.charts.LineChart;

import playground.vsp.analysis.modules.monetaryTransferPayments.MoneyEventHandler;
import playground.vsp.analysis.modules.userBenefits.UserBenefitsCalculator;
import playground.vsp.analysis.modules.userBenefits.WelfareMeasure;


/**
 * @author Ihab
 *
 */

public class WelfareAnalysisControlerListener implements StartupListener, IterationEndsListener {
	private static final Logger log = Logger.getLogger(WelfareAnalysisControlerListener.class);

	private final ScenarioImpl scenario;
	private MoneyEventHandler moneyHandler = new MoneyEventHandler();
	private TripAnalysisHandler tripAnalysisHandler = new TripAnalysisHandler();
	
	private Map<Integer, Double> it2userBenefits_logsum = new HashMap<Integer, Double>();
	private Map<Integer, Integer> it2invalidPersons_logsum = new HashMap<Integer, Integer>();
	private Map<Integer, Integer> it2invalidPlans_logsum = new HashMap<Integer, Integer>();

	private Map<Integer, Double> it2userBenefits_selected = new HashMap<Integer, Double>();
	private Map<Integer, Integer> it2invalidPersons_selected = new HashMap<Integer, Integer>();
	private Map<Integer, Integer> it2invalidPlans_selected = new HashMap<Integer, Integer>();
	
	private Map<Integer, Double> it2tollSum = new HashMap<Integer, Double>();
	private Map<Integer, Integer> it2stuckEvents = new HashMap<Integer, Integer>();
	private Map<Integer, Double> it2totalTravelTime = new HashMap<Integer, Double>();
	
	public WelfareAnalysisControlerListener(ScenarioImpl scenario){
		this.scenario = scenario;
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		event.getControler().getEvents().addHandler(moneyHandler);
		event.getControler().getEvents().addHandler(tripAnalysisHandler);
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		writeAnalysis(event);
	}

	private void writeAnalysis(IterationEndsEvent event) {
		
		UserBenefitsCalculator userBenefitsCalculator_logsum = new UserBenefitsCalculator(this.scenario.getConfig(), WelfareMeasure.LOGSUM, false);
		this.it2userBenefits_logsum.put(event.getIteration(), userBenefitsCalculator_logsum.calculateUtility_money(event.getControler().getPopulation()));
		this.it2invalidPersons_logsum.put(event.getIteration(), userBenefitsCalculator_logsum.getPersonsWithoutValidPlanCnt());
		this.it2invalidPlans_logsum.put(event.getIteration(), userBenefitsCalculator_logsum.getInvalidPlans());

		UserBenefitsCalculator userBenefitsCalculator_selected = new UserBenefitsCalculator(this.scenario.getConfig(), WelfareMeasure.SELECTED, false);		
		this.it2userBenefits_selected.put(event.getIteration(), userBenefitsCalculator_selected.calculateUtility_money(event.getControler().getPopulation()));
		this.it2invalidPersons_selected.put(event.getIteration(), userBenefitsCalculator_selected.getPersonsWithoutValidPlanCnt());
		this.it2invalidPlans_selected.put(event.getIteration(), userBenefitsCalculator_selected.getInvalidPlans());

		double tollSum = this.moneyHandler.getSumOfMonetaryAmounts();
		this.it2tollSum.put(event.getIteration(), (-1) * tollSum);
		this.it2stuckEvents.put(event.getIteration(), this.tripAnalysisHandler.getAgentStuckEvents());
		this.it2totalTravelTime.put(event.getIteration(), this.tripAnalysisHandler.getTotalTravelTime());
		
		String fileName = this.scenario.getConfig().controler().getOutputDirectory() + "/welfareAnalysis.csv";
		File file = new File(fileName);
			
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("Iteration;" +
					"User Benefits (LogSum);Number of Invalid Persons (LogSum);Number of Invalid Plans (LogSum);" +
					"User Benefits (Selected);Number of Invalid Persons (Selected);Number of Invalid Plans (Selected);" +
					"Total Monetary Payments;Welfare (LogSum);Welfare (Selected);Total Travel Time (sec);Number of Agent Stuck Events");
			bw.newLine();
			for (Integer it : this.it2userBenefits_selected.keySet()){
				bw.write(it + ";" + this.it2userBenefits_logsum.get(it) + ";" + this.it2invalidPersons_logsum.get(it) + ";" + this.it2invalidPlans_logsum.get(it)
						+ ";" + this.it2userBenefits_selected.get(it) + ";" + this.it2invalidPersons_selected.get(it) + ";" + this.it2invalidPlans_selected.get(it)
						+ ";" + this.it2tollSum.get(it)
						+ ";" + (this.it2userBenefits_logsum.get(it) + this.it2tollSum.get(it))
						+ ";" + (this.it2userBenefits_selected.get(it) + this.it2tollSum.get(it))
						+ ";" + this.it2totalTravelTime.get(it)
						+ ";" + this.it2stuckEvents.get(it)
						);
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// ##################################################
		
		String[] xValues = new String[this.it2tollSum.size()];
		int cc = 0;
		for (Integer xValue : it2tollSum.keySet()){
			xValues[cc] = xValue.toString();
			cc++;
		}
		
		LineChart chart = new LineChart("Welfare analysis", "Iteration", "Monetary Units", xValues);
	    
		// selected
		double[] yWerte1 = new double[this.it2userBenefits_selected.size()];
		int counter1 = 0;
		for (Integer iteration : it2userBenefits_selected.keySet()){
			xValues[counter1] = iteration.toString();
			yWerte1[counter1] = it2userBenefits_selected.get(iteration);
			counter1++;
		}
		chart.addSeries("User benefits (selected)", yWerte1);
		
		// logsum
		double[] yWerte2 = new double[this.it2userBenefits_logsum.size()];
		int counter2 = 0;
		for (Integer iteration : it2userBenefits_logsum.keySet()){
			xValues[counter2] = iteration.toString();
			yWerte2[counter2] = it2userBenefits_logsum.get(iteration);
			counter2++;
		}
		chart.addSeries("User benefits (logsum)", yWerte2);
		
		// toll sum
		double[] yWerte3 = new double[this.it2tollSum.size()];
		int counter3 = 0;
		for (Integer iteration : it2tollSum.keySet()){
			xValues[counter3] = iteration.toString();
			yWerte3[counter3] = it2tollSum.get(iteration);
			counter3++;
		}
		chart.addSeries("Toll sum", yWerte3);
		
		// welfare logsum
		double[] yWerte4 = new double[this.it2tollSum.size()];
		int counter4 = 0;
		for (Integer iteration : it2tollSum.keySet()){
			xValues[counter4] = iteration.toString();
			yWerte4[counter4] = (it2userBenefits_logsum.get(iteration) + it2tollSum.get(iteration));
			counter4++;
		}
		chart.addSeries("Welfare (logsum)", yWerte4);
		
		// welfare selected
		double[] yWerte5 = new double[this.it2tollSum.size()];
		int counter5 = 0;
		for (Integer iteration : it2tollSum.keySet()){
			xValues[counter5] = iteration.toString();
			yWerte5[counter5] = (it2userBenefits_selected.get(iteration) + it2tollSum.get(iteration));
			counter5++;
		}
		chart.addSeries("Welfare (selected)", yWerte5);
		
		String outputFile = this.scenario.getConfig().controler().getOutputDirectory() + "/welfareAnalysis.png";
		chart.saveAsPng(outputFile, 1000, 800); // File Export
	}

}
