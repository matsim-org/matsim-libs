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
package playground.ivt.maxess.nestedlogitaccessibility.depalmaconstrained;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.testcases.MatsimTestUtils;
import playground.ivt.maxess.nestedlogitaccessibility.framework.Alternative;
import playground.ivt.maxess.nestedlogitaccessibility.framework.ChoiceSetIdentifier;
import playground.ivt.maxess.nestedlogitaccessibility.framework.Nest;
import playground.ivt.maxess.nestedlogitaccessibility.framework.NestedChoiceSet;
import playground.ivt.maxess.nestedlogitaccessibility.framework.NestedLogitModel;
import playground.ivt.maxess.prepareforbiogeme.tripbased.Trip;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.osgeo.proj4j.parser.Proj4Keyword.f;

/**
 * @author thibautd
 */
public class CorrectedUtilityCreatorTest {
	private static final Logger log = Logger.getLogger( CorrectedUtilityCreatorTest.class );
	private static final int CAPACITY = 15;

	// Only one nest: theory would need to be adapted for logit
	enum NestId {first}

	@Test
	public void simpleTest() {
		final Scenario scenario = loadScenario();
		runSimpleTest( scenario , p -> createChoiceSet( 1 , p , scenario ) );
	}

	@Test
	@Ignore( "needs some theoretical work to be solved" )
	public void simpleTestWithPartialChoiceSet() {
		final Scenario scenario = loadScenario();
		runSimpleTest( scenario ,
				p -> createChoiceSet( 0.8 , p , scenario ) );
	}

	private void runSimpleTest(
			final Scenario scenario,
			final ChoiceSetIdentifier<NestId> choiceSetIdentifier ) {

		final NestedLogitModel<NestId> model =
				new NestedLogitModel<>(
						// "random" utilities, identical for all agents to foster competition
						( p, a ) -> Double.parseDouble( a.getAlternative().getDestination().getId().toString() ),
						choiceSetIdentifier );

		final CorrectedUtilityCreator.CorrectedUtility<NestId> correctedUtil =
				new CorrectedUtilityCreator<NestId>( scenario , "work" ).createCorrectedUtility( model );

		final Demand<NestId> resultingDemand =
				new Demand<>(
						new NestedLogitModel<>(
								correctedUtil,
								choiceSetIdentifier ),
						scenario );

		final ConstrainedAccessibilityConfigGroup configGroup = (ConstrainedAccessibilityConfigGroup) scenario.getConfig().getModule( ConstrainedAccessibilityConfigGroup.GROUP_NAME );
		resultingDemand.getSummedDemandPerFacility().forEachValue(
				d -> {
					Assert.assertTrue(
							"got a demand exceeding capacity: "+d+" > "+(configGroup.getCapacityScalingFactor() * CAPACITY),
							d <= configGroup.getCapacityScalingFactor() * CAPACITY + MatsimTestUtils.EPSILON );
					return true;
				} );
	}

	private Map<String,NestedChoiceSet<NestId>> createChoiceSet(
			final double samplingRate,
			final Person p,
			final Scenario scenario ) {
		final List<Alternative<NestId>> firstList = new ArrayList<>(  );

		final Random r = new Random( p.getId().toString().hashCode() );

		scenario.getActivityFacilities().getFacilities().values().stream()
				.filter( f -> f.getActivityOptions().containsKey( "work" ) )
				.filter( f -> r.nextDouble() <= samplingRate )
				.forEach( f -> {
					firstList.add(
							new Alternative<>(
									NestId.first ,
									Id.create( f.getId().toString()+"-1" , Alternative.class ) ,
									new Trip( f , Collections.emptyList() , f )));
				} );

		final NestedChoiceSet<NestId> choiceSet =
				new NestedChoiceSet<>(
						new Nest<>(
								NestId.first,
								1,
								firstList ) );

		return Collections.singletonMap( "default" , choiceSet );
	}

	private Scenario loadScenario() {
		final Config config = ConfigUtils.loadConfig("../../examples/scenarios/chessboard/config.xml");
		final ConstrainedAccessibilityConfigGroup configGroup = new ConstrainedAccessibilityConfigGroup();
		config.addModule(configGroup);

		final Scenario scenario = ScenarioUtils.loadScenario( config );

		final List<ActivityFacility> facilitiesWithWork =
				scenario.getActivityFacilities().getFacilities().values().stream()
					.filter( f -> f.getActivityOptions().containsKey( "work" ) )
					.collect( Collectors.<ActivityFacility>toList() );

		facilitiesWithWork.stream()
				.forEach( f -> f.getActivityOptions().get( "work" ).setCapacity( CAPACITY ) );
		final int nFacilities = facilitiesWithWork.size();
		log.info( "test instance has "+nFacilities+" facilities to choose from" );

		// adapt scaling ratio to be tight
		configGroup.setCapacityScalingFactor( 1.1 * scenario.getPopulation().getPersons().size() / (double) (CAPACITY * nFacilities ) );

		return scenario;
	}
}
