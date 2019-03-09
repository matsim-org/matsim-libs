/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.contrib.locationchoice.bestresponse;

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup.ApproximationLevel;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

class ChoiceSet {

	private int numberOfAlternatives;
	private ApproximationLevel approximationLevel;
	private List<Id<ActivityFacility>> destinations = new LinkedList<Id<ActivityFacility>>();
	private List<Id<ActivityFacility>> notYetVisited = new LinkedList<Id<ActivityFacility>>();
	private final Network network;
	private final ActivityFacilities facilities;
	private final Scenario scenario;
	private final Map<String, Double> teleportedModeSpeeds;
	private final Map<String, Double> beelineDistanceFactors;
	private final boolean reUsePlans;

	private final Map<Id<ActivityFacility>, Id<Link>> nearestLinks;

	@Override
	public String toString() {
		StringBuilder stb = new StringBuilder() ;
		stb.append("destinations:") ;
		for ( Id<ActivityFacility> id : destinations ) {
			stb.append( " " ).append( id );
		}
		stb.append("\n") ;
		stb.append("notYetVisited:" ) ;
		for ( Id<ActivityFacility> id : notYetVisited ) {
			stb.append( " " ).append( id );
		}
		return stb.toString() ;
	}

	ChoiceSet(ApproximationLevel approximationLevel, Scenario scenario, Map<Id<ActivityFacility>, Id<Link>> nearestLinks,
		    Map<String, Double> teleportedModeSpeeds, Map<String, Double> beelineDistanceFactors) {
		this.approximationLevel = approximationLevel;
		this.network = scenario.getNetwork();
		this.facilities = scenario.getActivityFacilities();
		this.scenario = scenario;
		this.nearestLinks = nearestLinks;
		this.teleportedModeSpeeds = teleportedModeSpeeds;
		this.beelineDistanceFactors = beelineDistanceFactors;

		DestinationChoiceConfigGroup dccg = (DestinationChoiceConfigGroup) this.scenario.getConfig().getModule(DestinationChoiceConfigGroup.GROUP_NAME);
		this.numberOfAlternatives = dccg.getProbChoiceSetSize();
		this.reUsePlans = dccg.getReUseTemporaryPlans();
	}

	 void addDestination(Id<ActivityFacility> facilityId) {
		this.destinations.add(facilityId);
		this.notYetVisited.add(facilityId);
	}

	Id<ActivityFacility> getWeightedRandomChoice( int actlegIndex,
								    ScoringFunctionFactory scoringFunction, Plan plan, TripRouter tripRouter, double pKVal,
								    int iteration ) {

		List<ScoredAlternative> list;

		if (this.destinations.size() > 0) {
			list = this.createReducedChoiceSetWithPseudoScores(actlegIndex, this.facilities, scoringFunction, plan,
				  tripRouter );
		} else {
			// if we have no destinations defined so far, we can shorten this
			// currently handled activity which should be re-located
			Activity act = (Activity) plan.getPlanElements().get(actlegIndex);
//			list = createEmptyChoiceMap( act.getFacilityId() );
			list = Collections.singletonList( new ScoredAlternative( 0., act.getFacilityId() ) ) ;
			// (the "0" is a dummy entry!)
		}

		Random random = new Random((long) (Long.MAX_VALUE / iteration * pKVal));

		//	a couple of random draws to come to the "chaotic" region:
		for (int i = 0; i < 10; i++) {
			random.nextDouble();
		}
		int randomIndex = random.nextInt( list.size() ) ;

		return list.get( randomIndex ).getAlternativeId() ;

		// The following is sampling from the choice set, taking the best alternative with 60% proba, the next with 24% proba, etc.  Follow where it is coming from.  I don't
		// know why that could be a good approach. kai, mar'19
		// ignored now.  kai, mar'19

//		/*
//		 * list is filled by summing up the normalized scores (in descending order!):
//		 * Thus small original scores have a large normalized sum score. This is required as we put elements with decreasing
//		 * original score into the choice set as long as there is space left (number of alternatives).
//		 *
//		 * The list is thus here traversed (or sorted) in reverse order (descending), such that small normalized scores
//		 * (== alternatives having large original scores) are visited first.
//		 *  TODO: Check if this really makes a difference ... (I think not!)... -> maybe do natural order and add "break"
//		 *
//		 * The last entry, that is still larger (descending order) than the random number is returned as id.
//		 * Essentially this is Monte Carlo sampling.
//		 *
//		 * NOTE: Scores are not only normalized, but also accumulated!!!
//		 */
//		Id<ActivityFacility> id = list.get(list.firstKey());
//		for (Entry<Double, Id<ActivityFacility>> entry : list.entrySet()) {
//			if (entry.getKey() > randomNumber + 0.000000000000000001) {
//				id = entry.getValue();
//			}
//		}
//
//		return id;
	}

