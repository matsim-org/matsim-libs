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

package playground.ikaddoura.analysis.detailedPersonTripAnalysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
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
import org.matsim.contrib.taxi.run.*;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.utils.charts.XYLineChart;

import com.google.inject.Inject;

import playground.ikaddoura.analysis.detailedPersonTripAnalysis.handler.BasicPersonTripAnalysisHandler;
import playground.ikaddoura.analysis.detailedPersonTripAnalysis.handler.NoiseAnalysisHandler;


/**
 * 
 * 
 * @author ikaddoura
 *
 */

public class AnalysisControlerListener implements IterationEndsListener {
		
	private static final Logger log = Logger.getLogger(AnalysisControlerListener.class);
	
	private final SortedMap<Integer, Double> iteration2userBenefits = new TreeMap<>();
	private final SortedMap<Integer, Double> iteration2totalTollPayments = new TreeMap<>();
	private final SortedMap<Integer, Double> iteration2totalTravelTime = new TreeMap<>();
	private final SortedMap<Integer, Double> iteration2totalNoiseDamages = new TreeMap<>();
	
	@Inject
	private Scenario scenario;
	
	@Inject
	private BasicPersonTripAnalysisHandler basicHandler;
	
	@Inject
	private NoiseAnalysisHandler noiseHandler;
	
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		
		String runDirectory = this.scenario.getConfig().controler().getOutputDirectory();
		if (!runDirectory.endsWith("/")) runDirectory = runDirectory + "/";
		
		String outputPathAnalysisIteration = runDirectory + "ITERS/it." + event.getIteration() + "/";
		
		double userBenefits = 0.;
		Map<Id<Person>, Double> personId2userBenefit = new HashMap<>();
		for (Person person : scenario.getPopulation().getPersons().values()) {
			double userBenefit = person.getSelectedPlan().getScore() / scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney();
			personId2userBenefit.put(person.getId(), userBenefit);
			userBenefits += userBenefit;
		}
		
		PersonTripNoiseAnalysis analysis = new PersonTripNoiseAnalysis();
		
		if (event.getIteration() == this.scenario.getConfig().controler().getLastIteration()) {
			log.info("Print trip information...");
			analysis.printTripInformation(outputPathAnalysisIteration, TaxiModule.TAXI_MODE, basicHandler, noiseHandler);
			analysis.printTripInformation(outputPathAnalysisIteration, TransportMode.car, basicHandler, noiseHandler);
			analysis.printTripInformation(outputPathAnalysisIteration, null, basicHandler, noiseHandler);
			log.info("Print trip information... Done.");

			log.info("Print person information...");
			analysis.printPersonInformation(outputPathAnalysisIteration, TaxiModule.TAXI_MODE, personId2userBenefit, basicHandler, noiseHandler);	
			analysis.printPersonInformation(outputPathAnalysisIteration, TransportMode.car, personId2userBenefit, basicHandler, noiseHandler);	
			analysis.printPersonInformation(outputPathAnalysisIteration, null, personId2userBenefit, basicHandler, noiseHandler);	
			log.info("Print person information... Done.");
		}

		analysis.printAggregatedResults(outputPathAnalysisIteration, TaxiModule.TAXI_MODE, personId2userBenefit, basicHandler, noiseHandler);
		analysis.printAggregatedResults(outputPathAnalysisIteration, TransportMode.car, personId2userBenefit, basicHandler, noiseHandler);
		analysis.printAggregatedResults(outputPathAnalysisIteration, null, personId2userBenefit, basicHandler, noiseHandler);
		
		// all iterations
				
		this.iteration2userBenefits.put(event.getIteration(), userBenefits);
		this.iteration2totalTollPayments.put(event.getIteration(), basicHandler.getTotalPaymentsByPersons());
		this.iteration2totalTravelTime.put(event.getIteration(), basicHandler.getTotalTravelTimeByPersons());
		this.iteration2totalNoiseDamages.put(event.getIteration(), noiseHandler.getAffectedNoiseCost());
						
