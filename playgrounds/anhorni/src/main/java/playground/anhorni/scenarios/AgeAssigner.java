/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.anhorni.scenarios;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import org.apache.log4j.Logger;
import org.matsim.core.population.PersonImpl;


public class AgeAssigner {
	
	private int populationSize = 0;
	private int numberOfTowns = 0;
	private int years [][];
	private static int numberOfAgeCategories = 10; 
	private static String path = "src/main/java/playground/anhorni/scenarios/3towns/";
	private BufferedWriter bufferedWriter = null;	
	private final static Logger log = Logger.getLogger(AgeAssigner.class);
	
	
	public AgeAssigner(int populationSize, int numberOfTowns) {
		this.populationSize = populationSize;
		this.numberOfTowns = numberOfTowns;
		this.init();
	}
		
	private void init() {	
		years = new int[numberOfTowns][numberOfAgeCategories];
		int townSize =  this.populationSize / numberOfTowns;
		
		for (int i = 0; i < numberOfTowns; i++) {
			for (int j = 0; j < AgeAssigner.numberOfAgeCategories; j++) {
				years[i][j] = (int) (townSize / numberOfAgeCategories);
			}
		}		
		try {           
            bufferedWriter = new BufferedWriter(new FileWriter(path + "output/population_ages.txt"));           
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } 	
	}
	
	public void finalize() {
		try {
            if (bufferedWriter != null) {
                bufferedWriter.flush();
                bufferedWriter.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
		try {
			super.finalize();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
	
	public void assignAge(PersonImpl person) {		
		Integer townIndex = (Integer) person.getCustomAttributes().get("townId");		
		int i = -1;
		boolean assigned = false;
		while (!assigned && i < AgeAssigner.numberOfAgeCategories) {
			i++;
			if (years[townIndex][i] > 0) {
				person.setAge(i);
				assigned = true;
				years[townIndex][i]--;
			}
			
		}
		if (i == AgeAssigner.numberOfAgeCategories) {
			log.error("Error assigning the persons' age." + " Town: " + townIndex + " Person: " + person.getId());
		}
		try {                       
            bufferedWriter.append(person.getId() + "\t" + person.getAge());
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } 	
	}	
}
