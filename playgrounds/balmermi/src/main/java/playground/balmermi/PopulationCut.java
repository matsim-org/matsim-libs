/* *********************************************************************** *
 * project: org.matsim.*
 * ScenarioCut.java
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

package playground.balmermi;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.utils.geometry.CoordImpl;

public class PopulationCut {

	//////////////////////////////////////////////////////////////////////

	private static void reduceScenario(String[] args) {
		Scenario scenario = new ScenarioLoaderImpl(args[0]).loadScenario();
		Coord min = new CoordImpl(args[1],args[2]);
		Coord max = new CoordImpl(args[3],args[4]);

		System.out.println("Removing persons... " + (new Date()));
		Set<Id> toRemove = new HashSet<Id>();
		for (Person p : scenario.getPopulation().getPersons().values()) {
			boolean removeIt = false;
			for (Plan plan : p.getPlans()) {
				ActivityImpl a = ((PlanImpl) plan).getFirstActivity();
				Coord c = a.getCoord();
				if (c == null) { removeIt = true; }
				else {
					if (c.getX() < min.getX()) { removeIt = true; }
					if (c.getX() > max.getX()) { removeIt = true; }
					if (c.getY() < min.getY()) { removeIt = true; }
					if (c.getY() > max.getY()) { removeIt = true; }
				}
			}
			if (removeIt) { toRemove.add(p.getId()); }
		}
		System.out.println("=> "+toRemove.size()+" persons to remove.");
		for (Id id : toRemove) { scenario.getPopulation().getPersons().remove(id); }
		System.out.println("=> "+scenario.getPopulation().getPersons().size()+" persons left.");
		System.out.println("done. " + (new Date()));
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork(), ((ScenarioImpl) scenario).getKnowledges()).writeFile(scenario.getConfig().plans().getOutputFile());
	}

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(final String[] args) {
//		String [] args2 = {args[0],"671100","256900","681600","263200"};
//		String [] args2 = {args[0],"661100","246900","671100","256900"};
		String [] args2 = {args[0],"651100","246900","661100","256900"};
		reduceScenario(args2);
	}
}
