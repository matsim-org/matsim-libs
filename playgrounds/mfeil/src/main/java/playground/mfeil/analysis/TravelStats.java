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

package playground.mfeil.analysis;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.RouteUtils;

/**
 *
 * Calculates at the end of each iteration the following statistics:
 * <ul>
 * 	<li>average of the average leg distance per plan (worst plans only)</li>
 * 	<li>average of the average leg distance per plan (best plans only)</li>
 * 	<li>average of the average leg distance per plan (selected plans only)</li>
 * 	<li>average of the average leg distance per plan (all plans)</li>
 * </ul>
 *
 *
 * @author anhorni
 */

/*
 * TODO: [AH] This is copy-paste based on "ScoreStats". Refactoring needed!
 *
 */
public class TravelStats implements StartupListener, IterationEndsListener, ShutdownListener {

	final private static int INDEX_WORST = 0;
	final private static int INDEX_BEST = 1;
	final private static int INDEX_AVERAGE = 2;
	final private static int INDEX_EXECUTED = 3;

	final private Population population;
	final private Network network;
	final private BufferedWriter outDistance;
	final private BufferedWriter outTime;
	final private Controler controler;

	private final boolean createPNG;
	private double[][] historyDistance = null;
	private double[][] historyTime = null;
	private int minIteration = 0;

	private final static Logger log = Logger.getLogger(TravelStats.class);

