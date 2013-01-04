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

import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;
import java.util.Vector;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.locationchoice.bestresponse.PlanTimesAdapter.ApproximationLevel;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.old.PlanRouterAdapter;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.ScoringFunctionAccumulator;

public class ChoiceSet {
		
	// *************************************
	/**
	 * Presumably some exponent that is used to re-weight scores.  Problem is that it does not work with negative scores.
	 * Negative scores should not happen because then the activity should be dropped, but clearly the negativeness can
	 * be a consequence of the approximations, which means that it needs to be handeled.  The way this is done
	 * looks like a hack to me.  kai, jan'13
	 */
	private double exponent;
	private int numberOfAlternatives;
	
	private ApproximationLevel approximationLevel;	
	// *************************************	
	private List<Id> destinations = new Vector<Id>();
	private List<Id> notYetVisited = new Vector<Id>();
	private PlanRouterAdapter router = null;
	private Network network = null;
	private Config config;
		
	public ChoiceSet(ApproximationLevel approximationLevel, PlanRouterAdapter router, Network network, Config config) {
		this.approximationLevel = approximationLevel;
		this.router = router;
		this.network = network;
		this.config = config;
		
		this.exponent = Double.parseDouble(config.locationchoice().getProbChoiceExponent());
		this.numberOfAlternatives = Integer.parseInt(config.locationchoice().getProbChoiceSetSize());
	}
	
	public void addDestination(Id id) {
		this.destinations.add(id);
		this.notYetVisited.add(id);
	}
	
	public boolean hasUnvisited() {
		if (this.notYetVisited.size() > 0) return true;
		else return false;
	}
	
	public Id visitNext() {
		Id id = this.notYetVisited.get(0);
		this.notYetVisited.remove(0);
		return id;
	}
	
