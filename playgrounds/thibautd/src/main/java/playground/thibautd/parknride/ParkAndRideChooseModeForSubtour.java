/* *********************************************************************** *
 * project: org.matsim.*
 * ParkAndRideChooseModeForSubtour.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.parknride;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.groups.PlanomatConfigGroup.TripStructureAnalysisLayerOption;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.population.algorithms.PermissibleModesCalculator;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.population.algorithms.PlanAnalyzeSubtours;

import playground.thibautd.router.ActivityWrapperFacility;
import playground.thibautd.router.TripRouter;

/**
 * A special module meant at handling subtour mode choice for trips,
 * with a special handling of park and ride.
 *
 * It is based on the code of the ChooseRandomLegModeForSubtour class
 * @author thibautd
 */
public class ParkAndRideChooseModeForSubtour implements PlanAlgorithm {
	private static final Logger log =
		Logger.getLogger(ParkAndRideChooseModeForSubtour.class);
	// enable during developement to get more checks
	private static final boolean SELF_CHECKING = true;

	private static class Candidate {
		Integer subTourIndex;
		String newTransportMode;
	}

	private final Collection<String> modes;
	private final Collection<String> chainBasedModes;
	private final Random rng;
	private final PlanAnalyzeSubtours planAnalyzeSubtours;
	private final TripRouter tripRouter;
	private final ParkAndRideIncluder includer;

	private PermissibleModesCalculator permissibleModesCalculator;
	private TripStructureAnalysisLayerOption tripStructureAnalysisLayer = TripStructureAnalysisLayerOption.link;
	
	/**
	 * @param includer
	 * @param tripRouter
	 * @param permissibleModesCalculator
	 * @param modes all modes, including park and ride and chain based modes
	 * @param chainBasedModes chain based modes. Park and ride is not considered chain based.
	 * @param rng
	 */
	public ParkAndRideChooseModeForSubtour(
			final ParkAndRideIncluder includer,
			final TripRouter tripRouter,
			final PermissibleModesCalculator permissibleModesCalculator,
			final String[] modes,
			final String[] chainBasedModes,
			final Random rng) {
		this.includer = includer;
		this.tripRouter = tripRouter;
		this.permissibleModesCalculator = permissibleModesCalculator;
		this.modes = Arrays.asList(modes);
		this.chainBasedModes = Arrays.asList(chainBasedModes);
		this.rng = rng;
		this.planAnalyzeSubtours = new PlanAnalyzeSubtours();
		log.info("Chain based modes: " + this.chainBasedModes.toString());
	}

	@Override
	public void run(final Plan plan) {
		List<PlanElement> planStructure = includer.extractPlanStructure( plan );
		checkSequence( planStructure );
		Id homeLocation = null;

		if (plan.getPlanElements().size() > 1) {
			if (tripStructureAnalysisLayer == TripStructureAnalysisLayerOption.link) {
				homeLocation = ((Activity) plan.getPlanElements().get(0)).getLinkId();
			}
			else if (tripStructureAnalysisLayer == TripStructureAnalysisLayerOption.facility) {
				homeLocation = ((Activity) plan.getPlanElements().get(0)).getFacilityId();
			}
			Collection<String> permissibleModesForThisPlan = permissibleModesCalculator.getPermissibleModes(plan);

			planAnalyzeSubtours.setTripStructureAnalysisLayer( tripStructureAnalysisLayer );
			planAnalyzeSubtours.run( wrapPlanStructure( planStructure ) );

			List<Candidate> choiceSet =
				determineChoiceSet(
						homeLocation,
						planStructure,
						permissibleModesForThisPlan );

			if (!choiceSet.isEmpty()) {
				Candidate whatToDo = choiceSet.get(rng.nextInt(choiceSet.size()));
				List<PlanElement> subTour = planAnalyzeSubtours.getSubtours().get(whatToDo.subTourIndex);
				changeLegModeTo(
						plan,
						subTour,
						whatToDo.newTransportMode);
			}
		}

		checkSequence( plan.getPlanElements() );
	}

	private static Plan wrapPlanStructure(final List<PlanElement> planStructure) {
		Plan plan = new PlanImpl();
		plan.getPlanElements().addAll( planStructure );
		return plan;
	}

