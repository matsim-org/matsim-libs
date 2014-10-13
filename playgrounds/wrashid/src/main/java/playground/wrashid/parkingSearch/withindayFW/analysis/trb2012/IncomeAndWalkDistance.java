/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.wrashid.parkingSearch.withindayFW.analysis.trb2012;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.DoubleValueHashMap;
import org.matsim.contrib.parking.lib.obj.IntegerValueHashMap;
import org.matsim.contrib.parking.lib.obj.Matrix;


public class IncomeAndWalkDistance {

	public static void main(String[] args) {
		String basePath = "H:/data/experiments/TRBAug2012/runs/run77/output/";
		String fileName = basePath + "houseHoldIncome.txt";

		DoubleValueHashMap<Id<Person>> income = readIncome(fileName);

		int iterationNumber = 35;

		 //String fileNameInputFile = ".walkTimes.txt";
		String fileNameInputFile = ".parkingCostLog.txt";

		String walkTimesFile = basePath + "ITERS/it." + iterationNumber + "/" + iterationNumber + fileNameInputFile;

		IntegerValueHashMap<Id> numberOfParkingActs = new IntegerValueHashMap<Id>();
		DoubleValueHashMap<Id> walkTimes = new DoubleValueHashMap<Id>();
		Matrix walkTimesMatrix = GeneralLib.readStringMatrix(walkTimesFile, "\t");

		for (int i = 1; i < walkTimesMatrix.getNumberOfRows(); i++) {
			String parkingIdString = walkTimesMatrix.getString(i, 1);
			if (parkingIdString.contains("gp") || parkingIdString.contains("stp")) {
				Id<Person> personId = Id.create(walkTimesMatrix.getString(i, 0), Person.class);
				double walkTime = walkTimesMatrix.getDouble(i, 2);
				
				if (walkTime!=0){
					numberOfParkingActs.increment(personId);
					walkTimes.incrementBy(personId, walkTime);
				}
			}
		}

		for (Id<Person> personId : walkTimes.keySet()) {
			double averageWalkTime = walkTimes.get(personId) / numberOfParkingActs.get(personId);
			walkTimes.put(personId, averageWalkTime);
		}

		IntegerValueHashMap<Integer> categoryFrequency = new IntegerValueHashMap<Integer>();
		DoubleValueHashMap<Integer> categorySum = new DoubleValueHashMap<Integer>();
		for (Id<Person> personId : walkTimes.keySet()) {
			int incomeCategory = getIncomeCategory(income.get(personId));
			categorySum.incrementBy(incomeCategory, walkTimes.get(personId));
			categoryFrequency.increment(incomeCategory);
		}

		for (Integer incomeCategory : categorySum.keySet()) {
			categorySum.put(incomeCategory, categorySum.get(incomeCategory) / categoryFrequency.get(incomeCategory));
		}

		categorySum.printToConsole();

	}

	public static DoubleValueHashMap<Id<Person>> readIncome(String fileName) {
		DoubleValueHashMap<Id<Person>> houseHoldIncome = new DoubleValueHashMap<>();

		Matrix morningMatrix = GeneralLib.readStringMatrix(fileName, "\t");

		for (int i = 1; i < morningMatrix.getNumberOfRows(); i++) {
			Id<Person> id = Id.create(morningMatrix.getString(i, 0), Person.class);
			double income = morningMatrix.getDouble(i, 1);
			houseHoldIncome.put(id, income);
		}
		return houseHoldIncome;
	}

	public static int getIncomeCategory(double income) {
		if (income < 2000) {
			return 0;
		} else if (income < 4000) {
			return 1;
		} else if (income < 6000) {
			return 2;
		} else if (income < 8000) {
			return 3;
		} else if (income < 10000) {
			return 4;
		} else if (income < 12000) {
			return 5;
		} else if (income < 14000) {
			return 6;
		} else if (income < 16000) {
			return 7;
		} else {
			return 8;
		}
	}

}
