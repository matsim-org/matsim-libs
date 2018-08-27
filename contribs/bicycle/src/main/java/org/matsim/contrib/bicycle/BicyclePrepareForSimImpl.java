/* *********************************************************************** *
 * project: org.matsim.*												   *
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
package org.matsim.contrib.bicycle;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.controler.PrepareForSim;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.population.algorithms.XY2Links;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.Lockable;
import org.matsim.facilities.ActivityFacilities;

class BicyclePrepareForSimImpl implements PrepareForSim {

	private final GlobalConfigGroup globalConfigGroup;
	private final Scenario scenario;
	private final Network network;
	private final Population population;
	private final ActivityFacilities activityFacilities;
	private final Provider<TripRouter> tripRouterProvider;

	@Inject
	BicyclePrepareForSimImpl(GlobalConfigGroup globalConfigGroup, Scenario scenario, Network network, Population population, ActivityFacilities activityFacilities, Provider<TripRouter> tripRouterProvider) {
		this.globalConfigGroup = globalConfigGroup;
		this.scenario = scenario;
		this.network = network;
		this.population = population;
		this.activityFacilities = activityFacilities;
		this.tripRouterProvider = tripRouterProvider;
	}

	@Override
	public void run() {
		if (scenario instanceof Lockable) {
			((Lockable)scenario).setLocked();
			// see comment in ScenarioImpl. kai, sep'14
		}

		{
			Network carNetwork = NetworkUtils.createNetwork();
			Network bicyleNetwork = NetworkUtils.createNetwork();
			
			Set<String> car = new HashSet<>();
			car.add("car");
			
			Set<String> bicycle = new HashSet<>();
			bicycle.add("bicycle");
			
			new TransportModeNetworkFilter(network).filter(carNetwork, car);
			new TransportModeNetworkFilter(network).filter(bicyleNetwork, bicycle);
	
			XY2Links xy2LinksCar = new XY2Links(carNetwork, null);
			XY2Links xy2LinksBicycle = new XY2Links(bicyleNetwork, null);

			for (Person person : population.getPersons().values()) {
				// TODO check if bike user or car user. DONE?!
//				String bla = person.getSelectedPlan().getPlanElements().get(1).toString();
				Leg firstLeg = (Leg) person.getSelectedPlan().getPlanElements().get(1);
				String mode = firstLeg.getMode();
				// VERY important: This works *only* if *all* persons have only trips which are all with
				// the same mode (for the whole day)
//				System.out.println(bla);

				if (mode.equals("car")){
					xy2LinksCar.run(person);
				} else {
					xy2LinksBicycle.run(person);
				}
			}
		}
		
		PlanRouter planRouter = new PlanRouter(tripRouterProvider.get());

		for (Person person : population.getPersons().values()) {
			planRouter.run(person);
		}
	
		if (population instanceof Lockable) {
			((Lockable) population).setLocked();
		}
	}
}