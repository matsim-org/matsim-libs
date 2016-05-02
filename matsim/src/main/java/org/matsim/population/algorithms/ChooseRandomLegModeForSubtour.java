/* *********************************************************************** *
 * project: org.matsim.*
 * ChangeLegMode.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.population.algorithms;

import java.util.*;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.LegImpl;
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Subtour;
import org.matsim.core.router.TripStructureUtils.Trip;

/**
 * Changes the transportation mode of one random non-empty subtour in a plan to a randomly chosen
 * different mode given a list of possible modes, considering that the means of transport
 * follows the law of mass conservation.
 *
 * @author michaz
 * @see SubtourModeChoice
 */
public class ChooseRandomLegModeForSubtour implements PlanAlgorithm {

	private static Logger logger = Logger.getLogger(ChooseRandomLegModeForSubtour.class);
	
	private static class Candidate {
		final Subtour subtour;
		final String newTransportMode;

		public Candidate(
				final Subtour subtour,
				final String newTransportMode) {
			this.subtour = subtour;
			this.newTransportMode = newTransportMode;
		}
	}

	private Collection<String> modes;
	private final Collection<String> chainBasedModes;
	private Collection<String> singleTripSubtourModes;

	private final StageActivityTypes stageActivityTypes;
	private final MainModeIdentifier mainModeIdentifier;

	private final Random rng;

	private PermissibleModesCalculator permissibleModesCalculator;

	private boolean anchorAtFacilities = false;
	
	public ChooseRandomLegModeForSubtour(
			final StageActivityTypes stageActivityTypes,
			final MainModeIdentifier mainModeIdentifier,
			final PermissibleModesCalculator permissibleModesCalculator,
			final String[] modes,
			final String[] chainBasedModes,
			final Random rng) {
		this.stageActivityTypes = stageActivityTypes;
		this.mainModeIdentifier = mainModeIdentifier;
		this.permissibleModesCalculator = permissibleModesCalculator;
		this.modes = Arrays.asList(modes);
		this.chainBasedModes = Arrays.asList(chainBasedModes);
		this.singleTripSubtourModes = this.chainBasedModes;
		
		this.rng = rng;
		logger.info("Chain based modes: " + this.chainBasedModes.toString());
	}

	/**
	 * Some subtour consist of only a single trip, e.g. if a trip starts and ends on the same link or facility.
	 * By default, for those modes, the normal chain based modes are available. But in certain cases, not all
	 * the modes should be available for such trips (e.g. car-sharing does not make much sense for such a trip),
	 * thus the list of modes available for single-trip subtours can be specified independently. As mentioned,
	 * it is initialized by the constructor to the full list of chain based modes.
	 * 
	 * @param singleTripSubtourModes
	 */
	public void setSingleTripSubtourModes(final String[] singleTripSubtourModes) {
		this.singleTripSubtourModes = Arrays.asList(singleTripSubtourModes);
	}
	
	@Override
	public void run(final Plan plan) {
		if (plan.getPlanElements().size() <= 1) {
			return;
		}

		final Id<? extends BasicLocation> homeLocation = anchorAtFacilities ?
			((Activity) plan.getPlanElements().get(0)).getFacilityId() :
			((Activity) plan.getPlanElements().get(0)).getLinkId();
		Collection<String> permissibleModesForThisPlan = permissibleModesCalculator.getPermissibleModes(plan);

		List<Candidate> choiceSet =
			determineChoiceSet(
					homeLocation,
					TripStructureUtils.getTrips( plan , stageActivityTypes ),
					TripStructureUtils.getSubtours(
						plan,
						stageActivityTypes,
						anchorAtFacilities),
					permissibleModesForThisPlan);

		if (!choiceSet.isEmpty()) {
			Candidate whatToDo = choiceSet.get(rng.nextInt(choiceSet.size()));
			applyChange( whatToDo , plan );
		}
	}