		writeIterationStats(
				this.iteration2userBenefits,
				this.iteration2totalTollPayments,
				this.iteration2totalTravelTime,
				this.iteration2totalNoiseDamages,
				runDirectory
				);
		
		XYLineChart chart1 = new XYLineChart("Total travel time", "Iteration", "Hours");
		double[] iterations1 = new double[event.getIteration() + 1];
		double[] values1a = new double[event.getIteration() + 1];
		for (int i = this.scenario.getConfig().controler().getFirstIteration(); i <= event.getIteration(); i++) {
			iterations1[i] = i;
			values1a[i] = this.iteration2totalTravelTime.get(i) / 3600.;
		}
		chart1.addSeries("Total travel time", iterations1, values1a);
		chart1.saveAsPng(runDirectory + "totalTravelTime.png", 800, 600);
		
		XYLineChart chart2 = new XYLineChart("System welfare, user benefits, noise damages and toll revenues", "Iteration", "EUR");
		double[] iterations2 = new double[event.getIteration() + 1];
		double[] values2a = new double[event.getIteration() + 1];
		double[] values2b = new double[event.getIteration() + 1];
		double[] values2c = new double[event.getIteration() + 1];
		double[] values2d = new double[event.getIteration() + 1];

		for (int i = this.scenario.getConfig().controler().getFirstIteration(); i <= event.getIteration(); i++) {
			iterations2[i] = i;
			values2a[i] = this.iteration2userBenefits.get(i) + this.iteration2totalTollPayments.get(i) - this.iteration2totalNoiseDamages.get(i);
			values2b[i] = this.iteration2userBenefits.get(i);
			values2c[i] = this.iteration2totalTollPayments.get(i);
			values2d[i] = this.iteration2totalNoiseDamages.get(i);
		}
		chart2.addSeries("System welfare", iterations2, values2a);
		chart2.addSeries("User benefits", iterations2, values2b);
		chart2.addSeries("Toll revenues", iterations2, values2c);
		chart2.addSeries("Noise damages", iterations2, values2d);
		chart2.saveAsPng(runDirectory + "systemWelfare_userBenefits_noiseDamages_tollRevenues.png", 800, 600);
		
		XYLineChart chart3 = new XYLineChart("Noise damages [EUR]", "Iteration", "EUR");
		double[] iterations3 = new double[event.getIteration() + 1];
		double[] values3 = new double[event.getIteration() + 1];
		for (int i = this.scenario.getConfig().controler().getFirstIteration(); i <= event.getIteration(); i++) {
			iterations3[i] = i;
			values3[i] = this.iteration2totalNoiseDamages.get(i);
		}
		chart3.addSeries("Noise damages", iterations3, values3);
		chart3.saveAsPng(runDirectory + "noiseDamages.png", 800, 600);
	}
	
	private static void writeIterationStats(
			SortedMap<Integer, Double> iteration2userBenefits,
			SortedMap<Integer, Double> iteration2totalTollPayments,
			SortedMap<Integer, Double> iteration2totalTravelTime,
			SortedMap<Integer, Double> iteration2totalNoiseDamages,
			String outputDirectory) {
		
		String fileName = outputDirectory + "welfare-noise-analysis.csv";
		File file = new File(fileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			
			bw.write("Iteration ; Total user benefits [monetary units] ; Total toll payments (by persons) [monetary units] ; Total travel time [hours]; Total noise damages [monetary units]; System welfare [monetary units]");
			bw.newLine();
			
			for (Integer iteration : iteration2userBenefits.keySet()) {
				bw.write(iteration + " ; "
						+ iteration2userBenefits.get(iteration) + " ; "
						+ iteration2totalTollPayments.get(iteration) + " ; "
						+ iteration2totalTravelTime.get(iteration) / 3600. + " ; "
						+ iteration2totalNoiseDamages.get(iteration) + ";"
						+ (iteration2totalTollPayments.get(iteration) + iteration2userBenefits.get(iteration) - iteration2totalNoiseDamages.get(iteration))
						);
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
