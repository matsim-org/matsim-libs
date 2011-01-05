package org.matsim.locationchoice.bestresponse;

import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;
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
				
		NavigableMap<Double, Id> map = this.createChoiceSet(actlegIndex, person, coordPre, coordPost, facilities, scoringFunction, plan, travelTime, travelCost);
		
		// score 0 is included as random range = 0.0d (inclusive) to 1.0d (exclusive)
		// TODO: Do I have to modify the seed here by the iteration number (i.e., do we in every iteration chose the same value)?
		double randomScore = random.nextDouble();
		return map.higherEntry(randomScore - 0.00001).getValue();
	}
	
	private NavigableMap<Double,Id> createChoiceSet(int actlegIndex, Person person,
			Coord coordPre, Coord coordPost, ActivityFacilities facilities, ScoringFunctionAccumulator scoringFunction,
			Plan plan, TravelTime travelTime, TravelCost travelCost) {
		
		Activity act = (Activity) plan.getPlanElements().get(actlegIndex);
		
		LeastCostPathCalculator leastCostPathCalculator = 
			new FullNetworkDijkstraFactory().createPathCalculator(network, travelCost, travelTime);
		
		Activity previousActivity = ((PlanImpl)plan).getPreviousActivity(((PlanImpl)plan).
				getPreviousLeg((ActivityImpl)act));
		
		Node fromNode = network.getLinks().get(previousActivity.getLinkId()).getToNode();
		double startTime = previousActivity.getEndTime();
		
		((FullNetworkDijkstra)leastCostPathCalculator).calcLeastCostTree(fromNode, startTime);
		
		NavigableMap<Double,Id> map = new TreeMap<Double,Id>();						
		// if no epsilons are used!
		double largestValue = -1.0 * Double.MAX_VALUE;
		Id facilityIdWithLargestScore = act.getFacilityId();
		
		for (Id destinationId : this.destinations) {
			// tentatively set 
			((ActivityImpl)act).setFacilityId(destinationId);
			((ActivityImpl)act).setCoord(facilities.getFacilities().get(destinationId).getCoord());
			((ActivityImpl)act).setLinkId((Id)((NetworkImpl) this.network).getNearestLink(facilities.getFacilities().get(destinationId).getCoord()).getId());
			
			scoringFunction.reset();
			PlanImpl planTmp = new PlanImpl();
			planTmp.copyPlan(plan);
			this.adaptAndScoreTimes((PlanImpl)plan,  actlegIndex,  planTmp, scoringFunction,
					leastCostPathCalculator, this.approximationLevel);
			scoringFunction.finish();
			double score = scoringFunction.getScore();
									
			if (score > largestValue) {
				largestValue = score;
				facilityIdWithLargestScore = destinationId;
			}
			map.put(score, destinationId);
		}
		// Sorting not necessary as TreeMap is sorted already
		// keys in ascending order by default -> descendingKeySet
		
		// find the sum of the scores to normalize scores
		double totalScore = this.getTotalScore(map, (largestValue < 0.0));
		NavigableMap<Double,Id> mapCorrected = this.generateReducedChoiceSet(map, totalScore, (largestValue < 0.0));
				
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
	
	private double getTotalScore(NavigableMap<Double,Id> map, boolean negativeLargestValue) {
		int n = 0;
		double totalScore = 0.0;
		for (Double key : map.descendingKeySet()) {
			if (n >= numberOfAlternatives) break;			
			double score = Math.pow(key, exponent);
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
	
	private NavigableMap<Double,Id> generateReducedChoiceSet(NavigableMap<Double,Id> map, double totalScore, boolean negativeLargestValue) {
		NavigableMap<Double,Id> mapNormalized = new TreeMap<Double,Id>();
		int n = 0;
		double sumScore = 0.0;
		for (Double key : map.descendingKeySet()) {
			if (n >= numberOfAlternatives) break;
			double score = Math.pow(key, exponent);
			
			if (negativeLargestValue) {
				// if we only have negative values -> change sign of score
				score *= -1.0;
			}			
			if (score > 0.0) {
				sumScore += (score / totalScore);				
				mapNormalized.put(sumScore , map.get(key));
			}
			n++;	
		}
		return mapNormalized;
	}
			
	public void adaptAndScoreTimes(PlanImpl plan, int actlegIndex, PlanImpl planTmp, ScoringFunctionAccumulator scoringFunction, 
			LeastCostPathCalculator leastCostPathCalculator, int approximationLevel) {
		PlanTimesAdapter adapter = new PlanTimesAdapter(approximationLevel, leastCostPathCalculator, this.network, this.config);
		adapter.adaptAndScoreTimes(plan, actlegIndex, planTmp, scoringFunction, router);
	}
}
