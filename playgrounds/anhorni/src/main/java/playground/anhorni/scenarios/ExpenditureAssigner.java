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

import org.matsim.core.population.PersonImpl;


public class ExpenditureAssigner {
	private int numberOfStrata = 0;	
	private double [] mu;
	private double [] sigma;
	private String path = null;
	
	private Random [] randomNumberGenerators;
	private BufferedWriter bufferedWriter = null;
	
	public ExpenditureAssigner(int numberOfStrata, double [] mu, double [] sigma, String path) {
		this.numberOfStrata = numberOfStrata;
		this.mu = mu;
		this.sigma = sigma;
		this.path = path;
		this.init();
	}
	
	
	private void init() {	
		this.randomNumberGenerators = new Random[this.numberOfStrata];				
		for (int i = 0; i < this.numberOfStrata; i++) {
			this.randomNumberGenerators[i] = new Random(109876L);
		}
		
		try {           
            bufferedWriter = new BufferedWriter(new FileWriter(path + "output/population_expenditures.txt"));           
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
	
	public void assignExpenditureFixed(PersonImpl person) {		
		Integer townId =(Integer)(person.getCustomAttributes().get("townId"));
		double expenditure = mu[townId];
		
		// age
		expenditure *= (1.0 + 2 * (person.getAge()) / 9.0);		
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
	
			
	public void assignExpenditureGaussian(PersonImpl person) {		
		//not working for writer
		//person.getCustomAttributes().put("expenditure", 
		//		mu[stratumIndex] + this.randomNumberGenerators[stratumIndex].nextGaussian() * sigma[stratumIndex]);
		// dirty hack
		Integer townId =(Integer) (person.getCustomAttributes().get("townId"));
		double expenditure = mu[townId] + this.randomNumberGenerators[townId].nextGaussian() * sigma[townId];
		
		// age
		expenditure *= (1.0 + 2 * (person.getAge()) / 9.0);			
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
