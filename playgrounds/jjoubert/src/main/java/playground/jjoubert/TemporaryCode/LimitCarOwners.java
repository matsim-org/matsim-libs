/* *********************************************************************** *
 * project: org.matsim.*
 * LimitCarOwners.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.jjoubert.TemporaryCode;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.IOUtils;

public class LimitCarOwners {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Logger log = Logger.getLogger(LimitCarOwners.class);
		List<Id> persons = new ArrayList<Id>();
		try {
			BufferedReader people = IOUtils.getBufferedReader("C:/temp/GIS/OlderPersons.csv");
			BufferedReader cars = IOUtils.getBufferedReader("C:/temp/R/WithCars.csv");
			BufferedWriter output = IOUtils.getBufferedWriter("C:/temp/Cars.csv");
			
			try{
				output.write("Person_id,Long,Lat");
				output.newLine();
				
				log.info("Reading people with cars");
				int counter = 0;
				int multiplier = 1;
				String line = cars.readLine();
				while((line = cars.readLine()) != null){
					String[] values = line.split(",");
					int numCars = Integer.parseInt(values[5]);
					if(numCars == 1){
						persons.add(new IdImpl(values[1]));
					}
					// Report progress.
					if(++counter == multiplier){
						log.info("   processed: " + counter);
						multiplier *= 2;
					}
				}
				log.info("   processed: " + counter + " (Done)");
				log.info("Number of car owners: " + persons.size());
				
				log.info("Processing coordinates");
				counter = 0;
				multiplier = 1;
				line = people.readLine();
				while((line = people.readLine()) != null){
					String[] values = line.split(",");
					if(persons.contains(new IdImpl(values[0]))){
						output.write(values[0]);
						output.write(",");
						output.write(values[1]);
						output.write(",");
						output.write(values[2]);
						output.newLine();
					}
					
					// Report progress.
					if(++counter == multiplier){
						log.info("   processed: " + counter);
						multiplier *= 2;
					}
				}
				log.info("   processed: " + counter + " (Done)");
			} finally{
				people.close();
				cars.close();
				output.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("------------------------------");
		log.info("      Process completed");
		log.info("==============================");
	}

}

