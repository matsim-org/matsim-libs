/* *********************************************************************** *
 * project: org.matsim.*
 * LegScoringFunctionWithDetailedRecord.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.yu.scoring.postProcessing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.core.scoring.charyparNagel.LegScoringFunction;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Counter;

import playground.yu.utils.charts.TimeScatterChart;
import playground.yu.utils.container.Collection2Array;
import playground.yu.utils.io.SimpleWriter;

public class LegScoringFunctionWithDetailedRecord extends LegScoringFunction {
	private static Map<String/* actType */, Tuple<List<Double>/*
															 * departure times
															 * [min]
															 */, List<Double>/*
																			 * travel
																			 * times
																			 * [
																			 * s
																			 * ]
																			 */>> data = new HashMap<String, Tuple<List<Double>, List<Double>>>();

	public LegScoringFunctionWithDetailedRecord(Plan plan,
			CharyparNagelScoringParameters params) {
		super(plan, params);
	}

	@Override
	protected double calcLegScore(final double departureTime,
			final double arrivalTime, final Leg leg) {

		this.addData(departureTime, arrivalTime, leg);

		return super.calcLegScore(departureTime, arrivalTime, leg);
	}

	private void addData(final double departureTime, final double arrivalTime,
			final Leg leg) {
		String actType = ((PlanImpl) this.plan).getNextActivity(leg).getType()
				.substring(0, 1);
		Tuple<List<Double>, List<Double>> tuple = data.get(actType);
		if (tuple == null) {
			tuple = new Tuple<List<Double>, List<Double>>(
					new ArrayList<Double>(), new ArrayList<Double>());
			data.put(actType, tuple);
		}
		tuple.getFirst().add(departureTime /* s */);
		tuple.getSecond().add((arrivalTime - departureTime) / 60d/* min */);
	}

	public static void createChart(String filenameBase) {
		TimeScatterChart chart = new TimeScatterChart(
				"departure time - travel time diagramm",
				"departure time [min]", "travel time [s]");

		for (String destinationActType : data.keySet()) {
			String series;
			if (destinationActType.equals("h")) {
				series = "home";
			} else if (destinationActType.equals("w")) {
				series = "work";
			} else if (destinationActType.equals("e")) {
				series = "education";
			} else if (destinationActType.equals("s")) {
				series = "shopping";
			} else if (destinationActType.equals("l")) {
				series = "leisure";
			} else {
				series = "transit";
			}
			SimpleWriter writer = new SimpleWriter(filenameBase + series
					+ ".log");
			writer.writeln("departure time\ttravel time");

			Tuple<List<Double>, List<Double>> tuple = data
					.get(destinationActType);
			double[] departTimes = Collection2Array.toArrayFromDouble(tuple.getFirst());
			double[] travTimes = Collection2Array.toArrayFromDouble(tuple.getSecond());

			chart.addSeries(series, departTimes, travTimes);

			if (departTimes.length != travTimes.length) {
				throw new RuntimeException(
						"Lists of departure times and travel times should have the same size!");
			}

			Counter cnt = new Counter(series + " :\t");
			for (int i = 0; i < departTimes.length; i++) {
				writer.writeln(departTimes[i] + "\t" + travTimes[i]);
				writer.flush();
				cnt.incCounter();
			}
			writer.close();

		}
		chart.saveAsPng(filenameBase + "png", 1024, 768);
	}
}
