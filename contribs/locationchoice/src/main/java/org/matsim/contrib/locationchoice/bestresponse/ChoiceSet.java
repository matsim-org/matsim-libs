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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup.ApproximationLevel;
import org.matsim.contrib.locationchoice.router.BackwardFastMultiNodeDijkstra;
import org.matsim.contrib.locationchoice.utils.PlanUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.ImaginaryNode;
import org.matsim.core.router.InitialNode;
import org.matsim.core.router.MultiNodeDijkstra;
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
			stb.append( " "+id ) ;
		}
		stb.append("\n") ;
		stb.append("notYetVisited:" ) ;
		for ( Id<ActivityFacility> id : notYetVisited ) {
			stb.append( " "+id ) ;
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
				
//		this.exponent = Double.parseDouble(config.locationchoice().getProbChoiceExponent());
//		if ( wrnCnt < 1 ) {
//			wrnCnt++ ;
//			if ( this.exponent != 1. ) {
//				Logger.getLogger(this.getClass()).warn("Exponent is presumably some exponent that is used to re-weight scores. " +
//					" Problem is that it does not work with negative scores. " +
//					"Negative scores should not happen because then the activity should be dropped, " +
//					"but clearly the negativeness can be a consequence of the approximations, " +
//					"which means that it needs to be handeled.  " +
//					"The way this is done looks like a hack to me.  kai, jan'13" ) ;
//			}
//		}
		DestinationChoiceConfigGroup dccg = (DestinationChoiceConfigGroup) this.scenario.getConfig().getModule(DestinationChoiceConfigGroup.GROUP_NAME);
		this.numberOfAlternatives = dccg.getProbChoiceSetSize();
		this.reUsePlans = dccg.getReUseTemporaryPlans();
	}
	
	public void addDestination(Id<ActivityFacility> facilityId) {
		this.destinations.add(facilityId);
		this.notYetVisited.add(facilityId);
	}
	
	public boolean hasUnvisited() {
		if (this.notYetVisited.size() > 0) return true;
		else return false;
	}
	
	public Id<ActivityFacility> visitNext() {
		Id<ActivityFacility> id = this.notYetVisited.get(0);
		this.notYetVisited.remove(0);
		return id;
	}
	
	public void reset() {
		for (Id<ActivityFacility> id : this.destinations) {
			this.notYetVisited.add(id);
		}
	}
	
	public int getNumberOfDestinations() {
		return this.destinations.size();
	}
	
	public void shuffle(Random rnd) {
		// set random seed
		Collections.shuffle(this.notYetVisited, rnd);
	}
	
	public Id<ActivityFacility> getWeightedRandomChoice(int actlegIndex,
			ScoringFunctionFactory scoringFunction, Plan plan, TripRouter tripRouter, double pKVal,
			MultiNodeDijkstra forwardMultiNodeDijkstra, BackwardFastMultiNodeDijkstra backwardMultiNodeDijkstra,
			int interation) {
				
		TreeMap<Double, Id<ActivityFacility>> map;
		
		// if we have no destinations defined so far, we can shorten this
		if (this.destinations.size() > 0) {
			map = this.createReducedChoiceSetWithScores(actlegIndex, this.facilities, scoringFunction, plan, 
					tripRouter, forwardMultiNodeDijkstra, backwardMultiNodeDijkstra);		
		} else {
			// currently handled activity which should be re-located
			Activity act = (Activity) plan.getPlanElements().get(actlegIndex);
			Id<ActivityFacility> facilityIdWithLargestScore = act.getFacilityId();
			map = createEmptyChoiceMap(facilityIdWithLargestScore);
		}
				
		/*  the same seed for every agent????? kai, jan'13
		 * 
		 * corrected, thx.
		 * Probably this error was not dramatic as the cs is different for every agent anyway.
		 */
		Random random = new Random((long) (Long.MAX_VALUE / interation * pKVal));
		
		//	a couple of random draws to come to the "chaotic" region:
		for (int i = 0; i < 10; i++) {
			random.nextDouble();
		}
		double randomScore = random.nextDouble();
		
		/*
		 * map is filled by summing up the normalized scores (in descending order!):
		 * Thus small original scores have a large normalized sum score. This is required as we put elements with decreasing
		 * original score into the choice set as long as there is space left (number of alternatives).
		 * 
		 * The map is thus here traversed (or sorted) in reverse order (descending), such that small normalized scores 
		 * (== alternatives having large original scores) are visited first.
		 *  TODO: Check if this really makes a difference ... (I think not!)... -> maybe do natural order and add "break" 
		 * 
		 * The last entry, that is still larger (descending order) than the random number is returned as id.
		 * Essentially this is Monte Carlo sampling.
		 * 
		 * NOTE: Scores are not only normalized, but also accumulated!!!
		 */
		Id<ActivityFacility> id = map.get(map.firstKey());		
		for (Entry<Double, Id<ActivityFacility>> entry : map.entrySet()) {
	        if (entry.getKey() > randomScore + 0.000000000000000001) {
	        	id = entry.getValue();
	        }
	    }
		
//		// ich würde es wohl in etwa wie folgt probieren:
//		// assumes that entry.getKey() just has the normal scores, not something modified.
//		// what is pKVal doing??
//		double sum = 0 ;
//		double offset = map.firstKey() ; // oder lastKey; denke aber, dass das eigentlich egal ist.
//		int ii = 0 ;
//		for ( Map.Entry<Double,Id> entry : map.entrySet() ) {
//			ii ++ ;
//			if ( ii > this.numberOfAlternatives ) break ;
//			sum += entry.getKey()-offset ;
//		}
//
//		double sum2 = 0. ;
//		double rnd = MatsimRandom.getRandom().nextDouble() * sum ; // note *sum!!  could also deal with offset here
//		for ( Map.Entry<Double,Id> entry : map.entrySet() ) {
//			sum2 += entry.getKey() - offset ;
//			if ( sum2 > rnd ) {
//				return entry.getValue() ;
//			}
//		}
//		throw new RuntimeException("at this point, I see no reason why it should ever get here; may need some fix later ...") ;
		
		return id;
	}
	
	private TreeMap<Double, Id<ActivityFacility>> createReducedChoiceSetWithScores(
			int actlegIndex,
			ActivityFacilities facilities,
			ScoringFunctionFactory scoringFunction,
			Plan plan,
			TripRouter router,
			MultiNodeDijkstra forwardMultiNodeDijkstra,
			BackwardFastMultiNodeDijkstra backwardMultiNodeDijkstra) {

		// currently handled activity which should be re-located
		Activity act = (Activity) plan.getPlanElements().get(actlegIndex);
		
		// We need to calculate the multi node dijkstra stuff only in case localRouting is used.
		if (this.approximationLevel == DestinationChoiceConfigGroup.ApproximationLevel.localRouting ) {
			Node fromNode;
			/*
			 * Assuming that both, forward and backward Dijkstra, route to the end nodes of links where
			 * potential activities are located. Is this correct??
			 * Otherwise, an ImaginaryNode for each routing direction has to be created.
			 * cdobler, oct'13 
			 */
			List<InitialNode> destinationNodes = new ArrayList<InitialNode>();
			for (Id<ActivityFacility> destinationId : this.destinations) {
				ActivityFacility destinationFacility = this.scenario.getActivityFacilities().getFacilities().get(destinationId);
				Id<Link> linkId = destinationFacility.getLinkId();
				Link destinationLink;
				if (linkId != null) {
					destinationLink = this.network.getLinks().get(linkId);				
				} else destinationLink = NetworkUtils.getNearestLink(((Network) this.network), destinationFacility.getCoord());
				
				Node toNode = destinationLink.getToNode();
				InitialNode initialToNode = new InitialNode(toNode, 0.0, 0.0);
				destinationNodes.add(initialToNode);
			}
			ImaginaryNode destinationNode = forwardMultiNodeDijkstra.createImaginaryNode(destinationNodes);
			
			// ---
			
//			Activity previousActivity = ((PlanImpl)plan).getPreviousActivity(((PlanImpl)plan).getPreviousLeg(act));
			Leg previousLeg = PlanUtils.getPreviousLeg(plan, act);
			Activity previousActivity = PlanUtils.getPreviousActivity(plan, previousLeg);
			fromNode = this.network.getLinks().get(previousActivity.getLinkId()).getToNode();

			forwardMultiNodeDijkstra.calcLeastCostPath(fromNode, destinationNode, previousActivity.getEndTime(), plan.getPerson(), null);			
			
//		ForwardDijkstraMultipleDestinations leastCostPathCalculatorForward = new ForwardDijkstraMultipleDestinations(network, travelCost, travelTime);
//		leastCostPathCalculatorForward.calcLeastCostTree(fromNode, previousActivity.getEndTime());
			
			// ---
			
//			Activity nextActivity = ((PlanImpl)plan).getNextActivity(((PlanImpl)plan).getNextLeg(act));
			Leg nextLeg = PlanUtils.getNextLeg(plan, act);
			Activity nextActivity = PlanUtils.getPreviousActivity(plan, nextLeg);
			fromNode = this.network.getLinks().get(nextActivity.getLinkId()).getToNode();
			
			/*
			 * The original code below uses the relocated activities end time as start time. Does this make sense?
			 * Probably yes, since the trip to the next destination is short??
			 * BUT: if we use that activities end time, we could also use another ForwardMultiNodeDijsktra...
			 * Switched to nextActivity.startTime() since this time is also available in PlanTimesAdapter.computeTravelTimeFromLocalRouting()
			 * where the path's created by the Dijkstra are used. So far (I think), the estimated start times
			 * where used there (leastCostPathCalculatorBackward.setEstimatedStartTime(act.getEndTime())).
			 * 
			 * cdobler oct'13
			 */
			backwardMultiNodeDijkstra.calcLeastCostPath(fromNode, destinationNode, act.getEndTime(), plan.getPerson(), null);
			
//		BackwardDijkstraMultipleDestinations leastCostPathCalculatorBackward = new BackwardDijkstraMultipleDestinations(network, travelCost, travelTime);
//		leastCostPathCalculatorBackward.setEstimatedStartTime(act.getEndTime());
//		// the backwards Dijkstra will expand from the _next_ activity location backwards to all locations in the system.  This is the time
//		// at which this is anchored.  (Clearly too early, but probably not that bad as an approximation.)
//
//		leastCostPathCalculatorBackward.calcLeastCostTree(fromNode, -1.0);
//		// "-1.0" is ignored.  It is not clear to me why we first set the (approximated) start time separately, and then ignore the startTime.
//		// (The Dijkstra algo does not care if the start time is approximated or exact.)  kai, jan'13
			
			// ---	
		}
		
		// Handling duplicates. This was may the source for (small) random fluctuations
		// yyyy which duplicates?  and how are they handled?  kai, jan'13
		//
		// that was not the problem of the fluctuations. alternatives with the same score are deterministically 
		// handled now by including the id for compareTo evaluation.
		// comment should be removed.
		ArrayList<ScoredAlternative> list = new ArrayList<ScoredAlternative>();						
		double largestValue = Double.NEGATIVE_INFINITY; 
		Id<ActivityFacility> facilityIdWithLargestScore = act.getFacilityId();
		
		/*
		 * TODO: 
		 * Can this be merged with the for loop above? So far, facilities are looked up twice.
		 * cdobler, oct'13
		 */
		
		Plan planTmp = null;

		// In case we try to re-use a single copy of the plan: create the copy here and re-use it within the loop.
		if (this.reUsePlans) planTmp = PlanUtils.createCopy(plan);
				
		for (Id<ActivityFacility> destinationId : this.destinations) {
			// tentatively set
			ActivityFacility facility = facilities.getFacilities().get(destinationId);
			
			// As far as I can see, activity location is updated in the plan. Then the routes from and to that activity 
			// are calculated. The resulting travel times are written to the temporary plan. If this is true, it should 
			// not be necessary to update the activity location in the copied plan? I am not sure about this, therefore 
			// keep the update in the "if(this.ReUsePlans)" block. cdobler oct'15
			PlanUtils.setFacilityId(act, destinationId);
			PlanUtils.setCoord(act, facility.getCoord());
			PlanUtils.setLinkId(act, this.nearestLinks.get(destinationId));
			
//			PlanImpl planTmp = new PlanImpl();
//			planTmp.copyFrom(plan);
			
			if (this.reUsePlans) {
				// we have to update the copied plan
				Activity actTmp = (Activity) planTmp.getPlanElements().get(actlegIndex);
				PlanUtils.setFacilityId(actTmp, destinationId);
				PlanUtils.setCoord(actTmp, facility.getCoord());
				PlanUtils.setLinkId(actTmp, this.nearestLinks.get(destinationId));
			}
			// If we don't re-use a single copy of the plan, create a new one.
			else planTmp = PlanUtils.createCopy(plan);
			
			final double score =
					this.adaptAndScoreTimes(
							plan,
							planTmp,
							scoringFunction,
							router,
							this.approximationLevel);
			
			if (score > largestValue) {
				largestValue = score;
				facilityIdWithLargestScore = destinationId;
			}
			list.add(new ScoredAlternative(score, destinationId));
		}
		TreeMap<Double, Id<ActivityFacility>> mapCorrected = this.generateReducedChoiceSet(list);
		if (mapCorrected.size() > 0) {
			return mapCorrected;
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
			return createEmptyChoiceMap(facilityIdWithLargestScore);
		}
	}
	private TreeMap<Double, Id<ActivityFacility>> createEmptyChoiceMap(Id<ActivityFacility> facilityIdWithLargestScore) {
		TreeMap<Double, Id<ActivityFacility>> mapTmp = new TreeMap<Double, Id<ActivityFacility>>();
		mapTmp.put(1.1, facilityIdWithLargestScore);
		return mapTmp;
		}
	
