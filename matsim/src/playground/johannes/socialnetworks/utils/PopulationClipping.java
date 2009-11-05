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
package playground.johannes.socialnetworks.utils;

import java.util.LinkedList;
import java.util.List;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioLoaderImpl;

/**
 * @author illenberger
 *
 */
public class PopulationClipping {

	private static final String MODULE_NAME = "populationClipping";
	
	public static void main(String[] args) {
		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(args[0]);
		loader.loadScenario();
		Scenario data = loader.getScenario();
		Config config = data.getConfig();
		
		String outputDir = config.getParam(MODULE_NAME, "output");
		double x = Double.parseDouble(config.getParam(MODULE_NAME, "x"));
		double y = Double.parseDouble(config.getParam(MODULE_NAME, "y"));
		
		double r = 0;
		double width = 0;
		double height = 0;
		
		String radius = config.findParam(MODULE_NAME, "radius");
		if(radius != null) {
			r = Double.parseDouble(radius);
		} else {
			width = Double.parseDouble(config.getParam(MODULE_NAME, "width"));
			height = Double.parseDouble(config.getParam(MODULE_NAME, "height"));
		}
		
		double minX = x;
		double maxX = x + width;
		double minY = y;
		double maxY = y + height;
		
		PopulationImpl pop = (PopulationImpl) data.getPopulation();//FIXME
		List<PersonImpl> remove = new LinkedList<PersonImpl>();
		for(PersonImpl p : pop.getPersons().values()) {
			for(int i = 1; i < p.getPlans().size(); i = 1)
				p.getPlans().remove(i);
			
			PlanImpl selected = p.getSelectedPlan();
			for(int i = 1; i < selected.getPlanElements().size(); i = 1) {
				selected.getPlanElements().remove(i);
			}
			Coord c = p.getPlans().get(0).getFirstActivity().getCoord();
			
			if(r > 0) {
				double dx = Math.abs(c.getX() - x);
				double dy = Math.abs(c.getY() - y);
				if(Math.sqrt(dx*dx + dy*dy) > r)
					remove.add(p);
			} else {
				if(!((c.getX() >= minX) && (c.getX() <= maxX) && (c.getY() >= minY) && (c.getY() <= maxY)))
					remove.add(p);
			}
		}
		
		for(PersonImpl p : remove)
			pop.getPersons().remove(p.getId());
		
		PopulationWriter writer = new PopulationWriter(pop, outputDir + "plans.xml", "v4", 1);
		writer.write();
		
		for(double f = 0.001; f < 0.01; f += 0.001) {
			String file = String.format("%1$splans.%2$.3f.xml", outputDir, f);
			writer = new PopulationWriter(pop, file, "v4", f);
			writer.write();
		}
		
		for(double f = 0.01; f <= 0.1; f += 0.01) {
			String file = String.format("%1$splans.%2$.2f.xml", outputDir, f);
			writer = new PopulationWriter(pop, file, "v4", f);
			writer.write();
		}
	}

}
