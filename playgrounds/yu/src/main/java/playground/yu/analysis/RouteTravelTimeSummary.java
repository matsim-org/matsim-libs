/* *********************************************************************** *
 * project: org.matsim.*
 * RouteTravelTimeSummary.java
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

package playground.yu.analysis;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.matsim.core.controler.ControlerIO;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.charts.XYLineChart;

import playground.yu.utils.container.Collection2Array;
import playground.yu.utils.io.SimpleWriter;

public class RouteTravelTimeSummary implements AfterMobsimListener,
		BeforeMobsimListener, StartupListener, ShutdownListener {
	private RouteTravelTimeMeasure rttm;
	private Collection<Integer> iters = new Vector<Integer>();
	private Map<String/* route Id */, Map<Integer/* iter */, Double/*
																 * avg.
																 * traveltime
																 */>> routeIdTravTimes = new HashMap<String, Map<Integer, Double>>();
	private Map<String/* route Id */, Map<Integer/* iter */, Integer/*
																	 * avg.
																	 * traveltime
																	 */>> routeIdUserNbs = new HashMap<String, Map<Integer, Integer>>();

	@Override
	public void notifyStartup(StartupEvent event) {
		rttm = new RouteTravelTimeMeasure();
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		event.getControler().getEvents().addHandler(rttm);
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		event.getControler().getEvents().removeHandler(rttm);
		// output of this iteration
		Map<String, Double> routeTravTimes = rttm.getAvgTravelTimes();
		Map<String, Integer> routeUserNbs = rttm.getNbTakingRoute();

		int iter = event.getIteration();
		// route travel times
		for (String routeId : routeTravTimes.keySet()) {
			Map<Integer, Double> iterTravTime = routeIdTravTimes.get(routeId);
			if (iterTravTime == null) {
				iterTravTime = new HashMap<Integer, Double>();
				routeIdTravTimes.put(routeId, iterTravTime);
			}
			iterTravTime.put(iter, routeTravTimes.get(routeId));
		}
		// number of agents using routes respectively
		for (String routeId : routeUserNbs.keySet()) {
			Map<Integer, Integer> iterRouteUserNbs = routeIdUserNbs
					.get(routeId);
			if (iterRouteUserNbs == null) {
				iterRouteUserNbs = new HashMap<Integer, Integer>();
				routeIdUserNbs.put(routeId, iterRouteUserNbs);
			}
			iterRouteUserNbs.put(iter, routeUserNbs.get(routeId));
		}

		rttm.reset(iter);
		iters.add(iter);
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		double[] xs = Collection2Array.toDoubleArray(iters);
		iters.clear();

		XYLineChart travTimeChart = new XYLineChart("Route Travel Time",
				"Iteration", "Route travel time"), routeUserNbChart = new XYLineChart(
				"Route User Number", "Iteration", "Route user number");

		ControlerIO ctlIO = event.getControler().getControlerIO();

		SimpleWriter travTimeWriter = new SimpleWriter(
				ctlIO.getOutputFilename("routeTravelTime.log")), routeUserNbWriter = new SimpleWriter(
				ctlIO.getOutputFilename("routeUserNb.log"));

		StringBuilder travTimeHead = new StringBuilder(
				"Average travel time of route\nIteration"), routeUserNbHead = new StringBuilder(
				"Number of agents using route\nIteration");

		for (String routeId : routeIdTravTimes.keySet()) {
			travTimeHead.append("\t");
			travTimeHead.append(routeId);

			routeUserNbHead.append("\t");
			routeUserNbHead.append(routeId);
		}
		travTimeWriter.writeln(travTimeHead);
		routeUserNbWriter.writeln(routeUserNbHead);

		List<double[]> travTimeYss = new Vector<double[]>(), routeUserNbYss = new Vector<double[]>();

		for (String catagory : routeIdTravTimes.keySet()) {
			double[] travTimeYs = new double[xs.length], routeUserNbYs = new double[xs.length];

			Map<Integer, Double> iterTravTimes = routeIdTravTimes.get(catagory);
			Map<Integer, Integer> iterRouteUserNb = routeIdUserNbs
					.get(catagory);

			for (int idx = 0; idx < xs.length; idx++) {
				Double avgTravTime = iterTravTimes.get((int) xs[idx]);
				travTimeYs[idx] = avgTravTime != null ? avgTravTime : 0d;

				Integer routeUserNb = iterRouteUserNb.get((int) xs[idx]);
				routeUserNbYs[idx] = routeUserNb != null ? routeUserNb : 0;
			}

			travTimeChart.addSeries(catagory, xs, travTimeYs);
			travTimeYss.add(travTimeYs);

			routeUserNbChart.addSeries(catagory, xs, routeUserNbYs);
			routeUserNbYss.add(routeUserNbYs);
		}

		travTimeChart.saveAsPng(ctlIO.getOutputFilename("routeTravelTime.png"),
				1024, 768);
		routeUserNbChart.saveAsPng(ctlIO.getOutputFilename("routeUserNb.png"),
				1024, 768);

		for (int idx = 0; idx < xs.length; idx++) {
			StringBuilder travTimeLine = new StringBuilder(
					Integer.toString((int) xs[idx])), routeUserNbLine = new StringBuilder(
					Integer.toString((int) xs[idx]));
			for (double[] ys : travTimeYss) {
				travTimeLine.append('\t');
				travTimeLine.append(ys[idx]);
			}
			for (double[] ys : routeUserNbYss) {
				routeUserNbLine.append('\t');
				routeUserNbLine.append(ys[idx]);
			}
			travTimeWriter.writeln(travTimeLine);
			travTimeWriter.flush();

			routeUserNbWriter.writeln(routeUserNbLine);
			routeUserNbWriter.flush();
		}
		travTimeWriter.close();
		routeUserNbWriter.close();

		routeIdTravTimes.clear();
		routeIdUserNbs.clear();
	}
}
