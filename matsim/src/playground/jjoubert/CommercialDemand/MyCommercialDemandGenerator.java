/* *********************************************************************** *
 * project: org.matsim.*
 * MyCommercialDemandGenerator.java
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

package playground.jjoubert.CommercialDemand;

import org.matsim.demandmodeling.primloc.CumulativeDistribution;

public class MyCommercialDemandGenerator {
	
	private static final int populationSize = 10;
	private static final String ROOT = "/Users/johanwjoubert/MATSim/workspace/MATSimData/CommercialDemand/";

	public static void main(String[] args){
		//TODO Read major locations
		
		//TODO Read minor locations
		
		// Read cumulative distribution function (CDF) for number of activities per chain
		CumulativeDistribution numberOfActivitiesCDF = CumulativeDistribution.readDistributionFromFile(ROOT + "InputData/numberOfActivitiesCDF.txt");
		// read cumulative distribution function (CDF) for the activity duration
		CumulativeDistribution activityDurationCDF = CumulativeDistribution.readDistributionFromFile(ROOT + "InputData/activityDurationCDF.txt");
		
		
		
		for(int i = 0; i < populationSize; i++ ){
			//TODO Sample major location
			
			
			int activitiesPerChain = (int) numberOfActivitiesCDF.sampleFromCDF();
			
			for(int activity = 0; activity < activitiesPerChain; activity++){
				//TODO Sample minor point
				
				@SuppressWarnings("unused")
				double activityDuration = activityDurationCDF.sampleFromCDF();
			}
		}
		
		//TODO Write plans.xml file
	}






//	private static int sampleNumberOfActivities() {
//		int number = 0;
//		float random = RAND.nextFloat();
//		
//		int index = 0;
//		while(number == 0 & index < numberOfActivitiesCDF.size() ){
//			if( numberOfActivitiesCDF.get(index) > random){
//				number = numberOfActivities.get(index);
//			} else{
//				index++;
//			}
//		}
//		if(number == 0){
//			System.err.println("Number of activities found to be zero!");
//			System.exit(1);
//		}
//		return number;
//	}

	

//	private static void readNumberOfActivitiesCDF() {
//		try {
//			Scanner inputCDF = new Scanner(new BufferedReader(new FileReader(new File(ROOT + "InputData/numberOfActivitiesCDF.txt"))));
//			@SuppressWarnings("unused")
//			String header = inputCDF.nextLine();
//			
//			while(inputCDF.hasNextLine()){
//				String [] line = inputCDF.nextLine().split(",");
//				if(line.length == 2){
//					int number = Integer.parseInt(line[0]);
//					numberOfActivities.add(number);
//					float prob = Float.parseFloat(line[1]);
//					numberOfActivitiesCDF.add(prob);
//				} else{
//					System.err.println("In reading the cumulative distribution function for the number of activities per chain:");
//					System.err.println("   Expected two values: numberOfActivities value and an associated probability!");
//					System.exit(1);
//				}
//			}
//			
//			// Check that the distribution function is truly cumulative
//			for(int i = 1; i < numberOfActivitiesCDF.size(); i++){
//				if(numberOfActivitiesCDF.get(i) < numberOfActivitiesCDF.get(i-1)){
//					System.err.println("The ArrayList numberOfActivitiesCDF is not cumulative!");
//				}
//			}
//			
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		}
//	}
	
//	private static void readActivityDurationCDF() {
//		try {
//			Scanner inputCDF = new Scanner(new BufferedReader(new FileReader(new File(ROOT + "InputData/activityDurationCDF.txt"))));
//			@SuppressWarnings("unused")
//			String header = inputCDF.nextLine();
//			
//			while(inputCDF.hasNextLine()){
//				String [] line = inputCDF.nextLine().split(",");
//				if(line.length == 2){
//					int number = Integer.parseInt(line[0]);
//					activityDuration.add(number);
//					float prob = Float.parseFloat(line[1]);
//					activityDurationCDF.add(prob);
//				} else{
//					System.err.println("In reading the cumulative distribution function for the activity duration:");
//					System.err.println("   Expected two values: activityDuration value and an associated probability!");
//					System.exit(1);
//				}
//			}
//			
//			// Check that the distribution function is truly cumulative
//			for(int i = 1; i < activityDurationCDF.size(); i++){
//				if(activityDurationCDF.get(i) < activityDurationCDF.get(i-1)){
//					System.err.println("The ArrayList activityDurationCDF is not cumulative!");
//				}
//			}
//			
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		}
//	}


}
