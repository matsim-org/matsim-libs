/* *********************************************************************** *
 * project: org.matsim.*
 * SwitchNonChainBasedModeAlgorithm.java
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
package playground.ivt.replanning;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.LegImpl;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.population.algorithms.PermissibleModesCalculator;
import org.matsim.population.algorithms.PermissibleModesCalculatorImpl;
import org.matsim.population.algorithms.PlanAlgorithm;

import java.util.*;

/**
 * @author thibautd
 */
public class SwitchNonChainBasedModeAlgorithm implements PlanAlgorithm {

	private final StageActivityTypes stageActivityTypes;
	private final MainModeIdentifier mainModeIdentifier;

	private final double mutationProbability;
	private final Random rng;

	private PermissibleModesCalculator permissibleModesCalculator;

	public SwitchNonChainBasedModeAlgorithm(
			final TripRouter tripRouter,
			final String[] nonChainBasedModes,
			final double mutationProbability,
			final Random rng) {
		this( tripRouter.getStageActivityTypes(),
				tripRouter.getMainModeIdentifier(),
				new PermissibleModesCalculatorImpl( 
					nonChainBasedModes,
					false ), // do not care about car availability (irrelevant)
				mutationProbability,
				rng );
	}

	/**
	 * @param permissibleModesCalculator The permissible mode calculator
	 * should only return non chain based modes!
	 */
	public SwitchNonChainBasedModeAlgorithm(
			final StageActivityTypes stageActivityTypes,
			final MainModeIdentifier mainModeIdentifier,
			final PermissibleModesCalculator permissibleModesCalculator,
			final double mutationProbability,
			final Random rng) {
		this.stageActivityTypes = stageActivityTypes;
		this.mainModeIdentifier = mainModeIdentifier;
		this.permissibleModesCalculator = permissibleModesCalculator;
		this.mutationProbability = mutationProbability;
		
		this.rng = rng;
	}

	@Override
	public void run(final Plan plan) {
		if (plan.getPlanElements().size() <= 1) {
			return;
		}

		final Collection<String> modesColl = permissibleModesCalculator.getPermissibleModes(plan);
		final String[] modes = modesColl.toArray( new String[ modesColl.size() ] );

		final List<Trip> switchableTrips = getTripsOfModes( modes , plan );

		for ( Trip t : switchableTrips ) {
			if ( rng.nextDouble() < mutationProbability ) {
				// do not care if we change the mode again to the same...
				final String newMode = modes[ rng.nextInt( modes.length ) ];
				TripRouter.insertTrip(
						plan,
						t.getOriginActivity(),
						Collections.singletonList( new LegImpl( newMode ) ),
						t.getDestinationActivity() );
			}
		}
	}

	private List<Trip> getTripsOfModes(
			final String[] modes,
			final Plan plan) {
		final List<Trip> out = new ArrayList<Trip>();
		for ( Trip t : TripStructureUtils.getTrips( plan , stageActivityTypes ) ) {
			final String mainMode = mainModeIdentifier.identifyMainMode( t.getTripElements() );
			if ( contains( modes , mainMode ) ) out.add( t );
		}
		return out;
	}

	private boolean contains(
			final String[] modes,
			final String mainMode) {
		for ( String m : modes ) {
			if ( m.equals( mainMode ) ) return true;
		}
		return false;
	}

}

