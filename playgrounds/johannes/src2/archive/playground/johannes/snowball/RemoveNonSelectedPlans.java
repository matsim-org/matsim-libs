/* *********************************************************************** *
 * project: org.matsim.*
 * RemoveNonSelectedPlans.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.johannes.snowball;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.matsim.config.Config;
import org.matsim.controler.ScenarioData;
import org.matsim.gbl.Gbl;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.PopulationWriter;
import org.matsim.population.algorithms.PlansScenarioCut;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.geometry.CoordImpl;

/**
 * @author illenberger
 *
 */
public class RemoveNonSelectedPlans {

	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Config config = Gbl.createConfig(args);
		ScenarioData data = new ScenarioData(config);
		Random rnd = new Random(Gbl.getConfig().global().getRandomSeed());
		
		System.out.println("Loading persons...");
		Population plans = data.getPopulation();
		
		System.out.println("Removing non seleceted plans...");
		for(Person p : plans) {
			Plan selected = p.getSelectedPlan();
			int size = selected.getActsLegs().size();
			for(int i = 1; i < size; i++) {
				selected.getActsLegs().remove(i);
				i--;
				size--;
			}
			
			p.getPlans().clear();
			p.addPlan(selected);
		}
		
		Coord center = getMean(plans);
		double radius = 20000;
		Coord min = new CoordImpl(center.getX() - radius, center.getY() - radius);
		Coord max = new CoordImpl(center.getX() + radius, center.getY() + radius);
		new PlansScenarioCut(min, max).run(plans);
		
		System.out.println("Removing persons...");
		int size = plans.getPersons().size();
		int targetSize = 10000;
		double frac = targetSize/(double)size;
		List<Person> remove = new LinkedList<Person>();
		for(Person p : plans) {
			rnd.nextDouble();
			if(rnd.nextDouble() > frac)
				remove.add(p);
		}
		for(Person p : remove)
			plans.getPersons().remove(p.getId());
		
		System.out.println("Removed "+(size-plans.getPersons().size())+" persons. size = " + plans.getPersons().size());
		System.out.println("Writing plans...");
		
		PopulationWriter writer = new PopulationWriter(plans);
		writer.write();
	}

	private static Coord getMean(Population plans) {
		double sumX = 0;
		double sumY = 0;
		for(Person p : plans) {
			sumX += p.getSelectedPlan().getFirstActivity().getCoord().getX();
			sumY += p.getSelectedPlan().getFirstActivity().getCoord().getY();
		}
		double x = sumX/(double)plans.getPersons().size();
		double y = sumY/(double)plans.getPersons().size();
		
		return new CoordImpl(x, y);
	}
}
