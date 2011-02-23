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
import org.matsim.core.router.util.TravelTime;

/**
 * switch TravelCostCalculatorFactory evetually also PersonalizableTravelCost
 * before Replanning only with ReRoute to create diverse routes
 * 
 * @author yu
 * 
 */
public class LeastFreeSpeedTravelTimeCostListener implements
		IterationStartsListener {
	public static class LeastFreeSpeedTravelTimeTravelCostCalculatorFactoryImpl
			implements TravelCostCalculatorFactory {

		public PersonalizableTravelCost createTravelCostCalculator(
				PersonalizableTravelTime timeCalculator,
				PlanCalcScoreConfigGroup cnScoringGroup) {
			return new LeastFreeSpeedTravelTimeCostCalculator(timeCalculator);
		}

	}

	public static class LeastFreeSpeedTravelTimeCostCalculator implements
			TravelMinCost, PersonalizableTravelCost {

		// protected final TravelTime timeCalculator;

		// private final double travelCostFactor;

		// private final double marginalUtlOfDistance;

		public LeastFreeSpeedTravelTimeCostCalculator(
				final TravelTime timeCalculator
		// , PlanCalcScoreConfigGroup cnScoringGroup
		) {
			// this.timeCalculator = timeCalculator;
			/*
			 * Usually, the travel-utility should be negative (it's a
			 * disutility) but the cost should be positive. Thus negate the
			 * utility.
			 */
			// travelCostFactor = -cnScoringGroup.getTraveling_utils_hr() /
			// 3600d +
			// cnScoringGroup
			// .getPerforming_utils_hr() / 3600d ;

			// this.marginalUtlOfDistance =
			// cnScoringGroup.getMarginalUtlOfDistanceCar();

			// marginalUtlOfDistance =
			// cnScoringGroup.getMonetaryDistanceCostRateCar()
			// * cnScoringGroup.getMarginalUtilityOfMoney() * B;

		}

		@Override
		public double getLinkGeneralizedTravelCost(final Link link,
				final double time) {
			return ((LinkImpl) link).getFreespeedTravelTime();
			// double travelTime = timeCalculator.getLinkTravelTime(link, time);
			// // if (marginalUtlOfDistance == 0.0) {
			// return travelTime
			// // * travelCostFactor
			// / link.getCapacity(time);
			// }
			// return travelTime * travelCostFactor - marginalUtlOfDistance
			// * link.getLength();
		}

		@Override
		public double getLinkMinimumTravelCost(final Link link) {
			return ((LinkImpl) link).getFreespeedTravelTime();
			// if (marginalUtlOfDistance == 0.0) {
			// return link.getLength() / link.getFreespeed()
			// // * travelCostFactor
			// / link.getCapacity();
			// }
			// return link.getLength() / link.getFreespeed() * travelCostFactor
			// - marginalUtlOfDistance * link.getLength();
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
					.setTravelCostCalculatorFactory(new LeastFreeSpeedTravelTimeTravelCostCalculatorFactoryImpl());
		}
	}

	public static void main(String[] args) {
		Controler controler = new SingleReRouteSelectedControler(args[0]);
		controler
				.addControlerListener(new LeastFreeSpeedTravelTimeCostListener());
		controler.setWriteEventsInterval(1);
		controler.setOverwriteFiles(true);
		controler.run();
	}

}
