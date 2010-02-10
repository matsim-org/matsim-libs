/* *********************************************************************** *
 * project: org.matsim.*
 * TravelStats.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.anhorni.locationchoice.analysis.plans;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.io.IOUtils;

import playground.anhorni.locationchoice.preprocess.helper.Utils;

/**
 *
 * Calculates at the end of each iteration the following statistics:
 * <ul>
 * <li>average leg travel measure (worst plans only)</li>
 * <li>average leg travel measure (best plans only)</li>
 * <li>average leg travel measure (selected plans only)</li>
 * <li>average leg travel measure (all plans)</li>
 * </ul>
 * 
 * @author anhorni
 */

/*
 * TODO: [AH] This is copy-paste based on "ScoreStats". Refactoring needed!
 */
public class TravelStats implements StartupListener, IterationEndsListener, ShutdownListener {

	final private static int INDEX_WORST = 0;
	final private static int INDEX_BEST = 1;
	final private static int INDEX_ALL = 2;
	final private static int INDEX_EXECUTED = 3;
	final private static int INDEX_MEDIANEXECUTED = 4;

	private Population population;
	private final boolean createPNG;
	private double[][] history = null;
	private int minIteration = 0;
	
	private PlanLegsTravelMeasureCalculator calculator;
	private String measure = "all";
	
	private List<Double> legTravelMeasures = new Vector<Double>();
	private boolean wayThere = false;
	
	private DecimalFormat formatter = new DecimalFormat("0.0");

	private final static Logger log = Logger.getLogger(TravelStats.class);

