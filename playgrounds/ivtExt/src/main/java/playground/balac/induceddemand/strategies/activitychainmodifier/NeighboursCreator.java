package playground.balac.induceddemand.strategies.activitychainmodifier;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.locationchoice.utils.PlanUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacility;


public class NeighboursCreator {
	private final StageActivityTypes stageActivityTypes;
	private QuadTree<ActivityFacility> shopFacilityQuadTree;
	private QuadTree<ActivityFacility> leisureFacilityQuadTree;
	private Scenario scenario;
	private LeastCostPathCalculator pathCalculator;
	private ScoringFunctionFactory scoringFunctionFactory;
	private static final Logger logger = Logger.getLogger(NeighboursCreator.class);

	public NeighboursCreator(StageActivityTypes stageActivityTypes,
			QuadTree<ActivityFacility> shopFacilityQuadTree, QuadTree<ActivityFacility> leisureFacilityQuadTree,
			Scenario scenario, LeastCostPathCalculator pathCalculator, ScoringFunctionFactory scoringFunctionFactory){
		this.shopFacilityQuadTree = shopFacilityQuadTree;
		this.leisureFacilityQuadTree = leisureFacilityQuadTree;
		this.scenario = scenario;
		this.stageActivityTypes = stageActivityTypes;
		this.pathCalculator = pathCalculator;
		this.scoringFunctionFactory = scoringFunctionFactory;
		
		
	}

	public void findBestNeighbour(Plan plan) {

		List<Plan> newPlans = new LinkedList<Plan>();
		
		newPlans.addAll(getAllChainsWthRemoving(plan));
		newPlans.addAll(getAllChainsWthSwapping(plan));
		newPlans.addAll(getAllChainsWthInserting(plan));
		
		//we need start and end times of all the activities in order to be able to score the plan
		updateTimesForScoring(newPlans);
		scoreChains(newPlans);
		
		double score = plan.getScore();
		Plan bestPlan = plan;
		boolean foundBetter = false;
		for (Plan newPlan : newPlans) {
			if (newPlan.getScore() > score) {
				bestPlan = newPlan;
				foundBetter = true;
				score = newPlan.getScore();
			}
			logger.info(newPlan.getScore());
		}
		if (foundBetter)
			PlanUtils.copyFrom(bestPlan, plan);
		
		//we need to remove the end times of the activities that should not have it defined 
		updateTimes(plan);
	}
	
