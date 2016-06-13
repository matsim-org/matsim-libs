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
package playground.ivt.maxess.nestedlogitaccessibility.depalmaconstrained.scripts.simplemikrozansusconstrainedaccessibility;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.ActivityFacility;
import org.matsim.utils.objectattributes.ObjectAttributes;
import playground.ivt.maxess.nestedlogitaccessibility.depalmaconstrained.SingleNest;
import playground.ivt.maxess.nestedlogitaccessibility.framework.Alternative;
import playground.ivt.maxess.nestedlogitaccessibility.framework.ChoiceSetIdentifier;
import playground.ivt.maxess.nestedlogitaccessibility.framework.Nest;
import playground.ivt.maxess.nestedlogitaccessibility.framework.NestedChoiceSet;
import playground.ivt.maxess.nestedlogitaccessibility.framework.PrismSampler;
import playground.ivt.maxess.nestedlogitaccessibility.scripts.NestedAccessibilityConfigGroup;
import playground.ivt.maxess.prepareforbiogeme.tripbased.Trip;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author thibautd
 */
public class SimpleChoiceSetIdentifier implements ChoiceSetIdentifier<SingleNest> {
	private final ObjectAttributes personAttributes;
	private final PrismSampler prismSampler;
	private final ThreadLocal<TripRouter> router;
	private final UtilityConfigGroup utilityConfigGroup;

	@Inject
	public SimpleChoiceSetIdentifier(
			final Provider<TripRouter> tripRouter,
			final Scenario scenario,
			final NestedAccessibilityConfigGroup configGroup) {
		this.router = ThreadLocal.withInitial( tripRouter::get );
		this.personAttributes = scenario.getPopulation().getPersonAttributes();
		this.prismSampler =
				new PrismSampler(
						configGroup.getActivityType(),
						configGroup.getChoiceSetSize(),
						scenario.getActivityFacilities(),
						configGroup.getDistanceBudget() );
		this.utilityConfigGroup = (UtilityConfigGroup) scenario.getConfig().getModule( UtilityConfigGroup.GROUP_NAME );
	}

	@Override
	public Map<String, NestedChoiceSet<SingleNest>> identifyChoiceSet( final Person p ) {
		final Nest<SingleNest> nest =
				new Nest.Builder<SingleNest>()
					.setMu( 1 )
					.setName( SingleNest.nest )
					.addAlternatives( calcAlternatives( p ) )
					.build();

		return Collections.singletonMap(
				"default",
				new NestedChoiceSet<>( nest ) );
	}

	private Iterable<Alternative<SingleNest>> calcAlternatives( final Person p ) {
		// for constraints to be valid, we need choice set for each person to be stable
		prismSampler.resetRandomSeed( p.getId().toString().hashCode() );

		final ActivityFacility origin = prismSampler.getOrigin( p );
		final List<ActivityFacility> prism = prismSampler.calcSampledPrism( origin );

		final String mode = utilityConfigGroup.getAlwaysUseCar() || isCarAvailable( p ) ?
				"car" : "pt";

		final Collection<Alternative<SingleNest>> alternatives = new ArrayList<>( prism.size() );
		int i = 0;
		for ( ActivityFacility destination : prism ) {
			final Alternative<SingleNest> alternative =
					new Alternative<>(
							SingleNest.nest,
							Id.create( i++ , Alternative.class ),
							new Trip(
									origin,
									router.get().calcRoute(
											mode,
											origin,
											destination,
											12 * 3600,
											p ),
									destination ) );

			// quick and dirty
			if ( mode.equals( "pt" ) ) {
				alternative.getAlternative().getTrip().stream()
						.filter( pe -> pe instanceof Leg )
						.map( pe -> (Leg) pe )
						.filter( l -> l.getMode().equals( "walk" ) )
						.forEach( l -> l.setMode( "transit_walk" ) );
			}


			assert mode.equals( router.get().getMainModeIdentifier().identifyMainMode( alternative.getAlternative().getTrip() ) ) :
					mode +" != "+router.get().getMainModeIdentifier().identifyMainMode( alternative.getAlternative().getTrip() )
					+" for "+alternative.getAlternative().getTrip();
			alternatives.add( alternative );
		}
		return alternatives;
	}

	private boolean isCarAvailable( Person person ) {
		return !PersonUtils.getCarAvail( person ).equals( "never" );
	}

}
