/* *********************************************************************** *
 * project: org.matsim.*
 * MultiAnalyzerWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.benjamin.scenarios.munich.analysis.kuhmo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.SortedMap;

import org.matsim.api.core.v01.Id;

import playground.benjamin.scenarios.munich.analysis.EmissionUtils;
import playground.benjamin.scenarios.munich.analysis.filter.PersonFilter;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;

/**
 * @author benjamin
 *
 */
public class MultiAnalyzerWriter {

	String outputPath;
	String runName;
	EmissionUtils emissionUtils;
	PersonFilter personFilter;

	public MultiAnalyzerWriter(String outputPath){
		this.outputPath = outputPath;
		emissionUtils = new EmissionUtils();
		personFilter = new PersonFilter();
	}

	public void writeWelfareTollInformation() {

	}

	public void writeEmissionInformation(SortedMap<UserGroup, SortedMap<String, Double>> group2TotalEmissions){
		File file = new File(this.outputPath + "emissionInformation_" + runName + ".txt");

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));

			for(String pollutant : emissionUtils.getListOfPollutants()){
				bw.write("\t" + pollutant);
			}
			bw.newLine();

			for(UserGroup userGroup : group2TotalEmissions.keySet()){
				Map<String, Double> pollutant2TotalEmissions = group2TotalEmissions.get(userGroup);

				bw.write(userGroup.toString());
				for(String pollutant : pollutant2TotalEmissions.keySet()){
					Double pollutantValue = pollutant2TotalEmissions.get(pollutant);
					bw.write("\t" + pollutantValue.toString());
				}
				bw.newLine();
			}
			//	    bw.flush();
			bw.close();
		} catch (IOException e) {

		}
	}

	protected String getRunName() {
		return runName;
	}

	protected void setRunName(String runName) {
		this.runName = runName;
	}

	public void writeCarDistanceInformation(Map<Id, Double> personId2CarDistance) {
		File file = new File(this.outputPath + "carDistanceInformation_" + runName + ".txt");

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));

			bw.write("\t car users \t total car distance [km] \t avg car distance [km]");
			bw.newLine();

			for(UserGroup userGroup : UserGroup.values()){
				double sumOfCarDistancesInGroup = 0.0;
				int groupSize = 0;
				for(Id personId : personId2CarDistance.keySet()){
					if(personFilter.isPersonIdFromUserGroup(personId, userGroup)){
						sumOfCarDistancesInGroup += personId2CarDistance.get(personId);
						groupSize++;
					}
				}
				double avgCarDistance = sumOfCarDistancesInGroup / groupSize ;
				String row = userGroup.toString() + "\t" + groupSize + "\t" + (sumOfCarDistancesInGroup / 1000.) + "\t" + (avgCarDistance / 1000.);
				bw.write(row);
				bw.newLine();
			}
			bw.close();
		} catch (IOException e) {

		}
	}

	public void writeavgTTInformation(Map<String, Map<Id, Double>> mode2personId2TravelTime) {
		File file = new File(this.outputPath + "avgTTInformation_" + runName + ".txt");

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));

			bw.write("\t mode \t users \t avg travelTime [min]");
			bw.newLine();

			for(UserGroup userGroup : UserGroup.values()){
				bw.write(userGroup.toString());
				for(String mode : mode2personId2TravelTime.keySet()){
					Map<Id, Double> personId2TravelTime = mode2personId2TravelTime.get(mode);

					double sumOfTravelTimes = 0.0;
					int groupSize = 0;

					for(Id personId : personId2TravelTime.keySet()){
						if(personFilter.isPersonIdFromUserGroup(personId, userGroup)){
							sumOfTravelTimes += personId2TravelTime.get(personId);
							groupSize++;
						}
					}
					double avgTravelTimeOfMode_mins = (sumOfTravelTimes / groupSize) / 60.;
					if(groupSize != 0){
						String modeInfo = "\t" + mode + "\t" + groupSize + "\t" + avgTravelTimeOfMode_mins;
						bw.write(modeInfo);
						bw.newLine();
					}
				}
			}
			bw.close();
		} catch (IOException e) {

		}
	}
}