	/**
	 * @param population
	 * @param filename
	 * @param createPNG true if in every iteration, the distance statistics should be visualized in a graph and written to disk.
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public TravelStats(final Controler controler, final Population population, final Network network, final String filenameDistance, final String filenameTime, final boolean createPNG) throws FileNotFoundException, IOException {
		this.controler = controler;
		this.population = population;
		this.network = network;
		this.createPNG = createPNG;
		this.outDistance = IOUtils.getBufferedWriter(filenameDistance);
		this.outDistance.write("ITERATION\tavg. EXECUTED\tavg. WORST\tavg. AVG\tavg. BEST\n");
		this.outTime = IOUtils.getBufferedWriter(filenameTime);
		this.outTime.write("ITERATION\tavg. EXECUTED\tavg. WORST\tavg. AVG\tavg. BEST\n");
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
			this.historyDistance = new double[4][iterations+1];
			this.historyTime = new double[4][iterations+1];
		}
	}

	public void notifyIterationEnds(final IterationEndsEvent event) {
		double sumAvgPlanLegTravelDistanceWorst = 0.0;
		double sumAvgPlanLegTravelDistanceBest = 0.0;
		double sumAvgPlanLegTravelDistanceAll = 0.0;
		double sumAvgPlanLegTravelDistanceExecuted = 0.0;
		double sumAvgPlanLegTravelTimeWorst = 0.0;
		double sumAvgPlanLegTravelTimeBest = 0.0;
		double sumAvgPlanLegTravelTimeAll = 0.0;
		double sumAvgPlanLegTravelTimeExecuted = 0.0;
		int nofLegTravelWorst = 0;
		int nofLegTravelBest = 0;
		int nofLegTravelAvg = 0;
		int nofLegTravelExecuted = 0;

		for (Person person : this.population.getPersons().values()) {
			Plan worstPlan = null;
			Plan bestPlan = null;
			double worstScore = Double.POSITIVE_INFINITY;
			double bestScore = Double.NEGATIVE_INFINITY;
			double sumAvgLegTravelDistance = 0.0;
			double sumAvgLegTravelTime = 0.0;
			double cntAvgLegTravel = 0;
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

				// avg. leg travel distance
				double[] dist = getAvgLegCharacteristics(plan);
				sumAvgLegTravelDistance+=dist[0];
				sumAvgLegTravelTime+=dist[1];
				cntAvgLegTravel+=dist[2];

				// executed plan? --------------------------------------------------
				if (plan.isSelected()) {
					double[] distExec = getAvgLegCharacteristics(plan);
					sumAvgPlanLegTravelDistanceExecuted += distExec[0];
					sumAvgPlanLegTravelTimeExecuted += distExec[1];
					nofLegTravelExecuted += distExec[2];
				}
			}

			if (worstPlan != null) {
				double[] distWorst = getAvgLegCharacteristics(worstPlan);
				sumAvgPlanLegTravelDistanceWorst += distWorst[0];
				sumAvgPlanLegTravelTimeWorst += distWorst[1];
				nofLegTravelWorst+= distWorst[2];
			}
			if (bestPlan != null) {
				double[] distBest = getAvgLegCharacteristics(bestPlan);
				sumAvgPlanLegTravelDistanceBest += distBest[0];
				sumAvgPlanLegTravelTimeBest += distBest[1];
				nofLegTravelBest+= distBest[2];
			}
			if (cntAvgLegTravel > 0) {
				sumAvgPlanLegTravelDistanceAll += (sumAvgLegTravelDistance / cntAvgLegTravel);
				sumAvgPlanLegTravelTimeAll += (sumAvgLegTravelTime / cntAvgLegTravel);
				nofLegTravelAvg++;
			}
		}
		log.info("-- average of the average leg distance (executed plans only): " + (sumAvgPlanLegTravelDistanceExecuted / nofLegTravelExecuted));
		log.info("-- average of the average leg distance (worst plans only): " + (sumAvgPlanLegTravelDistanceWorst / nofLegTravelWorst));
		log.info("-- average of the average leg distance (all plans): " + (sumAvgPlanLegTravelDistanceAll / nofLegTravelAvg));
		log.info("-- average of the average leg distance (best plans only): " + (sumAvgPlanLegTravelDistanceBest / nofLegTravelBest));
		log.info("-- average of the average leg travel time (executed plans only): " + (sumAvgPlanLegTravelTimeExecuted / nofLegTravelExecuted));
		log.info("-- average of the average leg travel time (worst plans only): " + (sumAvgPlanLegTravelTimeWorst / nofLegTravelWorst));
		log.info("-- average of the average leg travel time (all plans): " + (sumAvgPlanLegTravelTimeAll / nofLegTravelAvg));
		log.info("-- average of the average leg travel time (best plans only): " + (sumAvgPlanLegTravelTimeBest / nofLegTravelBest));

		try {
			this.outDistance.write(event.getIteration() + "\t" + (sumAvgPlanLegTravelDistanceExecuted / nofLegTravelExecuted) + "\t" +
					(sumAvgPlanLegTravelDistanceWorst / nofLegTravelWorst) + "\t" + (sumAvgPlanLegTravelDistanceAll / nofLegTravelAvg) + "\t" + (sumAvgPlanLegTravelDistanceBest / nofLegTravelBest) + "\n");
			this.outDistance.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			this.outTime.write(event.getIteration() + "\t" + (sumAvgPlanLegTravelTimeExecuted / nofLegTravelExecuted) + "\t" +
					(sumAvgPlanLegTravelTimeWorst / nofLegTravelWorst) + "\t" + (sumAvgPlanLegTravelTimeAll / nofLegTravelAvg) + "\t" + (sumAvgPlanLegTravelTimeBest / nofLegTravelBest) + "\n");
			this.outTime.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (this.historyDistance != null) {
			int index = event.getIteration() - this.minIteration;
			this.historyDistance[INDEX_WORST][index] = (sumAvgPlanLegTravelDistanceWorst / nofLegTravelWorst);
			this.historyDistance[INDEX_BEST][index] = (sumAvgPlanLegTravelDistanceBest / nofLegTravelBest);
			this.historyDistance[INDEX_AVERAGE][index] = (sumAvgPlanLegTravelDistanceAll / nofLegTravelAvg);
			this.historyDistance[INDEX_EXECUTED][index] = (sumAvgPlanLegTravelDistanceExecuted / nofLegTravelExecuted);

			if (event.getIteration() != this.minIteration) {
				// create chart when data of more than one iteration is available.
				XYLineChart chart = new XYLineChart("Leg Travel Distance Statistics", "iteration", "average of the average leg distance per plan ");
				double[] iterations = new double[index + 1];
				for (int i = 0; i <= index; i++) {
					iterations[i] = i + this.minIteration;
				}
				double[] values = new double[index + 1];
				System.arraycopy(this.historyDistance[INDEX_WORST], 0, values, 0, index + 1);
				chart.addSeries("worst plan", iterations, values);
				System.arraycopy(this.historyDistance[INDEX_BEST], 0, values, 0, index + 1);
				chart.addSeries("best plan", iterations, values);
				System.arraycopy(this.historyDistance[INDEX_AVERAGE], 0, values, 0, index + 1);
				chart.addSeries("avg. of plans", iterations, values);
				System.arraycopy(this.historyDistance[INDEX_EXECUTED], 0, values, 0, index + 1);
				chart.addSeries("executed plan", iterations, values);
				chart.addMatsimLogo();
				chart.saveAsPng(event.getControler().getControlerIO().getOutputFilename("traveldistancestats.png"), 800, 600);
			}
			if (index == this.historyDistance[0].length) {
				// we cannot store more information, so disable the graph feature.
				this.historyDistance = null;
			}
		}

		if (this.historyTime != null) {
			int index = event.getIteration() - this.minIteration;
			this.historyTime[INDEX_WORST][index] = (sumAvgPlanLegTravelTimeWorst / nofLegTravelWorst);
			this.historyTime[INDEX_BEST][index] = (sumAvgPlanLegTravelTimeBest / nofLegTravelBest);
			this.historyTime[INDEX_AVERAGE][index] = (sumAvgPlanLegTravelTimeAll / nofLegTravelAvg);
			this.historyTime[INDEX_EXECUTED][index] = (sumAvgPlanLegTravelTimeExecuted / nofLegTravelExecuted);

			if (event.getIteration() != this.minIteration) {
				// create chart when data of more than one iteration is available.
				XYLineChart chart = new XYLineChart("Leg Travel Time Statistics", "iteration", "average of the average leg travel time");
				double[] iterations = new double[index + 1];
				for (int i = 0; i <= index; i++) {
					iterations[i] = i + this.minIteration;
				}
				double[] values = new double[index + 1];
				System.arraycopy(this.historyTime[INDEX_WORST], 0, values, 0, index + 1);
				chart.addSeries("worst plan", iterations, values);
				System.arraycopy(this.historyTime[INDEX_BEST], 0, values, 0, index + 1);
				chart.addSeries("best plan", iterations, values);
				System.arraycopy(this.historyTime[INDEX_AVERAGE], 0, values, 0, index + 1);
				chart.addSeries("avg. of plans", iterations, values);
				System.arraycopy(this.historyTime[INDEX_EXECUTED], 0, values, 0, index + 1);
				chart.addSeries("executed plan", iterations, values);
				chart.addMatsimLogo();
				chart.saveAsPng(event.getControler().getControlerIO().getOutputFilename("traveltimestats.png"), 800, 600);
			}
			if (index == this.historyTime[0].length) {
				// we cannot store more information, so disable the graph feature.
				this.historyTime = null;
			}
		}

		if (this.controler.getIterationNumber()>0 && this.controler.getIterationNumber()%10==0) new ASPGeneral (this.controler.getIterationNumber(), this.controler.getLastIteration(), this.controler.getControlerIO().getOutputPath(), (NetworkImpl) this.network);
	}

	public void notifyShutdown(final ShutdownEvent controlerShudownEvent) {
		try {
			this.outDistance.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			this.outTime.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * @return the history of ltd in last iterations
	 */
	//public double[][] getHistory() {
	//	return this.history.clone();
	//}

	private double[] getAvgLegCharacteristics(final Plan plan){

		double planTravelDistance=0.0;
		double planTravelTime=0.0;
		int numberOfLegs=0;

		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Leg) {
				final Leg leg = (Leg) pe;
				if (!leg.getMode().toString().equals(TransportMode.car.toString())) {
					planTravelDistance+= leg.getRoute().getDistance();
				}
				else if (leg.getRoute() instanceof NetworkRoute) {
					planTravelDistance += RouteUtils.calcDistance((NetworkRoute) leg.getRoute(), this.network);
				}
				planTravelTime += leg.getTravelTime();
				numberOfLegs++;
			}
		}
		if (numberOfLegs>0) return new double[]{planTravelDistance,planTravelTime,numberOfLegs};
		return new double[]{0.0,0.0,0.0};
	}

}