	private List<Candidate> determineChoiceSet(
			final Id homeLocation,
			final List<PlanElement> planStructure,
			final Collection<String> permissibleModesForThisPerson) {
		ArrayList<Candidate> choiceSet = new ArrayList<Candidate>();
		for (Integer subTourIndex : planAnalyzeSubtours.getSubtourIndexation()) {
			if (subTourIndex < 0) {
				continue;
			}
			List<PlanElement> subTour = planAnalyzeSubtours.getSubtours().get(subTourIndex);
			if (containsUnknownMode(subTour)) {
				continue;
			}
			Set<String> usableChainBasedModes = new HashSet<String>();
			Id subtourStartLocation;
			if (tripStructureAnalysisLayer == TripStructureAnalysisLayerOption.link) {
				subtourStartLocation = ((Activity) subTour.get(0)).getLinkId();
			}
			else if (tripStructureAnalysisLayer == TripStructureAnalysisLayerOption.facility) {
				subtourStartLocation = ((Activity) subTour.get(0)).getFacilityId();
			}
			else {
				throw new RuntimeException( "unknow trip structure analysis layer value "+tripStructureAnalysisLayer);
			}
			
			for (String mode : chainBasedModes) {
				Id vehicleLocation = homeLocation;
				Activity lastAct =
					findLastLegUsing(
							planStructure.subList(
								0,
								planStructure.indexOf(subTour.get(0)) + 1),
							mode);
				if (lastAct != null) {
					vehicleLocation = getLocationId(lastAct);
				}
				if (vehicleLocation.equals(subtourStartLocation)) {
					usableChainBasedModes.add(mode);
				}
			}
			
			Set<String> usableModes = new HashSet<String>();
			if (isMassConserving( subTour )) { // We can only replace a subtour if it doesn't itself move a vehicle from one place to another
				for (String candidate : permissibleModesForThisPerson) {
					if (chainBasedModes.contains( candidate )) {
						if (usableChainBasedModes.contains( candidate )) {
							usableModes.add( candidate );
						}
					}
					else if (candidate.equals( ParkAndRideConstants.PARK_N_RIDE_LINK_MODE )) {
						if (usableChainBasedModes.contains( TransportMode.car ) && subTour.size() >= 5) {
							usableModes.add( candidate );
						}
					}
					else {
						usableModes.add( candidate );
					}
				} 
			}
			usableModes.remove( getTransportMode( subTour ) );
			for (String transportMode : usableModes) {
				Candidate candidate = new Candidate();
				candidate.subTourIndex = subTourIndex;
				candidate.newTransportMode = transportMode;
				choiceSet.add(candidate);
			}
		}
		return choiceSet;
	}

