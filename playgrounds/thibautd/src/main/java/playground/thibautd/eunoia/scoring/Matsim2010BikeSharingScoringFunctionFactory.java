/* *********************************************************************** *
 * project: org.matsim.*
 * Matsim2010BikeSharingScoring.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.thibautd.eunoia.scoring;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.pt.PtConstants;

import eu.eunoiaproject.bikesharing.framework.BikeSharingConstants;
import eu.eunoiaproject.bikesharing.framework.router.TransitMultiModalAccessRoutingModule;
import eu.eunoiaproject.bikesharing.framework.scenario.BikeSharingFacilities;
import eu.eunoiaproject.bikesharing.scoring.StepBasedFare;
import eu.eunoiaproject.bikesharing.scoring.StepBasedFareConfigGroup;
import eu.eunoiaproject.elevation.ElevationProvider;
import eu.eunoiaproject.elevation.ObjectAttributesBasedElevationProvider;
import eu.eunoiaproject.elevation.scoring.SimpleElevationScorer;
import eu.eunoiaproject.elevation.scoring.SimpleElevationScorerParameters;

import playground.ivt.matsim2030.scoring.MATSim2010ScoringFunctionFactory;

/**
 * @author thibautd
 */
public class Matsim2010BikeSharingScoringFunctionFactory implements ScoringFunctionFactory {
	private final MATSim2010ScoringFunctionFactory delegate;
	private final ElevationProvider<Id> elevationProvider;
	private final Config config;
	private final SimpleElevationScorerParameters params;

	public Matsim2010BikeSharingScoringFunctionFactory(
			final Scenario scenario,
			final SimpleElevationScorerParameters params) {
		this.delegate =
			new MATSim2010ScoringFunctionFactory(
					scenario,
					new StageActivityTypesImpl(
						TransitMultiModalAccessRoutingModule.DEPARTURE_ACTIVITY_TYPE,
						PtConstants.TRANSIT_ACTIVITY_TYPE,
						BikeSharingConstants.INTERACTION_TYPE ) ); 	
		this.config = scenario.getConfig();
		this.elevationProvider =
			new ObjectAttributesBasedElevationProvider(
					scenario.getActivityFacilities().getFacilityAttributes(),
					((BikeSharingFacilities)
					 scenario.getScenarioElement(
						 BikeSharingFacilities.ELEMENT_NAME )).getFacilitiesAttributes() );
		this.params = params;
	}

	@Override
	public ScoringFunction createNewScoringFunction(final Person person) {
		final SumScoringFunction function = (SumScoringFunction) delegate.createNewScoringFunction(person);
		function.addScoringFunction(
				new StepBasedFare(
					config.planCalcScore(),
					(StepBasedFareConfigGroup)
						config.getModule(
							StepBasedFareConfigGroup.GROUP_NAME ) ) );
		function.addScoringFunction(
				new SimpleElevationScorer(
					params,
					elevationProvider ) );
		return function;
	}
}

