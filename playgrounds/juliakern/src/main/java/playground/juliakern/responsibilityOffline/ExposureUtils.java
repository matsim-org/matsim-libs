/* *********************************************************************** *
 * project: org.matsim.*
 * SpatialAveragingForLinkEmissions.java
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
package playground.juliakern.responsibilityOffline;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;

import playground.juliakern.distribution.ResponsibilityEvent;

/**
 * class to analyze responsibility events 
 *  
 *  print information on responsibility or exposure from responsibility events to a text file
 *   personal: per agent
 *   otherwise: accumulated for all causing/receiving agents
 *  
 * @author julia
 *
 */

public class ExposureUtils {

	public void printResponsibilityInformation(ArrayList<ResponsibilityEvent> responsibilityAndExposure,
			String outputPath) {
		// sum, average
		
		// sum = #persons x concentration x time
		// timeXpersons = #persons x time
		
		Double sum =0.0;
		Double timeXpersons =0.0;
		
		Map<Id, Double> person2timeXconcentration = new HashMap<Id, Double>();
		Map<Id, Double> person2time = new HashMap<Id, Double>();
		Map<Id, Double> person2average = new HashMap<Id, Double>();
		
		for(ResponsibilityEvent re : responsibilityAndExposure){
			Id personId = re.getResponsiblePersonId();
			if(person2timeXconcentration.containsKey(personId)){
				Double newValue = person2timeXconcentration.get(personId)+re.getExposureValue();
				person2timeXconcentration.put(personId, newValue);
				Double newTime = person2time.get(personId) + re.getDuration();
				person2time.put(personId, newTime);
			}else{
				person2timeXconcentration.put(personId, re.getExposureValue());
				person2time.put(personId, re.getDuration());
			}
		}
		
		Double personalTotalMax = -1.0;
		Double personalTotalMin = Double.MAX_VALUE;
		
		for(Id personId: person2timeXconcentration.keySet()){
			Double value = person2timeXconcentration.get(personId);
			sum += value;
			timeXpersons += person2time.get(personId);
			person2average.put(personId, (value/person2time.get(personId))); // wie viel sinn macht es, das auszurechnen?
			if(value<personalTotalMin)personalTotalMin=value;
			if(value>personalTotalMax)personalTotalMax=value;
			
		}
		
		Double personalAverageAverage =0.0;
		Double personalAverageMin = Double.MAX_VALUE;
		Double personalAverageMax = -1.0;
		
		for(Id personId: person2average.keySet()){
			Double pa = person2average.get(personId);
			personalAverageAverage+= pa;
			if(pa<personalAverageMin)personalAverageMin=pa;
			if(pa>personalAverageMax)personalAverageMax=pa;
		}
		personalAverageAverage /= person2average.size();
		
		// write results
		BufferedWriter buffW;
		try {
			buffW = new BufferedWriter(new FileWriter(outputPath));

			// header: Person base case compare case difference
			buffW.write("Responsibility Information");
			buffW.newLine();
			
			buffW.write("Person x time x concentration : " + sum);
			buffW.newLine();
			buffW.write("Average of average responsibility : " + personalAverageAverage);
			buffW.newLine();
			buffW.write("Maximal average responsibility : " + personalAverageMax);
			buffW.newLine();
			buffW.write("Minimal average responsibility : " +personalAverageMin);
			buffW.newLine();
			buffW.write("Average total responsibility : " + (sum/person2timeXconcentration.size()));
			buffW.newLine();
			buffW.write("Maximal total responsibility : " + personalTotalMax);
			buffW.newLine();
			buffW.write("Minimal total responsibility : " +personalTotalMin);
			buffW.newLine();
			buffW.write("Total exposure time : " +timeXpersons);
			buffW.newLine();
			
			buffW.close();
		} catch (IOException e) {
			System.err.println("Error creating " + outputPath + ".");
		}
			
	}

	public void printPersonalResponsibilityInformation(
			ArrayList<ResponsibilityEvent> responsibility, String outputPath) {
		
		Map <Id, ArrayList<ResponsibilityEvent>> person2ResponsibilityEvents = new HashMap<Id, ArrayList<ResponsibilityEvent>>();
		
		for(ResponsibilityEvent ree: responsibility){
			if(!person2ResponsibilityEvents.containsKey(ree.getResponsiblePersonId())){
				person2ResponsibilityEvents.put(ree.getResponsiblePersonId(), new ArrayList<ResponsibilityEvent>());
			}
			person2ResponsibilityEvents.get(ree.getResponsiblePersonId()).add(ree);
		}
		
		BufferedWriter buffW;
		try{
			buffW = new BufferedWriter(new FileWriter(outputPath));

			buffW.write("Personal Responsibility Information");
			buffW.newLine();buffW.newLine();
			
			
			for (Id personId: person2ResponsibilityEvents.keySet()) {
				buffW.write("PersonId: " + personId);
				buffW.newLine();
				
				Double personalTotal =0.0;
				for(ResponsibilityEvent ree: person2ResponsibilityEvents.get(personId)){
					personalTotal += ree.getExposureValue();
				}
				
				buffW.write("Total responsibility: " + personalTotal);
				buffW.newLine();
				
				for (ResponsibilityEvent ree : person2ResponsibilityEvents.get(personId)) {
					buffW.write(ree.getResponsibilityInformation());
					buffW.newLine();
				}
				buffW.newLine();
			}
			buffW.close();
		}catch(IOException e){
			System.err.println("Error creating " + outputPath + ".");
		}	
	}

