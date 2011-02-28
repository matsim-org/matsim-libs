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
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactory;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.router.util.TravelMinCost;

/**
 * switch TravelCostCalculatorFactory evetually also PersonalizableTravelCost
 * before Replanning only with ReRoute to create diverse routes
 * 
 * @author yu
 * 
 */
public class SpeedRatioWeightedTimeListener implements IterationStartsListener {
	public static class SpeedRatioWeightedTimeTravelCostCalculatorFactoryImpl
			implements TravelCostCalculatorFactory {

		public PersonalizableTravelCost createTravelCostCalculator(
				PersonalizableTravelTime timeCalculator,
				PlanCalcScoreConfigGroup cnScoringGroup) {
			return new SpeedRatioWeightedTimeTravelCostCalculator(
					timeCalculator);
		}

	}

	public static class SpeedRatioWeightedTimeTravelCostCalculator implements
			TravelMinCost, PersonalizableTravelCost {
		private PersonalizableTravelTime timeCalculator;

		public SpeedRatioWeightedTimeTravelCostCalculator(
				PersonalizableTravelTime timeCalculator) {
			this.timeCalculator = timeCalculator;
		}

		@Override
		public double getLinkGeneralizedTravelCost(final Link link,
				final double time) {
			double travelTime = timeCalculator.getLinkTravelTime(link, time);

			return travelTime
					/ (link.getLength() / travelTime / link.getFreespeed(time));
		}

		@Override
		public double getLinkMinimumTravelCost(final Link link) {
			double travelTime = ((LinkImpl) link).getFreespeedTravelTime();

			return travelTime
					/ (link.getLength() / travelTime / link.getFreespeed());
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
					.setTravelCostCalculatorFactory(new SpeedRatioWeightedTimeTravelCostCalculatorFactoryImpl());
			// ctl
			// .setLeastCostPathCalculatorFactory(new
			// MinimizeLinkAmountDijkstraFactory());
		}
	}

	public static void main(String[] args) {
		Controler controler = new SingleReRouteSelectedControler(args[0]);
		controler.addControlerListener(new SpeedRatioWeightedTimeListener());
		controler.setWriteEventsInterval(1);
		controler.setOverwriteFiles(true);
		controler.run();
	}

}
