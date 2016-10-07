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
package playground.agarwalamit.munich.analysis.userGroup;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.io.IOUtils;

import playground.benjamin.scenarios.munich.analysis.filter.PersonFilter;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;

/**
 * @author amit
 */

public class UserGroupToTollData {

	private final Map<Id<Person>, Double> person2Toll = new HashMap<>();
	private final Map<UserGroup, Double> userGrpToToll = new TreeMap<>();


	public static void main(String[] args) {
		String runDir = "../../../repos/runs-svn/detEval/emissionCongestionInternalization/output/1pct/run10/policies/bau/analysis/";

		String inFile = runDir+"/simpleAverageToll.txt";
		String outFile = runDir+"averagedUserGroupToll.txt";

		UserGroupToTollData taug = new UserGroupToTollData();
		taug.readFileAndStoreData(inFile);
		taug.filterAndWriteUserGroupData(outFile);
	}

	private void readFileAndStoreData (String file){
		BufferedReader reader = IOUtils.getBufferedReader(file);
		boolean headerLine = true; // assuming first line is always a header line

		try {
			String line = reader.readLine();
			while (line!=null){
				String parts [] = line.split("\t");

				if(!headerLine){
					Id<Person> personId = Id.createPersonId(parts[0]);
					double toll = Double.valueOf(parts[1]);

					person2Toll.put(personId, toll);
				} else { // first line will jump here and then no more header lines.
					headerLine = false;
				}
				line = reader.readLine();
			}

		} catch (IOException e) {
			throw new RuntimeException("Data is not read from file. Reason: "
					+ e);
		}
	}

	private void filterAndWriteUserGroupData(String outFile){
		PersonFilter pf = new PersonFilter();

		for(UserGroup ug : UserGroup.values()){
			userGrpToToll.put(ug, 0.);
		}

		for(UserGroup ug : UserGroup.values()){
			for(Id<Person> pId : person2Toll.keySet()){
				if(pf.isPersonIdFromUserGroup(pId, ug)){
					double tollSoFar = userGrpToToll.get(ug);
					userGrpToToll.put(ug, tollSoFar+person2Toll.get(pId));
				}
			}
		}

		BufferedWriter writer = IOUtils.getBufferedWriter(outFile);
		try {
			writer.write("UserGroup\t toll \n");
			for(UserGroup ug :userGrpToToll.keySet()){
				writer.write(ug+"\t"+userGrpToToll.get(ug)+"\n");
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: "
					+ e);
		}
	}
}
