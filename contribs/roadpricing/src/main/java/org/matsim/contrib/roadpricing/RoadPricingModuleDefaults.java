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

package org.matsim.contrib.roadpricing;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.ControlerDefaults;
import org.matsim.core.controler.ControlerDefaultsModule;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.net.URL;
import java.util.Collections;

final class RoadPricingModuleDefaults extends AbstractModule {

	private final RoadPricingScheme roadPricingScheme;

	RoadPricingModuleDefaults(RoadPricingScheme roadPricingScheme) {
		this.roadPricingScheme = roadPricingScheme;
	}

	@Override
	public void install() {
		// This is not optimal yet. Modules should not need to have parameters.
		// But I am not quite sure yet how to best handle custom scenario elements. mz

		// use ControlerDefaults configuration, replacing the TravelDisutility with a toll-dependent one
//		install(AbstractModule.override(Collections.<AbstractModule>singletonList(new ControlerDefaultsModule()), new RoadPricingModule(roadPricingScheme)));
		throw new RuntimeException("we just broke this") ;
	}


//	/**
//	 * This class binding ensure that there is a SINGLE, consistent RoadPricingScheme in the Scenario.
//	 */
//	static class RoadPricingInitializer {
//		@Inject
//		RoadPricingInitializer(RoadPricingScheme roadPricingScheme, Scenario scenario) {
//			RoadPricingScheme scenarioRoadPricingScheme = (RoadPricingScheme) scenario.getScenarioElement(RoadPricingScheme.ELEMENT_NAME);
//			if (scenarioRoadPricingScheme == null) {
//				scenario.addScenarioElement(RoadPricingScheme.ELEMENT_NAME, roadPricingScheme);
//			} else {
//				if (roadPricingScheme != scenarioRoadPricingScheme) {
//					throw new RuntimeException("Trying to bind multiple, different RoadPricingSchemes (must be singleton)");
//				}
//			}
//		}
//	}


	/**
	 * Provides the {@link RoadPricingScheme} from either the given instance, or
	 * read from file using the file specified in the {@link RoadPricingConfigGroup}.
	 */
	static class RoadPricingSchemeProvider implements Provider<RoadPricingScheme> {

		private final Config config;
		private Scenario scenario;

		@Inject
		RoadPricingSchemeProvider(Config config, Scenario scenario) {
			/* TODO Check if we can get the Config from the Scenario */
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

				if (rpConfig.getTollLinksFile() == null) {
					throw new RuntimeException("Road pricing inserted but neither toll links file nor RoadPricingScheme given.  "
							+ "Such an execution path is not allowed.  If you want a base case without toll, "
							+ "construct a zero toll file and insert that. ");
				}
				URL tollLinksFile = ConfigGroup.getInputFileURL(this.config.getContext(), rpConfig.getTollLinksFile());
				RoadPricingSchemeImpl rpsImpl = RoadPricingUtils.createAndRegisterMutableScheme(scenario );
				new RoadPricingReaderXMLv1(rpsImpl).parse(tollLinksFile);
				return rpsImpl;
			}
		}
	}


	static class TravelDisutilityIncludingTollFactoryProvider implements Provider<TravelDisutilityFactory> {

		private final Scenario scenario;
		private final RoadPricingScheme scheme;

		@Inject
		TravelDisutilityIncludingTollFactoryProvider(Scenario scenario) {
			this.scenario = scenario;
			this.scheme = RoadPricingUtils.getScheme( scenario ) ;
		}

		@Override
		public TravelDisutilityFactory get() {
			final Config config = scenario.getConfig();
			final TravelDisutilityFactory originalTravelDisutilityFactory = ControlerDefaults.createDefaultTravelDisutilityFactory(scenario);
			RoadPricingTravelDisutilityFactory travelDisutilityFactory = new RoadPricingTravelDisutilityFactory(
					originalTravelDisutilityFactory, scheme, config.planCalcScore().getMarginalUtilityOfMoney()
			);
			travelDisutilityFactory.setSigma(config.plansCalcRoute().getRoutingRandomness());
			return travelDisutilityFactory;
		}

	}

}
