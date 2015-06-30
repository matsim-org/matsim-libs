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
import org.matsim.core.scenario.ScenarioUtils;
import playground.thibautd.maxess.prepareforbiogeme.framework.*;
import playground.thibautd.maxess.prepareforbiogeme.tripbased.BasicTripChoiceSetRecordFiller;
import playground.thibautd.maxess.prepareforbiogeme.tripbased.Trip;

import java.util.*;

/**
 * @author thibautd
 */
public class RunDumbTest {
	public static void main( final String[] args ) {
		final String inputChains = args[ 0 ];
		final String outputFile = args[ 1 ];

		final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new MatsimPopulationReader( sc ).parse(inputChains);

		Converter.<Trip,ChoiceSituation<Trip>>builder()
				.withChoiceSetSampler(
						new ChoiceSetSampler<Trip,ChoiceSituation<Trip>>() {
							@Override
							public ChoiceSet<Trip> sampleChoiceSet(final Person decisionMaker, final ChoiceSituation<Trip> choice) {
								final Map<String, Trip> alts = new HashMap<String, Trip>();
								alts.put( "TRIP" , choice.getChoice() );
								return new ChoiceSet<Trip>(
										decisionMaker,
										"TRIP",
										alts );
							}
						})
				.withChoicesIdentifier(
						new ChoicesIdentifier<ChoiceSituation<Trip>>() {
							@Override
							public List<ChoiceSituation<Trip>> identifyChoices(final Plan p) {
								final List<ChoiceSituation<Trip>> choices = new ArrayList<>();
								for ( TripStructureUtils.Trip t : TripStructureUtils.getTrips(p, EmptyStageActivityTypes.INSTANCE) ) {
									choices.add( new ChoiceSituationImpl<Trip>( new Trip( null , null , null ) ) );
								}
								return choices;
							}
						})
				.withRecordFiller(new BasicTripChoiceSetRecordFiller(Collections.singletonList("TRIP")))
				.create()
				.convert(
						sc.getPopulation(),
						outputFile );
	}
}
