/* *********************************************************************** *
 * project: org.matsim.*
 * ScoreStats.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.andreas.P2.stats;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

import playground.andreas.P2.helper.PConfigGroup;
import playground.andreas.P2.pbox.Cooperative;
import playground.andreas.P2.pbox.PBox;
import playground.andreas.P2.plan.PPlan;

/**
 * Calculates at the end of each iteration the following statistics:
 * <ul>
 * <li>average score of the cooperatives</li>
 * <li>average positive score of the cooperatives</li>
 * <li>average budget of the cooperatives</li>
 * <li>average vehicle fleet size of the cooperatives</li>
 * <li>average score per vehicle</li>
 * <li>average number of trips served</li>
 * <li>percentage of cooperatives with positive score</li>
 * </ul>
 * The calculated values are written to a file, each iteration on a separate line, and as png.
 *
 * @author aneumann based on {@link org.matsim.analysis.ScoreStats} by mrieser
 */
public class PStats implements StartupListener, IterationEndsListener, ShutdownListener {

	private final static Logger log = Logger.getLogger(PStats.class);
	final private static int INDEX_ML = 0;
	final private static int INDEX_MLPOS = 1;
	final private static int INDEX_BUDGET = 2;
	final private static int INDEX_VEHICLES = 3;
	final private static int INDEX_SCOREPERVEHICLE = 4;
	final private static int INDEX_PASSENGERSSERVED = 5;
	final private static int INDEX_POSCOOPS = 6;

	private BufferedWriter pStatsWriter;

	private double[][] history = null;
	private int minIteration = 0;
	private PBox pBox;
	private PConfigGroup pConfig;

	public PStats(PBox pBox, PConfigGroup pConfig) throws UncheckedIOException {
		log.info("enabled");
		this.pBox = pBox;
		this.pConfig = pConfig;
	}

