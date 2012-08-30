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
package playground.andreas.aas.modules.multiAnalyzer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;

import playground.andreas.aas.modules.multiAnalyzer.scenarios.munich.analysis.EmissionUtils;
import playground.andreas.aas.modules.multiAnalyzer.scenarios.munich.analysis.filter.PersonFilter;
import playground.andreas.aas.modules.multiAnalyzer.scenarios.munich.analysis.filter.UserGroup;

/**
 * @author aneumann, benjamin
 *
 */
public class MultiAnalyzerWriter {
	private final static Logger log = Logger.getLogger(MultiAnalyzerWriter.class);

	private PersonFilter personFilter = new PersonFilter();;

	public void writeWelfareTollInformation(String outputFolder, Config config, Population pop, Map<Id,Double> personId2Toll) {
		String fileName = outputFolder + "welfareTollInformation.txt";
		UserWelfareCalculator userWelfareCalculator = new UserWelfareCalculator(config);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(fileName)));

			bw.write("user group \t users \t toll payers \t user logsum [EUR] \t toll payments [EUR]");
			bw.newLine();

			for(UserGroup userGroup : UserGroup.values()){
				bw.write(userGroup.toString());
				
				Population userGroupPop = this.personFilter.getPopulation(pop, userGroup);

				double userWelfareOfGroup = userWelfareCalculator.calculateLogsum(userGroupPop);
				int personWithNoValidPlanCnt = userWelfareCalculator.getNoValidPlanCnt();
				log.warn(": users with no valid plan (all scores ``== null'' or ``<= 0.0'') in group " + userGroup + " : " + personWithNoValidPlanCnt);
				
				double tollRevenueOfGroup = 0.0;
				int groupSize = 0;

				for(Id personId : personId2Toll.keySet()){
					if(this.personFilter.isPersonIdFromUserGroup(personId, userGroup)){
						tollRevenueOfGroup += personId2Toll.get(personId);
						groupSize++;
					}
				}
				// need to take the absolute value since money events are negative from the users' perspective.
				double absoluteTollRevenueUserGroup = Math.abs(tollRevenueOfGroup);
				String row = "\t" + userGroupPop.getPersons().size() + "\t" + groupSize + "\t" + userWelfareOfGroup + "\t" + absoluteTollRevenueUserGroup;
				bw.write(row);
				bw.newLine();
				userWelfareCalculator.reset();
			}
			bw.close();
			log.info("Finished writing output to " + fileName);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void writeEmissionInformation(String outputFolder, SortedMap<UserGroup, SortedMap<String, Double>> group2TotalEmissions){
		String fileName = outputFolder + "emissionInformation.txt";

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(fileName)));

			bw.write("user group");
			for(String pollutant : new EmissionUtils().getListOfPollutants()){
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
			bw.close();
			log.info("Finished writing output to " + fileName);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void writeAvgCarDistanceInformation(String outputFolder, Map<Id, Double> personId2CarDistance, Map<UserGroup, Double> userGroup2carTrips) {
		String fileName = outputFolder + "avgCarDistanceInformation.txt";

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(fileName)));

			bw.write("user group \t car users \t car departures \t total car distance [km] \t avg car distance per car user [km] \t avg car distance per car departure [km]");
			bw.newLine();

			for(UserGroup userGroup : UserGroup.values()){
				double totalCarDistanceInGroup_km = 0.0;
				int groupSize = 0;
				for(Id personId : personId2CarDistance.keySet()){
					if(this.personFilter.isPersonIdFromUserGroup(personId, userGroup)){
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
			log.info("Finished writing output to " + fileName);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeDetailedCarDistanceInformation(String outputFolder, Map<Id, Double> personId2carDistance) {
		log.warn(": number of car users in distance map (users with departure events): " + personId2carDistance.size());
		
		String fileName = outputFolder + "detailedCarDistanceInformation.txt";

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(fileName)));

			bw.write("person id \t user group \t total car distance [km]");
//			bw.write("user group \t total car distance [km]");
			bw.newLine();
			
			for(UserGroup userGroup : UserGroup.values()){
				for(Id personId : personId2carDistance.keySet()){
					if(this.personFilter.isPersonIdFromUserGroup(personId, userGroup)){
						Double individualCarDistance_km = personId2carDistance.get(personId) / 1000.;

						bw.write(personId.toString() + "\t");
						bw.write(userGroup.toString() + "\t");
						bw.write(individualCarDistance_km.toString());
						bw.newLine();
					}
				}
			}
			bw.close();
			log.info("Finished writing output to " + fileName);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void writeAvgTTInformation(String outputFolder, Map<String, Map<Id, Double>> mode2personId2TravelTime, Map<UserGroup, Map<String, Double>> userGroup2mode2noOfTrips) {
		log.warn(": number of car users in traveltime map (users with departure and arrival events): " + mode2personId2TravelTime.get(TransportMode.car).size());
		
		String fileName = outputFolder + "avgTTInformation.txt";

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(fileName)));

			bw.write("user group \t mode \t users \t departures \t total travelTime [min] \t avg travelTime per user [min] \t avg travelTime per departure [min]");
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
						if(this.personFilter.isPersonIdFromUserGroup(personId, userGroup)){
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
			log.info("Finished writing output to " + fileName);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}