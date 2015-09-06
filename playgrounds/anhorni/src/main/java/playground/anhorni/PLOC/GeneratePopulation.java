/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PersonImpl;

public class GeneratePopulation {
			
	public GeneratePopulation() {
	}
	
	public void generatePopulation(int populationSize, ExpenditureAssigner expenditureAssigner,  Population staticPopulation) {
		
		for (int i = 0; i < populationSize; i++) {
			Person p = PersonImpl.createPerson(Id.create(i, Person.class));

			// assign home town
			int townId = 0;
			if (i >= (populationSize / 2)) {
				townId = 1;
			}
			p.getCustomAttributes().put("townId", townId);
			staticPopulation.addPerson(p);
		}
		expenditureAssigner.assignExpenditures(staticPopulation);
	}
}