	public void reset() {
		for (Id id : this.destinations) {
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
	
	public Id getWeightedRandomChoice(int actlegIndex, Coord coordPre,
			Coord coordPost, ActivityFacilities facilities, ScoringFunctionAccumulator scoringFunction, 
			Plan plan, TravelTime travelTime, 
			TravelDisutility travelCost, int iteration) {
				
		TreeMap<Double, Id> map = this.createChoiceSet(actlegIndex, facilities, scoringFunction, plan, travelTime, travelCost);
		
		// score 0 is included as random range = 0.0d (inclusive) to 1.0d (exclusive)
		// TODO: Do I have to modify the seed here by the iteration number (i.e., do we in every iteration chose the same value)?
		Random random = new Random(iteration * 102830259L);
		for (int i = 0; i < 10; i++) {
			random.nextDouble();
		}
		double randomScore = random.nextDouble();
		
		Id id = map.get(map.firstKey());
		for (Entry<Double, Id> entry : map.entrySet()) {
	        if (entry.getKey() > randomScore + 0.000000000000000001) {
	        	id = entry.getValue();
	        }
	    }
		return id;
	}
	
	private TreeMap<Double,Id> createChoiceSet(int actlegIndex, ActivityFacilities facilities, ScoringFunctionAccumulator scoringFunction,
			Plan plan, TravelTime travelTime, TravelDisutility travelCost) {
		
		Activity act = (Activity) plan.getPlanElements().get(actlegIndex);
		
		// ---
		
		ForwardDijkstraMultipleDestinations leastCostPathCalculatorForward = new ForwardDijkstraMultipleDestinations(network, travelCost, travelTime);
				
		Activity previousActivity = ((PlanImpl)plan).getPreviousActivity(((PlanImpl)plan).getPreviousLeg(act));
		Node fromNode = network.getLinks().get(previousActivity.getLinkId()).getToNode();
		leastCostPathCalculatorForward.calcLeastCostTree(fromNode, previousActivity.getEndTime());
		
		// ---
		
		BackwardDijkstraMultipleDestinations leastCostPathCalculatorBackward = new BackwardDijkstraMultipleDestinations(network, travelCost, travelTime);
		Activity nextActivity = ((PlanImpl)plan).getNextActivity(((PlanImpl)plan).getNextLeg(act));
		fromNode = network.getLinks().get(nextActivity.getLinkId()).getToNode();

		leastCostPathCalculatorBackward.setEstimatedStartTime(act.getEndTime());
		// the backwards Dijkstra will expand from the _next_ activity location backwards to all locations in the system.  This is the time
		// at which this is anchored.  (Clearly too early, but probably not that bad as an approximation.)

		leastCostPathCalculatorBackward.calcLeastCostTree(fromNode, -1.0);
		// "-1.0" is ignored.  It is not clear to me why we first set the (approximated) start time separately, and then ignore the startTime.
		// (The Dijkstra algo does not care of the start time is approximated or exact.)  kai, jan'13
		
		// ---
		
		// Handling duplicates. This was may the source for (small) random fluctuations
		// which duplicates?  and how are they handled?  kai, jan'13
		List<ScoredAlternative> list = new Vector<ScoredAlternative>();						
		// if no epsilons are used!
		double largestValue = -1.0 * Double.MAX_VALUE; // would prefer Double.NEGATIVE_INFINITY. kai, jan'13
		Id facilityIdWithLargestScore = act.getFacilityId();
		
		for (Id destinationId : this.destinations) {
			// tentatively set 
			((ActivityImpl)act).setFacilityId(destinationId);
			((ActivityImpl)act).setCoord(facilities.getFacilities().get(destinationId).getCoord());
			((ActivityImpl)act).setLinkId(((NetworkImpl) this.network).getNearestLink(facilities.getFacilities().get(destinationId).getCoord()).getId());
			
			scoringFunction.reset();
			PlanImpl planTmp = new PlanImpl();
			planTmp.copyFrom(plan);
			this.adaptAndScoreTimes((PlanImpl)plan,  actlegIndex,  planTmp, scoringFunction,
					leastCostPathCalculatorForward, leastCostPathCalculatorBackward, this.approximationLevel);
			
			// not needed anymore
			// why not?  kai, jan'13
			//scoringFunction.finish();
			double score = scoringFunction.getScore();
			scoringFunction.reset();
									
			if (score > largestValue) {
				largestValue = score;
				facilityIdWithLargestScore = destinationId;
			}
			list.add(new ScoredAlternative(score, destinationId));
		}	
		// find the sum of the scores to normalize scores
		Collections.sort(list);
		double totalScore = this.getTotalScore(list, (largestValue < 0.0));
		TreeMap<Double,Id> mapCorrected = this.generateReducedChoiceSet(list, totalScore, (largestValue < 0.0));
				
		// score 0 is included as random range = 0.0d (inclusive) to 1.0d (exclusive)
		// TODO: Do I have to modify the seed here by the iteration number (i.e., do we in every iteration chose the same value)?
		if (mapCorrected.size() > 0) {
			return mapCorrected;
		}
		else  {	
			TreeMap<Double,Id> mapTmp = new TreeMap<Double,Id>();
			mapTmp.put(1.1, facilityIdWithLargestScore);
			return mapTmp;
		}
	}
	
	private double getTotalScore(List<ScoredAlternative> list, boolean largestValueIsNegative) {
		int n = 0;
		double totalScore = 0.0;
		for (ScoredAlternative sa : list) {
			if (n >= numberOfAlternatives) break;			
			double score = Math.pow(sa.getScore(), exponent);
			if (largestValueIsNegative) {
				// if we only have negative values -> change sign of score // see my comment at the declaration of this.exponent.  kai, jan'13
				score *= -1.0;
			}
			if (score > 0.0) {
				totalScore += score;
			}
			n++;
		}
		return totalScore;
	}
	
	private TreeMap<Double,Id> generateReducedChoiceSet(List<ScoredAlternative> list, double totalScore, boolean largestValueIsNegative) {
		TreeMap<Double,Id> mapNormalized = new TreeMap<Double,Id>(java.util.Collections.reverseOrder());
		int n = 0;
		double sumScore = 0.0;
		for (ScoredAlternative sa : list) {
			if (n >= numberOfAlternatives) break;
			double score = Math.pow(sa.getScore(), exponent);
			
			if (largestValueIsNegative) {
				// if we only have negative values -> change sign of score // see my comment at the declaration of this.exponent.  kai, jan'13
				score *= -1.0;
			}			
			if (score > 0.0) {
				sumScore += (score / totalScore);				
				mapNormalized.put(sumScore , sa.getAlternativeId());
			}
			n++;	
		}
		return mapNormalized;
	}
			
	/*package*/ void adaptAndScoreTimes(PlanImpl plan, int actlegIndex, PlanImpl planTmp, ScoringFunctionAccumulator scoringFunction, 
			LeastCostPathCalculator leastCostPathCalculatorForward, LeastCostPathCalculator leastCostPathCalculatorBackward, ApproximationLevel approximationLevelTmp ) {
		PlanTimesAdapter adapter = new PlanTimesAdapter(approximationLevelTmp , leastCostPathCalculatorForward, leastCostPathCalculatorBackward, 
				this.network, this.config);
		adapter.adaptAndScoreTimes(plan, actlegIndex, planTmp, scoringFunction, router);
	}
}
