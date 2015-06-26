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
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.io.IOUtils;

import playground.benjamin.scenarios.munich.analysis.filter.PersonFilter;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;

/**
 * @author amit
 */

public class UserGrpToTollData {
	
	public static void main(String[] args) {

		String outputDir = "/Users/amit/Documents/repos/runs-svn/detEval/emissionCongestionInternalization/output/1pct/run9/";
		String [] runCases = {"baseCaseCtd","ei","ci","eci"};
		for (String run : runCases){
			new UserGrpToTollData().run(outputDir+run+"/analysis/");
		}
		
	}
	
	private void run(String outputFolder){
		BufferedReader reader = IOUtils.getBufferedReader(outputFolder+"/simpleAverageToll.txt");
		Map<Id<Person>, Double> pId2Tolls= new HashMap<>();
		try {
			String line = reader.readLine();
			
			while (line!=null){
				String args[] = line.split("\t");
				String pId = args[0];
//				String toll = args[1];
				
				if(!args[1].equalsIgnoreCase("averageToll")){
					Id<Person> person = Id.createPersonId(pId);
					pId2Tolls.put(person, Double.valueOf(args[1]));
				}
				
				line = reader.readLine();
			}
			
			
		} catch (IOException e) {
			e.printStackTrace();
			
		}
		
		SortedMap<UserGroup, Double> userGrpToToll = new TreeMap<UserGroup, Double>();
		PersonFilter pf = new PersonFilter();
		double totalToll =0;

		for(UserGroup ug : UserGroup.values()){
			userGrpToToll.put(ug, 0.);
		}

		for(UserGroup ug : UserGroup.values()){
			for(Id<Person> pId : pId2Tolls.keySet()){
				if(pf.isPersonIdFromUserGroup(pId, ug)){
					double tollSoFar = userGrpToToll.get(ug);
					userGrpToToll.put(ug, tollSoFar+pId2Tolls.get(pId));
					totalToll = totalToll+pId2Tolls.get(pId);
				}
			}
		}

		String outputFile = outputFolder+"//avgTollData_2.txt";
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFile);

		try {
			writer.write("UserGroup \t toll \n");

			for(UserGroup ug : userGrpToToll.keySet()){
				writer.write(ug+"\t"+userGrpToToll.get(ug)+"\n");
			}
			writer.write("total toll \t"+totalToll+"\n");
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: "
					+ e);
		}
		
	}
	

}