	private List<Candidate> determineChoiceSet(
			final Id<? extends BasicLocation> homeLocation,
			final List<Trip> trips,
			final Collection<Subtour> subtours,
			final Collection<String> permissibleModesForThisPerson) {
		final ArrayList<Candidate> choiceSet = new ArrayList<Candidate>();
		for ( Subtour subtour : subtours ) {
			if ( !subtour.isClosed() ) {
				continue;
			}

			if ( containsUnknownMode( subtour ) ) {
				continue;
			}

			final Set<String> usableChainBasedModes = new LinkedHashSet<>();
			final Id<? extends BasicLocation> subtourStartLocation = anchorAtFacilities ?
				subtour.getTrips().get( 0 ).getOriginActivity().getFacilityId() :
				subtour.getTrips().get( 0 ).getOriginActivity().getLinkId();
			
			final Collection<String> testingModes =
				subtour.getTrips().size() == 1 ?
					singleTripSubtourModes :
					chainBasedModes;

			for (String mode : testingModes) {
				Id<? extends BasicLocation> vehicleLocation = homeLocation;
				Activity lastDestination =
					findLastDestinationOfMode(
						trips.subList(
							0,
							trips.indexOf( subtour.getTrips().get( 0 ) )),
						mode);
				if (lastDestination != null) {
					vehicleLocation = getLocationId( lastDestination );
				}
				if (vehicleLocation.equals(subtourStartLocation)) {
					usableChainBasedModes.add(mode);
				}
			}
			
			Set<String> usableModes = new LinkedHashSet<>();
			if (isMassConserving(subtour)) { // We can only replace a subtour if it doesn't itself move a vehicle from one place to another
				for (String candidate : permissibleModesForThisPerson) {
					if (chainBasedModes.contains(candidate)) {
						if (usableChainBasedModes.contains(candidate)) {
							usableModes.add(candidate);
						}
					} else {
						usableModes.add(candidate);
					}
				} 
			}

			usableModes.remove(getTransportMode(subtour));
			for (String transportMode : usableModes) {
				choiceSet.add(
						new Candidate(
							subtour,
							transportMode ));
			}
		}
		return choiceSet;
	}

	private boolean containsUnknownMode(final Subtour subtour) {
		for (Trip trip : subtour.getTrips()) {
			if (!modes.contains( mainModeIdentifier.identifyMainMode( trip.getTripElements() ))) {
				return true;
			}
		}
		return false;
	}

	private boolean isMassConserving(final Subtour subtour) {
		for (String mode : chainBasedModes) {
			if (!isMassConserving(subtour, mode)) {
				return false;
			} 
		}
		return true;
	}

	private boolean isMassConserving(
			final Subtour subtour,
			final String mode) {
		final Activity firstOrigin =
			findFirstOriginOfMode(
					subtour.getTrips(),
					mode);

		if (firstOrigin == null) {
			return true;
		}

		final Activity lastDestination =
			findLastDestinationOfMode(
					subtour.getTrips(),
					mode);

		return atSameLocation(firstOrigin, lastDestination);
	}

	private Id<? extends BasicLocation> getLocationId(Activity activity) {
		return anchorAtFacilities ?
			activity.getFacilityId() :
			activity.getLinkId();
	}
	
	private boolean atSameLocation(Activity firstLegUsingMode,
			Activity lastLegUsingMode) {
		return anchorAtFacilities ?
			firstLegUsingMode.getFacilityId().equals(
					lastLegUsingMode.getFacilityId() ) :
			firstLegUsingMode.getLinkId().equals(
					lastLegUsingMode.getLinkId() );
	}

	private Activity findLastDestinationOfMode(
			final List<Trip> tripsToSearch,
			final String mode) {
		final List<Trip> reversed = new ArrayList<>(tripsToSearch);
		Collections.reverse( reversed );
		for (Trip trip : reversed) {
			if ( mode.equals( mainModeIdentifier.identifyMainMode( trip.getTripElements() ) ) ) {
				return trip.getDestinationActivity();
			}
		}
		return null;
	}
	
	private Activity findFirstOriginOfMode(
			final List<Trip> tripsToSearch,
			final String mode) {
		for (Trip trip : tripsToSearch) {
			if ( mode.equals( mainModeIdentifier.identifyMainMode( trip.getTripElements() ) ) ) {
				return trip.getOriginActivity();
			}
		}
		return null;
	}

	private String getTransportMode(final Subtour subtour) {
		return mainModeIdentifier.identifyMainMode(
				subtour.getTrips().get( 0 ).getTripElements() );
	}

	private static void applyChange(
			final Candidate whatToDo,
			final Plan plan) {
		for (Trip trip : whatToDo.subtour.getTrips()) {
			TripRouter.insertTrip(
					plan,
					trip.getOriginActivity(),
					Collections.singletonList( new LegImpl( whatToDo.newTransportMode ) ),
					trip.getDestinationActivity());
		}
	}

	public void setAnchorSubtoursAtFacilitiesInsteadOfLinks(
			final boolean anchorAtFacilities) {
		this.anchorAtFacilities = anchorAtFacilities;
	}

}
