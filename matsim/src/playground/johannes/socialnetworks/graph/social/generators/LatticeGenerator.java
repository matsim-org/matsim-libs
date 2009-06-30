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
package playground.johannes.socialnetworks.graph.social.generators;

import org.matsim.api.basic.v01.population.BasicPerson;
import org.matsim.api.basic.v01.population.BasicPlan;
import org.matsim.api.basic.v01.population.BasicPopulation;
import org.matsim.core.basic.v01.BasicActivityImpl;
import org.matsim.core.basic.v01.BasicPersonImpl;
import org.matsim.core.basic.v01.BasicPlanImpl;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.geometry.CoordImpl;

/**
 * @author illenberger
 *
 */
public class LatticeGenerator {

	public BasicPopulation<BasicPerson<?>> generate(int width, int hight) {
//		SocialNetwork<BasicPerson<?>> socialnet = new SocialNetwork<BasicPerson<?>>();
		BasicPopulation population = new PopulationImpl();
		int counter = 0;
		for(int row = 1; row < hight; row+=1) {
			for(int col = 1; col < width; col+=1) {
				BasicPerson<BasicPlan<?>> person = new BasicPersonImpl<BasicPlan<?>>(new IdImpl(counter));
				counter++;
				
				BasicPlan<?> plan = new BasicPlanImpl(person);
				BasicActivityImpl act = new BasicActivityImpl("home");
				act.setCoord(new CoordImpl(col, row));
				plan.addActivity(act);
				person.getPlans().add(plan);
				
				population.getPersons().put(person.getId(), person);
//				socialnet.addEgo(person);
			}
		}
		return population;
	}
}
