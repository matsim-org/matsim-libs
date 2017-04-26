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

package playground.michalm.demand.taxi;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.*;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.taxi.run.TaxiModule;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.GenericRouteImpl;

public abstract class AbstractDemandGeneratorFromServedRequests {
	private final Scenario scenario;
	private final Network network;
	private final PopulationFactory pf;

	public AbstractDemandGeneratorFromServedRequests(Scenario scenario) {
		this.scenario = scenario;
		pf = scenario.getPopulation().getFactory();
		network = (Network)scenario.getNetwork();
	}

	protected Person generatePassenger(ServedRequest request, double startTime) {
		Plan plan = pf.createPlan();

		// start
		Activity startAct = createActivityFromCoord("start", request.getFrom());
		startAct.setEndTime(startTime);

		// end
		Activity endAct = createActivityFromCoord("end", request.getTo());

		// trip
		Leg leg = pf.createLeg(TaxiModule.TAXI_MODE);
		leg.setRoute(new GenericRouteImpl(startAct.getLinkId(), endAct.getLinkId()));

		plan.addActivity(startAct);
		plan.addLeg(leg);
		plan.addActivity(endAct);

		Person passenger = pf.createPerson(Id.createPersonId(request.getId()));
		passenger.addPlan(plan);
		scenario.getPopulation().addPerson(passenger);
		return passenger;
	}

	private Activity createActivityFromCoord(String actType, Coord coord) {
		Activity activity = (Activity)pf.createActivityFromCoord(actType, coord);
		final Coord coord1 = coord;
		Link link = NetworkUtils.getNearestLinkExactly(network, coord1);
		activity.setLinkId(link.getId());
		return activity;
	}

	public void write(String plansFile) {
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(plansFile);
	}
}
