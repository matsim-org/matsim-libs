/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.andreas.P2.stats.pStatsOverview;

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
import playground.andreas.P2.helper.PConstants.CoopState;
import playground.andreas.P2.operator.Cooperative;
import playground.andreas.P2.pbox.PBox;
import playground.andreas.P2.replanning.PPlan;

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
public class PStatsOverview implements StartupListener, IterationEndsListener, ShutdownListener {

	private final static Logger log = Logger.getLogger(PStatsOverview.class);
	
	final private static int INDEX_NCOOPS = 0;
	final private static int INDEX_NCOOPSPOS = 1;
	final private static int INDEX_NROUTES = 2;
	final private static int INDEX_NROUTESPOS = 3;	
	final private static int INDEX_NPAX = 4;
	final private static int INDEX_NPAXPOS = 5;
	final private static int INDEX_NVEH = 6;
	final private static int INDEX_NVEHPOS = 7;
	final private static int INDEX_NBUDGET = 8;
	final private static int INDEX_NBUDGETPOS = 9;
	final private static int INDEX_NSCORE = 10;
	final private static int INDEX_NSCOREPOS = 11;
	
	final private static int INDEX_SHAREPOSCOOP = 12;
	final private static int INDEX_SHAREPOSROUTES = 13;
	final private static int INDEX_SHAREPOSPAX = 14;
	final private static int INDEX_SHAREPOSVEH = 15;
	
	final private static int INDEX_MEANPOSCOOP = 16;
	final private static int INDEX_MEANPOSROUTES = 17;
	final private static int INDEX_MEANPOSPAX = 18;
	final private static int INDEX_MEANPOSVEH = 19;
	final private static int INDEX_SIGMAUPPERPOSCOOP = 20;
	final private static int INDEX_SIGMAUPPERPOSROUTES = 21;
	final private static int INDEX_SIGMAUPPERPOSPAX = 22;
	final private static int INDEX_SIGMAUPPERPOSVEH = 23;
	final private static int INDEX_SIGMALOWERPOSCOOP = 24;
	final private static int INDEX_SIGMALOWERPOSROUTES = 25;
	final private static int INDEX_SIGMALOWERPOSPAX = 26;
	final private static int INDEX_SIGMALOWERPOSVEH = 27;

	private BufferedWriter pStatsWriter;

	private double[][] history = null;
	private int minIteration = 0;
	private PBox pBox;
	private PConfigGroup pConfig;

	private RecursiveStatsContainer statsContainer;
	private RecursiveStatsApproxContainer statsApproxContainer;

	public PStatsOverview(PBox pBox, PConfigGroup pConfig) throws UncheckedIOException {
		this.pBox = pBox;
		this.pConfig = pConfig;
	}