	private boolean containsUnknownMode(final List<PlanElement> subTour) {
		for (PlanElement planElement : subTour) {
			if (planElement instanceof Leg) {
				Leg leg = (Leg) planElement;
				if (!modes.contains(leg.getMode())) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean isMassConserving(final List<PlanElement> subTour) {
		for (String mode : chainBasedModes) {
			if (!isMassConserving(subTour, mode)) {
				return false;
			} 
		}
		return true;
	}

	private boolean isMassConserving(
			final List<PlanElement> subTour,
			final String mode) {
		Activity firstLegUsingMode = findFirstLegUsing(subTour, mode);
		if (firstLegUsingMode == null) {
			return true;
		}
		else {
			Activity lastLegUsingMode = findLastLegUsing(subTour, mode);
			if (atSameLocation(firstLegUsingMode, lastLegUsingMode)) {
				return true;
			}
			else {
				return false;
			}
		}
	}

	private Id getLocationId(final Activity activity) {
		if (tripStructureAnalysisLayer == TripStructureAnalysisLayerOption.link) {
			return activity.getLinkId();
		} else {
			return activity.getFacilityId();
		}
	}
	
	private boolean atSameLocation(
			final Activity firstLegUsingMode,
			final Activity lastLegUsingMode) {
		if (tripStructureAnalysisLayer == TripStructureAnalysisLayerOption.link) {
			return firstLegUsingMode.getLinkId().equals(lastLegUsingMode.getLinkId());
		} else {
			return firstLegUsingMode.getFacilityId().equals(lastLegUsingMode.getFacilityId());
		}
	}

	private Activity findLastLegUsing(
			final List<PlanElement> subTour,
			final String mode) {
		List<PlanElement> reversedSubTour = new ArrayList<PlanElement>(subTour);
		Collections.reverse(reversedSubTour);
		return findFirstLegUsing(reversedSubTour, mode);
	}
	
	private Activity findFirstLegUsing(
			final List<PlanElement> subTour,
			final String mode) {
		for (PlanElement planElement : subTour) {
			if (planElement instanceof Leg) {
				Leg leg = (Leg) planElement;
				if (leg.getMode().equals(mode)) {
					return (Activity) subTour.get(subTour.indexOf(leg) - 1);
				}
			}
		}
		return null;
	}

	private String getTransportMode(final List<PlanElement> tour) {
		return ((Leg) (tour.get(1))).getMode();
	}

	private void changeLegModeTo(
			final Plan plan,
			final List<PlanElement> subtour,
			final String newMode) {
		if (newMode.equals( ParkAndRideConstants.PARK_N_RIDE_LINK_MODE )) {
			changeLegModeToPnr( plan , subtour );
		}
		else {
			changeLegModeToNonPnr( plan , subtour , newMode );
		}
	}

	private void changeLegModeToPnr(
			final Plan plan,
			final List<PlanElement> subtour) {
		int size = subtour.size();

		if (size < 5) {
			throw new RuntimeException( "subtour "+subtour+" in plan "+plan.getPlanElements()+" is too short for park and ride" );
		}

		boolean included = includer.routeAndIncludePnrTrips(
				(Activity) subtour.get( 0 ),
				(Activity) subtour.get( 2 ),
				(Activity) subtour.get( size - 3 ),
				(Activity) subtour.get( size - 1 ),
				plan);

		List<PlanElement> toRouteWithPt = included ?
				subtour.subList( 2 , size - 2 ) :
				subtour;

		changeLegModeToNonPnr(
				plan,
				toRouteWithPt,
				TransportMode.pt );
	}

	private void changeLegModeToNonPnr(
			final Plan plan,
			final List<PlanElement> subtour,
			final String newMode) {
		Iterator<PlanElement> iter = subtour.iterator();
		List<PlanElement> planElements = plan.getPlanElements();
		Person person = plan.getPerson();

		if ( iter.hasNext() ) {
			Activity origin = (Activity) iter.next();

			while (iter.hasNext()) {
				iter.next(); // skip leg
				Activity destination = (Activity) iter.next();
				List<? extends PlanElement> trip =
					tripRouter.calcRoute(
							newMode,
							new ActivityWrapperFacility( origin ),
							new ActivityWrapperFacility( destination ),
							getEndTime( origin , planElements ),
							person);
				TripRouter.insertTrip(
						planElements,
						origin,
						trip,
						destination);
				origin = destination;
			}
		}
	}

	public void setTripStructureAnalysisLayer(
			final TripStructureAnalysisLayerOption tripStructureAnalysisLayer) {
		planAnalyzeSubtours.setTripStructureAnalysisLayer(tripStructureAnalysisLayer);
		this.tripStructureAnalysisLayer = tripStructureAnalysisLayer;
	}

	private static double getEndTime(
			final Activity act,
			final List<PlanElement> plan) {
		double endTime = act.getEndTime();

		if (endTime == Time.UNDEFINED_TIME) {
			double startTime = act.getStartTime();

			if (startTime == Time.UNDEFINED_TIME) {
				for (PlanElement pe : plan) {
					if (pe instanceof Activity) {
						Activity currentAct = (Activity) pe;
						double currentEnd = currentAct.getEndTime();
						double currentStart = currentAct.getStartTime();
						double dur = (currentAct instanceof ActivityImpl ? ((ActivityImpl) currentAct).getMaximumDuration() : Time.UNDEFINED_TIME);
						if (currentEnd != Time.UNDEFINED_TIME) {
							// use fromcurrentAct.currentEnd as time for routing
							startTime = currentEnd;
						}
						else if ((currentStart != Time.UNDEFINED_TIME) && (dur != Time.UNDEFINED_TIME)) {
							// use fromcurrentAct.currentStart + fromcurrentAct.duration as time for routing
							startTime = currentStart + dur;
						}
						else if (dur != Time.UNDEFINED_TIME) {
							// use last used time + fromcurrentAct.duration as time for routing
							startTime += dur;
						}
						else {
							throw new RuntimeException("currentActivity has neither end-time nor duration." + currentAct);
						}
					}
					else {
						startTime += ((Leg) pe).getTravelTime();
					}

					if (pe == act) break;
				}
			}

			endTime = startTime + act.getMaximumDuration();
		}

		return endTime;
	}

	private static void checkSequence(final List<PlanElement> sequence) {
		if (SELF_CHECKING) {
			boolean lastWasActivity = false;

			for (PlanElement pe : sequence) {
				if (pe instanceof Activity) {
					if (lastWasActivity) {
						throw new RuntimeException( "wrong plan element sequence in "+sequence );
					}
					lastWasActivity = true;
				}
				else if (pe instanceof Leg) {
					if (!lastWasActivity) {
						throw new RuntimeException( "wrong plan element sequence in "+sequence );
					}
					lastWasActivity = false;
				}
				else {
					throw new RuntimeException( "unexpected plan element type "+pe.getClass()+" for plan element "+pe+" in "+sequence );
				}
			}
		}
	}
}
