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

package playground.ikaddoura.analysis.welfare;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.charts.XYLineChart;

import playground.vsp.analysis.modules.monetaryTransferPayments.MoneyEventHandler;
import playground.vsp.analysis.modules.userBenefits.UserBenefitsCalculator;
import playground.vsp.analysis.modules.userBenefits.WelfareMeasure;


/**
 * @author ikaddoura
 *
 */

public class WelfareAnalysisControlerListener implements StartupListener, IterationEndsListener {
	private static final Logger log = Logger.getLogger(WelfareAnalysisControlerListener.class);

	private final Scenario scenario;
	private MoneyEventHandler moneyHandler = new MoneyEventHandler();
	private TripAnalysisHandler tripAnalysisHandler = new TripAnalysisHandler();
	
	private Map<Integer, Double> it2userBenefits_logsum = new TreeMap<Integer, Double>();
	private Map<Integer, Integer> it2invalidPersons_logsum = new TreeMap<Integer, Integer>();
	private Map<Integer, Integer> it2invalidPlans_logsum = new TreeMap<Integer, Integer>();

	private Map<Integer, Double> it2userBenefits_selected = new TreeMap<Integer, Double>();
	private Map<Integer, Integer> it2invalidPersons_selected = new TreeMap<Integer, Integer>();
	private Map<Integer, Integer> it2invalidPlans_selected = new TreeMap<Integer, Integer>();
	
	private Map<Integer, Double> it2tollSum = new TreeMap<Integer, Double>();
	private Map<Integer, Integer> it2stuckEvents = new TreeMap<Integer, Integer>();
	private Map<Integer, Double> it2totalTravelTimeAllModes = new TreeMap<Integer, Double>();
	private Map<Integer, Double> it2totalTravelTimeCarMode = new TreeMap<Integer, Double>();
		
	private Map<Integer, Double> it2carLegs = new TreeMap<Integer, Double>();
	private Map<Integer, Double> it2ptLegs = new TreeMap<Integer, Double>();
	private Map<Integer, Double> it2walkLegs = new TreeMap<Integer, Double>();

	
	public WelfareAnalysisControlerListener(Scenario scenario){
		this.scenario = scenario;
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		event.getServices().getEvents().addHandler(moneyHandler);
		event.getServices().getEvents().addHandler(tripAnalysisHandler);
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		writeAnalysis(event);
	}

