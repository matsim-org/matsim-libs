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
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

/**
 * switch TravelCostCalculatorFactory evetually also PersonalizableTravelCost
 * before Replanning only with ReRoute to create diverse routes
 * 
 * @author yu
 * 
 */
public class SpeedSquareWeightedTimeListener implements
		IterationStartsListener {
	public static class SpeedSquareWeightedTravelCostCalculatorFactoryImpl
			implements TravelDisutilityFactory {

		@Override
		public TravelDisutility createTravelDisutility(
				TravelTime timeCalculator,
				PlanCalcScoreConfigGroup cnScoringGroup) {
			return new SpeedSquareWeightedTravelTimeCostCalculator(
					timeCalculator);
		}

	}

	public static class SpeedSquareWeightedTravelTimeCostCalculator implements TravelDisutility {

		protected final TravelTime timeCalculator;

		// private final double travelCostFactor;

		// private final double marginalUtlOfDistance;

		public SpeedSquareWeightedTravelTimeCostCalculator(
				final TravelTime timeCalculator
		// , PlanCalcScoreConfigGroup cnScoringGroup
		) {
			this.timeCalculator = timeCalculator;
			/*
			 * Usually, the travel-utility should be negative (it's a
			 * disutility) but the cost should be positive. Thus negate the
			 * utility.
			 */
			// travelCostFactor = -cnScoringGroup.getTraveling_utils_hr() /
			// 3600d +
			// cnScoringGroup
			// .getPerforming_utils_hr() / 3600d
			;

			// this.marginalUtlOfDistance =
			// cnScoringGroup.getMarginalUtlOfDistanceCar();

			// marginalUtlOfDistance =
			// cnScoringGroup.getMonetaryDistanceCostRateCar()
			// * cnScoringGroup.getMarginalUtilityOfMoney() * B;

		}

		@Override
		public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
			double travelTime = timeCalculator.getLinkTravelTime(link, time, person, vehicle);
			// if (marginalUtlOfDistance == 0.0) {
			return travelTime
					// * travelCostFactor
					/ (link.getLength() / travelTime)
					/ (link.getLength() / travelTime);
			// }
			// return travelTime * travelCostFactor - marginalUtlOfDistance
			// * link.getLength();
		}

		@Override
		public double getLinkMinimumTravelDisutility(final Link link) {
			// if (marginalUtlOfDistance == 0.0) {
			return link.getLength() / link.getFreespeed()
			// * travelCostFactor
					/ link.getFreespeed() / link.getFreespeed();
			// }
			// return link.getLength() / link.getFreespeed() * travelCostFactor
			// - marginalUtlOfDistance * link.getLength();
		}

	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		Controler ctl = event.getControler();
		if (event.getIteration() > ctl.getConfig().controler().getFirstIteration()) {
			ctl.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bindTravelDisutilityFactory().toInstance(new SpeedSquareWeightedTravelCostCalculatorFactoryImpl());
				}
			});
		}
	}

	// /**
	// * changes the value of monetaryDistanceCostRateCar from default value 0
	// to
	// * -0.00012 by the end of the first iteration
	// */
	// @Override
	// public void notifyIterationEnds(IterationEndsEvent event) {
	// Controler ctl = event.getControler();
	// int iter = event.getIteration();/* firstIter+1, +2, +3 */
	// if (iter == ctl.getFirstIteration()) {
	// PlanCalcScoreConfigGroup scoringCfg = ctl.getConfig().planCalcScore();
	// scoringCfg.setMonetaryDistanceCostRateCar(-0.00012);
	// ctl
	// .setScoringFunctionFactory(new CharyparNagelScoringFunctionFactory(
	// scoringCfg));
	// }
	// }

	// public static void main(String[] args) {
	// Controler controler = new ControlerWithRemoveOldestPlan(args);
	// controler.addControlerListener(new SingleReRouteSelectedListener());
	// controler.setWriteEventsInterval(0);
	// controler.setCreateGraphs(false);
	// controler.run();
	// }

}
