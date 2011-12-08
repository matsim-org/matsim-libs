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
	// construction
	// /////////////////////////////////////////////////////////////////////////
	public ComprehensiveChoiceModel() {
		subtoursAnalyser.setTripStructureAnalysisLayer(
				PlanomatConfigGroup.TripStructureAnalysisLayerOption.link );
	}

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
		subtoursAnalyser.run( plan );
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

		List< LegChoiceNode > possibleStringsLeaves = constructModeChains(
				new LegChoiceNode(),
				decisionMaker,
				fullChoiceSets);

		Map< List<Alternative> , Double > probabilities =
			new HashMap< List<Alternative> , Double >();

		int alternativesCount = 0;
		int rejectedCount = 0;
		int notRejectedNotAddedCount = 0;
		double totalCumulatedProb = 0;
		double addedCumulatedProb = 0;
		for (LegChoiceNode node : possibleStringsLeaves) {
			alternativesCount++;
			double prob = node.getProbability();
			totalCumulatedProb += prob;

			if (prob > 0) {
				addedCumulatedProb += prob;
				probabilities.put( node.getChainString() , prob );
			}
			else if (node.isRejected()) {
				rejectedCount++;
			}
			else {
				// really? a probability 0 (or less ???) alternative which is not due to
				// a subtour-based reject? This can happen, but this is more
				// probably due to a bug.
				notRejectedNotAddedCount ++;
			}
		}

		if (Math.abs( totalCumulatedProb - 1d ) > EPSILON ||
				Math.abs( addedCumulatedProb - 1d ) > EPSILON ) {
			throw new RuntimeException( "choice procedure failed: "+
					"total sum of probabilities = "+totalCumulatedProb+
					", added sum of probabilities = "+addedCumulatedProb+
					" for agent "+plan.getPerson().getId()+
					" with plan of length "+plan.getPlanElements().size()+
					" and "+probabilities.size()+" possible mode chains (non-0 probability), "+
					alternativesCount+" complete mode chains (including 0-prob), "+
					rejectedCount+" alternative mode chains where rejected, "+
					notRejectedNotAddedCount+" alternatives were not rejected but not added");
		}

		return probabilities;
	}

	// /////////////////////////////////////////////////////////////////////////
	// choice set construction
	// /////////////////////////////////////////////////////////////////////////
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
	private List< LegChoiceNode > constructModeChains(
			final LegChoiceNode previousLeg,
			final DecisionMaker decisionMaker,
			final List< List<Alternative> > fullChoiceSets) {
		// if no more legs,  the last leg was a "leaf" leg (ie a node corresponding
		// to the full mode chain): return it in a list.
		if (fullChoiceSets == null) return Arrays.asList( previousLeg );

		// construct the list of full choice sets for upcoming legs
		List<Alternative> currentAlternatives = fullChoiceSets.get(0);
		List< List<Alternative> > remainingAlternatives =
			fullChoiceSets.size() > 1 ?
			fullChoiceSets.subList(1, fullChoiceSets.size()) :
			null;

		List< LegChoiceNode > leaves = new ArrayList< LegChoiceNode >();
		// process child nodes
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

		int i = 0;
		for (Integer father : subtoursAnalyser.getParentTours()) {
			output[i] = father == null ? NO_FATHER_SUBTOUR : father;
			i++;
		}

		return output;
	}

	/**
	 * represents a node of the tree of possible mode chains.
	 *
	 * The interest of such a representation is that a node is
	 * able to provide the restricted list of modes to its successors.
	 */
	private class LegChoiceNode {
		private final LegChoiceNode previousLeg;
		private final Alternative alt;
		private final double prob;
		private final boolean rejected;

		// /////////////////////////////////////////////////////////////////////
		// construction
		// /////////////////////////////////////////////////////////////////////
		public LegChoiceNode() {
			this(null, null, null, null);
		}

		public LegChoiceNode(
				final DecisionMaker decider,
				final LegChoiceNode previousLeg,
				final Alternative alt,
				final List<Alternative> fullChoiceSet) {
			this.previousLeg = previousLeg;
			this.alt = alt;

			List<Alternative> restrictedChoiceSet = new ArrayList<Alternative>();

			// previousLeg == null for the root, which does not correspond to a leg
			if (previousLeg != null) {
				List<String> possibleModes = previousLeg.getPossibleModes(
							subtourIndices[ getIndex() ] );

				// first of all: is the alternative corresponding to this not
				// allowed?
				if (!possibleModes.remove( extractMode( alt ) )) {
					// this node is not possible: do not continue
					//this.alternatives = null;
					this.prob = 0d;
					this.rejected = true;
					return;
				}
				else {
					restrictedChoiceSet.add( alt );
				}

				// add all allowed alternatives
				for (Alternative currentAlt : fullChoiceSet) {
					if (possibleModes.remove( extractMode( currentAlt ) )) {
						restrictedChoiceSet.add( currentAlt );
					}
				}

				// P(t_1, ... , t_i) =
				this.prob =
					// P(t_1, ... , t_{i-1}) *
					previousLeg.getProbability() *
					// P(t_i | t_1, ... , t_{i-1})
					tripLevelModel.getChoiceProbabilities(
							decider, restrictedChoiceSet).get( alt );
			}
			else {
				prob = 1;
			}

			this.rejected = false;
		}

		/**
		 * @return the mode, including car pooling
		 */
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

		// /////////////////////////////////////////////////////////////////////
		// interface
		// /////////////////////////////////////////////////////////////////////
		public int getIndex() {
			if (previousLeg == null) return -1;
			return previousLeg.getIndex() + 1;
		}

		/**
		 * returns the list of possible modes for the next leg.
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
		public List<String> getPossibleModes(final int subtour) {
			if (previousLeg == null) return new ArrayList<String>( allModes );

			//List<String> possibleModes = previousLeg.getPossibleModes( subtour );
			List<String> possibleModes = new ArrayList<String>( allModes );

			String thisMode = extractMode( alt );
			if ( subtourIndices[ getIndex() ] == subtour ) {
				// the next leg is in the same subtour as us
				if (chainBasedModes.contains( thisMode )) {
					possibleModes.clear();
					possibleModes.add( thisMode );
				}
				else {
					possibleModes.removeAll( chainBasedModes );
				}
			}
			else if ( subtourFatherTable[ subtour ] == subtourIndices[ getIndex() ]) {
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
				return previousLeg.getPossibleModes( subtour );
			}

			// handle car availability: driver legs are car legs
			if ( thisMode.equals( TransportMode.car ) ) {
				possibleModes.add( DRIVER_MODE );
			}
			else if ( thisMode.equals( DRIVER_MODE ) ) {
				possibleModes.add( TransportMode.car );
			}

			return possibleModes;
		}

		public double getProbability() {
			return prob;
		}

		public List<Alternative> getChainString() {
			if (previousLeg == null) return new ArrayList<Alternative>();

			List<Alternative> string = previousLeg.getChainString();
			string.add( alt );

			return string;
		}

		/**
		 * for debug: executed on leaves, is allows to count the number of total
		 * mode chains which where rejected based on mode possibilities.
		 */
		public boolean isRejected() {
			return rejected ? true : (previousLeg != null && previousLeg.isRejected());
		}
	}
}
