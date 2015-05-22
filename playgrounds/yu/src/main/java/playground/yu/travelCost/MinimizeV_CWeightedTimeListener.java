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

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.network.LinkImpl;
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
public class MinimizeV_CWeightedTimeListener implements IterationStartsListener {
	public static class MinimizeV_CWeightedTimeTravelCostCalculatorFactoryImpl
			implements TravelDisutilityFactory {
		private final VolumesAnalyzer volumes;
		private final double flowCapFactor, capPeriod;

		public MinimizeV_CWeightedTimeTravelCostCalculatorFactoryImpl(
				final VolumesAnalyzer volumes, final double flowCapFactor,
				final double capPeriod) {
			this.volumes = volumes;
			this.flowCapFactor = flowCapFactor;
			this.capPeriod = capPeriod;
		}

		@Override
		public TravelDisutility createTravelDisutility(
				TravelTime timeCalculator,
				PlanCalcScoreConfigGroup cnScoringGroup) {
			return new MinimizeV_CWeightedTimeTravelCostCalculator(
					timeCalculator, volumes, flowCapFactor, capPeriod);
		}

	}

	public static class MinimizeV_CWeightedTimeTravelCostCalculator implements
			TravelDisutility {

		protected final TravelTime timeCalculator;
		private final VolumesAnalyzer volumes;
		private final double flowCapFactor, capPeriod;

		// private final double travelCostFactor;

		// private final double marginalUtlOfDistance;

		public MinimizeV_CWeightedTimeTravelCostCalculator(
				final TravelTime timeCalculator, final VolumesAnalyzer volumes,
				final double flowCapFactor, final double capPeriod) {
			this.timeCalculator = timeCalculator;
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
			this.volumes = volumes;
			this.flowCapFactor = flowCapFactor;
			this.capPeriod = capPeriod;
		}

		@Override
		public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
			int[] vols = volumes.getVolumesForLink(link.getId());
			double vol = vols != null ? vols[(int) time / 3600] : 0d;
			return timeCalculator.getLinkTravelTime(link, time, person, vehicle)
					* (1 + vol / flowCapFactor
							/ (link.getCapacity(time) / (capPeriod / 3600d)));
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
		public double getLinkMinimumTravelDisutility(final Link link) {
			return ((LinkImpl) link).getFreespeedTravelTime();
			// if (marginalUtlOfDistance == 0.0) {
			// return link.getLength() / link.getFreespeed()
			// // * travelCostFactor
			// / link.getCapacity();
			// }
			// return link.getLength() / link.getFreespeed() * travelCostFactor
			// - marginalUtlOfDistance * link.getLength();
		}

	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		final Controler ctl = event.getControler();
		if (event.getIteration() > ctl.getConfig().controler().getFirstIteration()) {
			ctl.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bindTravelDisutilityFactory().toInstance(new MinimizeV_CWeightedTimeTravelCostCalculatorFactoryImpl(
							ctl.getVolumes(), ctl.getConfig()
							.qsim().getFlowCapFactor(),
							ctl.getScenario().getNetwork().getCapacityPeriod()));
				}
			});
		}
	}

	public static void main(String[] args) {
		Controler controler = new SingleReRouteSelectedControler(args[0]);
		controler.addControlerListener(new MinimizeV_CWeightedTimeListener());
		controler.getConfig().controler().setWriteEventsInterval(1);
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		controler.run();
	}

}