	/**
	 * The "score", which is behind the "Double" in the TreeMap, is some pseudo score 0.6, 0.84, ..., see {ChoiceSet#generateReducedChoiceSet(ArrayList)}.
	 * Well, no, not any more, just setting all of them to 0.2.
	 */
	private List<ScoredAlternative> createReducedChoiceSetWithPseudoScores(
		  int actlegIndex,
		  ActivityFacilities facilities,
		  ScoringFunctionFactory scoringFunction,
		  Plan plan,
		  TripRouter router ) {

		// currently handled activity which should be re-located
		Activity activityToRelocate = (Activity) plan.getPlanElements().get(actlegIndex);

		// We need to calculate the multi node dijkstra stuff only in case localRouting is used.
//		if (this.approximationLevel == DestinationChoiceConfigGroup.ApproximationLevel.localRouting ) {
//			Node fromNode;
//			/*
//			 * Assuming that both, forward and backward Dijkstra, route to the end nodes of links where
//			 * potential activities are located. Is this correct??
//			 * Otherwise, an ImaginaryNode for each routing direction has to be created.
//			 * cdobler, oct'13
//			 */
//			List<InitialNode> destinationNodes = new ArrayList<InitialNode>();
//			for (Id<ActivityFacility> destinationId : this.destinations) {
//				ActivityFacility destinationFacility = this.scenario.getActivityFacilities().getFacilities().get(destinationId);
//				Id<Link> linkId = destinationFacility.getLinkId();
//				Link destinationLink;
//				if (linkId != null) {
//					destinationLink = this.network.getLinks().get(linkId);
//				} else destinationLink = NetworkUtils.getNearestLink(((Network) this.network), destinationFacility.getCoord());
//
//				Node toNode = destinationLink.getToNode();
//				InitialNode initialToNode = new InitialNode(toNode, 0.0, 0.0);
//				destinationNodes.add(initialToNode);
//			}
//			ImaginaryNode destinationNode = forwardMultiNodeDijkstra.createImaginaryNode(destinationNodes);
//
//			// ---
//
//			//			Activity previousActivity = ((PlanImpl)plan).getPreviousActivity(((PlanImpl)plan).getPreviousLeg(activityToRelocate));
//			Leg previousLeg = LCPlanUtils.getPreviousLeg(plan, activityToRelocate );
//			Activity previousActivity = LCPlanUtils.getPreviousActivity(plan, previousLeg );
//			fromNode = this.network.getLinks().get(previousActivity.getLinkId()).getToNode();
//
//			forwardMultiNodeDijkstra.calcLeastCostPath(fromNode, destinationNode, previousActivity.getEndTime(), plan.getPerson(), null);
//
//			//		ForwardDijkstraMultipleDestinations leastCostPathCalculatorForward = new ForwardDijkstraMultipleDestinations(network, travelCost, travelTime);
//			//		leastCostPathCalculatorForward.calcLeastCostTree(fromNode, previousActivity.getEndTime());
//
//			// ---
//
//			//			Activity nextActivity = ((PlanImpl)plan).getNextActivity(((PlanImpl)plan).getNextLeg(activityToRelocate));
//			Leg nextLeg = LCPlanUtils.getNextLeg(plan, activityToRelocate );
//			Activity nextActivity = LCPlanUtils.getPreviousActivity(plan, nextLeg );
//			fromNode = this.network.getLinks().get(nextActivity.getLinkId()).getToNode();
//
//			/*
//			 * The original code below uses the relocated activities end time as start time. Does this make sense?
//			 * Probably yes, since the trip to the next destination is short??
//			 * BUT: if we use that activities end time, we could also use another ForwardMultiNodeDijsktra...
//			 * Switched to nextActivity.startTime() since this time is also available in PlanTimesAdapter.computeTravelTimeFromLocalRouting()
//			 * where the path's created by the Dijkstra are used. So far (I think), the estimated start times
//			 * where used there (leastCostPathCalculatorBackward.setEstimatedStartTime(activityToRelocate.getEndTime())).
//			 *
//			 * cdobler oct'13
//			 */
//			backwardMultiNodeDijkstra.calcLeastCostPath(fromNode, destinationNode, activityToRelocate.getEndTime(), plan.getPerson(), null);
//
//			//		BackwardDijkstraMultipleDestinations leastCostPathCalculatorBackward = new BackwardDijkstraMultipleDestinations(network, travelCost, travelTime);
//			//		leastCostPathCalculatorBackward.setEstimatedStartTime(activityToRelocate.getEndTime());
//			//		// the backwards Dijkstra will expand from the _next_ activity location backwards to all locations in the system.  This is the time
//			//		// at which this is anchored.  (Clearly too early, but probably not that bad as an approximation.)
//			//
//			//		leastCostPathCalculatorBackward.calcLeastCostTree(fromNode, -1.0);
//			//		// "-1.0" is ignored.  It is not clear to me why we first set the (approximated) start time separately, and then ignore the startTime.
//			//		// (The Dijkstra algo does not care if the start time is approximated or exact.)  kai, jan'13
//
//			// ---
//		}

		// Handling duplicates. This was may the source for (small) random fluctuations
		// yyyy which duplicates?  and how are they handled?  kai, jan'13
		//
		// that was not the problem of the fluctuations. alternatives with the same score are deterministically 
		// handled now by including the id for compareTo evaluation.
		// comment should be removed.
		ArrayList<ScoredAlternative> list = new ArrayList<ScoredAlternative>();
		double largestValue = Double.NEGATIVE_INFINITY;
		Id<ActivityFacility> facilityIdWithLargestScore = activityToRelocate.getFacilityId();

		/*
		 * TODO:
		 * Can this be merged with the for loop above? So far, facilities are looked up twice.
		 * cdobler, oct'13
		 */

		Plan planTmp = null;

		// In case we try to re-use a single copy of the plan: create the copy here and re-use it within the loop.
		if (this.reUsePlans) planTmp = LCPlanUtils.createCopy(plan );

		for (Id<ActivityFacility> destinationId : this.destinations) {
			// tentatively set
			ActivityFacility facility = facilities.getFacilities().get(destinationId);

			// As far as I can see, activity location is updated in the plan. Then the routes from and to that activity 
			// are calculated. The resulting travel times are written to the temporary plan. If this is true, it should 
			// not be necessary to update the activity location in the copied plan? I am not sure about this, therefore 
			// keep the update in the "if(this.ReUsePlans)" block. cdobler oct'15
			LCPlanUtils.setFacilityId(activityToRelocate, destinationId );
			LCPlanUtils.setCoord(activityToRelocate, facility.getCoord() );
			LCPlanUtils.setLinkId(activityToRelocate, this.nearestLinks.get(destinationId ) );

			//			PlanImpl planTmp = new PlanImpl();
			//			planTmp.copyFrom(plan);

			if (this.reUsePlans) {
				// we have to update the copied plan
				Activity actTmp = (Activity) planTmp.getPlanElements().get(actlegIndex);
				LCPlanUtils.setFacilityId(actTmp, destinationId );
				LCPlanUtils.setCoord(actTmp, facility.getCoord() );
				LCPlanUtils.setLinkId(actTmp, this.nearestLinks.get(destinationId ) );
			}
			// If we don't re-use a single copy of the plan, create a new one.
			else planTmp = LCPlanUtils.createCopy(plan );

			PlanTimesAdapter adapter = new PlanTimesAdapter( this.approximationLevel,
				  router, this.scenario, this.teleportedModeSpeeds, this.beelineDistanceFactors);

			//  Try:
			// * remove plan as argument
			// * duplicate following method, and rename into "adaptTimes" and "scorePlan".
			// * try, under testing, to thin out each method, to those two separate functionalities
			// * then try to replace by more central language constructs.

			final double score = adapter.adaptTimesAndScorePlan( plan, planTmp, scoringFunction );

			if (score > largestValue) {
				largestValue = score;
				facilityIdWithLargestScore = destinationId;
			}
			list.add(new ScoredAlternative(score, destinationId));
		}

		Collections.sort( list );
		if ( !list.isEmpty() ) {
			return list.subList( 0,Math.min( list.size(), 5) ) ;
		}
		else  {
			/* how is this supposed to happen at all?  kai, jan'13
			 *
			 * If the choice set is empty (for example due to sampling) use the current activity location.
			 * TODO: add a mechanism to at least keep, a minimal number of alternatives (minimally the chosen
			 * location of the previous iteration. Then this "if" can go.
			 *
			 * yyyyyy If facilityIdWithLargestScore is defined, it is also in list and thus in mapCorrected.  No?  kai, feb'13
			 */
			//			TreeMap<Double,Id> mapTmp = new TreeMap<Double,Id>();
			//			mapTmp.put(1.1, facilityIdWithLargestScore);
			//			return mapTmp;
//			return createEmptyChoiceMap(facilityIdWithLargestScore);
			return Collections.singletonList( new ScoredAlternative( largestValue, facilityIdWithLargestScore ) )  ;
		}
	}
//	private TreeMap<Double, Id<ActivityFacility>> createEmptyChoiceMap(Id<ActivityFacility> facilityIdWithLargestScore) {
//		TreeMap<Double, Id<ActivityFacility>> mapTmp = new TreeMap<>();
//		mapTmp.put(1.1, facilityIdWithLargestScore);
//		return mapTmp;
//	}

