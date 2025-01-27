/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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
package org.matsim.contrib.av.flow;

import java.net.URL;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

/**
 * @author jbischoff
 * This is an example how to set different flow capacity consumptions for different vehicles.
 * Two groups of agents, one equipped with AVs (having an improved flow of factor 2), the other one using ordinary cars are traveling on two different routes in a grid network
 * , highlighting the difference between vehicles.
 * Network flow capacities are the same on all links.
 * All agents try to depart at the same time. The queue is emptied twice as fast for the agents using an AV.
 *
 */
public class RunAvExample {
	public void run(URL configUrl, boolean otfvis) {
		Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(configUrl, new OTFVisConfigGroup()));
		addPopulation(scenario);

		VehicleType avType = VehicleUtils.createVehicleType(Id.create("autonomousVehicleType", VehicleType.class ), TransportMode.car);
		avType.setFlowEfficiencyFactor(2.0);
		scenario.getVehicles().addVehicleType(avType);

		for (int i = 0; i < 192; i++) {
			//agents on lower route get AVs as vehicles, agents on upper route keep a standard vehicle (= default, if nothing is set)
			Id<Vehicle> vid = Id.createVehicleId("lower_" + i);
			Vehicle v = scenario.getVehicles().getFactory().createVehicle(vid, avType);
			scenario.getVehicles().addVehicle(v);
		}

		Controler controler = new Controler(scenario);
		if (otfvis) {
			controler.addOverridingModule(new OTFVisLiveModule());
		}
		controler.run();
	}

	static void addPopulation(Scenario scenario) {

		Population pop = scenario.getPopulation();
		//192 agents on upper route and lower route
		PopulationFactory f = pop.getFactory();
		for (int i = 0; i < 192; i++) {
			Person p = f.createPerson(Id.createPersonId("lower_" + i));
			Plan plan = f.createPlan();
			Activity act0 = f.createActivityFromLinkId("dummy", Id.createLinkId(122));
			act0.setEndTime(8 * 3600);
			plan.addActivity(act0);
			plan.addLeg(f.createLeg("car"));
			Activity act1 = f.createActivityFromLinkId("dummy", Id.createLinkId(131));
			plan.addActivity(act1);
			p.addPlan(plan);
			pop.addPerson(p);

		}

		for (int i = 0; i < 192; i++) {
			Person p = f.createPerson(Id.createPersonId("upper_" + i));
			Plan plan = f.createPlan();
			Activity act0 = f.createActivityFromLinkId("dummy", Id.createLinkId(143));
			act0.setEndTime(8 * 3600);
			plan.addActivity(act0);
			plan.addLeg(f.createLeg("car"));
			Activity act1 = f.createActivityFromLinkId("dummy", Id.createLinkId(152));
			plan.addActivity(act1);
			p.addPlan(plan);
			pop.addPerson(p);

		}

	}
}
