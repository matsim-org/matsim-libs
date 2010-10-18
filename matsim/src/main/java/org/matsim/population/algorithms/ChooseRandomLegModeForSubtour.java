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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.groups.PlanomatConfigGroup.TripStructureAnalysisLayerOption;

/**
 * Changes the transportation mode of one random non-empty subtour in a plan to a randomly chosen
 * different mode given a list of possible modes, considering that the means of transport
 * follows the law of mass conservation.
 *
 * @author michaz
 * @see SubtourModeChoice
 * 
 */
public class ChooseRandomLegModeForSubtour implements PlanAlgorithm {

	private static Logger logger = Logger.getLogger(ChooseRandomLegModeForSubtour.class);
	
	private static class Candidate {
		Integer subTourIndex;
		String newTransportMode;
	}

	private final Collection<String> chainBasedModes;

	private final Random rng;

	private PlanAnalyzeSubtours planAnalyzeSubtours;

	private PermissibleModesCalculator permissibleModesCalculator;

	private TripStructureAnalysisLayerOption tripStructureAnalysisLayer;
	
	private Id homeLocation;
	
	private Plan plan;

	public ChooseRandomLegModeForSubtour(final PermissibleModesCalculator permissibleModesCalculator, String[] chainBasedModes, final Random rng) {
		this.permissibleModesCalculator = permissibleModesCalculator;
		this.chainBasedModes = Arrays.asList(chainBasedModes);
		this.rng = rng;
		this.planAnalyzeSubtours = new PlanAnalyzeSubtours();
	}

	@Override
	public void run(final Plan plan) {
		logger.info("Chain based modes: " + chainBasedModes.toString());
		this.plan = plan;
		if (plan.getPlanElements().size() > 1) {
			if (tripStructureAnalysisLayer == TripStructureAnalysisLayerOption.link) {
				homeLocation = ((Activity) plan.getPlanElements().get(0)).getLinkId();
			} else if (tripStructureAnalysisLayer == TripStructureAnalysisLayerOption.facility) {
				homeLocation = ((Activity) plan.getPlanElements().get(0)).getFacilityId();
			}
			Collection<String> permissibleModesForThisPlan = permissibleModesCalculator.getPermissibleModes(plan);
			planAnalyzeSubtours.run(plan);
			List<Candidate> choiceSet = determineChoiceSet(permissibleModesForThisPlan);
			if (!choiceSet.isEmpty()) {
				Candidate whatToDo = choiceSet.get(rng.nextInt(choiceSet.size()));
				List<PlanElement> subTour = planAnalyzeSubtours.getSubtours().get(whatToDo.subTourIndex);
				changeLegModeTo(subTour, whatToDo.newTransportMode);
				logger.debug("Agent " + plan.getPerson().getId() + " changed their mode on subtour #"+whatToDo.subTourIndex+" to " + whatToDo.newTransportMode);
			}
		} else {
			// Nothing to do - the whole plan does not contain a
			// subtour. It isn't even its own subtour because it isn't a
			// tour.
		}
	}

	private List<Candidate> determineChoiceSet(Collection<String> permissibleModesForThisPerson) {
		ArrayList<Candidate> choiceSet = new ArrayList<Candidate>();
		for (Integer subTourIndex : planAnalyzeSubtours.getSubtourIndexation()) {
			if (subTourIndex < 0) {
				continue;
			}
			List<PlanElement> subTour = planAnalyzeSubtours.getSubtours().get(subTourIndex);
			Set<String> usableChainBasedModes = new HashSet<String>();
			Id subtourStartLocation;
			if (tripStructureAnalysisLayer == TripStructureAnalysisLayerOption.link) {
				subtourStartLocation = ((Activity) subTour.get(0)).getLinkId();
			} else if (tripStructureAnalysisLayer == TripStructureAnalysisLayerOption.facility) {
				subtourStartLocation = ((Activity) subTour.get(0)).getFacilityId();
			} else {
				throw new RuntimeException();
			}
			
			for (String mode : chainBasedModes) {
				Id vehicleLocation = homeLocation;
				Activity lastAct = findLastLegUsing(plan.getPlanElements().subList(0, plan.getPlanElements().indexOf(subTour.get(0)) + 1), mode);
				if (lastAct != null) {
					vehicleLocation = getLocationId(lastAct);
				}
				if (vehicleLocation.equals(subtourStartLocation)) {
					usableChainBasedModes.add(mode);
				}
			}
			
			Set<String> usableModes = new HashSet<String>();
			if (isMassConserving(subTour)) { // We can only replace a subtour if it doesn't itself move a vehicle from one place to another
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
			usableModes.remove(getTransportMode(subTour));
			for (String transportMode : usableModes) {
				Candidate candidate = new Candidate();
				candidate.subTourIndex = subTourIndex;
				candidate.newTransportMode = transportMode;
				choiceSet.add(candidate);
			}
		}
		return choiceSet;
	}

	private boolean isMassConserving(List<PlanElement> subTour) {
		for (String mode : chainBasedModes) {
			if (!isMassConserving(subTour, mode)) {
				return false;
			} 
		}
		return true;
	}

	private boolean isMassConserving(List<PlanElement> subTour, String mode) {
		Activity firstLegUsingMode = findFirstLegUsing(subTour, mode);
		if (firstLegUsingMode == null) {
			return true;
		} else {
			Activity lastLegUsingMode = findLastLegUsing(subTour, mode);
			if (atSameLocation(firstLegUsingMode, lastLegUsingMode)) {
				return true;
			} else {
				return false;
			}
		}
	}

	private Id getLocationId(Activity activity) {
		if (tripStructureAnalysisLayer == TripStructureAnalysisLayerOption.link) {
			return activity.getLinkId();
		} else {
			return activity.getFacilityId();
		}
	}
	
	private boolean atSameLocation(Activity firstLegUsingMode,
			Activity lastLegUsingMode) {
		if (tripStructureAnalysisLayer == TripStructureAnalysisLayerOption.link) {
			return firstLegUsingMode.getLinkId().equals(lastLegUsingMode.getLinkId());
		} else {
			return firstLegUsingMode.getFacilityId().equals(lastLegUsingMode.getFacilityId());
		}
	}

	private Activity findLastLegUsing(List<PlanElement> subTour, String mode) {
		List<PlanElement> reversedSubTour = new ArrayList<PlanElement>(subTour);
		Collections.reverse(reversedSubTour);
		return findFirstLegUsing(reversedSubTour, mode);
	}
	
	private Activity findFirstLegUsing(List<PlanElement> subTour, String mode) {
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

	private void changeLegModeTo(final List<PlanElement> tour, final String newMode) {
		for (PlanElement pe : tour) {
			if (pe instanceof Leg) {
				((Leg) pe).setMode(newMode);
			}
		}
	}

	public void setTripStructureAnalysisLayer(
			TripStructureAnalysisLayerOption tripStructureAnalysisLayer) {
		planAnalyzeSubtours.setTripStructureAnalysisLayer(tripStructureAnalysisLayer);
		this.tripStructureAnalysisLayer = tripStructureAnalysisLayer;
	}

}
