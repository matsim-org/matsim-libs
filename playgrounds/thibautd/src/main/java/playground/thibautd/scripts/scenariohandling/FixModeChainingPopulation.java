/* *********************************************************************** *
 * project: org.matsim.*
 * FixModeChainingPopulation.java
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
package playground.thibautd.scripts.scenariohandling;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.StreamingDeprecated;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Subtour;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.MatsimFacilitiesReader;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author thibautd
 */
public class FixModeChainingPopulation {
	public static void main(final String[] args) {
		final String inPopulation = args[ 0 ];
		final String outPopulation = args[ 1 ];
		// necessary for V4...
		final String facilitiesFile = args.length > 2 ? args[ 2 ] : null;
		
		if ( true ) {
			throw new RuntimeException("Thibaut, I think could could do the following easily with StreamingPopulationWriter. "
					+ "If you truly need to write v4, could add a writeV4(...) into that class.  Ok?  kai, jul'16") ;
		}

		final Scenario scenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		if ( facilitiesFile != null ) new MatsimFacilitiesReader( scenario ).readFile( facilitiesFile );
		final Population pop = (Population) scenario.getPopulation();
		StreamingDeprecated.setIsStreaming(pop, true);

		final StreamingPopulationWriter writer =
			new StreamingPopulationWriter( );
//		writer.setWriterHandler( new PopulationWriterHandlerImplV4( scenario.getNetwork() ) );
		writer.writeStartPlans( outPopulation );

		final Counter correctionCounter = new Counter( "correcting plan # " );
		StreamingDeprecated.addAlgorithm(pop, new PlanModeChainCorrectingAlgorithm( correctionCounter , facilitiesFile != null ));
		StreamingDeprecated.addAlgorithm(pop, new PersonAlgorithm() {
			@Override
			public void run(final Person person) {
				writer.writePerson( person );
			}
		});

		new PopulationReader( scenario ).readFile( inPopulation );
		writer.writeEndPlans();
		correctionCounter.printCounter();
	}
}

class PlanModeChainCorrectingAlgorithm implements PlanAlgorithm, PersonAlgorithm {

	private final Counter counter = new Counter( "[PlanModeChainCorrectingAlgorithm] person # " );
	private final Counter corrCounter;
	private final boolean useFacilities;
	private static final Collection<String> CHAIN_BASED = Arrays.asList( TransportMode.car , TransportMode.bike );

	public PlanModeChainCorrectingAlgorithm(final Counter counter, final boolean useFacilities) {
		this.useFacilities = useFacilities;
		this.corrCounter = counter;
	}

	@Override
	public void run(final Person person) {
		counter.incCounter();
		for ( Plan p : person.getPlans() ) run( p );
	}

	@Override
	public void run(final Plan plan) {
		if ( isConsistent( plan ) ) return;
		corrCounter.incCounter();

		setMode( plan , containsCar( plan ) ? TransportMode.car : TransportMode.pt );
	}

	private static void setMode(final Plan plan, final String mode) {
		for ( PlanElement pe : plan.getPlanElements() ) {
			if ( pe instanceof Leg ) ((Leg) pe).setMode( mode );
		}
	}

	private static boolean containsCar(final Plan plan) {
		for ( PlanElement pe : plan.getPlanElements() ) {
			if ( pe instanceof Leg && ((Leg) pe).getMode().equals( TransportMode.car ) ) {
				return true;
			}
		}
		return false;
	}

	private boolean isConsistent(final Plan plan) {
		// assume no complex trips
		for ( Subtour subtour : TripStructureUtils.getSubtours( plan , EmptyStageActivityTypes.INSTANCE , useFacilities ) ) {
			final String mode = getModeOrNullIfSeveral( subtour );
			if ( mode == null ) return false;
			if ( CHAIN_BASED.contains( mode ) && !sameModeInParents( subtour ) ) return false;
		}
		return true;
	}

	private static boolean sameModeInParents(final Subtour subtour) {
		final Subtour father = subtour.getParent();
		if ( father == null ) return true;

		final String mode = getModeOrNullIfSeveral( subtour );
		final String fatherMode = getModeOrNullIfSeveral( father );
		return mode.equals( fatherMode ) && sameModeInParents( father );
	}

	private static String getModeOrNullIfSeveral(final Subtour subtour) {
		String mode = null;
		for ( Trip t : subtour.getTripsWithoutSubSubtours() ) {
			final String currentMode = t.getLegsOnly().get( 0 ).getMode();
			if ( mode != null && ! mode.equals( currentMode ) ) return null;
			mode = currentMode;
		}
		return mode;
	}
}