	@Override
	public void notifyStartup(final StartupEvent event) {
		Controler controler = event.getControler();
		
		if(this.pConfig.getWriteStats()){
			this.pStatsWriter = IOUtils.getBufferedWriter(controler.getControlerIO().getOutputFilename("pStats.txt"));
			try {
				this.pStatsWriter.write("ITERATION\tavg. SCORE\tavg. POSITIVE SCORE\tavg. BUDGET\tavg. FLEET SIZE\tavg. SCORE PER VEHICLE\tavg. PASSENGERS SERVED\n");
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		} else {
			this.pStatsWriter = null;
		}		
		
		this.minIteration = controler.getFirstIteration();
		int maxIter = controler.getLastIteration();
		int iterations = maxIter - this.minIteration;
		if (iterations > 10000) iterations = 10000; // limit the history size
		this.history = new double[7][iterations+1];
	}

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		if(this.pConfig.getWriteStats()){
			
			int nofCooperatives = this.pBox.getCooperatives().size();
			
			double sumScoreML = 0.0;
			double sumScoreMLPos = 0.0;
			double sumBudget = 0.0;
			double sumVehicle = 0.0;
			double sumScorePerVehicle = 0.0;
			int sumPassengersServed = 0;
			double posCooperatives = 0.0;

			for (Cooperative cooperative : this.pBox.getCooperatives()) {
				
				List<PPlan> plans = cooperative.getAllPlans();
				
				double tempSumScoreML = 0.0;
				double tempSumVehicle = 0.0;
				double tempSumScorePerVehicle = 0.0;
				int tempSumPassengersServed = 0;
				
				int tempNofVehicle = 0;

				for (PPlan plan : plans) {
					tempSumScoreML += plan.getScore();					
					tempSumScorePerVehicle += (tempNofVehicle * tempSumScorePerVehicle + plan.getVehicleIds().size() * plan.getScorePerVehicle()) / (tempSumScorePerVehicle + plan.getVehicleIds().size());
					tempSumVehicle += plan.getVehicleIds().size();
					tempSumPassengersServed += plan.getTripsServed();
				}
				
				sumScoreML += tempSumScoreML;
				if(tempSumScoreML > 0){
					sumScoreMLPos += tempSumScoreML;
					posCooperatives++;
				}
				sumBudget += cooperative.getBudget();
				sumVehicle += tempSumVehicle;
				sumScorePerVehicle += tempSumScorePerVehicle;
				sumPassengersServed += tempSumPassengersServed;
				
			
			}
			
			log.info("-- avg. score of the cooperatives: " + (sumScoreML / nofCooperatives));
			log.info("-- avg. positive score of the cooperatives: " + (sumScoreMLPos / nofCooperatives));
			log.info("-- avg. budget of the cooperatives: " + (sumBudget / nofCooperatives));
			log.info("-- avg. vehicle fleet size of the cooperatives: " + (sumVehicle / nofCooperatives));
			log.info("-- avg. score per vehicle: " + (sumScorePerVehicle / nofCooperatives));	
			log.info("-- avg. number of trips served: " + (sumPassengersServed / nofCooperatives));
			log.info("-- percentage of cooperatives with positive score: " + ((posCooperatives / (double) nofCooperatives) * 100.0));	

			try {
				this.pStatsWriter.write(event.getIteration() + "\t" + (sumScoreML / nofCooperatives) + "\t" +
						(sumScoreMLPos / nofCooperatives) + "\t" + (sumBudget / nofCooperatives) + "\t" + (sumVehicle / nofCooperatives) + "\t" +
						(sumScorePerVehicle / nofCooperatives) + "\t" + (sumPassengersServed / nofCooperatives) + "\t" + ((posCooperatives / (double) nofCooperatives) * 100.0) + "\n");
				this.pStatsWriter.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}

			if (this.history != null) {
				int index = event.getIteration() - this.minIteration;
				this.history[INDEX_ML][index] = (sumScoreML / nofCooperatives);
				this.history[INDEX_MLPOS][index] = (sumScoreMLPos / nofCooperatives);
				this.history[INDEX_BUDGET][index] = (sumBudget / nofCooperatives);
				this.history[INDEX_VEHICLES][index] = (sumVehicle / nofCooperatives);
				this.history[INDEX_SCOREPERVEHICLE][index] = (sumScorePerVehicle / nofCooperatives);
				this.history[INDEX_PASSENGERSSERVED][index] = (sumPassengersServed / nofCooperatives);
				this.history[INDEX_POSCOOPS][index] = ((posCooperatives / (double) nofCooperatives) * 100.0);

				if (event.getIteration() != this.minIteration) {
					// create chart when data of more than one iteration is available.
					XYLineChart chart = new XYLineChart("Paratransit Statistics", "iteration", "score/budget/fleet size/trips");
					double[] iterations = new double[index + 1];
					for (int i = 0; i <= index; i++) {
						iterations[i] = i + this.minIteration;
					}
					double[] values = new double[index + 1];
					System.arraycopy(this.history[INDEX_ML], 0, values, 0, index + 1);
					chart.addSeries("avg. score", iterations, values);
					System.arraycopy(this.history[INDEX_MLPOS], 0, values, 0, index + 1);
					chart.addSeries("avg. positive score", iterations, values);
					System.arraycopy(this.history[INDEX_BUDGET], 0, values, 0, index + 1);
					chart.addSeries("avg. budget", iterations, values);
					System.arraycopy(this.history[INDEX_VEHICLES], 0, values, 0, index + 1);
					chart.addSeries("avg. fleet size", iterations, values);
					System.arraycopy(this.history[INDEX_SCOREPERVEHICLE], 0, values, 0, index + 1);
					chart.addSeries("avg. score per vehicle", iterations, values);
					System.arraycopy(this.history[INDEX_PASSENGERSSERVED], 0, values, 0, index + 1);
					chart.addSeries("avg. number of trips served", iterations, values);
					System.arraycopy(this.history[INDEX_POSCOOPS], 0, values, 0, index + 1);
					chart.addSeries("percentage of cooperatives with positive score", iterations, values);
					chart.addMatsimLogo();
					chart.saveAsPng(event.getControler().getControlerIO().getOutputFilename("pStats.png"), 800, 600);
				}
				if (index == (this.history[0].length - 1)) {
					// we cannot store more information, so disable the graph feature.
					this.history = null;
				}
			}
		}
	}

	@Override
	public void notifyShutdown(final ShutdownEvent controlerShudownEvent) {
		try {
			this.pStatsWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * @return the history of scores in last iterations
	 */
	public double[][] getHistory() {
		if (this.history == null) {
			return null;
		}
		return this.history.clone();
	}
}