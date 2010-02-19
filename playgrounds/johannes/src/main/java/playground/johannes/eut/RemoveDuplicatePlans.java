/* *********************************************************************** *
 * project: org.matsim.*
 * RemoveDuplicatePlans.java
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

/**
 *
 */
package playground.johannes.eut;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.misc.RouteUtils;

/**
 * @author illenberger
 *
 */
public class RemoveDuplicatePlans implements BeforeMobsimListener {

	private static final Logger log = Logger.getLogger(RemoveDuplicatePlans.class);

	private final Network network;

	public RemoveDuplicatePlans(Network network) {
		this.network = network;
	}

//	private Map<Person, Plan> selected;
//
//	public void notifyIterationStarts(IterationStartsEvent event) {
//		selected = new HashMap<Person, Plan>();
//		for(Person p : event.getControler().getPopulation()) {
//			selected.put(p, p.getSelectedPlan());
//		}
//
//	}

	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		int counter = 0;
		for (Person p : event.getControler().getPopulation().getPersons().values()) {
			Plan selected = p.getSelectedPlan();
			int cnt = p.getPlans().size();
			for(int i = 0; i < cnt; i++) {
				Plan plan = p.getPlans().get(i);
				if(selected != plan) {
					if(comparePlans(selected, plan)) {
						p.getPlans().remove(i);
						i--;
						cnt--;
						counter++;
					}
				}
			}


		}
		log.warn("Removed " + counter +" plans.");
	}

	private boolean comparePlans(Plan plan1, Plan plan2) {
		if (plan1.getPlanElements().size() > 1 && plan2.getPlanElements().size() > 1) {
			boolean plansDiffer = false;

			for (int i = 1; i < plan1.getPlanElements().size(); i += 2) {
				LegImpl leg2 = (LegImpl) plan2.getPlanElements().get(i);
				LegImpl leg1 = (LegImpl) plan1.getPlanElements().get(i);
				/*
				 * Compare sequence of nodes.
				 */
				if (RouteUtils.getNodes((NetworkRoute) leg2.getRoute(), this.network).equals(
						RouteUtils.getNodes((NetworkRoute) leg1.getRoute(), this.network))) {
					/*
					 * Compare departure times.
					 */
					if (leg2.getDepartureTime() != leg1.getDepartureTime()) {
						plansDiffer = true;
						break;
					}
				} else {
					plansDiffer = true;
					break;
				}
			}

			return !plansDiffer;

		} else
			return false;
	}
}
