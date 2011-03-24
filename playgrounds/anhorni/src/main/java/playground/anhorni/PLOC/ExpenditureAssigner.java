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

package playground.anhorni.PLOC;

import java.io.IOException;
import java.util.Random;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;


public class ExpenditureAssigner {
	private double mu [];
	private double sigma [];
	private String path = null;
	
	private Random randomNumberGenerator;
	private ObjectAttributes personAttributes;
	
	public ExpenditureAssigner(double mu [], double sigma [], String path, Random randomNumberGenerator, ObjectAttributes personAttributes) {
		this.mu = mu;
		this.sigma = sigma;
		this.path = path;
		this.personAttributes = personAttributes;
		this.randomNumberGenerator = randomNumberGenerator;	
	}
			
	public void assignExpenditures(PopulationImpl population) {
		for (Person p :population.getPersons().values()) {
			this.assignExpenditureGaussian((PersonImpl)p);
		}
		ObjectAttributesXmlWriter attributesWriter = new ObjectAttributesXmlWriter(personAttributes);
		try {
			attributesWriter.writeFile(path + "input/PLOC/3towns/personExpenditures.xml");
		} catch (IOException e1) {
			e1.printStackTrace();
		}   
	}
				
	public void assignExpenditureGaussian(PersonImpl person) {		
		int townId = (Integer) person.getCustomAttributes().get("townId");
		double expenditure = Math.sqrt(Math.pow(this.randomNumberGenerator.nextGaussian() * sigma[townId] + mu[townId], 2)) ;
		personAttributes.putAttribute(person.getId().toString(), "expenditure", expenditure); 
	}	
}