	private void updateTimes(Plan plan) {
		boolean firstActivity = true;
		Activity lastActivity = null;
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				lastActivity = (Activity) pe;
				if (firstActivity) {
					((Activity) pe).setStartTime(Time.UNDEFINED_TIME);
					firstActivity = false;
				}
				if (((Activity) pe).getMaximumDuration() != Time.UNDEFINED_TIME) {
					((Activity) pe).setEndTime(Time.UNDEFINED_TIME);
				}
			}
		}
		
		lastActivity.setEndTime(Time.UNDEFINED_TIME);
		
	}

	private void updateTimesForScoring(List<Plan> newPlans) {
		
		for (Plan plan : newPlans) {
			double time = 0.0;
			boolean firstActivity = true;
			Activity lastActivity = null;
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {
					if (!firstActivity) {
						((Activity)pe).setStartTime(time);
						
					}
					lastActivity = (Activity) pe;

					firstActivity =  false;
					if (((Activity) pe).getEndTime() == Time.UNDEFINED_TIME)
						((Activity) pe).setEndTime(time + ((Activity)pe).getMaximumDuration());
					
					if (((Activity) pe).getEndTime() != Time.UNDEFINED_TIME) {
						
						if (time < ((Activity)pe).getEndTime())
							
							time = ((Activity) pe).getEndTime();
					}
					else
						time += ((Activity) pe).getMaximumDuration();					
			
				}
				else
					time += ((Leg)pe).getTravelTime();
				
				
			}
			lastActivity.setEndTime(Time.UNDEFINED_TIME);
			
		}
		
		
	}
	/**
	 * Creates all chains with one swap of the activities in the original plan
	 * @param plan original plan
	 * @return List<Plan> a list of all the possible plans with one swap
	 */
	private List<Plan> getAllChainsWthSwapping(Plan plan) {
		List<Plan> newPlans = new LinkedList<Plan>();
		NetworkImpl network = (NetworkImpl)scenario.getNetwork();
		List<Activity> tOld = TripStructureUtils.getActivities(plan, this.stageActivityTypes);

		for (int outerIndex = 1; outerIndex < tOld.size() - 2; outerIndex++) {
			
			for(int innerIndex = outerIndex + 1; innerIndex < tOld.size() - 1; innerIndex++) {
				Plan newPlan = PlanUtils.createCopy(plan);
				newPlan.setPerson(plan.getPerson());
				List<Activity> t = TripStructureUtils.getActivities(newPlan, this.stageActivityTypes);

				Activity act1 = t.get(outerIndex);
				Activity act2 = t.get(innerIndex);
				
				if (!act1.getType().equals(act2.getType())) {
					
					int index1 = newPlan.getPlanElements().indexOf(act1);
					int index2 = newPlan.getPlanElements().indexOf(act2);
			
					newPlan.getPlanElements().set(index1, act2);
					newPlan.getPlanElements().set(index2, act1);					
					
					//updating the travel times for the affected trips
					
					Link startLink = network.getLinks().get(t.get(outerIndex - 1).getLinkId());
					Link endLink = network.getLinks().get(act2.getLinkId());

					Leg previousLeg = (Leg) plan.getPlanElements().get(index1 - 1);
					double travelTime = estimateTravelTime(startLink, endLink, plan.getPerson(), previousLeg.getDepartureTime(), previousLeg.getMode());
					previousLeg.setTravelTime(travelTime);
					
					startLink = network.getLinks().get(act2.getLinkId());
					endLink = network.getLinks().get(t.get(outerIndex + 1).getLinkId());
					
					Leg nextLeg = (Leg) plan.getPlanElements().get(index1 + 1);
					travelTime = estimateTravelTime(startLink, endLink, plan.getPerson(), nextLeg.getDepartureTime(), nextLeg.getMode());
					nextLeg.setTravelTime(travelTime);
					
					if (innerIndex > outerIndex + 1) {
						
						startLink = network.getLinks().get(t.get(innerIndex - 1).getLinkId());
						endLink = network.getLinks().get(act1.getLinkId());

						previousLeg = (Leg) plan.getPlanElements().get(index2 - 1);
						travelTime = estimateTravelTime(startLink, endLink, plan.getPerson(), previousLeg.getDepartureTime(), previousLeg.getMode());
						previousLeg.setTravelTime(travelTime);
					}
					
					startLink = network.getLinks().get(act1.getLinkId());
					endLink = network.getLinks().get(t.get(innerIndex + 1).getLinkId());
					
					nextLeg = (Leg) plan.getPlanElements().get(index2 + 1);
					travelTime = estimateTravelTime(startLink, endLink, plan.getPerson(), nextLeg.getDepartureTime(), nextLeg.getMode());
					nextLeg.setTravelTime(travelTime);
					
					newPlans.add(newPlan);	

				}				
			}			
		}
		return newPlans;		
	}
	
	private List<Plan> getAllChainsWthInserting(Plan plan) {
		List<Plan> newPlans = new LinkedList<Plan>();
		PopulationFactory pf = scenario.getPopulation().getFactory() ;
		NetworkImpl network = (NetworkImpl)scenario.getNetwork();
		Person person = plan.getPerson();
	//	Set<String> insertableActivities = new TreeSet<String>();
		
		String actTypes = (String) this.scenario.getPopulation().getPersonAttributes().getAttribute(plan.getPerson().getId().toString(),
				"activities");
		String[] allActTypes = actTypes.split(",");

	//	insertableActivities.add("leisure");
	//	insertableActivities.add("shop");
	//	insertableActivities.add("home");

		List<Activity> t = TripStructureUtils.getActivities(plan, this.stageActivityTypes);

		for (int index = 1; index < t.size() - 1; index++) {

			int actIndex = plan.getPlanElements().indexOf(t.get(index));

			for (String actType:allActTypes) {
				Plan newPlan = PlanUtils.createCopy(plan);
				newPlan.setPerson(plan.getPerson());
				Activity newActivity;
				ActivityFacility actFacility;
				Link startLink;
				if (actType.equals("home")) {
					
					Activity primaryActivity = getPersonHomeLocation(t);					
					startLink = network.getLinks().get(primaryActivity.getLinkId());
					newActivity = pf.createActivityFromCoord(actType,
							primaryActivity.getCoord());
					newActivity.setMaximumDuration(3600.0);
					newActivity.setFacilityId(primaryActivity.getFacilityId());
					newActivity.setLinkId(startLink.getId());

				}
				else {
					
					actFacility = findActivityLocation(actType, 
							((Activity)plan.getPlanElements().get(actIndex)).getCoord());
					
					
					startLink = network.getNearestLinkExactly(actFacility.getCoord());
					newActivity = pf.createActivityFromCoord(actType,
							actFacility.getCoord());
					newActivity.setFacilityId(actFacility.getId());
					newActivity.setLinkId(network.getNearestLinkExactly(actFacility.getCoord()).getId());
					newActivity.setMaximumDuration(3600.0);

				}
				
				
				newPlan.getPlanElements().add(actIndex, newActivity);
				
				Leg legAfterInsertedActivity = ( (Leg) newPlan.getPlanElements().get(actIndex + 2) );
				String modeOfTheLegToBeInserted = legAfterInsertedActivity.getMode();
				Leg newLeg = pf.createLeg(modeOfTheLegToBeInserted);
				Link endLink = network.getNearestLinkExactly(t.get(index).getCoord());
				double now = 0;
				
				//estimating the time of the departure for the trip to be inserted
				//(only duration of the activities is considered and not the length of the trips)
				if (t.get(index - 1).getEndTime() != Time.UNDEFINED_TIME)
				
					now = t.get(index - 1).getEndTime();
				else {
					
					for (int i = index - 2; i > -1; i--) {
						if (t.get(i).getEndTime() != Time.UNDEFINED_TIME) {
							
							now = t.get(i).getEndTime();
							for (int j = i + 1; j < index; j++) {
								now += t.get(j).getMaximumDuration();
							}
							break;
							
							
						}
						
					}
				}
				
				double startOfThePreviousLeg = now;
				now += 3600.0; //the duration of the inserted activity
				
				double estimatedTravelTime = estimateTravelTime(startLink, endLink, person, now, modeOfTheLegToBeInserted);
				
				newLeg.setTravelTime(estimatedTravelTime);
				newPlan.getPlanElements().add(actIndex + 1, newLeg);
				
				Link startLinkPreviousLeg = network.getNearestLinkExactly(t.get(index - 1).getCoord());

				Leg previousLeg = (Leg) plan.getPlanElements().get(actIndex - 1);
				String modeOfPreviousLeg = previousLeg.getMode();
				double previousTravelTime = estimateTravelTime(startLinkPreviousLeg, startLink, person,
						startOfThePreviousLeg, modeOfPreviousLeg);
				previousLeg.setTravelTime(previousTravelTime);
				newPlans.add(newPlan);
			}
			
		}		
		
		return newPlans;		
	}
	
	private double estimateTravelTime(Link startLink, Link endLink, Person person, double now, String mode) {
		
		double travelTime = 0.0;
		if (mode.equals("car")) {
			Path path = this.pathCalculator.calcLeastCostPath(startLink.getToNode(), endLink.getToNode(), 
					now, person, null ) ;
			travelTime = path.travelTime;
		}
		else {
			double beelineFactor = scenario.getConfig().plansCalcRoute().getBeelineDistanceFactors().get(mode);
			double modeSpeed = scenario.getConfig().plansCalcRoute().getTeleportedModeSpeeds().get(mode);
			
			double distance = CoordUtils.calcEuclideanDistance(startLink.getCoord(), endLink.getCoord());
			
			travelTime = distance * beelineFactor / modeSpeed;
			
		}
		return travelTime;
	}

	private List<Plan> getAllChainsWthRemoving(Plan plan) {
		
		List<Plan> newPlans = new LinkedList<Plan>();
		
		List<Activity> t = TripStructureUtils.getActivities(plan, this.stageActivityTypes);

		
		if (t.size() == 1 || t.size() == 2)
			//nothing to remove
			return newPlans;
		//look at all the activities (not including first and last) and try to remove it
		for (int index = 1; index < t.size() - 2; index++) {
		
			Plan newPlan = PlanUtils.createCopy(plan);
			
			newPlan.setPerson(plan.getPerson());
			
			if ((t.get(index).getType().equals("work") || t.get(index).getType().equals("education")))
				//don't remove mandatory activities
				continue;
			int actIndex = plan.getPlanElements().indexOf(t.get(index));

			Leg previousLeg = ((Leg) newPlan.getPlanElements().get(actIndex - 1));
			Leg nextLeg = ((Leg) newPlan.getPlanElements().get(actIndex + 1));
			
			boolean previous = false;
			boolean next = false;
			
			if (previousLeg.getMode().equals("car") ||
					previousLeg.getMode().equals("bike"))
				previous = true;
			
			if (nextLeg.getMode().equals("car") ||
					nextLeg.getMode().equals("bike"))
				next = true;
			
			//removing the activity and the trip after that activity
			newPlan.getPlanElements().remove(actIndex);
			newPlan.getPlanElements().remove(actIndex);			
			
			//setting all trips to the same mode in order to save some time figuring out which mode can be used here
			if (!previousLeg.getMode().equals(nextLeg.getMode()) && !(!previous && !next)) {
				String previousLegMode = previousLeg.getMode();
				for(PlanElement pe : newPlan.getPlanElements()) {
					
					if (pe instanceof Leg)
						((Leg) pe).setMode(previousLegMode);
				}
				
			}					
			
			newPlans.add(newPlan);
		}
		return newPlans;		
	}
	
	private void scoreChains(List<Plan> plansToScore){
		
		for (Plan plan : plansToScore) {
			ScoringFunction scoringFunction = this.scoringFunctionFactory.createNewScoringFunction(plan.getPerson());

			for (PlanElement pe: plan.getPlanElements()) {
				
				if (pe instanceof Leg) 
					scoringFunction.handleLeg((Leg) pe);
				
				else				
					scoringFunction.handleActivity((Activity) pe);
				
			}
			scoringFunction.finish();
			double score = scoringFunction.getScore();
			plan.setScore(score);
		}
	}
	
	private Activity getPersonHomeLocation(List<Activity> allActivities) {
		
		for(Activity a : allActivities) 
			
			if (a.getType().equals("home"))
				
				return a;
		
		throw new NullPointerException("The activity type home is not known to the agent!");
	}	
	
	private ActivityFacility findActivityLocation(String actType, Coord coord) {		
		
		if (actType.equals("leisure"))
			return (ActivityFacility)leisureFacilityQuadTree.getClosest(coord.getX(), coord.getY());		

		else if (actType.equals("shop"))
		
			return (ActivityFacility)shopFacilityQuadTree.getClosest(coord.getX(), coord.getY());		
		else 
			throw new NullPointerException("The activity type: " + actType + " ,is not known!");
		
	}

}
