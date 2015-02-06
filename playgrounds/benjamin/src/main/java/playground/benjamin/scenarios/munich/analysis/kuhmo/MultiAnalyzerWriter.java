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
import java.util.ArrayList;
import java.util.Map;
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.emissions.utils.EmissionUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

import playground.benjamin.scenarios.munich.analysis.filter.PersonFilter;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;
import playground.vsp.analysis.modules.userBenefits.UserBenefitsCalculator;
import playground.vsp.analysis.modules.userBenefits.WelfareMeasure;

/**
 * @author benjamin
 *
 */
public class MultiAnalyzerWriter {
	private static final Logger logger = Logger.getLogger(MultiAnalyzerWriter.class);

	File outputDir;
	String runName;
	EmissionUtils emissionUtils;
	PersonFilter personFilter;

	public MultiAnalyzerWriter(String outputPath){
		this.outputDir = new File(outputPath + "analysis");
		this.outputDir.mkdir();
		
		emissionUtils = new EmissionUtils();
		personFilter = new PersonFilter();
	}

	public void writeWelfareTollInformation(String configFile, Population pop, Map<Id<Person>,Double> personId2Toll) {
		String fileName = this.outputDir + "/welfareTollInformation_" + runName + ".txt";
		File file = new File(fileName);
		
		Config config = ConfigUtils.loadConfig(configFile);
		UserBenefitsCalculator userBenefitsCalculator = new UserBenefitsCalculator(config, WelfareMeasure.LOGSUM, false);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));

			bw.write("user group \t users \t toll payers \t user logsum [EUR] \t toll payments [EUR]");
			bw.newLine();

			for(UserGroup userGroup : UserGroup.values()){
				bw.write(userGroup.toString());
				
				Population userGroupPop = personFilter.getPopulation(pop, userGroup);

				double userWelfareOfGroup = userBenefitsCalculator.calculateUtility_money(userGroupPop);
				int personWithNoValidPlanCnt = userBenefitsCalculator.getPersonsWithoutValidPlanCnt();
				logger.warn(runName + ": users with no valid plan (all scores ``== null'' or ``<= 0.0'') in group " + userGroup + " : " + personWithNoValidPlanCnt);
				
				double tollRevenueOfGroup = 0.0;
				int groupSize = 0;

				for(Id personId : personId2Toll.keySet()){
					if(personFilter.isPersonIdFromUserGroup(personId, userGroup)){
						tollRevenueOfGroup += personId2Toll.get(personId);
						groupSize++;
					}
				}
				// need to take the absolute value since money events are negative from the users' perspective.
				double absoluteTollRevenueUserGroup = Math.abs(tollRevenueOfGroup);
				String row = "\t" + userGroupPop.getPersons().size() + "\t" + groupSize + "\t" + userWelfareOfGroup + "\t" + absoluteTollRevenueUserGroup;
				bw.write(row);
				bw.newLine();
				userBenefitsCalculator.reset();
			}
			bw.close();
			logger.info("Finished writing output to " + fileName);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void writeEmissionInformation(SortedMap<UserGroup, SortedMap<String, Double>> group2TotalEmissions){
		String fileName = this.outputDir + "/emissionInformation_" + runName + ".txt";
		File file = new File(fileName);

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));

			bw.write("user group");
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
			bw.close();
			logger.info("Finished writing output to " + fileName);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void writeAvgCarDistanceInformation(Map<Id, Double> personId2CarDistance, Map<UserGroup, Double> userGroup2carTrips) {
		String fileName = this.outputDir + "/avgCarDistanceInformation_" + runName + ".txt";
		File file = new File(fileName);

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));

			bw.write("user group \t car users \t car departures \t total car distance [km] \t avg car distance per car user [km] \t avg car distance per car departure [km]");
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
			e.printStackTrace();
		}
	}
	
	public void writeDetailedCarDistanceInformation(Map<Id, Double> personId2carDistance) {
		String fileName = this.outputDir + "/detailedCarDistanceInformation_" + runName + ".txt";
		File file = new File(fileName);

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));

			bw.write("person id \t user group \t total car distance [km]");
//			bw.write("user group \t total car distance [km]");
			bw.newLine();
			
			for(UserGroup userGroup : UserGroup.values()){
				for(Id personId : personId2carDistance.keySet()){
					if(personFilter.isPersonIdFromUserGroup(personId, userGroup)){
						Double individualCarDistance_km = personId2carDistance.get(personId) / 1000.;

						bw.write(personId.toString() + "\t");
						bw.write(userGroup.toString() + "\t");
						bw.write(individualCarDistance_km.toString());
						bw.newLine();
					}
				}
			}
			bw.close();
			logger.info("Finished writing output to " + fileName);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void writeAvgTTInformation(Map<String, Map<Id, Double>> mode2personId2TravelTime, Map<UserGroup, Map<String, Double>> userGroup2mode2noOfTrips) {
		String fileName = this.outputDir + "/avgTTInformation_" + runName + ".txt";
		File file = new File(fileName);

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));

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
			e.printStackTrace();
		}
	}
	
	protected String getRunName() {
		return runName;
	}

	public void setRunName(String runName) {
		this.runName = runName;
	}

	public void writeEmissionCostInformation(
			SortedMap<UserGroup, SortedMap<String, Double>> group2totalEmissionCosts) {
		String fileName = this.outputDir + "/emissionCostInformation_" + runName + ".txt";
		File file = new File(fileName);

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));

			bw.write("user group");
			
			ArrayList<String> listOfPollutants = new ArrayList<String>();
			// list of pollutants
			for(UserGroup ug: group2totalEmissionCosts.keySet()){
				for(String pollutant: group2totalEmissionCosts.get(ug).keySet()){
					if(!listOfPollutants.contains(pollutant) && group2totalEmissionCosts.get(ug).get(pollutant)>0.0){
						listOfPollutants.add(pollutant);
					}
				}
			}
			
			for(int i=0; i<listOfPollutants.size(); i++){
				bw.write("\t" + listOfPollutants.get(i));
			}
	
			bw.newLine();

			for(UserGroup userGroup : group2totalEmissionCosts.keySet()){
				Map<String, Double> pollutant2TotalEmissions = group2totalEmissionCosts.get(userGroup);

				bw.write(userGroup.toString());
				for(int i=0; i<listOfPollutants.size(); i++){
					Double pollutantValue = pollutant2TotalEmissions.get(listOfPollutants.get(i));
					bw.write("\t" + pollutantValue.toString());
				}
				bw.newLine();
			}
			bw.close();
			logger.info("Finished writing output to " + fileName);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
}