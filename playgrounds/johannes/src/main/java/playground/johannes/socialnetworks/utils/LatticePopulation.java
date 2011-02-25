/* *********************************************************************** *
 * project: org.matsim.*
 * KMLWriter.java
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
package playground.johannes.socialnetworks.utils;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.geometry.CoordImpl;

/**
 * @author illenberger
 *
 */
public class LatticePopulation {

	public Population generate(int width, int hight) {
//		SocialNetwork<BasicPerson<?>> socialnet = new SocialNetwork<BasicPerson<?>>();
		Population population = new ScenarioImpl().getPopulation();
		int counter = 0;
		for(int row = 1; row < hight; row+=1) {
			for(int col = 1; col < width; col+=1) {
				Person person = new PersonImpl(new IdImpl(counter));
				counter++;
				
				Plan plan = new PlanImpl(person);
//				BasicActivityImpl act = new BasicActivityImpl("home");
//				act.setCoord(new CoordImpl(col, row));
				PopulationFactory pb = population.getFactory() ;
				Activity act = pb.createActivityFromCoord("home", new CoordImpl(col,row) ); 
				plan.addActivity(act);
				person.addPlan( plan ) ;
				
				population.addPerson(person);
//				socialnet.addEgo(person);
			}
		}
		return population;
	}
}
