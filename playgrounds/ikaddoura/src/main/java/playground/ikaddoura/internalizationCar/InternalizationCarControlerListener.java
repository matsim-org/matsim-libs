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
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;

import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;

import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.charts.LineChart;

import playground.ikaddoura.analysis.IKEventHandler;
import playground.vsp.analysis.modules.monetaryTransferPayments.MoneyEventHandler;
import playground.vsp.analysis.modules.userBenefits.UserBenefitsCalculator;
import playground.vsp.analysis.modules.userBenefits.WelfareMeasure;


/**
 * @author Ihab
 *
 */

public class InternalizationCarControlerListener implements StartupListener, IterationEndsListener {
	private static final Logger log = Logger.getLogger(InternalizationCarControlerListener.class);

	private final ScenarioImpl scenario;
	private TollHandler tollHandler;
	private MoneyEventHandler moneyHandler = new MoneyEventHandler();
	private Map<Integer, Double> it2userBenefits_logsum = new HashMap<Integer, Double>();
	private Map<Integer, Double> it2userBenefits_selected = new HashMap<Integer, Double>();
	private Map<Integer, Double> it2tollSum = new HashMap<Integer, Double>();
	
	public InternalizationCarControlerListener(ScenarioImpl scenario, TollHandler tollHandler){
		this.scenario = scenario;
		this.tollHandler = tollHandler;
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		
		EventsManager eventsManager = event.getControler().getEvents();
		
		event.getControler().getEvents().addHandler(new MarginalCongestionHandlerV2(eventsManager, scenario));
		event.getControler().getEvents().addHandler(new MarginalCostPricingCarHandler(eventsManager, scenario));
		
		event.getControler().getEvents().addHandler(tollHandler);
		event.getControler().getEvents().addHandler(moneyHandler);

	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		
		log.info("Set average tolls for each link Id and 15 min time bin.");
		tollHandler.setLinkId2timeBin2avgToll();
		
		writeAnalysis(event);
	}

	private void writeAnalysis(IterationEndsEvent event) {

		UserBenefitsCalculator userBenefitsCalculator_logsum = new UserBenefitsCalculator(this.scenario.getConfig(), WelfareMeasure.LOGSUM);
		this.it2userBenefits_logsum.put(event.getIteration(), userBenefitsCalculator_logsum.calculateUtility_money(event.getControler().getPopulation()));

//		UserBenefitsCalculator userBenefitsCalculator_selected = new UserBenefitsCalculator(this.scenario.getConfig(), WelfareMeasure.SELECTED);		
//		this.it2userBenefits_selected.put(event.getIteration(), userBenefitsCalculator_selected.calculateUtility_money(event.getControler().getPopulation()));

		double scoreSum_selected = 0.;
		for (Person person : event.getControler().getPopulation().getPersons().values()) {
			scoreSum_selected = scoreSum_selected + person.getSelectedPlan().getScore();
		}
		this.it2userBenefits_selected.put(event.getIteration(), scoreSum_selected);

		double tollSum = 0.;
		for (Double amount : moneyHandler.getPersonId2amount().values()){
			tollSum = tollSum + amount;
		}
		this.it2tollSum.put(event.getIteration(), (-1) * tollSum);
		
		String fileName = this.scenario.getConfig().controler().getOutputDirectory() + "/welfareAnalysis.csv";
		File file = new File(fileName);
			
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("Iteration;User Benefits (LogSum);User Benefits (Selected);Total Monetary Payments;Welfare (LogSum);Welfare (Selected)");
			bw.newLine();
			for (Integer it : this.it2userBenefits_selected.keySet()){
				bw.write(it + ";" + this.it2userBenefits_logsum.get(it)
						+ ";" + this.it2userBenefits_selected.get(it) 
						+ ";" + this.it2tollSum.get(it)
						+ ";" + (this.it2userBenefits_logsum.get(it) + this.it2tollSum.get(it))
						+ ";" + (this.it2userBenefits_selected.get(it) + this.it2tollSum.get(it))
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