//	private double getTotalScore(ArrayList<ScoredAlternative> list) {
//		double totalScore = 0.0;
//		
//		int nrElements = Math.min(list.size(), this.numberOfAlternatives);
//		
//		for (int index = 0; index < nrElements; index++)  {
//			ScoredAlternative sa = list.get(index);
//			totalScore += sa.getScore() + this.getOffset(list, nrElements);
//		}
//		return totalScore;
//	}
	

//	private double getOffset(ArrayList<ScoredAlternative> list, int nrElements) {
//		double smallestScore = list.get(nrElements - 1).getScore();
//		return Math.min(smallestScore, 0.0) * (-1.0); // if smallest score is negative, then add offsets!
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
//	private TreeMap<Double,Id> generateReducedChoiceSet(ArrayList<ScoredAlternative> list) {
//		/* 
//		 * list is given here in descending order -> see compareTo in ScoredAlternative
//		 */		
//		Collections.sort(list);
//		double totalScore = this.getTotalScore(list);
//		
//		int nrElements = Math.min(list.size(), this.numberOfAlternatives);
//		
//		TreeMap<Double,Id> mapNormalized = new TreeMap<Double,Id>(java.util.Collections.reverseOrder());
//		double sumScore = 0.0;
//		for (int index = 0; index < nrElements; index++)  {
//			ScoredAlternative sa = list.get(index);
//			sumScore += (sa.getScore() + this.getOffset(list, nrElements)) / totalScore;				
//			mapNormalized.put(sumScore , sa.getAlternativeId());	
//		}
//		return mapNormalized;
//	}
	
	/*
	 * Use approximation to golden ratio (contains sqrt) with specified number of alternatives 
	 * (more than around 5 makes no sense actually)
	 */
	private TreeMap<Double, Id<ActivityFacility>> generateReducedChoiceSet(ArrayList<ScoredAlternative> list) {
		/* 
		 * list is given here in descending order -> see compareTo in ScoredAlternative
		 */		
		Collections.sort(list);
		int nrElements = Math.min(list.size(), this.numberOfAlternatives);
		
		TreeMap<Double, Id<ActivityFacility>> mapNormalized = new TreeMap<Double, Id<ActivityFacility>>(java.util.Collections.reverseOrder());
		for (int index = 0; index < nrElements; index++)  {
			double indexNormalized = 1.0 - Math.pow(0.4, (index + 1));
			ScoredAlternative sa = list.get(index);				
			mapNormalized.put(indexNormalized, sa.getAlternativeId());	
		}
		return mapNormalized;
	}
	
	double adaptAndScoreTimes( Plan plan,
			Plan planTmp,
			ScoringFunctionFactory scoringFunction,
			TripRouter router,
			ApproximationLevel approximationLevelTmp ) {
		PlanTimesAdapter adapter = new PlanTimesAdapter(approximationLevelTmp,
				router, this.scenario, this.teleportedModeSpeeds, this.beelineDistanceFactors);
		return adapter.adaptTimesAndScorePlan(plan, planTmp, scoringFunction);
	}
}
