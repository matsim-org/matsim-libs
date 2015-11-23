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

import eu.eunoiaproject.bikesharing.framework.BikeSharingConstants;
import eu.eunoiaproject.bikesharing.framework.router.TransitMultiModalAccessRoutingModule;
import eu.eunoiaproject.bikesharing.scoring.StepBasedFare;
import eu.eunoiaproject.bikesharing.scoring.StepBasedFareConfigGroup;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.pt.PtConstants;
import playground.ivt.matsim2030.scoring.MATSim2010ScoringFunctionFactory;
import playground.thibautd.router.multimodal.DenivelationAlongRouteScoring;
import playground.thibautd.router.multimodal.LinkSlopeScorer;

/**
 * @author thibautd
 */
public class Matsim2010BikeSharingScoringFunctionFactory implements ScoringFunctionFactory {
	private final MATSim2010ScoringFunctionFactory delegate;
	private final Scenario scenario;
	private final LinkSlopeScorer scorer;

	public Matsim2010BikeSharingScoringFunctionFactory(
			final Scenario scenario,
			final LinkSlopeScorer scorer ) {
		this.delegate =
			new MATSim2010ScoringFunctionFactory(
					scenario,
					new StageActivityTypesImpl(
						TransitMultiModalAccessRoutingModule.DEPARTURE_ACTIVITY_TYPE,
						PtConstants.TRANSIT_ACTIVITY_TYPE,
						BikeSharingConstants.INTERACTION_TYPE ) ); 	
		this.scenario = scenario;
		this.scorer = scorer;
	}

	@Override
	public ScoringFunction createNewScoringFunction(final Person person) {
		final SumScoringFunction function = (SumScoringFunction) delegate.createNewScoringFunction(person);
		function.addScoringFunction(
				new StepBasedFare(
					scenario.getConfig().planCalcScore(),
					(StepBasedFareConfigGroup)
						scenario.getConfig().getModule(
							StepBasedFareConfigGroup.GROUP_NAME ) ) );
		function.addScoringFunction(
				createDenivelationScoring(
					person.getSelectedPlan(),
					TransportMode.bike ) );
		function.addScoringFunction(
				createDenivelationScoring(
					person.getSelectedPlan(),
					BikeSharingConstants.MODE ) );
		return function;
	}

	private DenivelationAlongRouteScoring createDenivelationScoring(
			final Plan plan,
			final String mode) {
		return new DenivelationAlongRouteScoring(
				scorer,
				plan,
				mode);
	}
}