	public void printExposureInformation(ArrayList<ResponsibilityEvent> responsibility,
			String outputPath) {
		
		// sum = #persons x time x concentration
		// average concentration = sum/time/#persons
		// personal average average 
		// personal average min, max 
		
		Double sum =0.0; // sum over person2timeXconcentration
		Double timeXpersons = 0.0; // sum over person2time
		Map<Id, Double> person2timeXconcentration = new HashMap<Id, Double>();
		Map<Id, Double> person2time = new HashMap<Id, Double>();
		Map<Id, Double> person2average = new HashMap<Id, Double>();
		
		for(ResponsibilityEvent exe: responsibility){
			Id personId = exe.getReceivingPersonId();
			if (person2timeXconcentration.containsKey(exe.getReceivingPersonId())) {
				Double newValue = person2timeXconcentration.get(personId) + exe.getExposureValue();
				person2timeXconcentration.put(personId, newValue);
				Double newTime = person2time.get(personId)+exe.getDuration();
				person2time.put(personId, newTime);
			}else{
				person2timeXconcentration.put(personId,	exe.getExposureValue());
				person2time.put(personId, exe.getDuration());
			}
		}
		
		// calculate average, sum, time sum ...
		
		for(Id personId: person2timeXconcentration.keySet()){
			sum += person2timeXconcentration.get(personId);
			timeXpersons += person2time.get(personId);
			person2average.put(personId, (person2timeXconcentration.get(personId)/person2time.get(personId)));
		}
		
		Double personalAverageAverage =0.0;
		Double personalAverageMin = Double.MAX_VALUE;
		Double personalAverageMax = -1.0;
		
		for(Id personId: person2average.keySet()){
			Double pa = person2average.get(personId);
			personalAverageAverage+= pa;
			if(pa<personalAverageMin)personalAverageMin=pa;
			if(pa>personalAverageMax)personalAverageMax=pa;
		}
		personalAverageAverage /= person2average.size();
		
		
		BufferedWriter buffW;
		try {
			buffW = new BufferedWriter(new FileWriter(outputPath));
	
			// header: Person base case compare case difference
			buffW.write("Exposure Information");
			buffW.newLine();
			
			buffW.write("Person x time x concentration : " + sum);
			buffW.newLine();
			buffW.write("Average of average exposure : " + personalAverageAverage);
			buffW.newLine();
			buffW.write("Maximal average exposure : " + personalAverageMax);
			buffW.newLine();
			buffW.write("Minimal average exposure : " +personalAverageMin);
			buffW.newLine();
			buffW.write("Total exposure time : " +timeXpersons);
			buffW.newLine();
			
			buffW.close();
		} catch (IOException e) {
			System.err.println("Error creating " + outputPath + ".");
		}
		
	}

	public void printPersonalExposureInformation(ArrayList<ResponsibilityEvent> responsibilityAndExposure,String outputPath) {
				
		Map <Id, ArrayList<ResponsibilityEvent>> person2ResponsibilityEvents = new HashMap<Id, ArrayList<ResponsibilityEvent>>();
		
		for(ResponsibilityEvent ree: responsibilityAndExposure){
			if(!person2ResponsibilityEvents.containsKey(ree.getReceivingPersonId())){
				person2ResponsibilityEvents.put(ree.getReceivingPersonId(), new ArrayList<ResponsibilityEvent>());
			}
			person2ResponsibilityEvents.get(ree.getReceivingPersonId()).add(ree);
		}
		
		BufferedWriter buffW;
		try{
			buffW = new BufferedWriter(new FileWriter(outputPath));
	
			buffW.write("Personal Exposure Information");
			buffW.newLine();buffW.newLine();
			
			
			for (Id personId: person2ResponsibilityEvents.keySet()) {
				buffW.write("PersonId: " + personId);
				buffW.newLine();
				
				Double personalTotal =0.0;
				for(ResponsibilityEvent ree: person2ResponsibilityEvents.get(personId)){
					personalTotal += ree.getExposureValue();
				}
				
				buffW.write("Total exposure: " + personalTotal);
				buffW.newLine();
				
				for (ResponsibilityEvent ree : person2ResponsibilityEvents.get(personId)) {
					buffW.write(ree.getExposureInformation());
					buffW.newLine();
				}
				buffW.newLine();
			}
			buffW.close();
		}catch(IOException e){
			System.err.println("Error creating " + outputPath + ".");
		}	 	
	}
}
