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
import java.util.Random;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;


public class ExpenditureAssigner {
	private double mu [];
	private double sigma [];
	private String path = null;
	
	private Random randomNumberGenerator;
	private long seed;
	private BufferedWriter bufferedWriter = null;
	
	public ExpenditureAssigner(double mu [], double sigma [], String path, long seed) {
		this.mu = mu;
		this.sigma = sigma;
		this.path = path;
		this.seed = seed;
		this.init();	
	}
		
	private void init() {	
		this.randomNumberGenerator = new Random(this.seed);		
		try {           
            bufferedWriter = new BufferedWriter(new FileWriter(path + "output/PLOC/3towns/population_expenditures.txt"));           
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } 	
	}
	
	public void assignExpenditures(PopulationImpl population) {
		for (Person p :population.getPersons().values()) {
			this.assignExpenditureGaussian((PersonImpl)p);
		}
		try {
			bufferedWriter.flush();
			bufferedWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}   
	}
				
	public void assignExpenditureGaussian(PersonImpl person) {		
		//not working for writer
		//person.getCustomAttributes().put("expenditure", 
		//		mu[stratumIndex] + this.randomNumberGenerators[stratumIndex].nextGaussian() * sigma[stratumIndex]);
		// dirty hack
		int townId = (Integer) person.getCustomAttributes().get("townId");
		
		double expenditure = Math.sqrt(Math.pow(this.randomNumberGenerator.nextGaussian() * sigma[townId] + mu[townId], 2)) ;	
		person.createDesires(String.valueOf(expenditure));
		
		try {                       
            //Start writing to the output stream
            bufferedWriter.append(person.getId() + "\t" + String.valueOf(expenditure));
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } 
	}	
}
