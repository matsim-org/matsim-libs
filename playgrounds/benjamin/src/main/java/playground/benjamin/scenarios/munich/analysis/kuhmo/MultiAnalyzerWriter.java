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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

import playground.benjamin.scenarios.munich.analysis.EmissionUtils;
import playground.benjamin.scenarios.munich.analysis.filter.PersonFilter;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;

/**
 * @author benjamin
 *
 */
public class MultiAnalyzerWriter {
	private static final Logger logger = Logger.getLogger(MultiAnalyzerWriter.class);

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

	public void writeCarDistanceInformation(Map<Id, Double> personId2CarDistance, Map<UserGroup, Double> userGroup2carTrips) {
		String fileName = this.outputPath + "carDistanceInformation_" + runName + ".txt";
		File file = new File(fileName);

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));

			bw.write("\t car users \t # of car departures \t total car distance [km] \t avg car distance per car user [km] \t avg car distance per car departure [km]");
			bw.newLine();

			for(UserGroup userGroup : UserGroup.values()){
				double totalCarDistanceInGroup_km = 0.0;
				int groupSize = 0;
				for(Id personId : personId2CarDistance.keySet()){
					if(personFilter.isPersonIdFromUserGroup(personId, userGroup)){
						totalCarDistanceInGroup_km += (personId2CarDistance.get(personId)) / 1000.;
						groupSize++;
					}
				}
				double avgCarDistancePerCarUser_km = totalCarDistanceInGroup_km / groupSize;
				double noOfCarTripsInGroup = userGroup2carTrips.get(userGroup);
				double avgCarDistancePerTrip_km = totalCarDistanceInGroup_km / noOfCarTripsInGroup;
				String row = userGroup.toString() + "\t" + groupSize + "\t" + noOfCarTripsInGroup + "\t" + totalCarDistanceInGroup_km + "\t" + avgCarDistancePerCarUser_km + "\t" + avgCarDistancePerTrip_km;
				bw.write(row);
				bw.newLine();
			}
			bw.close();
			logger.info("Finished writing output to " + fileName);
		} catch (IOException e) {

		}
	}

	public void writeAvgTTInformation(Map<String, Map<Id, Double>> mode2personId2TravelTime, Map<UserGroup, Map<String, Double>> userGroup2mode2noOfTrips) {
		String fileName = this.outputPath + "avgTTInformation_" + runName + ".txt";
		File file = new File(fileName);

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));

			bw.write("\t mode \t users \t # of departures \t total travelTime [min] \t avg travelTime per user [min] \t avg travelTime per departure [min]");
			bw.newLine();

			for(UserGroup userGroup : UserGroup.values()){
				for(String mode : mode2personId2TravelTime.keySet()){

					if(userGroup2mode2noOfTrips.get(userGroup).get(mode) == null){
						// do nothing
					} else {
						bw.write(userGroup.toString());
					}
					
					Map<Id, Double> personId2TravelTime = mode2personId2TravelTime.get(mode);

					double sumOfTravelTimes = 0.0;
					int groupSize = 0;
					for(Id personId : personId2TravelTime.keySet()){
						if(personFilter.isPersonIdFromUserGroup(personId, userGroup)){
							sumOfTravelTimes += personId2TravelTime.get(personId);
							groupSize++;
						}
					}
					double sumOfTravelTimes_min = sumOfTravelTimes / 60.;
					double avgTravelTimeOfModePerUser_mins = sumOfTravelTimes_min / groupSize;
					
					Double avgTravelTimeOfModePerTrip_mins = null;
					Double noOfModeTripsInGroup = null;
					if(userGroup2mode2noOfTrips.get(userGroup).get(mode) == null){
						// do nothing
					} else {
						noOfModeTripsInGroup = userGroup2mode2noOfTrips.get(userGroup).get(mode);
						avgTravelTimeOfModePerTrip_mins = sumOfTravelTimes_min / noOfModeTripsInGroup;
					}
					if(groupSize != 0){
						String modeInfo = "\t" + mode + "\t" + groupSize + "\t" + noOfModeTripsInGroup + "\t" + sumOfTravelTimes_min + "\t" + avgTravelTimeOfModePerUser_mins + "\t" + avgTravelTimeOfModePerTrip_mins;
						bw.write(modeInfo);
						bw.newLine();
					}
				}
			}
			bw.close();
			logger.info("Finished writing output to " + fileName);
		} catch (IOException e) {

		}
	}
	
	protected String getRunName() {
		return runName;
	}

	protected void setRunName(String runName) {
		this.runName = runName;
	}
}