	/*
	 * We can have three cases here:
	 * 1. All scores positive
	 * 2. All scores negative
	 * 3. Mixed scores, negative and positive ones
	 *
	 * 1: This case is easy, just scale by total score and sum up for the ids, and then do the weighted random draw.
	 *
	 * 2: Adding an offset (minimal negative score) seems not the best idea in this case.
	 *  The preference order is maintained (to be proofed) but the ratios change a lot (to be proofed).
	 *  Is it better to apply -1/score? Are the ratios more reasonable?
	 *  I have a clear intuition for uniform distributions here,
	 *  i.e., that an option with score=4.0 should be chosen around 4 times more often than an option with score=1.0.
	 *  I can think of an array of length 5 with 4 elements of option 1 and 1 element of option 2.
	 *  But I am missing that intuition for negative scores and mixed scores. What about scores 1.0 and -3.0?
	 *
	 * 3: This is the most difficult case. We could do -1/score for negative scores but then we have a problem if the user comes up
	 * 	with all positive scores between 0.0 and 1.0.
	 *
	 *  A very efficient but also very simple solution would be to fix the number of alternatives and assign fixed probabilities.
	 *  Anyway, the question is, if the differences (ratios) between the k best alternatives have a significant meaning. If not, we
	 *  can get rid of this fancy overly complex Schnick-Snack.
	 *  Should not be too much of a problem to test, say max. 10 fixed alternatives with fixed probabilities, for example.
	 *
	 *  anhorni, dec 2013
	 *
	 */
	// A standard approach to get around that positive/negative problem would be to use exp(score) instead of score (logit).  Kai
	/*
	 * Use approximation to golden ratio (contains sqrt) with specified number of alternatives
	 * (more than around 5 makes no sense actually)
	 */
//	private TreeMap<Double, Id<ActivityFacility>> generateReducedChoiceSet(ArrayList<ScoredAlternative> list) {
//		// This is an attempt generate diversity, without falling back into the trap that locations just slide closer and closer to the home location.  I guess that the
//		// author did not want a sharp transition between the 1st five and the 6th, and thus introduced gradually declining probabilities.
//
//		// However, in the sense of matsim this does not make a difference: The iterations are not converged before all of them have been tried out.  In consequence,
//		// providing some with lower proba may(!) help being faster in the transients, but it will slow down final convergence.
//
//		// However, reaching final convergence may not be the true goal of matsim; at least, all the diversity generating stuff elsewhere also has the consequence that it will
//		// keep generating alternatives.
//
//		// sort the list (ScoredAlternative fulfills Comparable):
//		Collections.sort(list);
//
//		// say that we want about five alternatives (or less if we don't have enough):
//		int nrElements = Math.min(list.size(), this.numberOfAlternatives);
//
//		// take the first 5 alternatives and give them some pseudo-scores 0.6, 0.84, 0.936, 0.9744, ...
//		TreeMap<Double, Id<ActivityFacility>> mapNormalized = new TreeMap<>( java.util.Collections.reverseOrder() );
//		for (int index = 0; index < nrElements; index++)  {
////			double indexNormalized = 1.0 - Math.pow(0.4, (index + 1));
//			double indexNormalized = 0.2 ;
//			ScoredAlternative sa = list.get(index);
//			mapNormalized.put(indexNormalized, sa.getAlternativeId());
//		}
//		return mapNormalized;
//	}

}
