/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.anhorni.choiceSetGeneration.test;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.contrib.locationchoice.router.PlanRouterAdapter;

public class ExtractChoiceSetsRoutingTest implements AfterMobsimListener {

	private final static Logger log = Logger.getLogger(ExtractChoiceSetsRoutingTest.class);

//	private Controler controler = null;
	public ExtractChoiceSetsRoutingTest(Controler controler) {

//		this.controler = controler;

	}
	@Override
	public void notifyAfterMobsim(final AfterMobsimEvent event) {
		if (event.getIteration() < event.getControler().getConfig().controler().getLastIteration()) {
			return;
		}
		computeChoiceSet(event.getControler());
	}

	protected void computeChoiceSet(Controler controler) {

        NetworkImpl network = (NetworkImpl) controler.getScenario().getNetwork();

		Link link0 = NetworkUtils.getNearestLink(network, new Coord(681753.6875, 251900.64844999998));
		ActivityImpl fromAct = new ActivityImpl("home", link0.getId());

		Link link1 = NetworkUtils.getNearestLink(network, new Coord(695278.8125, 257607.125));
		ActivityImpl toAct = new ActivityImpl("shop", link1.getId());
		fromAct.setEndTime(0.0);

		LegImpl leg = computeLeg(fromAct, toAct, controler);
		log.info(leg.getTravelTime());
	}


	private LegImpl computeLeg(ActivityImpl fromAct, ActivityImpl toAct, Controler controler) {
		Person person = PersonImpl.createPerson(Id.create("1", Person.class));
		LegImpl leg = new org.matsim.core.population.LegImpl(TransportMode.car);
		PlanRouterAdapter router = new PlanRouterAdapter( controler );
		router.handleLeg(person, leg, fromAct, toAct, fromAct.getEndTime());
		return leg;
	}
}
