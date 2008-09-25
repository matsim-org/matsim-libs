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

import org.matsim.config.Config;
import org.matsim.controler.ScenarioData;
import org.matsim.gbl.Gbl;
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
		Config config = Gbl.createConfig(args);
		
		ScenarioData data = new ScenarioData(config);


		Population pop = data.getPopulation();
		
		for(Person p : pop) {
			for(int i = 1; i < p.getPlans().size(); i = 1)
				p.getPlans().remove(i);
			
			Plan selected = p.getSelectedPlan();
			for(int i = 1; i < selected.getActsLegs().size(); i = 1) {
				selected.removeLeg(i);
//				selected.removeAct(i+1);
			}
		}
		
		PopulationWriter writer = new PopulationWriter(pop, "population.xml", "v4", 100);
		writer.write();
	}

}
