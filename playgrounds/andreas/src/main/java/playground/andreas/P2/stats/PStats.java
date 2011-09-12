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
	
	final private static int INDEX_NCOOPS = 0;
	final private static int INDEX_NCOOPSPOS = 1;
	final private static int INDEX_NPAX = 2;
	final private static int INDEX_NPAXPOS = 3;
	final private static int INDEX_NVEH = 4;
	final private static int INDEX_NVEHPOS = 5;
	final private static int INDEX_NBUDGET = 6;
	final private static int INDEX_NBUDGETPOS = 7;
	final private static int INDEX_NSCORE = 8;
	final private static int INDEX_NSCOREPOS = 9;
	
	final private static int INDEX_SHAREPOSCOOP = 10;
	final private static int INDEX_SHAREPOSPAX = 11;
	final private static int INDEX_SHAREPOSVEH = 12;


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
				this.pStatsWriter.write("iter\tcoops\t+coops\tpax\t+pax\tveh\t+veh\tbudget\t+budget\tscore\t+score\tsharePosCoop\tsharePosPax\tsharePosVeh\t\n");
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
		this.history = new double[14][iterations+1];
	}

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		if(this.pConfig.getWriteStats()){
			
			double coop = 0.0;
			double coopPos = 0.0;
			double pax = 0.0;
			double paxPos = 0.0;
			double veh = 0.0;
			double vehPos = 0.0;
			double budget = 0.0;
			double budgetPos = 0.0;
			double score = 0.0;
			double scorePos = 0.0;

			for (Cooperative cooperative : this.pBox.getCooperatives()) {
				List<PPlan> plans = cooperative.getAllPlans();
				
				double coopPax = 0.0;
				double coopVeh = 0.0;
				double coopScore = 0.0;				

				for (PPlan plan : plans) {
					coopPax += plan.getTripsServed();
					coopVeh += plan.getVehicleIds().size();
					coopScore += plan.getScore();
				}
				
				coop++;
				pax += coopPax;				
				veh += coopVeh;
				budget += cooperative.getBudget();
				score += coopScore;				
				
				if(coopScore > 0){
					coopPos++;
					paxPos += coopPax;
					vehPos += coopVeh;
					budgetPos += cooperative.getBudget();
					scorePos += coopScore;					
				}				
			}
			
			double sharePosCoop = coopPos / coop * 100.0;
			double sharePosPax = paxPos / pax * 100.0;
			double sharePosVeh = vehPos / veh * 100.0;
			
			try {
				this.pStatsWriter.write(event.getIteration() + "\t" + (int) coop + "\t" + (int) coopPos + "\t" + (int) pax + "\t" + (int) paxPos + "\t" + (int) veh + "\t" + (int) vehPos + "\t" +
						budget + "\t" + budgetPos + "\t" + score + "\t" + scorePos + "\t" + sharePosCoop + "\t" + sharePosPax + "\t" + sharePosVeh + "\n");
				this.pStatsWriter.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}

			if (this.history != null) {
				int index = event.getIteration() - this.minIteration;
				
				this.history[INDEX_NCOOPS][index] = coop;
				this.history[INDEX_NCOOPSPOS][index] = coopPos;
				this.history[INDEX_NPAX][index] = pax;
				this.history[INDEX_NPAXPOS][index] = paxPos;
				this.history[INDEX_NVEH][index] = veh;
				this.history[INDEX_NVEHPOS][index] = vehPos;
				this.history[INDEX_NBUDGET][index] = budget;
				this.history[INDEX_NBUDGETPOS][index] = budgetPos;
				this.history[INDEX_NSCORE][index] = score;
				this.history[INDEX_NSCOREPOS][index] = scorePos;
				
				this.history[INDEX_SHAREPOSCOOP][index] = sharePosCoop;
				this.history[INDEX_SHAREPOSPAX][index] = sharePosPax;
				this.history[INDEX_SHAREPOSVEH][index] = sharePosVeh;

				if (event.getIteration() != this.minIteration) {
					// create chart when data of more than one iteration is available.
					
					XYLineChart size = new XYLineChart("Paratransit Statistics", "iteration", "coops/fleet size");
					XYLineChart scores = new XYLineChart("Paratransit Statistics", "iteration", "score/budget");
					XYLineChart shares = new XYLineChart("Paratransit Statistics", "iteration", "shares of positive coops");
					
					double[] iterations = new double[index + 1];
					for (int i = 0; i <= index; i++) {
						iterations[i] = i + this.minIteration;
					}
					double[] values = new double[index + 1];
					
					System.arraycopy(this.history[INDEX_NCOOPS], 0, values, 0, index + 1);
					size.addSeries("N coops", iterations, values);
					System.arraycopy(this.history[INDEX_NCOOPSPOS], 0, values, 0, index + 1);
					size.addSeries("N pos coops", iterations, values);
					System.arraycopy(this.history[INDEX_NVEH], 0, values, 0, index + 1);
					size.addSeries("N veh", iterations, values);
					System.arraycopy(this.history[INDEX_NVEHPOS], 0, values, 0, index + 1);
					size.addSeries("N pos veh", iterations, values);
					
					System.arraycopy(this.history[INDEX_NBUDGET], 0, values, 0, index + 1);
					scores.addSeries("budget", iterations, values);
					System.arraycopy(this.history[INDEX_NBUDGETPOS], 0, values, 0, index + 1);
					scores.addSeries("pos budget", iterations, values);
					System.arraycopy(this.history[INDEX_NSCORE], 0, values, 0, index + 1);
					scores.addSeries("score", iterations, values);
					System.arraycopy(this.history[INDEX_NSCOREPOS], 0, values, 0, index + 1);
					scores.addSeries("pos score", iterations, values);
					System.arraycopy(this.history[INDEX_NPAX], 0, values, 0, index + 1);
					scores.addSeries("N pax", iterations, values);
					System.arraycopy(this.history[INDEX_NPAXPOS], 0, values, 0, index + 1);
					scores.addSeries("N pos pax", iterations, values);	
					
					System.arraycopy(this.history[INDEX_SHAREPOSCOOP], 0, values, 0, index + 1);
					shares.addSeries("share pos coop", iterations, values);
					System.arraycopy(this.history[INDEX_SHAREPOSPAX], 0, values, 0, index + 1);
					shares.addSeries("share pos pax", iterations, values);
					System.arraycopy(this.history[INDEX_SHAREPOSVEH], 0, values, 0, index + 1);
					shares.addSeries("share pos veh", iterations, values);
										
					size.addMatsimLogo();
					scores.addMatsimLogo();
					shares.addMatsimLogo();
					size.saveAsPng(event.getControler().getControlerIO().getOutputFilename("pStats_size.png"), 800, 600);
					scores.saveAsPng(event.getControler().getControlerIO().getOutputFilename("pStats_score.png"), 800, 600);
					shares.saveAsPng(event.getControler().getControlerIO().getOutputFilename("pStats_shares.png"), 800, 600);
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