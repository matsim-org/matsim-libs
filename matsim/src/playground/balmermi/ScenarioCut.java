/* *********************************************************************** *
 * project: org.matsim.*
 * ScenarioParsing.java
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
import java.util.List;
import java.util.Set;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioLoader;
import org.matsim.core.api.facilities.ActivityOption;
import org.matsim.core.api.facilities.Facilities;
import org.matsim.core.api.facilities.Facility;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.Population;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.utils.geometry.CoordImpl;

public class ScenarioCut {

	//////////////////////////////////////////////////////////////////////
	// variables
	//////////////////////////////////////////////////////////////////////

	// ch.cut.640000.200000.740000.310000.xml
	static Coord min = new CoordImpl(640000.0,200000.0);
	static Coord max = new CoordImpl(740000.0,310000.0);
	
	private static boolean isInside(Facility f) {
		if (f == null) { return false; }
		Coord c = f.getCoord();
		if (c.getX() < min.getX()) { return false; }
		if (c.getX() > max.getX()) { return false; }
		if (c.getY() < min.getY()) { return false; }
		if (c.getY() > max.getY()) { return false; }
		return true;
	}
	//////////////////////////////////////////////////////////////////////
	// run
	//////////////////////////////////////////////////////////////////////

	public static void run() {
		
		System.out.println("read scenario data... " + (new Date()));
		ScenarioLoader loader = new ScenarioLoader(Gbl.getConfig());
		loader.loadPopulation();
		Scenario sd = loader.getScenario();
//		ScenarioImpl sd = new ScenarioImpl(Gbl.getConfig());
		Facilities facilities = sd.getFacilities();
		NetworkLayer network = (NetworkLayer) sd.getNetwork();
		Population population = sd.getPopulation();
		System.out.println("done. " + (new Date()));
		
		System.out.println("remove persons... " + (new Date()));
		Set<Id> toRemove = new HashSet<Id>();
		for (Person person : population.getPersons().values()) {
			if (person.getKnowledge() != null) {
				for (ActivityOption a : person.getKnowledge().getActivities()) {
					Facility f = a.getFacility();
					if (!isInside(f)) { toRemove.add(person.getId()); break; }
				}
			}
			List<Plan> plans = person.getPlans();
			for (Plan plan : plans) {
				for (int i=0, n=plan.getPlanElements().size(); i<n; i+=2) {
					Activity act = (Activity)plan.getPlanElements().get(i);
					Facility f = act.getFacility();
					if (!isInside(f)) { toRemove.add(person.getId()); break; }
				}
			}
		}
		System.out.println("=> "+toRemove.size()+" persons to remove.");
		for (Id id : toRemove) { population.getPersons().remove(id); }
		System.out.println("=> "+population.getPersons().size()+" persons left.");
		System.out.println("done. " + (new Date()));
		
		System.out.println("remove all routes and links... " + (new Date()));
		for (Person p : population.getPersons().values()) {
			for (Plan plan : p.getPlans()) {
				for (int i=0, n=plan.getPlanElements().size(); i<n; i++) {
					if (i%2 == 1) { ((Leg)plan.getPlanElements().get(i)).setRoute(null); }
					else { ((Activity)plan.getPlanElements().get(i)).setLink(null); }
				}
			}
		}
		System.out.println("done. " + (new Date()));

		System.out.println("remove facilities... " + (new Date()));
		toRemove.clear();
		for (Facility f : facilities.getFacilities().values()) {
			Coord c = f.getCoord();
			if (c.getX() < min.getX()) { toRemove.add(f.getId()); continue; }
			if (c.getX() > max.getX()) { toRemove.add(f.getId()); continue; }
			if (c.getY() < min.getY()) { toRemove.add(f.getId()); continue; }
			if (c.getY() > max.getY()) { toRemove.add(f.getId()); continue; }
		}
		System.out.println("=> "+toRemove.size()+" facilities to remove.");
		for (Id id : toRemove) { facilities.getFacilities().remove(id); }
		System.out.println("=> "+facilities.getFacilities().size()+" facilities left.");
		System.out.println("done. " + (new Date()));

		System.out.println("remove links... " + (new Date()));
		toRemove.clear();
		for (Link l : network.getLinks().values()) {
			Coord fc = l.getFromNode().getCoord();
			if (fc.getX() < min.getX()) { toRemove.add(l.getId()); continue; }
			if (fc.getX() > max.getX()) { toRemove.add(l.getId()); continue; }
			if (fc.getY() < min.getY()) { toRemove.add(l.getId()); continue; }
			if (fc.getY() > max.getY()) { toRemove.add(l.getId()); continue; }
			Coord tc = l.getToNode().getCoord();
			if (tc.getX() < min.getX()) { toRemove.add(l.getId()); continue; }
			if (tc.getX() > max.getX()) { toRemove.add(l.getId()); continue; }
			if (tc.getY() < min.getY()) { toRemove.add(l.getId()); continue; }
			if (tc.getY() > max.getY()) { toRemove.add(l.getId()); continue; }
		}
		System.out.println("=> "+toRemove.size()+" links to remove.");
		for (Id id : toRemove) { network.removeLink(network.getLink(id)); }
		System.out.println("=> "+network.getLinks().size()+" links left.");
		System.out.println("done. " + (new Date()));
		
		System.out.println("cleaning network... " + (new Date()));
		new NetworkCleaner().run(network);
		System.out.println("done. " + (new Date()));
		
		System.out.println("completing world... " + (new Date()));
		network.reconnect();
		Gbl.getWorld().complete();
		System.out.println("done. " + (new Date()));

		System.out.println("setting links in plan act... " + (new Date()));
		for (Person p : population.getPersons().values()) {
			for (Plan plan : p.getPlans()) {
				for (int i=0, n=plan.getPlanElements().size(); i<n; i+=2) {
					Activity act = (Activity)plan.getPlanElements().get(i);
					act.setLink(act.getFacility().getLink());
				}
			}
		}
		System.out.println("done. " + (new Date()));

		System.out.println("writing population... " + (new Date()));
		new PopulationWriter(population).write();
		System.out.println("done. " + (new Date()));

		System.out.println("writing network... " + (new Date()));
		new NetworkWriter(network).write();
		System.out.println("done. " + (new Date()));

		System.out.println("write facilities... " + (new Date()));
		new FacilitiesWriter(facilities).write();
		System.out.println("done. " + (new Date()));
		
	}

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(final String[] args) {
		Gbl.startMeasurement();

		Gbl.createConfig(args);
		Gbl.createWorld();

		run();

		Gbl.printElapsedTime();
	}
}
