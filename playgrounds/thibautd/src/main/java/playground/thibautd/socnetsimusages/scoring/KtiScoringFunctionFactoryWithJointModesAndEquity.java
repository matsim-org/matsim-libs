/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.thibautd.socnetsimusages.scoring;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.socnetsim.jointtrips.population.JointActingTypes;
import org.matsim.contrib.socnetsim.run.ScoringFunctionConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.pt.PtConstants;
import playground.ivt.matsim2030.scoring.MATSim2010ScoringFunctionFactory;
import playground.thibautd.socnetsimusages.traveltimeequity.EquityConfigGroup;
import playground.thibautd.socnetsimusages.traveltimeequity.StandardDeviationScorer;
import playground.thibautd.socnetsimusages.traveltimeequity.TravelTimesRecord;

import java.util.Set;

/**
 * @author thibautd
 */
public class KtiScoringFunctionFactoryWithJointModesAndEquity implements ScoringFunctionFactory {
	private final KtiScoringFunctionFactoryWithJointModes delegate;

	private final TravelTimesRecord travelTimesRecords;
	private final Set<String> activityType;
	private final double betaStdDev;

	@Inject
	public KtiScoringFunctionFactoryWithJointModesAndEquity(
				final Scenario scenario,
				final TravelTimesRecord travelTimesRecords,
				final Config config) {
		this( new MATSim2010ScoringFunctionFactory(
					scenario,
					new StageActivityTypesImpl(
							PtConstants.TRANSIT_ACTIVITY_TYPE,
							JointActingTypes.INTERACTION) ),
				scenario,
				travelTimesRecords,
				((ScoringFunctionConfigGroup) config.getModule( ScoringFunctionConfigGroup.GROUP_NAME) ).getJoinableActivityTypes(),
				((EquityConfigGroup) config.getModule( EquityConfigGroup.GROUP_NAME) ).getBetaStandardDev() );
	}

	public KtiScoringFunctionFactoryWithJointModesAndEquity(
				final ScoringFunctionFactory delegateFactory,
				final Scenario scenario,
				final TravelTimesRecord travelTimesRecords,
				final Set<String> activityType,
				final double betaStdDev) {
		this.delegate = new KtiScoringFunctionFactoryWithJointModes( delegateFactory , scenario );
		this.travelTimesRecords = travelTimesRecords;
		this.activityType = activityType;
		this.betaStdDev = betaStdDev;
	}

	@Override
	public ScoringFunction createNewScoringFunction(final Person person) {
		final SumScoringFunction function = delegate.createNewScoringFunction( person );

		function.addScoringFunction(
				new StandardDeviationScorer(
						travelTimesRecords,
						activityType,
						betaStdDev ) );
		return function;
	}
}
