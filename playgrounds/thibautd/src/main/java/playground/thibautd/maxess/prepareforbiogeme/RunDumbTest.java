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
package playground.thibautd.maxess.prepareforbiogeme;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.scenario.ScenarioUtils;
import playground.thibautd.maxess.prepareforbiogeme.framework.ChoiceSet;
import playground.thibautd.maxess.prepareforbiogeme.framework.ChoiceSetSampler;
import playground.thibautd.maxess.prepareforbiogeme.framework.Converter;
import playground.thibautd.maxess.prepareforbiogeme.framework.Converter.ChoicesIdentifier;
import playground.thibautd.maxess.prepareforbiogeme.tripbased.BasicTripChoiceSetRecordFiller;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author thibautd
 */
public class RunDumbTest {
	public static void main( final String[] args ) {
		final String inputChains = args[ 0 ];
		final String outputFile = args[ 1 ];

		final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new MatsimPopulationReader( sc ).parse(inputChains);

		Converter.<Trip>builder()
				.withChoiceSetSampler(
						new ChoiceSetSampler<Trip>() {
							@Override
							public ChoiceSet<Trip> sampleChoiceSet(final Person decisionMaker, final Trip choice) {
								final Map<String, Trip> alts = new HashMap<String, Trip>();
								alts.put( "TRIP" , choice );
								return new ChoiceSet<Trip>(
										decisionMaker,
										"TRIP",
										alts );
							}
						})
				.withChoicesIdentifier(
						new ChoicesIdentifier<Trip>() {
							@Override
							public List<Trip> indentifyChoices(final Plan p) {
								return TripStructureUtils.getTrips( p , EmptyStageActivityTypes.INSTANCE );
							}
						})
				.withRecordFiller( new BasicTripChoiceSetRecordFiller( Collections.singletonList( "TRIP" ) ) )
				.create()
				.convert(
						sc.getPopulation(),
						outputFile );
	}
}
