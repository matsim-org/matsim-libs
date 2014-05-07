/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.random;

import java.util.HashMap;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.lib.GeneralLib;

import playground.wrashid.lib.obj.TwoHashMapsConcatenated;
import playground.wrashid.parkingSearch.ppSim.jdepSim.zurich.ZHScenarioGlobal;

public class RandomNumbers {

	static TwoHashMapsConcatenated<Id, Integer, Random> randomNumbers;

	static int counter = 0;

	public static Random getRandomNumber(Id personId, Integer legIndex, String strategyName) {
		if (randomNumbers.get(personId, legIndex) == null) {
			int seed = ZHScenarioGlobal.loadIntParam("RandomNumbers.seed");
			seed += strategyName.toString().hashCode();
			seed += legIndex;
			seed += personId.toString().hashCode();

			randomNumbers.put(personId, legIndex, new Random(seed));
		}
		return randomNumbers.get(personId, legIndex);
	}

	public static void reset() {
		randomNumbers = new TwoHashMapsConcatenated<Id, Integer, Random>();
	}

	public static Random getGlobalbRandom() {
		return new Random(ZHScenarioGlobal.loadIntParam("RandomNumbers.seed") + counter++);
	}

	public static void main(String[] args) {
		// GeneralLib.generateHistogram(fileName, value, numberOfBins, title,
		// xLabel, yLabel);
		for (int i = 0; i < 10; i++) {
			System.out.println(getLinearRandomNumber(10));
		}
	}

	public static int getLinearRandomNumber(int maxSize) {
		int randomMultiplier = maxSize * (maxSize + 1) / 2;
		Random r = new Random();
		int randomInt = r.nextInt(randomMultiplier);

		int linearRandomNumber = 0;
		for (int i = maxSize; randomInt >= 0; i--) {
			randomInt -= i;
			linearRandomNumber++;
		}

		return linearRandomNumber;
	}
}
