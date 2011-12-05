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

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.population.algorithms.PlanAnalyzeSubtours;

/**
 * Chooses between mode sequences, rather than mode for individual trips,
 * by using a joint probability distribution.
 *
 * @author thibautd
 */
public class ComprehensiveChoiceModel {
	private ChoiceModel tripLevelModel = null;
	private final PlanAnalyzeSubtours subtoursAnalyser = new PlanAnalyzeSubtours();

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
		// init internal information
		subtoursAnalyser.run( plan );
		subtourIndices = subtoursAnalyser.getSubtourIndexation();
		subtourFatherTable = getFatherTable();

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

		// absolutely impossible to get here without bug
		throw new RuntimeException( "choice procedure failed: sum of probabilities = "+currentBound+
				" for agent "+plan.getPerson().getId());
	}

	/**
	 * @return a map linking mode sequences to choice probabilities
	 */
	public Map< List<Alternative> , Double > getChoiceProbabilities(
			final DecisionMaker decisionMaker,
			final Plan plan) {
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

		List< LegChoiceNode > possibleStringsLeaves = constructModeChains(
				new LegChoiceNode(),
				decisionMaker,
				fullChoiceSets);

		Map< List<Alternative> , Double > probabilities =
			new HashMap< List<Alternative> , Double >();

		for (LegChoiceNode node : possibleStringsLeaves) {
			double prob = node.getProbability();

			if (prob > EPSILON) {
				probabilities.put( node.getChainString() , prob );
			}
		}

		return probabilities;
	}

	/**
	 * @return the "leaves" nodes, ie, the full mode strings
	 */
	private List< LegChoiceNode > constructModeChains(
			final LegChoiceNode previousLeg,
			final DecisionMaker decisionMaker,
			final List< List<Alternative> > fullChoiceSets) {
		if (fullChoiceSets == null) return Arrays.asList( previousLeg );

		List<Alternative> currentAlternatives = fullChoiceSets.get(0);
		List< List<Alternative> > remainingAlternatives =
			fullChoiceSets.size() > 1 ?
			fullChoiceSets.subList(1, fullChoiceSets.size()) :
			null;

		List< LegChoiceNode > leaves = new ArrayList< LegChoiceNode >();
		for (Alternative alt : currentAlternatives) {
			LegChoiceNode currentNode = new LegChoiceNode(
					decisionMaker,
					previousLeg,
					alt,
					currentAlternatives);
			leaves.addAll( constructModeChains(
						currentNode,
						decisionMaker,
						remainingAlternatives) );
		}

		return leaves;
	}

	private int[] getFatherTable() {
		int nSubTours = this.subtoursAnalyser.getNumSubtours();
		int[] output = new int[nSubTours];
		List<Integer> alreadyHasFather = new ArrayList<Integer>(nSubTours);
		int currentSubTour;
		int count = 0;

		//this.orderedSubtourIndices = new int[nSubTours];

		// TODO: affect NO_FATHER to ALL home-based tours
		output[ subtourIndices[0] ] = NO_FATHER_SUBTOUR;
		//this.orderedSubtourIndices[count] = subtourStruct[0];
		alreadyHasFather.add(subtourIndices[0]);
		count++;

		for (int i=1; i < subtourIndices.length; i++) {
			if (count == nSubTours) {
				//each subtour has been examined, we can stop
				break;
			}

			currentSubTour = subtourIndices[ i ];

			if (!(alreadyHasFather.contains(currentSubTour))) {
				// As we go through legs in chronological order, the FATHER
				// of a subtour is the one to which pertains the leg preceding
				// this subtour.
				output[ currentSubTour ] = subtourIndices[ i - 1 ];
				//this.orderedSubtourIndices[count] = currentSubTour;
				alreadyHasFather.add(currentSubTour);
				count++;
			}
		}

		return output;
	}

	private class LegChoiceNode {
		private final LegChoiceNode previousLeg;
		private final Alternative alt;
		private final List<Alternative> alternatives;
		private final DecisionMaker decider;

		public LegChoiceNode() {
			this(null, null, null, null);
		}

		public LegChoiceNode(
				final DecisionMaker decider,
				final LegChoiceNode previousLeg,
				final Alternative alt,
				final List<Alternative> fullChoiceSet) {
			this.decider = decider;
			this.previousLeg = previousLeg;
			this.alt = alt;

			List<Alternative> restrictedChoiceSet = new ArrayList<Alternative>();

			if (previousLeg != null) {
				List<String> possibleModes = previousLeg.getPossibleModes(
							subtourIndices[ getIndex() ] );

				if (!possibleModes.remove( extractMode( alt ) )) {
					// this node is not possible: do not continue
					this.alternatives = null;
					return;
				}
				else {
					restrictedChoiceSet.add( alt );
				}
				for (Alternative currentAlt : fullChoiceSet) {
					if (possibleModes.remove( extractMode( currentAlt ) )) {
						restrictedChoiceSet.add( currentAlt );
					}
				}
			}

			this.alternatives = restrictedChoiceSet;
		}

		// TODO: put this in the TripRequest contract!
		private String extractMode(
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

		public int getIndex() {
			if (previousLeg == null) return -1;
			return previousLeg.getIndex() + 1;
		}

		/**
		 * @return the list of possible modes for the next leg, given the subtour
		 * it pertains
		 */
		public List<String> getPossibleModes(final int subtour) {
			if (previousLeg == null) return new ArrayList<String>( allModes );

			//List<String> possibleModes = previousLeg.getPossibleModes( subtour );
			List<String> possibleModes = new ArrayList<String>( allModes );

			if ( subtourIndices[ getIndex() ] == subtour ) {
				// the next leg is in the same subtour as us
				if (chainBasedModes.contains( alt.getMode() )) {
					possibleModes.clear();
					possibleModes.add( alt.getMode() );
				}
				else {
					possibleModes.removeAll( chainBasedModes );
				}
			}
			else if ( subtourFatherTable[ subtour ] == subtourIndices[ getIndex() ]) {
				// the requested subtour is a direct "son" of our: we influence it.
				if (chainBasedModes.contains( alt.getMode() )) {
					for (String mode : chainBasedModes) {
						if ( !mode.equals( alt.getMode() ) ) {
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
				return previousLeg.getPossibleModes( subtour );
			}

			// handle car availability
			if ( alt.getMode().equals( TransportMode.car ) ) {
				possibleModes.add( DRIVER_MODE );
			}
			else if ( alt.getMode().equals( DRIVER_MODE ) ) {
				possibleModes.add( TransportMode.car );
			}

			return possibleModes;
		}

		public double getProbability() {
			if (previousLeg == null) return 1;
			if (alternatives == null) return 0;

			double prob = previousLeg.getProbability();
			if (prob < EPSILON) return 0d;

			return prob
				* tripLevelModel.getChoiceProbabilities(
						decider, alternatives).get( alt );
		}

		public List<Alternative> getChainString() {
			if (previousLeg == null) return new ArrayList<Alternative>();

			List<Alternative> string = previousLeg.getChainString();
			string.add( alt );

			return string;
		}
	}
}
