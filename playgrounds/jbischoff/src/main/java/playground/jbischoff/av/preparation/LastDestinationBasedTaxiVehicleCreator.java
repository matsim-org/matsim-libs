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

package playground.jbischoff.av.preparation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.data.VehicleImpl;
import org.matsim.contrib.dvrp.data.file.VehicleWriter;
import org.matsim.contrib.util.random.WeightedRandomSelection;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author jbischoff
 *
 */
public class LastDestinationBasedTaxiVehicleCreator {

	private Scenario scenario;
	private String vehiclesFilePrefix = "C:/Users/Joschka/Documents/shared-svn/projects/audi_av/scenario/subscenarios/differentfleetposition/taxi_vehicles_";
	private WeightedRandomSelection<Id<Link>> wrs;
	public static void main(String[] args) {
		LastDestinationBasedTaxiVehicleCreator tdbtv = new LastDestinationBasedTaxiVehicleCreator();
		tdbtv.run();
	}
	void run (){
		Random rnd = MatsimRandom.getRandom();
		scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(
				"C:/Users/Joschka/Documents/runs-svn/bvg.run132.25pct/bvg.run132.25pct.output_network.xml.gz");
		new PopulationReader(scenario)
				.readFile("C:/Users/Joschka/Documents/shared-svn/projects/audi_av/scenario/plansWithCarsR0.10.xml.gz");
		
		Map<Id<Link>, Integer> endLocations = new HashMap<>();
		wrs = new WeightedRandomSelection<>();
		for (Person person : scenario.getPopulation().getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			Leg l1 = (Leg) plan.getPlanElements().get(1);
			if (l1.getMode().equals("taxi")) {
				Activity act2 = (Activity) plan.getPlanElements().get(2);
				if (act2.getStartTime() > 20 * 3600) {
					Id<Link> lid = act2.getLinkId();
					int value = 1;
					if (endLocations.containsKey(lid)){
						value += endLocations.get(lid);
					}
					endLocations.put(lid, (int) value);
				}
			}

		}
		int z = 0;
		for (Entry<Id<Link>, Integer> e : endLocations.entrySet()){
			wrs.add(e.getKey(), e.getValue());
			z+=e.getValue();
		}
		System.out.println(endLocations.size());
		System.out.println(z);
		for (int i = 5000; i<9100 ; i=i+1000 ){
			System.out.println(i);
			create(i);
		}
		
	}
	 void create(int amount) {
	    List<Vehicle> vehicles = new ArrayList<>();
		for (int i = 0 ; i< amount; i++){
		Link link = scenario.getNetwork().getLinks().get(wrs.select());
        Vehicle v = new VehicleImpl(Id.create("rt"+i, Vehicle.class), link, 5, Math.round(1), Math.round(25*3600));
        vehicles.add(v);

		}
		new VehicleWriter(vehicles).write(vehiclesFilePrefix+amount+".xml.gz");
	}
}
