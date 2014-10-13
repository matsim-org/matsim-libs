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
import org.matsim.contrib.parking.lib.obj.Matrix;


public class FilterWalkTimes {

	public static void main(String[] args) {
		String basePath = "H:/data/experiments/TRBAug2012/runs/run91/output/";
		String fileName = basePath + "houseHoldIncome.txt";

		int iterationNumber = 1;

		 String fileNameInputFile = ".walkTimes.txt";

		String walkTimesFile = basePath + "ITERS/it." + iterationNumber + "/" + iterationNumber + fileNameInputFile;

		Matrix walkTimesMatrix = GeneralLib.readStringMatrix(walkTimesFile, "\t");

		for (int i = 1; i < walkTimesMatrix.getNumberOfRows(); i++) {
			String parkingIdString = walkTimesMatrix.getString(i, 1);
			if (parkingIdString.contains("gp") || parkingIdString.contains("stp")) {
				Id<Person> personId = Id.create(walkTimesMatrix.getString(i, 0), Person.class);
				double walkTime = walkTimesMatrix.getDouble(i, 2);
				
				System.out.println(parkingIdString + "\t" + walkTime);
			}
		}
	}
	
}
