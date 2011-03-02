/* *********************************************************************** *
 * project: org.matsim.*
 * DiverseRouteListener.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.yu.travelCost;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactory;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.router.util.TravelMinCost;
import org.matsim.core.utils.misc.Time;

import playground.yu.replanning.reRoute.minimizeLinkAmount.MinimizeLinkAmountDijkstraFactory;
import playground.yu.utils.NotAnIntersection;

/**
 * switch TravelCostCalculatorFactory evetually also PersonalizableTravelCost
 * before Replanning only with ReRoute to create diverse routes
 * 
 * @author yu
 * 
 */
public class MinimizeLinkAmountListener implements IterationStartsListener {
	public static class MinimizeLinkAmountTravelCostCalculatorFactoryImpl
			implements TravelCostCalculatorFactory {

		public PersonalizableTravelCost createTravelCostCalculator(
				PersonalizableTravelTime timeCalculator,
				PlanCalcScoreConfigGroup cnScoringGroup) {
			return new MinimizeLinkAmountTravelCostCalculator();
		}

	}

	public static class MinimizeLinkAmountTravelCostCalculator implements
			TravelMinCost, PersonalizableTravelCost {

		@Override
		public double getLinkGeneralizedTravelCost(final Link link,
				final double time) {
			double cost = 0d;
			Node from = link.getFromNode();
			if (!NotAnIntersection.notAnIntersection(from)) {
				// recognize a real intersection
				cost += 1d;
			}
			return cost;
		}

		@Override
		public double getLinkMinimumTravelCost(final Link link) {
			return getLinkGeneralizedTravelCost(link, Time.UNDEFINED_TIME);
		}

		@Override
		public void setPerson(Person person) {
			// This cost function doesn't change with persons.
		}

	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		Controler ctl = event.getControler();
		if (event.getIteration() > ctl.getFirstIteration()) {
			ctl
					.setTravelCostCalculatorFactory(new MinimizeLinkAmountTravelCostCalculatorFactoryImpl());
			ctl
					.setLeastCostPathCalculatorFactory(new MinimizeLinkAmountDijkstraFactory());
		}
	}

	public static void main(String[] args) {
		Controler controler = new SingleReRouteSelectedControler(args[0]);
		controler.addControlerListener(new MinimizeLinkAmountListener());
		controler.setWriteEventsInterval(1);
		controler.setOverwriteFiles(true);
		controler.run();
	}

}
