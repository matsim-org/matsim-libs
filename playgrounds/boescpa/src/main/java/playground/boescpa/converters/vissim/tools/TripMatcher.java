/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.boescpa.converters.vissim.tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.math.stat.regression.SimpleRegression;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

import playground.boescpa.converters.vissim.ConvEvents;
import playground.boescpa.converters.vissim.tools.AbstractRouteConverter.Trip;

/**
 * Implements ConvEvents2Anm.TripMatcher with a simple linear regression as the similarity measure.
 *
 * @author boescpa
 */
public class TripMatcher implements ConvEvents.TripMatcher {

	private final static Logger log = Logger.getLogger(TripMatcher.class);

	private final static int TEN_PRCT_SCORE = 10000;

	private final static int LENGTH_SCORE = 1;
	private final static int INTERCEPT_SCORE = 100;
	private final static int SLOPE_SCORE = 100;
	private final static int MSE_SCORE = 100;

	private final static int NEG_OFFSET_IF_NOT_FOUNG = 1;

	@Override
	public HashMap<Id<Trip>, Integer> matchTrips(HashMap<Id<Trip>, Long[]> msTrips, HashMap<Id<Trip>, Long[]> amTrips) {

		int matchesWithHighScores = 0;
		int matchesWithVeryHighScores = 0;
		int progressCounter = 0;
		int progressChecker = 2;

		HashMap<Id<Trip>, Integer> countsPerAnmTrip = new HashMap<>();
		for (Id<Trip> amTrip : amTrips.keySet()) {
			countsPerAnmTrip.put(amTrip, 0);
		}
		List<Id<Trip>> amTripsKeySet = new ArrayList<>(amTrips.keySet());

		for (Id<Trip> msTrip : msTrips.keySet()) {
			progressCounter++;
			Long[] msTripZones = msTrips.get(msTrip);

			Id<Trip> bestMatchingAmTrip = null;
			int bestMatchScore = Integer.MIN_VALUE;

			// Shuffle key set:
			Collections.shuffle(amTripsKeySet);
			for (Id<Trip> amTrip : amTripsKeySet) {
				Long[] amTripZones = amTrips.get(amTrip);

				// Linear regression between the to trips:
				SimpleRegression simpleRegression = new SimpleRegression();
				for (int i = 0; i < msTripZones.length; i++) {
					boolean foundNone = true;
					for (int j = 0; j < amTripZones.length; j++) {
						if (msTripZones[i].equals(amTripZones[j])) {
							simpleRegression.addData(i,j);
							foundNone = false;
						}
					}
					if (foundNone) {
						int yPos = -(msTripZones.length - i) - NEG_OFFSET_IF_NOT_FOUNG;
						simpleRegression.addData(i, yPos);
					}
				}

				// Scoring:
				int matchScore = 0;

				// Criterion 1.1: Difference in length of trips not greater than 10%.
				if (((double)Math.abs(msTripZones.length - amTripZones.length))/((double)msTripZones.length) <= 0.1) {
					matchScore += TEN_PRCT_SCORE;
				}
				// Criterion 1.2: The smaller the difference in length, the better.
				matchScore -= (Math.abs(msTripZones.length - amTripZones.length)* LENGTH_SCORE);

				// Criterion 2: The closer the intercept to zero, the better.
				matchScore -= (int) (Math.abs(simpleRegression.getIntercept())* INTERCEPT_SCORE);

				// Criterion 3: The closer the slope to one, the better.
				matchScore -= (int) (Math.abs(1 - simpleRegression.getSlope())* SLOPE_SCORE);

				// Criterion 4: The smaller the mean square error of the regression, the better.
				matchScore -= (int) (Math.abs(simpleRegression.getMeanSquareError())* MSE_SCORE);

				if (matchScore > bestMatchScore) {
					bestMatchScore = matchScore;
					bestMatchingAmTrip = amTrip;
				}
			}

			countsPerAnmTrip.put(bestMatchingAmTrip, (countsPerAnmTrip.get(bestMatchingAmTrip) + 1));

			if (bestMatchScore >= 0.9* TEN_PRCT_SCORE) {
				matchesWithHighScores++;
				if (bestMatchScore >= 0.99* TEN_PRCT_SCORE) {
					matchesWithVeryHighScores++;
				}
			}

			// log progress:
			if (progressCounter >= progressChecker) {
				log.info(progressChecker + " trips matched.");
				progressChecker *= 2;
			}
		}

		log.info("Of total " + msTrips.size() + " trips, " + matchesWithHighScores + " were matched with a high score above " +
				0.9* TEN_PRCT_SCORE + " points.");
		log.info("Of total " + msTrips.size() + " trips, " + matchesWithVeryHighScores + " were matched with a very high score above " +
				0.99* TEN_PRCT_SCORE + " points.");

		return countsPerAnmTrip;
	}
}
