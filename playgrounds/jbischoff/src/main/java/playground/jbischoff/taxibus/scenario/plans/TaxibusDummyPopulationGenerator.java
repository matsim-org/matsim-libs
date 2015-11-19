/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.jbischoff.taxibus.scenario.plans;

import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author  jbischoff
 *
 */
public class TaxibusDummyPopulationGenerator {
public static void main(String[] args) {
	Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	new MatsimNetworkReader(scenario).readFile("../../../shared-svn/projects/vw_rufbus/scenario/input/networkptcc.xml");
	int pop = 1000;
	Random rand = MatsimRandom.getRandom();
	for (int i = 0; i<pop ; i++){
		int hx = 603906 -1500+rand.nextInt(3000);
		int hy = 5791651 -1500 + rand.nextInt(3000);
		Coord home = new Coord (hx,hy);
		int wx = 621155 -1500+rand.nextInt(3000);
		int wy = 5810010 -1500+rand.nextInt(3000);
		Coord work = new Coord (wx,wy);
		Person p = createPerson(scenario,home,work,i); 
		scenario.getPopulation().addPerson(p);
	}
	for (int i = 1000; i<pop*2 ; i++){
		int hx = 603906 -1500+rand.nextInt(3000);
		int hy = 5791651 -1500 + rand.nextInt(3000);
		int wx = 621155 -1500+rand.nextInt(3000);
		int wy = 5810010 -1500+rand.nextInt(3000);
		Coord work = new Coord (hx,hy);
		Coord home = new Coord (wx,wy);
		Person p = createPerson(scenario,home,work,i); 
		scenario.getPopulation().addPerson(p);
	}
	new PopulationWriter(scenario.getPopulation(),scenario.getNetwork()).write("../../../shared-svn/projects/vw_rufbus/scenario/input/taxibuspassengers.xml");
}

private static Person createPerson(Scenario scenario, Coord home, Coord work,int i) {
	Random r = MatsimRandom.getRandom();
	PopulationFactory f = scenario.getPopulation().getFactory();
	Person p = f.createPerson(Id.createPersonId(i));
	Plan plan = f.createPlan();
	p.addPlan(plan);
	Activity h1 =  f.createActivityFromCoord("home", home);
	h1.setEndTime(6*3600+r.nextInt(4*3600));
	Leg leg = f.createLeg("taxibus");
	Activity w = f.createActivityFromCoord("work", work);
	w.setEndTime(12*3600+r.nextInt(6*3600));
	Leg leg2 = f.createLeg("taxibus");
	Activity h2 =  f.createActivityFromCoord("home", home);
	plan.addActivity(h1);
	plan.addLeg(leg);
	plan.addActivity(w);
	plan.addLeg(leg2);
	plan.addActivity(h2);
	
	
	return p;
}
}
