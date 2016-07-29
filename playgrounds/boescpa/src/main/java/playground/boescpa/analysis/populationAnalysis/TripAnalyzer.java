/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package playground.boescpa.analysis.populationAnalysis;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Analyses trip duration and length for a given plans-file.
 *
 * @author boescpa
 */
public class TripAnalyzer extends PopulationAnalyzer {

	private final Map<String, long[]> distanceSums = new HashMap<>();
	private final Map<String, long[]> travelTimes = new HashMap<>();

	public TripAnalyzer(Population population) {
		super(population);
	}

	public TripAnalyzer(String pop2bAnalyzed) {
		super(pop2bAnalyzed);
	}

	public static void main(final String[] args) {
		final String pop2bAnalyzed = args[0];
		final String resultsDest = args[1];
		final String actTypeToAnalyze = args.length > 2 ? args[2] : null;
		new TripAnalyzer(pop2bAnalyzed).analyzePopulation(resultsDest, actTypeToAnalyze);
	}

	@Override
	final protected void reset() {
		distanceSums.clear();
		travelTimes.clear();
	}

	@Override
	final protected void analyzeAgent(Person person) {
		double formerActEndTime = 0;
		String mode = null;
		Coord formerActCoord = null;
		for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
			if (planElement instanceof Activity) {
				Activity activity = (Activity) planElement;
				if (formerActCoord != null && (getActivityType() == null || activity.getType().equals(getActivityType()))) {
					classifyTravelTime("total", activity.getStartTime()-formerActEndTime);
					classifyTravelDist("total", 1.44*CoordUtils.calcEuclideanDistance(formerActCoord, activity.getCoord()));
					classifyTravelTime(mode, activity.getStartTime()-formerActEndTime);
					classifyTravelDist(mode, 1.44*CoordUtils.calcEuclideanDistance(formerActCoord, activity.getCoord()));
				}
				formerActCoord = activity.getCoord();
				formerActEndTime = activity.getEndTime();
			} else if (planElement instanceof Leg){
				Leg leg = (Leg) planElement;
				mode = leg.getMode();
			} else {
				log.error("Unhandled implementation of PlanElement: " + planElement.toString());
			}
		}
	}

	private void classifyTravelDist(String mode, double travelDistance) {
		synchronized (distanceSums) {
			if (!distanceSums.keySet().contains(mode)) {
				distanceSums.put(mode, new long[5]);
				modes.add(mode);
			}
		}
		if (travelDistance < 1000) {
			synchronized (distanceSums) {distanceSums.get(mode)[0]++;}
		} else if (travelDistance < 5*1000) {
			synchronized (distanceSums) {distanceSums.get(mode)[1]++;}
		} else if (travelDistance < 10*1000) {
			synchronized (distanceSums) {distanceSums.get(mode)[2]++;}
		} else if (travelDistance < 50*1000) {
			synchronized (distanceSums) {distanceSums.get(mode)[3]++;}
		} else {
			synchronized (distanceSums) {distanceSums.get(mode)[4]++;}
		}
	}

	private void classifyTravelTime(String mode, double travelTime) {
		synchronized (travelTimes) {
			if (!travelTimes.keySet().contains(mode)) {
				travelTimes.put(mode, new long[5]);
				modes.add(mode);
			}
		}
		if (travelTime < 15*60) {
			synchronized (travelTimes) {travelTimes.get(mode)[0]++;}
		} else if (travelTime < 30*60) {
			synchronized (travelTimes) {travelTimes.get(mode)[1]++;}
		} else if (travelTime < 45*60) {
			synchronized (travelTimes) {travelTimes.get(mode)[2]++;}
		} else if (travelTime < 60*60) {
			synchronized (travelTimes) {travelTimes.get(mode)[3]++;}
		} else {
			synchronized (travelTimes) {travelTimes.get(mode)[4]++;}
		}
	}

	@Override
	final protected String getResultString(String mode) {
		String modeString = "************************" + "\n" + "mode: " + mode + "\n" + "\n";
		modeString = modeString + "0-1km   " + "\t" + distanceSums.get(mode)[0] + "\n";
		modeString = modeString + "1-5km   " + "\t" + distanceSums.get(mode)[1] + "\n";
		modeString = modeString + "5-10km  " + "\t" + distanceSums.get(mode)[2] + "\n";
		modeString = modeString + "10-50km " + "\t" + distanceSums.get(mode)[3] + "\n";
		modeString = modeString + ">50km   " + "\t" + distanceSums.get(mode)[4] + "\n" + "\n";
		modeString = modeString + "0-15min " + "\t" + travelTimes.get(mode)[0] + "\n";
		modeString = modeString + "15-30min" + "\t" + travelTimes.get(mode)[1] + "\n";
		modeString = modeString + "30-45min" + "\t" + travelTimes.get(mode)[2] + "\n";
		modeString = modeString + "45-60min" + "\t" + travelTimes.get(mode)[3] + "\n";
		modeString = modeString + ">60min  " + "\t" + travelTimes.get(mode)[4] + "\n";
		return modeString;
	}

	@Override
	final protected Tuple<String, double[]> getSeries(String mode) {
		return null;
	}
}
