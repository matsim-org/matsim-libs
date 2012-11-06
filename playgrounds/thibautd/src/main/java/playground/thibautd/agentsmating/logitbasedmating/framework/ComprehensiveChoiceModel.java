/* *********************************************************************** *
 * project: org.matsim.*
 * ComprehensiveChoiceModel.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.agentsmating.logitbasedmating.framework;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.groups.PlanomatConfigGroup;
import org.matsim.population.algorithms.PlanAnalyzeSubtours;

/**
 * Chooses between mode sequences, rather than mode for individual trips,
 * by using a joint probability distribution.
 *
 * @author thibautd
 */
public class ComprehensiveChoiceModel {
	private ChoiceModel tripLevelModel = null;
	private PlanAnalyzeSubtours subtoursAnalyser;

	private static final double EPSILON = 1E-7;
	private static final int NO_FATHER_SUBTOUR = Integer.MIN_VALUE;
	private static final String PASSENGER_MODE = "cp_pass";
	private static final String DRIVER_MODE = "cp_driver";

	// todo: import.
	private static final List<String> chainBasedModes =
		Arrays.asList(
				TransportMode.bike,
				TransportMode.car,
				DRIVER_MODE);

	private static final List<String> allModes = Arrays.asList(
		TransportMode.bike,
		TransportMode.car,
		TransportMode.pt,
		TransportMode.walk,
		PASSENGER_MODE,
		DRIVER_MODE);

	private final Random random = new Random( 182942 );

	private int[] subtourIndices = null;
	private int[] subtourFatherTable = null;

	// /////////////////////////////////////////////////////////////////////////
	// construction
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// getters / setters
	// /////////////////////////////////////////////////////////////////////////
	public void setTripLevelChoiceModel(
			final ChoiceModel model) {
		this.tripLevelModel = model;
	}

	public ChoiceModel getTripLevelChoiceModel() {
		return this.tripLevelModel;
	}

	// /////////////////////////////////////////////////////////////////////////
	// choice methods
	// /////////////////////////////////////////////////////////////////////////
	/**
	 * @return a list of {@link Alternative} instances, each corresponding to a
	 * trip in the plan.
	 */
	public List<Alternative> performChoice(
			final DecisionMaker decisionMaker,
			final Plan plan) {
		//perform choice
		Map< List<Alternative> , Double > probs = getChoiceProbabilities(
				decisionMaker , plan);

		double choice = random.nextDouble();

		double currentBound = EPSILON;

		for ( Map.Entry< List<Alternative> , Double > entry :
				probs.entrySet() ) {
			currentBound += entry.getValue();

			if (choice < currentBound) return entry.getKey();
		}

		// should never reach this line
		throw new RuntimeException( "choice procedure failed: sum of probabilities = "+currentBound+
				". This should have raisen an exception earlier!" );
	}

