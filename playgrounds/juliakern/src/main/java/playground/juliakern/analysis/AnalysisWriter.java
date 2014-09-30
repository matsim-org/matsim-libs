/* *********************************************************************** *
 * project: org.matsim.*
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
 * *********************************************************************** */

package playground.juliakern.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;

public class AnalysisWriter {

	public void writeCostPerKmInformation(
			Map<UserGroup, List<Double>> usergroup2listOfcostPerKm, String outPutDir) {
		for(UserGroup usergroup: usergroup2listOfcostPerKm.keySet()){
			// write new file for each user group
			String filename = outPutDir + "TollPaymentsPerKm_" + usergroup.toString() + ".txt";
			File file = new File(filename);
			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter(file));
				List<Double> tpklist = usergroup2listOfcostPerKm.get(usergroup);
				for(int i = 0; i<tpklist.size(); i++){
					bw.write(tpklist.get(i).toString());
					bw.newLine();
				}
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
}