	@Override
	public void notifyStartup(final StartupEvent event) {
		Controler controler = event.getControler();
		
		if(this.pConfig.getWriteStatsInterval() > 0){
			log.info("enabled");
			this.pStatsWriter = IOUtils.getBufferedWriter(controler.getControlerIO().getOutputFilename("pStats.txt"));
			try {
				this.pStatsWriter.write("iter\tcoops\t+coops\troutes\t+routes\tpax\t+pax\tveh\t+veh\tbudget\t+budget\tscore\t+score\tsharePosCoop\tsharePosRoutes\tsharePosPax\tsharePosVeh\tESmeanCoop+\tESstdDevCoop+\tESmeanRoutes+\tESstdDevRoutes+\tESmeanPax+\tESstdDevPax+\tESmeanVeh+\tESstdDevVeh+\tmeanCoop+\tstdDevCoop+\tmeanRoutes+\tstdDevRoutes+\tmeanPax+\tstdDevPax+\tmeanVeh+\tstdDevVeh+\t\n");
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		} else {
			this.pStatsWriter = null;
		}		
		
		this.minIteration = controler.getConfig().controler().getFirstIteration();
		int maxIter = controler.getConfig().controler().getLastIteration();
		int iterations = maxIter - this.minIteration;
		if (iterations > 10000) iterations = 10000; // limit the history size
		this.history = new double[29][iterations+1];
		this.statsContainer = new RecursiveStatsContainer();
		this.statsApproxContainer = new RecursiveStatsApproxContainer(0.1, 10);
	}

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		if(this.pConfig.getWriteStatsInterval() > 0){
			
			double coop = 0.0;
			double coopPos = 0.0;
			double routes = 0.0;
			double routesPos = 0.0;
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
				
				double coopRoutes = 0.0;
				double coopPax = 0.0;
				double coopVeh = 0.0;
				double coopScore = 0.0;				

				for (PPlan plan : plans) {
					coopRoutes++;
					coopPax += plan.getTripsServed();
					coopVeh += plan.getNVehicles();
					coopScore += plan.getScore();
				}
				
				coop++;
				routes += coopRoutes;
				pax += coopPax;				
				veh += coopVeh;
				budget += cooperative.getBudget();
				score += coopScore;				
				
				// statistics for each coop in business
				if(cooperative.getCoopState().equals(CoopState.INBUSINESS)){
					coopPos++;
					routesPos += coopRoutes;
					paxPos += coopPax;
					vehPos += coopVeh;
					budgetPos += cooperative.getBudget();
					scorePos += coopScore;					
				}				
			}
			
			double sharePosCoop = coopPos / coop * 100.0;
			double sharePosRoutes = routesPos / routes * 100.0;
			double sharePosPax = paxPos / pax * 100.0;
			double sharePosVeh = vehPos / veh * 100.0;
			
			this.statsContainer.handleNewEntry(coopPos, routesPos, paxPos, vehPos);
			this.statsApproxContainer.handleNewEntry(coopPos, routesPos, paxPos, vehPos);
			
			try {
				this.pStatsWriter.write(event.getIteration() + "\t" + (int) coop + "\t" + (int) coopPos + "\t" + (int) routes + "\t" + (int) routesPos + "\t" + (int) pax + "\t" + (int) paxPos + "\t" + (int) veh + "\t" + (int) vehPos + "\t" +
						(budget / coop) + "\t" + (budgetPos / coopPos) + "\t" + (score / routes) + "\t" + (scorePos / routesPos) + "\t" + sharePosCoop + "\t" + sharePosRoutes + "\t" + sharePosPax + "\t" + sharePosVeh + "\t" +
						statsApproxContainer.getArithmeticMeanCoops() + "\t" + statsApproxContainer.getStdDevCoop() + "\t" + statsApproxContainer.getArithmeticMeanRoutes() + "\t" + statsApproxContainer.getStdDevRoutes() + "\t" +
						statsApproxContainer.getArithmeticMeanPax() + "\t" + statsApproxContainer.getStdDevPax() + "\t" + statsApproxContainer.getArithmeticMeanVeh() + "\t" + statsApproxContainer.getStdDevVeh() + "\t" +
						statsContainer.getArithmeticMeanCoops() + "\t" + statsContainer.getStdDevCoop() + "\t" + statsContainer.getArithmeticMeanRoutes() + "\t" + statsContainer.getStdDevRoutes() + "\t" + 
						statsContainer.getArithmeticMeanPax() + "\t" + statsContainer.getStdDevPax() + "\t" + statsContainer.getArithmeticMeanVeh() + "\t" + statsContainer.getStdDevVeh() + "\n");
				this.pStatsWriter.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}

			if (this.history != null) {
				int index = event.getIteration() - this.minIteration;
				
				this.history[INDEX_NCOOPS][index] = coop;
				this.history[INDEX_NCOOPSPOS][index] = coopPos;
				this.history[INDEX_NROUTES][index] = routes;
				this.history[INDEX_NROUTESPOS][index] = routesPos;
				this.history[INDEX_NPAX][index] = pax;
				this.history[INDEX_NPAXPOS][index] = paxPos;
				this.history[INDEX_NVEH][index] = veh;
				this.history[INDEX_NVEHPOS][index] = vehPos;
				this.history[INDEX_NBUDGET][index] = budget / coop;
				this.history[INDEX_NBUDGETPOS][index] = budgetPos / coopPos;
				this.history[INDEX_NSCORE][index] = score / routes;
				this.history[INDEX_NSCOREPOS][index] = scorePos / routesPos;
				
				this.history[INDEX_SHAREPOSCOOP][index] = sharePosCoop;
				this.history[INDEX_SHAREPOSROUTES][index] = sharePosRoutes;
				this.history[INDEX_SHAREPOSPAX][index] = sharePosPax;
				this.history[INDEX_SHAREPOSVEH][index] = sharePosVeh;
				
				this.history[INDEX_MEANPOSCOOP][index] = statsApproxContainer.getArithmeticMeanCoops();
				this.history[INDEX_MEANPOSROUTES][index] = statsApproxContainer.getArithmeticMeanRoutes();
				this.history[INDEX_MEANPOSPAX][index] = statsApproxContainer.getArithmeticMeanPax();
				this.history[INDEX_MEANPOSVEH][index] = statsApproxContainer.getArithmeticMeanVeh();
				this.history[INDEX_SIGMAUPPERPOSCOOP][index] = statsApproxContainer.getArithmeticMeanCoops() + statsApproxContainer.getStdDevCoop();
				this.history[INDEX_SIGMAUPPERPOSROUTES][index] = statsApproxContainer.getArithmeticMeanRoutes() + statsApproxContainer.getStdDevRoutes();
				this.history[INDEX_SIGMAUPPERPOSPAX][index] = statsApproxContainer.getArithmeticMeanPax() + statsApproxContainer.getStdDevPax();
				this.history[INDEX_SIGMAUPPERPOSVEH][index] = statsApproxContainer.getArithmeticMeanVeh() + statsApproxContainer.getStdDevVeh();
				this.history[INDEX_SIGMALOWERPOSCOOP][index] = statsApproxContainer.getArithmeticMeanCoops() - statsApproxContainer.getStdDevCoop();
				this.history[INDEX_SIGMALOWERPOSROUTES][index] = statsApproxContainer.getArithmeticMeanRoutes() - statsApproxContainer.getStdDevRoutes();
				this.history[INDEX_SIGMALOWERPOSPAX][index] = statsApproxContainer.getArithmeticMeanPax() - statsApproxContainer.getStdDevPax();
				this.history[INDEX_SIGMALOWERPOSVEH][index] = statsApproxContainer.getArithmeticMeanVeh() - statsApproxContainer.getStdDevVeh();

				if ((event.getIteration() % this.pConfig.getWriteStatsInterval() == 0) ) {
					if (event.getIteration() != this.minIteration) {
						// create chart when data of more than one iteration is available.

						XYLineChart size = new XYLineChart("Paratransit Statistics", "iteration", "coops/routes/fleet size");
						XYLineChart scores = new XYLineChart("Paratransit Statistics", "iteration", "score/budget");
						XYLineChart passengers = new XYLineChart("Paratransit Statistics", "iteration", "pax");
						XYLineChart shares = new XYLineChart("Paratransit Statistics", "iteration", "shares of coops in business");
						XYLineChart relaxCoop = new XYLineChart("Paratransit Statistics", "iteration", "average and deviation of coops");
						XYLineChart relaxRoutes = new XYLineChart("Paratransit Statistics", "iteration", "average and deviation of routes");
						XYLineChart relaxPax = new XYLineChart("Paratransit Statistics", "iteration", "average and deviation of passengers");
						XYLineChart relaxVeh = new XYLineChart("Paratransit Statistics", "iteration", "average and deviation of vehicles");

						double[] iterations = new double[index + 1];
						for (int i = 0; i <= index; i++) {
							iterations[i] = i + this.minIteration;
						}
						double[] values = new double[index + 1];

						System.arraycopy(this.history[INDEX_NCOOPS], 0, values, 0, index + 1);
						size.addSeries("N coops", iterations, values);
						System.arraycopy(this.history[INDEX_NCOOPSPOS], 0, values, 0, index + 1);
						size.addSeries("N pos coops", iterations, values);
						System.arraycopy(this.history[INDEX_NROUTES], 0, values, 0, index + 1);
						size.addSeries("N routes", iterations, values);
						System.arraycopy(this.history[INDEX_NROUTESPOS], 0, values, 0, index + 1);
						size.addSeries("N pos routes", iterations, values);
						System.arraycopy(this.history[INDEX_NVEH], 0, values, 0, index + 1);
						size.addSeries("N veh", iterations, values);
						System.arraycopy(this.history[INDEX_NVEHPOS], 0, values, 0, index + 1);
						size.addSeries("N pos veh", iterations, values);

						System.arraycopy(this.history[INDEX_NBUDGET], 0, values, 0, index + 1);
						scores.addSeries("budget per coop", iterations, values);
						System.arraycopy(this.history[INDEX_NBUDGETPOS], 0, values, 0, index + 1);
						scores.addSeries("pos budget per pos coop", iterations, values);
						System.arraycopy(this.history[INDEX_NSCORE], 0, values, 0, index + 1);
						scores.addSeries("score per route", iterations, values);
						System.arraycopy(this.history[INDEX_NSCOREPOS], 0, values, 0, index + 1);
						scores.addSeries("pos score per pos route", iterations, values);
						
						System.arraycopy(this.history[INDEX_NPAX], 0, values, 0, index + 1);
						passengers.addSeries("N pax", iterations, values);
						System.arraycopy(this.history[INDEX_NPAXPOS], 0, values, 0, index + 1);
						passengers.addSeries("N pos pax", iterations, values);	

						System.arraycopy(this.history[INDEX_SHAREPOSCOOP], 0, values, 0, index + 1);
						shares.addSeries("share pos coop", iterations, values);
						System.arraycopy(this.history[INDEX_SHAREPOSROUTES], 0, values, 0, index + 1);
						shares.addSeries("share pos routes", iterations, values);
						System.arraycopy(this.history[INDEX_SHAREPOSPAX], 0, values, 0, index + 1);
						shares.addSeries("share pos pax", iterations, values);
						System.arraycopy(this.history[INDEX_SHAREPOSVEH], 0, values, 0, index + 1);
						shares.addSeries("share pos veh", iterations, values);

						System.arraycopy(this.history[INDEX_MEANPOSCOOP], 0, values, 0, index + 1);
						relaxCoop.addSeries("average number of pos coop", iterations, values);
						System.arraycopy(this.history[INDEX_SIGMAUPPERPOSCOOP], 0, values, 0, index + 1);
						relaxCoop.addSeries("average number of pos coop + 1 sigma", iterations, values);
						System.arraycopy(this.history[INDEX_SIGMALOWERPOSCOOP], 0, values, 0, index + 1);
						relaxCoop.addSeries("average number of pos coop - 1 sigma", iterations, values);
						
						System.arraycopy(this.history[INDEX_MEANPOSROUTES], 0, values, 0, index + 1);
						relaxRoutes.addSeries("average number of pos routes", iterations, values);
						System.arraycopy(this.history[INDEX_SIGMAUPPERPOSROUTES], 0, values, 0, index + 1);
						relaxRoutes.addSeries("average number of pos routes + 1 sigma", iterations, values);
						System.arraycopy(this.history[INDEX_SIGMALOWERPOSROUTES], 0, values, 0, index + 1);
						relaxRoutes.addSeries("average number of pos routes - 1 sigma", iterations, values);

						System.arraycopy(this.history[INDEX_MEANPOSPAX], 0, values, 0, index + 1);
						relaxPax.addSeries("average number of pos pax", iterations, values);
						System.arraycopy(this.history[INDEX_SIGMAUPPERPOSPAX], 0, values, 0, index + 1);
						relaxPax.addSeries("average number of pos pax + 1 sigma", iterations, values);
						System.arraycopy(this.history[INDEX_SIGMALOWERPOSPAX], 0, values, 0, index + 1);
						relaxPax.addSeries("average number of pos pax - 1 sigma", iterations, values);

						System.arraycopy(this.history[INDEX_MEANPOSVEH], 0, values, 0, index + 1);
						relaxVeh.addSeries("average number of pos veh", iterations, values);
						System.arraycopy(this.history[INDEX_SIGMAUPPERPOSVEH], 0, values, 0, index + 1);
						relaxVeh.addSeries("average number of pos veh + 1 sigma", iterations, values);
						System.arraycopy(this.history[INDEX_SIGMALOWERPOSVEH], 0, values, 0, index + 1);
						relaxVeh.addSeries("average number of pos veh - 1 sigma", iterations, values);

						size.addMatsimLogo();
						scores.addMatsimLogo();
						passengers.addMatsimLogo();
						shares.addMatsimLogo();
						relaxCoop.addMatsimLogo();
						relaxRoutes.addMatsimLogo();
						relaxPax.addMatsimLogo();
						relaxVeh.addMatsimLogo();

						size.saveAsPng(event.getControler().getControlerIO().getOutputFilename("pStats_size.png"), 800, 600);
						scores.saveAsPng(event.getControler().getControlerIO().getOutputFilename("pStats_score.png"), 800, 600);
						passengers.saveAsPng(event.getControler().getControlerIO().getOutputFilename("pStats_pax.png"), 800, 600);
						shares.saveAsPng(event.getControler().getControlerIO().getOutputFilename("pStats_shares.png"), 800, 600);
						relaxCoop.saveAsPng(event.getControler().getControlerIO().getOutputFilename("pStats_relaxCoop.png"), 800, 600);
						relaxRoutes.saveAsPng(event.getControler().getControlerIO().getOutputFilename("pStats_relaxRoutes.png"), 800, 600);
						relaxPax.saveAsPng(event.getControler().getControlerIO().getOutputFilename("pStats_relaxPax.png"), 800, 600);
						relaxVeh.saveAsPng(event.getControler().getControlerIO().getOutputFilename("pStats_relaxVeh.png"), 800, 600);
					}
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
		// check this, otherwise it may be a null-pointer \\ DR aug'13
		if(this.pConfig.getWriteStatsInterval() > 0){
			try {
				this.pStatsWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
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