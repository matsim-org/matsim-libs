	/* *********************************************************************** *
	 * project: org.matsim.*
	 * EmissionTravelCostCalculatorFactory.java
	 *                                                                         *
	 * *********************************************************************** *
	 *                                                                         *
	 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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


package playground.benjamin.scenarios.munich.exposure;

	import com.google.inject.Inject;
	import org.matsim.api.core.v01.network.Link;
	import org.matsim.api.core.v01.population.Person;
	import org.matsim.contrib.emissions.EmissionModule;
	import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
	import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
	import org.matsim.core.router.util.TravelDisutility;
	import org.matsim.core.router.util.TravelTime;
	import org.matsim.vehicles.Vehicle;
	import org.matsim.vehicles.Vehicles;
	import playground.vsp.airPollution.exposure.EmissionResponsibilityCostModule;


    /**
	 * @author benjamin
	 *
	 */
	public class EmissionResponsibilityTravelDisutilityCalculatorFactory implements TravelDisutilityFactory {

		@Inject private EmissionModule emissionModule;
		@Inject private EmissionResponsibilityCostModule emissionResponsibilityCostModule;
		@Inject private PlanCalcScoreConfigGroup cnScoringGroup;
		@Inject private Vehicles vehicles;

		@Override
		public TravelDisutility createTravelDisutility(TravelTime timeCalculator){
			final EmissionResponsibilityTravelDisutilityCalculator ertdc = new EmissionResponsibilityTravelDisutilityCalculator(timeCalculator, cnScoringGroup, emissionModule, emissionResponsibilityCostModule, vehicles );

			return new TravelDisutility(){

				@Override
				public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
					double linkTravelDisutility = ertdc.getLinkTravelDisutility(link, time, person, vehicle);
					return linkTravelDisutility;
				}
				
				@Override
				public double getLinkMinimumTravelDisutility(Link link) {
					return ertdc.getLinkMinimumTravelDisutility(link);
				}
			};
		}

}