	/**
	 * @return a map linking mode sequences to choice probabilities
	 */
	public Map< List<Alternative> , Double > getChoiceProbabilities(
			final DecisionMaker decisionMaker,
			final Plan plan) {
		// init internal information
		subtoursAnalyser = new PlanAnalyzeSubtours( plan );
		subtourIndices = subtoursAnalyser.getSubtourIndexation();
		subtourFatherTable = getFatherTable();

		List< List<Alternative> > fullChoiceSets = new ArrayList< List<Alternative> >();

		int count = 0;
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Leg) {
				fullChoiceSets.add(
						tripLevelModel.getChoiceSetFactory().createChoiceSet(
							decisionMaker,
							plan,
							count));
			}
			count++;
		}

		Map< List<Alternative> , Double > probabilities =
			new HashMap< List<Alternative> , Double >();

		constructModeChains(
				probabilities,
				new StringChainsConstructor( subtourIndices.length ),
				decisionMaker,
				0,
				fullChoiceSets);

		return probabilities;
	}

	// /////////////////////////////////////////////////////////////////////////
	// choice set construction
	// /////////////////////////////////////////////////////////////////////////
	/**
	 * @return the mode, including car pooling
	 */
	// TODO: put this in the TripRequest contract!
	private static String extractMode(
			final Alternative alt) {
		if (alt instanceof TripRequest) {
			switch ( ((TripRequest) alt).getTripType() ) {
				case DRIVER: return DRIVER_MODE;
				case PASSENGER: return PASSENGER_MODE;
				default: throw new RuntimeException("unknown trip request type: "
								 +((TripRequest) alt).getTripType());
			}
		}
		return alt.getMode();
	}

	/**
	 * Recursively constructs the possible mode chains, by explicitly
	 * constructing the tree of possible mode chains: each node has as successors
	 * the possible modes for the next leg.
	 *
	 * @param previousLeg the father node
	 * @param decisionmaker the decision maker
	 * @param fullChoiceSets a list of all possible modes for each coming leg,
	 * or null if we reached the last leg.
	 *
	 * @return the "leaves" nodes, ie, the full mode strings
	 */
	private void constructModeChains(
			final Map< List<Alternative> , Double > probabilityMap,
			final StringChainsConstructor stringChainsConstructor,
			final DecisionMaker decisionMaker,
			final int legIndex,
			final List< List<Alternative> > fullChoiceSets) {
		// if no more legs, we are at the end of the plan:
		// add the probability info and return
		//if (fullChoiceSets == null) {
		if (legIndex == fullChoiceSets.size()) {
			probabilityMap.put(
					stringChainsConstructor.getModeChain(),
					stringChainsConstructor.getModeChainProbability() );
			return;
		}

		// construct the list of full choice sets for upcoming legs
		List<Alternative> currentAlternatives = fullChoiceSets.get( legIndex );
		//List< List<Alternative> > remainingAlternatives =
		//	fullChoiceSets.size() > 1 ?
		//	fullChoiceSets.subList(1, fullChoiceSets.size()) :
		//	null;

		// process child nodes
		List<Alternative> restrictedChoiceSet;
		List<String> possibleModes;
		for (Alternative alt : currentAlternatives) {
			// set info corresponding to this node
			stringChainsConstructor.setAlternative( legIndex , alt );

			restrictedChoiceSet = new ArrayList<Alternative>();
			possibleModes = stringChainsConstructor.getPossibleModes( legIndex );

			// first of all: is the alternative corresponding to this not
			// allowed?
			if (!possibleModes.contains( extractMode( alt ) )) {
				// impossible branch: jump to the next
				continue;
			}
			else {
				restrictedChoiceSet.add( alt );
			}

			// add all allowed alternatives
			for (Alternative currentAlt : currentAlternatives) {
				if (possibleModes.contains( extractMode( currentAlt ) )) {
					restrictedChoiceSet.add( currentAlt );
				}
			}

			stringChainsConstructor.setTripLevelProbability(
					legIndex,
					tripLevelModel.getChoiceProbabilities(
						decisionMaker, restrictedChoiceSet).get( alt ) );

			// examine next level
			constructModeChains(
					probabilityMap,
					stringChainsConstructor,
					decisionMaker,
					legIndex + 1,
					//remainingAlternatives);
					fullChoiceSets);
		}
	}

	private int[] getFatherTable() {
		int nSubTours = this.subtoursAnalyser.getNumSubtours();
		int[] output = new int[nSubTours];

		int i = 0;
		for (Integer father : subtoursAnalyser.getParentTours()) {
			output[i] = father == null ? NO_FATHER_SUBTOUR : father;
			i++;
		}

		return output;
	}

	/**
	 * given information on the modes in the previous legs,
	 * gives the possible modes in a leg
	 */
	private class StringChainsConstructor {
		private final Alternative[] alternatives;
		private final double[] probabilities;

		public StringChainsConstructor(final int nLegs) {
			alternatives = new Alternative[ nLegs ];
			probabilities = new double[ nLegs ];
		}

		public List<String> getPossibleModes(final int legIndex) {
			List<String> possibleModes = new ArrayList<String>( allModes );
			keepOnlyPossibleModes(
					legIndex - 1,
					subtourIndices[ legIndex ],
					possibleModes );
			return possibleModes;
		}

		public void setAlternative(final int legIndex, final Alternative alt) {
			alternatives[ legIndex ] = alt;
		}

		/**
		 * takes the probability of choice of the given leg at its level,
		 * and remembers the probability of the mode chain until there.
		 */
		public void setTripLevelProbability(
				final int legIndex,
				final double prob) {
			probabilities[ legIndex ] = 
				(legIndex > 0 ? probabilities[ legIndex - 1 ] : 1d) * prob;
		}

		public List<Alternative> getModeChain() {
			return Arrays.asList( alternatives );
		}

		public double getModeChainProbability() {
			return probabilities[ probabilities.length - 1 ];
		}

		/**
		 * returns the list of possible modes for the subtour in parameter, as it is constrained
		 * by the leg in parameter.
		 *
		 * This list is computed in the following way:
		 * <br>
		 * <ul>
		 * <li> if the leg is in the same subtour:
		 * <ul>
		 *     <li> if this node correspnds to a chain based mode, only the mode
		 *     is possible.
		 *     <li> otherwise, no chain based mode is possible
		 * </ul>
		 * <li> if the leg is in a direct "son" subtour
		 * <ul>
		 *     <li> if this node correspnds to a chain based mode, only this chain
		 *     base mode is possible, in addition to all non-chain based modes
		 *     <li> otherwise, no chain based mode is possible
		 * </ul>
		 * <li> otherwise, the procedure is repeated at the upper level of the tree
		 * </ul>
		 *
		 * car passenger is considered a specific non-chain based mode, car driver is
		 * sonsidered as car mode.
		 *
		 * @return the list of possible modes for the next leg, given the subtour
		 * it pertains
		 */
		private void keepOnlyPossibleModes(
				final int legIndex,
				final int subtour,
				final List<String> possibleModes) {
			if (legIndex < 0 ) {
				return;
			}

			String thisMode = extractMode( alternatives[ legIndex ] );
			if ( subtourIndices[ legIndex ] == subtour ) {
				// the next leg is in the same subtour as us
				if (chainBasedModes.contains( thisMode )) {
					possibleModes.clear();
					possibleModes.add( thisMode );
				}
				else {
					possibleModes.removeAll( chainBasedModes );
				}
			}
			else if ( subtourFatherTable[ subtour ] == subtourIndices[ legIndex ]) {
				// the requested subtour is a direct "son" of our: we influence it.
				if (chainBasedModes.contains( thisMode )) {
					for (String mode : chainBasedModes) {
						if ( !mode.equals( thisMode ) ) {
							possibleModes.remove( mode );
						}
					}
				}
				else {
					possibleModes.removeAll( chainBasedModes );
				}
			}
			else {
				// we do not influence the leg.
				// perhaps the previous leg does?
				keepOnlyPossibleModes( legIndex - 1 , subtour , possibleModes );
				return;
			}

			// handle car availability: driver legs are car legs
			if ( thisMode.equals( TransportMode.car ) ) {
				possibleModes.add( DRIVER_MODE );
			}
			else if ( thisMode.equals( DRIVER_MODE ) ) {
				possibleModes.add( TransportMode.car );
			}
		}
	}
}
