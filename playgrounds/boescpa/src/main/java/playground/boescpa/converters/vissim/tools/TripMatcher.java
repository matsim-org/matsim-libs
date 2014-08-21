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

import org.apache.commons.math.stat.regression.SimpleRegression;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import playground.boescpa.converters.vissim.ConvEvents2Anm;

import java.util.HashMap;

/**
 * Implements ConvEvents2Anm.TripMatcher with a simple linear regression as the similarity measure.
 *
 * @author boescpa
 */
public class TripMatcher implements ConvEvents2Anm.TripMatcher {

	private final static Logger log = Logger.getLogger(TripMatcher.class);

	private final static int TenPrctScore = 10000;
	private final static int LengthScore = 1;
	private final static int InterceptScore = 10;
	private final static int SlopeScore = 100;
	private final static int MSEScore = 100;

	@Override
	public HashMap<Id, Integer> matchTrips(HashMap<Id, Long[]> msTrips, HashMap<Id, Long[]> amTrips) {

		int matchesWithHighScores = 0;
		int matchesWithVeryHighScores = 0;
		int progressCounter = 0;
		int progressChecker = 2;

		HashMap<Id, Integer> countsPerAnmTrip = new HashMap<Id, Integer>();
		for (Id amTrip : amTrips.keySet()) {
			countsPerAnmTrip.put(amTrip, 0);
		}

		for (Id msTrip : msTrips.keySet()) {
			progressCounter++;
			Long[] msTripZones = msTrips.get(msTrip);

			Id bestMatchingAmTrip = null;
			int bestMatchScore = Integer.MIN_VALUE;

			for (Id amTrip : amTrips.keySet()) {
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
						simpleRegression.addData(i,0);
					}
				}

				// Scoring:
				int matchScore = 0;

				// Criterion 1.1: Difference in length of trips not greater than 10%.
				if (((double)Math.abs(msTripZones.length - amTripZones.length))/((double)msTripZones.length) <= 0.1) {
					matchScore += TenPrctScore;
				}
				// Criterion 1.2: The smaller the difference in length, the better.
				matchScore -= (Math.abs(msTripZones.length - amTripZones.length)*LengthScore);

				// Criterion 2: The closer the intercept to zero, the better.
				matchScore -= (int) (Math.abs(simpleRegression.getIntercept())*InterceptScore);

				// Criterion 3: The closer the slope to one, the better.
				matchScore -= (int) (Math.abs(1 - simpleRegression.getSlope())*SlopeScore);

				// Criterion 4: The smaller the mean square error of the regression, the better.
				matchScore -= (int) (Math.abs(simpleRegression.getMeanSquareError())*MSEScore);

				if (matchScore > bestMatchScore) {
					bestMatchScore = matchScore;
					bestMatchingAmTrip = amTrip;
				}
			}

			countsPerAnmTrip.put(bestMatchingAmTrip, (countsPerAnmTrip.get(bestMatchingAmTrip) + 1));

			if (bestMatchScore >= 0.9*TenPrctScore) {
				matchesWithHighScores++;
				if (bestMatchScore >= 0.99*TenPrctScore) {
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
				0.9*TenPrctScore + " points.");
		log.info("Of total " + msTrips.size() + " trips, " + matchesWithVeryHighScores + " were matched with a very high score above " +
				0.99*TenPrctScore + " points.");

		return countsPerAnmTrip;
	}
}