	/**
	 * @param population
	 * @param filename
	 * @param createPNG true if in every iteration, the travel stats should be visualized in a graph and written to disk.
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public TravelStats(final boolean createPNG, PlanLegsTravelMeasureCalculator calculator, String measure, boolean wayThere)	{
		this.measure = measure;
		this.calculator = calculator;
		this.createPNG = createPNG;
		this.wayThere = wayThere;
	}

	public void notifyStartup(final StartupEvent event) {
		if (this.createPNG) {
			Controler controler = event.getControler();
			this.minIteration = controler.getFirstIteration();
			int maxIter = controler.getLastIteration();
			int iterations = maxIter - this.minIteration;
			if (iterations > 10000) {
				iterations = 1000; // limit the history size
			}
			this.history = new double[5][iterations+1];
		}
		this.population = event.getControler().getPopulation();
	}

	public void notifyIterationEnds(final IterationEndsEvent event) {
		
		this.legTravelMeasures.clear();
		
		double sumLegTravelMeasureWorst = 0.0;
		double sumLegTravelMeasureBest = 0.0;
		double sumLegTravelMeasureExecuted = 0.0;
		double sumLegTravelMeasureAll = 0.0;
		int nofLegTravelMeasureWorst = 0;
		int nofLegTravelMeasureBest = 0;
		int nofLegTravelMeasureExecuted = 0;
		int nofLegTravelMeasureAll = 0;
		
		for (Person person : this.population.getPersons().values()) {
			Plan worstPlan = null;
			Plan bestPlan = null;
			double worstScore = Double.POSITIVE_INFINITY;
			double bestScore = Double.NEGATIVE_INFINITY;
			for (Plan plan : person.getPlans()) {			
				if (plan.getScore() == null) {
					continue;
				}
				double score = plan.getScore().doubleValue();
				
				// worst plan -----------------------------------------------------
				if (worstPlan == null) {
					worstPlan = plan;
					worstScore = score;
				} else if (score < worstScore) {
					worstPlan = plan;
					worstScore = score;
				}

				// best plan -------------------------------------------------------
				if (bestPlan == null) {
					bestPlan = plan;
					bestScore = score;
				} else if (score > bestScore) {
					bestPlan = plan;
					bestScore = score;
				}
				this.calculator.handle(plan, this.wayThere);
				sumLegTravelMeasureAll += this.calculator.getSumLegsTravelMeasure();
				nofLegTravelMeasureAll += this.calculator.getNbrOfLegs();

				// executed plan? --------------------------------------------------
				if (plan.isSelected()) {
					legTravelMeasures.addAll(this.calculator.handle(plan, this.wayThere));
					sumLegTravelMeasureExecuted += this.calculator.getSumLegsTravelMeasure();
					nofLegTravelMeasureExecuted += this.calculator.getNbrOfLegs();				
				}
			}
			if (worstPlan != null) {
				this.calculator.handle(worstPlan, this.wayThere);				
				sumLegTravelMeasureWorst += this.calculator.getSumLegsTravelMeasure();
				nofLegTravelMeasureWorst += this.calculator.getNbrOfLegs();
			}
			if (bestPlan != null) {
				this.calculator.handle(bestPlan, this.wayThere);	
				sumLegTravelMeasureBest += this.calculator.getSumLegsTravelMeasure();;
				nofLegTravelMeasureBest += this.calculator.getNbrOfLegs();
			}
		}
		if (this.calculator.mode.equals("all") && this.calculator.getActType().equals("all")) {
			log.info("-- average leg travel " + this.measure + " way there= " + this.wayThere + " (executed plans only): " 
					+ formatter.format(sumLegTravelMeasureExecuted / nofLegTravelMeasureExecuted));
			log.info("-- average leg travel " + this.measure + " way there= " + this.wayThere + " (worst plans only): " 
					+ formatter.format(sumLegTravelMeasureWorst / nofLegTravelMeasureWorst));
			log.info("-- average leg travel " + this.measure + " way there= " + this.wayThere + " (all plans): " 
					+ formatter.format(sumLegTravelMeasureAll / nofLegTravelMeasureAll));
			log.info("-- average leg travel " + this.measure + " way there= " + this.wayThere + " (best plans only): " 
					+ formatter.format(sumLegTravelMeasureBest / nofLegTravelMeasureBest));
			log.info("-- median leg travel " + this.measure + " way there= " + this.wayThere + " (executed plans only): " 
					+ formatter.format(Utils.median(legTravelMeasures)));
		}

		if (this.history != null) {
			int index = event.getIteration() - this.minIteration;
			this.history[INDEX_WORST][index] = (sumLegTravelMeasureWorst / nofLegTravelMeasureWorst);
			this.history[INDEX_BEST][index] = (sumLegTravelMeasureBest / nofLegTravelMeasureBest);
			this.history[INDEX_ALL][index] = (sumLegTravelMeasureAll / nofLegTravelMeasureAll);
			this.history[INDEX_EXECUTED][index] = (sumLegTravelMeasureExecuted / nofLegTravelMeasureExecuted);
			this.history[INDEX_MEDIANEXECUTED][index] = Utils.median(legTravelMeasures);

			if (event.getIteration() != this.minIteration) {
				// create chart when data of more than one iteration is available.
				XYLineChart chart = new XYLineChart(
						"Leg travel " + this.measure + " statistics", "iteration", "avg. leg travel " + this.measure);
				double[] iterations = new double[index + 1];
				for (int i = 0; i <= index; i++) {
					iterations[i] = i + this.minIteration;
				}
				double[] values = new double[index + 1];
				System.arraycopy(this.history[INDEX_WORST], 0, values, 0, index + 1);
				chart.addSeries("worst plan", iterations, values);
				System.arraycopy(this.history[INDEX_BEST], 0, values, 0, index + 1);
				chart.addSeries("best plan", iterations, values);
				System.arraycopy(this.history[INDEX_ALL], 0, values, 0, index + 1);
				chart.addSeries("all plans", iterations, values);
				System.arraycopy(this.history[INDEX_EXECUTED], 0, values, 0, index + 1);
				chart.addSeries("executed plan", iterations, values);
				chart.addMatsimLogo();
				chart.saveAsPng(event.getControler().getControlerIO().getOutputFilename("travelstats_" + this.measure + "_" + calculator.getMode() + "_" +
						calculator.getActType() + "_" + "crowfly=" + calculator.isCrowFly() + "_way there =" + this.wayThere + ".png"), 800, 600);
				
				try {	
					BufferedWriter out = IOUtils.getBufferedWriter(
					    event.getControler().getControlerIO().getOutputFilename(
									"travelstats_" + this.measure + "_" + calculator.getMode() + "_" +
									calculator.getActType() + "_crowfly=" + calculator.isCrowFly() + "_way there =" + this.wayThere +".txt"));
					out.write("ITERATION\tavg. EXECUTED\tavg. WORST\tavg. AVG\tavg. BEST\tmedian EXECUTED\n");
					for (int i = 0; i < event.getIteration(); i++) {
						out.write(i + "\t" + 
								formatter.format(this.history[INDEX_EXECUTED][i]) + "\t" +
								formatter.format(this.history[INDEX_WORST][i]) + "\t" + 
								formatter.format(this.history[INDEX_ALL][i]) + "\t" +
								formatter.format(this.history[INDEX_BEST][i]) + "\t" +
								formatter.format(this.history[INDEX_MEDIANEXECUTED][i]) + "\n");
						out.flush();
					}
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}				
			}
			if (index == this.history[0].length) {
				// we cannot store more information, so disable the graph feature.
				this.history = null;
			}	
		}
	}

	public void notifyShutdown(final ShutdownEvent controlerShudownEvent) {
	}

	/**
	 * @return the history of ltd in last iterations
	 */
	public double[][] getHistory() {
		return this.history.clone();
	}
}
