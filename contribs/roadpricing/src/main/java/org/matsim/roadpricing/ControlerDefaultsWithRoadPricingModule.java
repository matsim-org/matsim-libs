/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * RoadPricingModule.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2015 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.roadpricing;

import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Provider;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.ControlerDefaults;
import org.matsim.core.controler.ControlerDefaultsModule;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;

public final class ControlerDefaultsWithRoadPricingModule extends AbstractModule {

	final RoadPricingScheme roadPricingScheme;

	public ControlerDefaultsWithRoadPricingModule() {
		this.roadPricingScheme = null;
	}

	public ControlerDefaultsWithRoadPricingModule(RoadPricingScheme roadPricingScheme) {
		this.roadPricingScheme = roadPricingScheme;
	}

	@Override
	public void install() {
		// This is not optimal yet. Modules should not need to have parameters.
		// But I am not quite sure yet how to best handle custom scenario elements. mz

		// use ControlerDefaults configuration, replacing the TravelDisutility with a toll-dependent one
		install(AbstractModule.override(Arrays.<AbstractModule>asList(new ControlerDefaultsModule()), new RoadPricingModule(roadPricingScheme)));
	}

	static class RoadPricingInitializer {
		@Inject
		RoadPricingInitializer(RoadPricingScheme roadPricingScheme, Scenario scenario) {
			RoadPricingScheme scenarioRoadPricingScheme = (RoadPricingScheme) scenario.getScenarioElement(RoadPricingScheme.ELEMENT_NAME);
			if (scenarioRoadPricingScheme == null) {
				scenario.addScenarioElement(RoadPricingScheme.ELEMENT_NAME, roadPricingScheme);
			} else {
				if (roadPricingScheme != scenarioRoadPricingScheme) {
					throw new RuntimeException();
				}
			}
		}
	}


	static class RoadPricingSchemeProvider implements Provider<RoadPricingScheme> {

		private final Config config;
		private Scenario scenario;

		@Inject
		RoadPricingSchemeProvider(Config config, Scenario scenario) {
			this.config = config;
			this.scenario = scenario;
		}

		@Override
		public RoadPricingScheme get() {
			RoadPricingScheme scenarioRoadPricingScheme = (RoadPricingScheme) scenario.getScenarioElement(RoadPricingScheme.ELEMENT_NAME);
			if (scenarioRoadPricingScheme != null) {
				return scenarioRoadPricingScheme;
			} else {
				RoadPricingConfigGroup rpConfig = ConfigUtils.addOrGetModule(config, RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class);
				
				String tollLinksFile = rpConfig.getTollLinksFile();
				if ( tollLinksFile == null ) {
					throw new RuntimeException("Road pricing inserted but neither toll links file nor RoadPricingScheme given.  "
							+ "Such an execution path is not allowed.  If you want a base case without toll, "
							+ "construct a zero toll file and insert that. ") ;
				}
				RoadPricingSchemeImpl rpsImpl = new RoadPricingSchemeImpl() ;
				new RoadPricingReaderXMLv1(rpsImpl).readFile(tollLinksFile);
				return rpsImpl;
			}
		}
	}

	static class TravelDisutilityIncludingTollFactoryProvider implements Provider<TravelDisutilityFactory> {

		private final Scenario scenario;
		private final RoadPricingScheme scheme;

		@Inject
		TravelDisutilityIncludingTollFactoryProvider(Scenario scenario, RoadPricingScheme scheme) {
			this.scenario = scenario;
			this.scheme = scheme;
		}

		@Override
		public TravelDisutilityFactory get() {
			final Config config = scenario.getConfig();
			final TravelDisutilityFactory originalTravelDisutilityFactory = ControlerDefaults.createDefaultTravelDisutilityFactory(scenario);
			//			if (!scheme.getType().equals(RoadPricingScheme.TOLL_TYPE_AREA)) {
			RoadPricingTravelDisutilityFactory travelDisutilityFactory = new RoadPricingTravelDisutilityFactory(
					originalTravelDisutilityFactory, scheme, config.planCalcScore().getMarginalUtilityOfMoney()
					);
			travelDisutilityFactory.setSigma(config.plansCalcRoute().getRoutingRandomness());
			return travelDisutilityFactory;
			//            } else {
			//                return originalTravelDisutilityFactory;
			//            }
		}

	}

}
