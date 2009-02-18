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

import org.matsim.controler.ScenarioData;
import org.matsim.facilities.Activity;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkWriter;
import org.matsim.network.algorithms.NetworkCleaner;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.PopulationWriter;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.geometry.CoordImpl;

public class ScenarioCut {

	//////////////////////////////////////////////////////////////////////
	// variables
	//////////////////////////////////////////////////////////////////////

	// ch.cut.640000.200000.740000.310000.xml
	static Coord min = new CoordImpl(640000.0,200000.0);
	static Coord max = new CoordImpl(740000.0,310000.0);
	
	private static boolean isInside(Facility f) {
		if (f == null) { return false; }
		Coord c = f.getCenter();
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
		ScenarioData sd = new ScenarioData(Gbl.getConfig());
		Facilities facilities = sd.getFacilities();
		NetworkLayer network = sd.getNetwork();
		Population population = sd.getPopulation();
		System.out.println("done. " + (new Date()));
		
		System.out.println("remove persons... " + (new Date()));
		Set<Id> toRemove = new HashSet<Id>();
		for (Person person : population.getPersons().values()) {
			if (person.getKnowledge() != null) {
				for (Activity a : person.getKnowledge().getActivities()) {
					Facility f = a.getFacility();
					if (!isInside(f)) { toRemove.add(person.getId()); break; }
				}
			}
			List<Plan> plans = person.getPlans();
			for (Plan plan : plans) {
				for (int i=0, n=plan.getActsLegs().size(); i<n; i+=2) {
					Act act = (Act)plan.getActsLegs().get(i);
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
				for (int i=0, n=plan.getActsLegs().size(); i<n; i++) {
					if (i%2 == 1) { ((Leg)plan.getActsLegs().get(i)).setRoute(null); }
					else { ((Act)plan.getActsLegs().get(i)).setLink(null); }
				}
			}
		}
		System.out.println("done. " + (new Date()));

		System.out.println("remove facilities... " + (new Date()));
		toRemove.clear();
		for (Facility f : facilities.getFacilities().values()) {
			Coord c = f.getCenter();
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
				for (int i=0, n=plan.getActsLegs().size(); i<n; i+=2) {
					Act act = (Act)plan.getActsLegs().get(i);
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
