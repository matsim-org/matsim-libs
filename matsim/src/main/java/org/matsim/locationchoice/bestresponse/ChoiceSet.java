package org.matsim.locationchoice.bestresponse;

import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.Random;
import java.util.TreeMap;
import java.util.Vector;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.ScoringFunctionAccumulator;

public class ChoiceSet {
		
	// *************************************
	private final double exponent = 3.0;
	private int numberOfAlternatives = 30;
	private int approximationLevel = 0;	
	// *************************************
	
	private List<Id> destinations = new Vector<Id>();
	private List<Id> notYetVisited = new Vector<Id>();
	private PlansCalcRoute router = null;
	private Network network = null;
	private Config config;
		
	public ChoiceSet(int approximationLevel, PlansCalcRoute router, Network network, Config config) {
		this.approximationLevel = approximationLevel;
		this.router = router;
		this.network = network;
		this.config = config;
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
	
	public Id getWeightedRandomChoice(int actlegIndex, Person person,
			Coord coordPre, Coord coordPost, ActivityFacilities facilities, Random random, 
			ScoringFunctionAccumulator scoringFunction, Plan plan, TravelTime travelTime, TravelCost travelCost) {
				
		SortedMap<Double, Id> map = this.createChoiceSet(actlegIndex, person, coordPre, coordPost, facilities, scoringFunction, plan, travelTime, travelCost);
		
		// score 0 is included as random range = 0.0d (inclusive) to 1.0d (exclusive)
		// TODO: Do I have to modify the seed here by the iteration number (i.e., do we in every iteration chose the same value)?
		double randomScore = random.nextDouble();
		
		Id id = map.get(map.firstKey());
		for (Entry<Double, Id> entry : map.entrySet()) {
	        if (entry.getKey() > randomScore + 0.000000000000000001) {
	        	id = entry.getValue();
	        }
	    }
		return id;
	}
	
	private SortedMap<Double,Id> createChoiceSet(int actlegIndex, Person person,
			Coord coordPre, Coord coordPost, ActivityFacilities facilities, ScoringFunctionAccumulator scoringFunction,
			Plan plan, TravelTime travelTime, TravelCost travelCost) {
		
		Activity act = (Activity) plan.getPlanElements().get(actlegIndex);
		
		DijkstraMultipleDestinationsFactory dijkstraFactory = new DijkstraMultipleDestinationsFactory();
		LeastCostPathCalculator leastCostPathCalculatorForward = dijkstraFactory.createPathCalculator(network, travelCost, travelTime);
				
		Activity previousActivity = ((PlanImpl)plan).getPreviousActivity(((PlanImpl)plan).
				getPreviousLeg(act));
		Node fromNode = network.getLinks().get(previousActivity.getLinkId()).getToNode();
		((ForwardDijkstraMultipleDestinations)leastCostPathCalculatorForward).calcLeastCostTree(fromNode, previousActivity.getEndTime());
		
		dijkstraFactory.setType("backward");
		LeastCostPathCalculator leastCostPathCalculatorBackward = dijkstraFactory.createPathCalculator(network, travelCost, travelTime);
		((BackwardDijkstraMultipleDestinations)leastCostPathCalculatorBackward).setEstimatedStartTime(act.getEndTime());
		
		// TODO: adapt startTime here: --------------------------------------------------
		Activity nextActivity = ((PlanImpl)plan).getNextActivity(((PlanImpl)plan).
				getNextLeg(act));
		fromNode = network.getLinks().get(nextActivity.getLinkId()).getToNode();
		((BackwardDijkstraMultipleDestinations)leastCostPathCalculatorBackward).calcLeastCostTree(fromNode, -1.0);
		
		// Handling duplicates. This was may the source for (small) random fluctuations
		List<ScoredAlternative> list = new Vector<ScoredAlternative>();						
		// if no epsilons are used!
		double largestValue = -1.0 * Double.MAX_VALUE;
		Id facilityIdWithLargestScore = act.getFacilityId();
		
		for (Id destinationId : this.destinations) {
			// tentatively set 
			((ActivityImpl)act).setFacilityId(destinationId);
			((ActivityImpl)act).setCoord(facilities.getFacilities().get(destinationId).getCoord());
			((ActivityImpl)act).setLinkId(((NetworkImpl) this.network).getNearestLink(facilities.getFacilities().get(destinationId).getCoord()).getId());
			
			scoringFunction.reset();
			PlanImpl planTmp = new PlanImpl();
			planTmp.copyPlan(plan);
			this.adaptAndScoreTimes((PlanImpl)plan,  actlegIndex,  planTmp, scoringFunction,
					leastCostPathCalculatorForward, leastCostPathCalculatorBackward, this.approximationLevel);
			scoringFunction.finish();
			double score = scoringFunction.getScore();
									
			if (score > largestValue) {
				largestValue = score;
				facilityIdWithLargestScore = destinationId;
			}
			list.add(new ScoredAlternative(score, destinationId));
		}	
		// find the sum of the scores to normalize scores
		Collections.sort(list);
		double totalScore = this.getTotalScore(list, (largestValue < 0.0));
		SortedMap<Double,Id> mapCorrected = this.generateReducedChoiceSet(list, totalScore, (largestValue < 0.0));
				
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
	
	private double getTotalScore(List<ScoredAlternative> list, boolean negativeLargestValue) {
		int n = 0;
		double totalScore = 0.0;
		for (ScoredAlternative sa : list) {
			if (n >= numberOfAlternatives) break;			
			double score = Math.pow(sa.getScore(), exponent);
			if (negativeLargestValue) {
				// if we only have negative values -> change sign of score
				score *= -1.0;
			}
			if (score > 0.0) {
				totalScore += score;
			}
			n++;
		}
		return totalScore;
	}
	
	private TreeMap<Double,Id> generateReducedChoiceSet(List<ScoredAlternative> list, double totalScore, boolean negativeLargestValue) {
		TreeMap<Double,Id> mapNormalized = new TreeMap<Double,Id>(java.util.Collections.reverseOrder());
		int n = 0;
		double sumScore = 0.0;
		for (ScoredAlternative sa : list) {
			if (n >= numberOfAlternatives) break;
			double score = Math.pow(sa.getScore(), exponent);
			
			if (negativeLargestValue) {
				// if we only have negative values -> change sign of score
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
			
	public void adaptAndScoreTimes(PlanImpl plan, int actlegIndex, PlanImpl planTmp, ScoringFunctionAccumulator scoringFunction, 
			LeastCostPathCalculator leastCostPathCalculatorForward, LeastCostPathCalculator leastCostPathCalculatorBackward, int approximationLevel) {
		PlanTimesAdapter adapter = new PlanTimesAdapter(approximationLevel, leastCostPathCalculatorForward, leastCostPathCalculatorBackward, this.network, this.config);
		adapter.adaptAndScoreTimes(plan, actlegIndex, planTmp, scoringFunction, router);
	}
}