	private void writeAnalysis(IterationEndsEvent event) {
		
		UserBenefitsCalculator userBenefitsCalculator_logsum = new UserBenefitsCalculator(this.scenario.getConfig(), WelfareMeasure.LOGSUM, true);
        this.it2userBenefits_logsum.put(event.getIteration(), userBenefitsCalculator_logsum.calculateUtility_money(event.getServices().getScenario().getPopulation()));
		this.it2invalidPersons_logsum.put(event.getIteration(), userBenefitsCalculator_logsum.getPersonsWithoutValidPlanCnt());
		this.it2invalidPlans_logsum.put(event.getIteration(), userBenefitsCalculator_logsum.getInvalidPlans());

		UserBenefitsCalculator userBenefitsCalculator_selected = new UserBenefitsCalculator(this.scenario.getConfig(), WelfareMeasure.SELECTED, true);
        this.it2userBenefits_selected.put(event.getIteration(), userBenefitsCalculator_selected.calculateUtility_money(event.getServices().getScenario().getPopulation()));
		this.it2invalidPersons_selected.put(event.getIteration(), userBenefitsCalculator_selected.getPersonsWithoutValidPlanCnt());
		this.it2invalidPlans_selected.put(event.getIteration(), userBenefitsCalculator_selected.getInvalidPlans());

		double tollSum = this.moneyHandler.getSumOfMonetaryAmounts();
		this.it2tollSum.put(event.getIteration(), (-1) * tollSum);
		this.it2stuckEvents.put(event.getIteration(), this.tripAnalysisHandler.getAgentStuckEvents());
		this.it2totalTravelTimeAllModes.put(event.getIteration(), this.tripAnalysisHandler.getTotalTravelTimeAllModes());
		this.it2totalTravelTimeCarMode.put(event.getIteration(), this.tripAnalysisHandler.getTotalTravelTimeCarMode());
		this.it2carLegs.put(event.getIteration(), (double) this.tripAnalysisHandler.getCarLegs());
		this.it2ptLegs.put(event.getIteration(), (double) this.tripAnalysisHandler.getPtLegs());
		this.it2walkLegs.put(event.getIteration(), (double) this.tripAnalysisHandler.getWalkLegs());
						
		String fileName = this.scenario.getConfig().controler().getOutputDirectory() + "/welfareAnalysis.csv";
		File file = new File(fileName);
			
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("Iteration;" +
					"User Benefits (LogSum);Number of Invalid Persons (LogSum);Number of Invalid Plans (LogSum);" +
					"User Benefits (Selected);Number of Invalid Persons (Selected);Number of Invalid Plans (Selected);" +
					"Total Monetary Payments;Welfare (LogSum);Welfare (Selected);Total Travel Time All Modes (sec);Total Travel Time Car Mode (sec);Avg Travel Time Per Car Trip (sec);Number of Agent Stuck Events;" +
					"Car Trips;Pt Trips;Walk Trips");
			bw.newLine();
			for (Integer it : this.it2userBenefits_selected.keySet()){
				bw.write(it + ";" + this.it2userBenefits_logsum.get(it) + ";" + this.it2invalidPersons_logsum.get(it) + ";" + this.it2invalidPlans_logsum.get(it)
						+ ";" + this.it2userBenefits_selected.get(it) + ";" + this.it2invalidPersons_selected.get(it) + ";" + this.it2invalidPlans_selected.get(it)
						+ ";" + this.it2tollSum.get(it)
						+ ";" + (this.it2userBenefits_logsum.get(it) + this.it2tollSum.get(it))
						+ ";" + (this.it2userBenefits_selected.get(it) + this.it2tollSum.get(it))
						+ ";" + this.it2totalTravelTimeAllModes.get(it)
						+ ";" + this.it2totalTravelTimeCarMode.get(it)
						+ ";" + (this.it2totalTravelTimeCarMode.get(it) / this.it2carLegs.get(it))
						+ ";" + this.it2stuckEvents.get(it)
						+ ";" + this.it2carLegs.get(it)
						+ ";" + this.it2ptLegs.get(it)
						+ ";" + this.it2walkLegs.get(it)
						);
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// ##################################################
		
		writeGraph("userBenefits_selected", "Monetary Units", it2userBenefits_selected);
		writeGraph("userBenefits_logsum", "Monetary Units", it2userBenefits_logsum);
		writeGraph("tollSum", "Monetary Units", it2tollSum);
		writeGraph("totalTravelTimeAllModes", "Seconds", it2totalTravelTimeAllModes);
		writeGraph("totalTravelTimeCarMode", "Seconds", it2totalTravelTimeCarMode);
		writeGraph("carTrips", "Number of Car Trips", it2carLegs);
		writeGraph("ptTrips", "Number of Pt Trips", it2ptLegs);
		writeGraph("walkTrips", "Number of Walk Trips", it2walkLegs);

		writeGraphSum("welfare_logsum", "Monetary Units", it2userBenefits_logsum, it2tollSum);
		writeGraphSum("welfare_selected", "Monetary Units", it2userBenefits_selected, it2tollSum);
		
		writeGraphDiv("avgTripTravelTimeCar", "Seconds", it2totalTravelTimeCarMode, it2carLegs);
	}

	private void writeGraphSum(String name, String yLabel, Map<Integer, Double> it2Double1, Map<Integer, Double> it2Double2) {
		
		XYLineChart chart = new XYLineChart(name, "Iteration", yLabel);

		double[] xValues = new double[it2Double1.size()];
		double[] yValues = new double[it2Double1.size()];
		int counter = 0;
		for (Integer iteration : it2Double1.keySet()){
			xValues[counter] = iteration.doubleValue();
			yValues[counter] = (it2Double1.get(iteration)) + (it2Double2.get(iteration));
			counter++;
		}
		
		chart.addSeries(name, xValues, yValues);
		
		XYPlot plot = chart.getChart().getXYPlot(); 
		NumberAxis axis = (NumberAxis) plot.getRangeAxis();
		axis.setAutoRange(true);
		axis.setAutoRangeIncludesZero(false);
		
		String outputFile = this.scenario.getConfig().controler().getOutputDirectory() + "/" + name + ".png";
		chart.saveAsPng(outputFile, 1000, 800); // File Export
	}
	
	private void writeGraphDiv(String name, String yLabel, Map<Integer, Double> it2Double1, Map<Integer, Double> it2Double2) {
		
		XYLineChart chart = new XYLineChart(name, "Iteration", yLabel);

		double[] xValues = new double[it2Double1.size()];
		double[] yValues = new double[it2Double1.size()];
		int counter = 0;
		for (Integer iteration : it2Double1.keySet()){
			xValues[counter] = iteration.doubleValue();
			yValues[counter] = (it2Double1.get(iteration)) / (it2Double2.get(iteration));
			counter++;
		}
		
		chart.addSeries(name, xValues, yValues);
		
		XYPlot plot = chart.getChart().getXYPlot(); 
		NumberAxis axis = (NumberAxis) plot.getRangeAxis();
		axis.setAutoRange(true);
		axis.setAutoRangeIncludesZero(false);
		
		String outputFile = this.scenario.getConfig().controler().getOutputDirectory() + "/" + name + ".png";
		chart.saveAsPng(outputFile, 1000, 800); // File Export
	}

	private void writeGraph(String name, String yLabel, Map<Integer, Double> it2Double) {

		XYLineChart chart = new XYLineChart(name, "Iteration", yLabel);

		double[] xValues = new double[it2Double.size()];
		double[] yValues = new double[it2Double.size()];
		int counter = 0;
		for (Integer iteration : it2Double.keySet()){
			xValues[counter] = iteration.doubleValue();
			yValues[counter] = it2Double.get(iteration);
			counter++;
		}
		
		chart.addSeries(name, xValues, yValues);
		
		XYPlot plot = chart.getChart().getXYPlot(); 
		NumberAxis axis = (NumberAxis) plot.getRangeAxis();
		axis.setAutoRange(true);
		axis.setAutoRangeIncludesZero(false);
		
		String outputFile = this.scenario.getConfig().controler().getOutputDirectory() + "/" + name + ".png";
		chart.saveAsPng(outputFile, 1000, 800); // File Export
	}

}
