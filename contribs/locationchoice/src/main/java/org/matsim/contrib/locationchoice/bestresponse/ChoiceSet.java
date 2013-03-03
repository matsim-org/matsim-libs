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
import java.util.List;
import java.util.Random;
import java.util.TreeMap;
import java.util.Vector;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
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
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.ScoringFunctionAccumulator;

public class ChoiceSet {
	private int numberOfAlternatives;	
	private ApproximationLevel approximationLevel;		
	private List<Id> destinations = new Vector<Id>();
	private List<Id> notYetVisited = new Vector<Id>();
	private final Network network;
	private Config config;
	private Scenario scenario;
	
	@Override
	public String toString() {
		StringBuilder stb = new StringBuilder() ;
		stb.append("destinations:") ;
		for ( Id id : destinations ) {
			stb.append( " "+id ) ;
		}
		stb.append("\n") ;
		stb.append("notYetVisited:" ) ;
		for ( Id id : notYetVisited ) {
			stb.append( " "+id ) ;
		}
		return stb.toString() ;
	}
	
//	private static int wrnCnt = 0 ;
		
	ChoiceSet(ApproximationLevel approximationLevel, Scenario scenario) {
		this.approximationLevel = approximationLevel;
		this.network = scenario.getNetwork() ;
		this.config = scenario.getConfig() ;
		this.scenario = scenario ;
		
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
	
	public Id getWeightedRandomChoice(int actlegIndex, ActivityFacilities facilities,
			ScoringFunctionAccumulator scoringFunction, Plan plan, ReplanningContext replanningContext, double pKVal) {
				
		TreeMap<Double, Id> map = this.createReducedChoiceSetWithScores(actlegIndex, facilities, scoringFunction, plan, replanningContext);
				
		/* yyyyyy the same seed for every agent????? kai, jan'13
		 * 
		 * corrected, thx.
		 * Probably this error was not dramatic as the cs is different for every agent anyway.
		 */
		Random random = new Random((long) (Long.MAX_VALUE / replanningContext.getIteration() * pKVal));
		
		/* yyyyyy what is this supposed to do??? kai, jan'13
		 * 
		 * a couple of random draws to come to the "chaotic" region
		 */
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
		 */
		Id id = map.get(map.firstKey());		
		for (Entry<Double, Id> entry : map.entrySet()) {
	        if (entry.getKey() > randomScore + 0.000000000000000001) {
	        	id = entry.getValue();
	        }
	    }
		// yyyyyy looks to me like: "the last facility with a double value larger than the random number is returned.  If this returns null,
		// then the first entry in the choice set is returned."  But why???  And why the same randomScore for all agents???  kai, jan'13

		return id;
	}
	
	private TreeMap<Double,Id> createReducedChoiceSetWithScores(int actlegIndex, ActivityFacilities facilities, ScoringFunctionAccumulator scoringFunction,
			Plan plan, ReplanningContext replanningContext) {
		TravelTime travelTime = replanningContext.getTravelTimeCalculator() ; 
		TravelDisutility travelCost = replanningContext.getTravelCostCalculator() ;
		TripRouter router = replanningContext.getTripRouterFactory().createTripRouter() ;
		
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
		// (The Dijkstra algo does not care if the start time is approximated or exact.)  kai, jan'13
		
		// ---
		
		// Handling duplicates. This was may the source for (small) random fluctuations
		// yyyy which duplicates?  and how are they handled?  kai, jan'13
		//
		// that was not the problem of the fluctuations. alternatives with the same score are deterministically 
		// handled now by including the id for compareTo evaluation.
		// comment should be removed.
		ArrayList<ScoredAlternative> list = new ArrayList<ScoredAlternative>();						
		double largestValue = Double.NEGATIVE_INFINITY; 
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
					leastCostPathCalculatorForward, leastCostPathCalculatorBackward, router, this.approximationLevel);
			
			// not needed anymore
			// yyyy why not?  kai, jan'13
			//scoringFunction.finish();
			
			double score = scoringFunction.getScore();
			scoringFunction.reset();
									
			if (score > largestValue) {
				largestValue = score;
				facilityIdWithLargestScore = destinationId;
			}
			list.add(new ScoredAlternative(score, destinationId));
		}	
		TreeMap<Double,Id> mapCorrected = this.generateReducedChoiceSet(list);
		if (mapCorrected.size() > 0) {
			return mapCorrected;
		}
		else  {	
			/* yyyyyy how is this supposed to happen at all?  kai, jan'13
			 * 
			 * If the choice set is empty (for example due to sampling) use the current activity location.
			 * TODO: add a mechanism to at least keep, a minimal number of alternatives (minimally the chosen 
			 * location of the previous iteration. Then this "if" can go.
			 */
			TreeMap<Double,Id> mapTmp = new TreeMap<Double,Id>();
			mapTmp.put(1.1, facilityIdWithLargestScore);
			return mapTmp;
		}
	}
	
	private double getTotalScore(ArrayList<ScoredAlternative> list) {
		double totalScore = 0.0;
		
		int nrElements = Math.min(list.size(), this.numberOfAlternatives);
		
		for (int index = 0; index < nrElements; index++)  {
			ScoredAlternative sa = list.get(index);
			totalScore += sa.getScore() + this.getOffset(list, nrElements);
		}
		return totalScore;
	}
	
	private double getOffset(ArrayList<ScoredAlternative> list, int nrElements) {
		double smallestScore = list.get(nrElements - 1).getScore();
		return Math.min(smallestScore, 0.0) * (-1.0); // if smallest score is negative, then add offsets!
	}
	
	/*
	 * Use offsets to only have positive scores for normalization.
	 * I do not scale/transform with exp() due to numerical problems.
	 */
	private TreeMap<Double,Id> generateReducedChoiceSet(ArrayList<ScoredAlternative> list) {
		/* 
		 * list is given here in descending order -> see compareTo in ScoredAlternative
		 */		
		Collections.sort(list);
		double totalScore = this.getTotalScore(list);
		
		int nrElements = Math.min(list.size(), this.numberOfAlternatives);
		
		TreeMap<Double,Id> mapNormalized = new TreeMap<Double,Id>(java.util.Collections.reverseOrder());
		double sumScore = 0.0;
		for (int index = 0; index < nrElements; index++)  {
			ScoredAlternative sa = list.get(index);
			sumScore += (sa.getScore() + this.getOffset(list, nrElements)) / totalScore;				
			mapNormalized.put(sumScore , sa.getAlternativeId());	
		}
		return mapNormalized;
	}
			
	void adaptAndScoreTimes(PlanImpl plan, int actlegIndex, PlanImpl planTmp, ScoringFunctionAccumulator scoringFunction, 
			LeastCostPathCalculator leastCostPathCalculatorForward, LeastCostPathCalculator leastCostPathCalculatorBackward, TripRouter router, 
			ApproximationLevel approximationLevelTmp  ) {
		PlanTimesAdapter adapter = new PlanTimesAdapter(approximationLevelTmp , leastCostPathCalculatorForward, leastCostPathCalculatorBackward, 
				router, this.scenario);
		adapter.adaptTimesAndScorePlan(plan, actlegIndex, planTmp, scoringFunction);
	}
}
