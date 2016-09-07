/* *********************************************************************** *
 * project: org.matsim.*
 * Scorer
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.vsptelematics.ub6;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.population.routes.NetworkRoute;

import javax.inject.Inject;

public class Scorer implements IterationEndsListener {

	private static final Logger log = Logger.getLogger(Scorer.class);

	private RouteTTObserver observer;
	private Population population;
	private Config config;

	@Inject
	public Scorer(RouteTTObserver observer, Population population, Config config) {
		this.observer = observer;
		this.population = population;
		this.config = config;
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		double alpha = config.planCalcScore().getLearningRate();
		for (Person person : population.getPersons().values()){
			for(Plan plan : person.getPlans()) {
				double tt = 0;
				Leg leg = (Leg) plan.getPlanElements().get(1);
				Route route = leg.getRoute();
				for (Id id : ((NetworkRoute) route).getLinkIds()) {
					if (id.toString().equals("4")) {

						tt = observer.avr_route1TTs;
						break;
					}
					else if (id.toString().equals("5")) {
						tt = observer.avr_route2TTs;
						break;
					}
				}
				double score = --tt / 3600.0;
//				log.error("score is : " + score);
				Double oldScore = plan.getScore();
				if (oldScore == null)
					oldScore = 0.0;

				plan.setScore(alpha * -tt / 3600.0 + (1 - alpha) * oldScore);		
			}
		}
		
	}


}