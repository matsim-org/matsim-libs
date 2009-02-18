/* *********************************************************************** *
 * project: org.matsim.*
 * SimplifyPersons.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.johannes.socialnets;

import java.util.LinkedList;
import java.util.List;

import org.matsim.config.Config;
import org.matsim.controler.ScenarioData;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.PopulationWriter;

/**
 * @author illenberger
 *
 */
public class SimplifyPersons {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Config config = Gbl.createConfig(new String[]{args[0]});
		
		ScenarioData data = new ScenarioData(config);

		double centerX = 683000;
		double centerY = 247000;
		double radius = Double.parseDouble(args[2]);
		
		double halfradius = (radius/2.0);
		double minX = centerX - halfradius;
		double maxX = centerX + halfradius;
		double minY = centerY - halfradius;
		double maxY = centerY + halfradius;
		
		Population pop = data.getPopulation();
		List<Person> remove = new LinkedList<Person>();
		for(Person p : pop) {
			for(int i = 1; i < p.getPlans().size(); i = 1)
				p.getPlans().remove(i);
			
			Plan selected = p.getSelectedPlan();
			for(int i = 1; i < selected.getActsLegs().size(); i = 1) {
				selected.getActsLegs().remove(i);
			}
			Coord c = p.getPlans().get(0).getFirstActivity().getCoord();
			if(!(c.getX() >= minX && c.getX() <= maxX && c.getY() >= minY && c.getY() <= maxY))
				remove.add(p);
		}
		
		for(Person p : remove)
			pop.getPersons().remove(p.getId());
		
		PopulationWriter writer = new PopulationWriter(pop, args[1], "v4", 100);
		writer.write();
	}

}
