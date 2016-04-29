package playground.balac.induceddemand.strategies.activitychainmodifier;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
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
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.scoring.functions.CharyparNagelScoringParametersForPerson;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacility;

/**
 * 
 * @author balacm
 *
 */

public class NeighboursCreator {
	private final StageActivityTypes stageActivityTypes;
	private QuadTree<ActivityFacility> shopFacilityQuadTree;
	private QuadTree<ActivityFacility> leisureFacilityQuadTree;
	private Scenario scenario;
	private LeastCostPathCalculator pathCalculator;
	private ScoringFunctionFactory scoringFunctionFactory;
	private static final Logger logger = Logger.getLogger(NeighboursCreator.class);
	private CharyparNagelScoringParametersForPerson parametersForPerson;
	
	private boolean allowSplittingWorkActivity = false;
	private HashMap scoreChange;
	
	public NeighboursCreator(StageActivityTypes stageActivityTypes,
			QuadTree<ActivityFacility> shopFacilityQuadTree, QuadTree<ActivityFacility> leisureFacilityQuadTree,
			Scenario scenario, LeastCostPathCalculator pathCalculator, 
			ScoringFunctionFactory scoringFunctionFactory, HashMap scoreChange, 
			CharyparNagelScoringParametersForPerson parametersForPerson){
		this.shopFacilityQuadTree = shopFacilityQuadTree;
		this.leisureFacilityQuadTree = leisureFacilityQuadTree;
		this.scenario = scenario;
		this.stageActivityTypes = stageActivityTypes;
		this.pathCalculator = pathCalculator;
		this.scoringFunctionFactory = scoringFunctionFactory;
		this.scoreChange = scoreChange;
		this.parametersForPerson = parametersForPerson;
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
		
		//search for the plan with the highest score
		for (Plan newPlan : newPlans) {
			if (newPlan.getScore() > score) {
				bestPlan = newPlan;
				foundBetter = true;
				score = newPlan.getScore();
				//this is used for the analysis later on the estimation precission
				scoreChange.put(plan.getPerson().getId().toString(), score);
			}
			//logger.info(newPlan.getScore());
		}
		if (foundBetter)
			PlanUtils.copyFrom(bestPlan, plan);
		
		//we need to remove the end times of the activities that should not have it defined 
		updateTimes(plan);
	}
	/**
	 * Activity start time is removed for the first
	 * and end time for the last activity.
	 * 
	 * @param plan plan to be updated
	 */
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
			
			}
		}
		
		lastActivity.setEndTime(Time.UNDEFINED_TIME);
		
	}

	/**
	 * Computing the estimated start times of
	 * all the activities inside of the plans.
	 * 
	 * This should be performed before scoring.
	 * 
	 * @param newPlans plans to be updated
	 */
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
					
					if (((Activity) pe).getEndTime() != Time.UNDEFINED_TIME) {
						
						if (time < ((Activity)pe).getEndTime())
							
							time = ((Activity) pe).getEndTime();
						else  {
							//the arrival time is after the initial end time so we have to move the end time
							((Activity) pe).setEndTime(time);							
						}
					}
				}
				else {
					if (((Leg)pe).getRoute() != null) {
						double tt = ((Leg)pe).getRoute().getTravelTime();
						time += tt;
						((Leg)pe).setTravelTime(tt);
						
					}
					else {
						time += ((Leg)pe).getTravelTime();
						
					}
				}				
			}
			lastActivity.setEndTime(Time.UNDEFINED_TIME);			
		}		
	}
	

	/**
	 * Creates all chains with one swap of the activities in the original plan.
	 * Updates the end times of the activities affected by the swap.
	 * 
	 * @param plan original plan
	 * @return List<Plan> a list of all the possible plans with one swap
	 */
	private List<Plan> getAllChainsWthSwapping(Plan plan) {		
		
		List<Plan> newPlans = new LinkedList<Plan>();
		List<Activity> tOld = TripStructureUtils.getActivities(plan, this.stageActivityTypes);
		int numberOfActivities = tOld.size();
		//don't swap first and last activity
		for (int outerIndex = 1; outerIndex < numberOfActivities - 2; outerIndex++) {
			
			for(int innerIndex = outerIndex + 1; innerIndex < numberOfActivities - 1; innerIndex++) {
				Plan newPlan = PlanUtils.createCopy(plan);
				newPlan.setPerson(plan.getPerson());
				List<Activity> tNew = TripStructureUtils.getActivities(newPlan, this.stageActivityTypes);

				Activity act1 = tNew.get(outerIndex);
				Activity act2 = tNew.get(innerIndex);
				
				
				if (!act1.getType().equals(act2.getType())) {
					double time = 0.0;
					double duration1 = act1.getEndTime() - act1.getStartTime();
					double duration2 = act2.getEndTime() - act2.getStartTime();
					
					int index1 = newPlan.getPlanElements().indexOf(act1);
					int index2 = newPlan.getPlanElements().indexOf(act2);
			
					newPlan.getPlanElements().set(index1, act2);
					newPlan.getPlanElements().set(index2, act1);					
					
					Activity previousActivity = tNew.get(outerIndex - 1);				
					
					//updating the travel times for the affected trips					

					Coord startCoord = tNew.get(outerIndex - 1).getCoord();
					Coord endCoord = act2.getCoord();
					
					//the leg that is before the activity with the lower index
					Leg previousLeg = (Leg) newPlan.getPlanElements().get(index1 - 1);
					double travelTime = estimateTravelTime(startCoord, endCoord, 
							plan.getPerson(), previousLeg.getDepartureTime(), previousLeg.getMode());
					
					previousLeg.setTravelTime(travelTime);
					
					if (previousLeg.getRoute() != null) 
						previousLeg.getRoute().setTravelTime(travelTime);
					
					//updating the end times of the swapped activity
					time = previousActivity.getEndTime() + travelTime + duration2;
					act2.setEndTime(time);					
					
					startCoord = act2.getCoord();
					endCoord = ((Activity)newPlan.getPlanElements().get(index1 + 2)).getCoord();
					//the leg that is after the activity with a lower index
					Leg nextLeg = (Leg) newPlan.getPlanElements().get(index1 + 1);
					travelTime = estimateTravelTime(startCoord, endCoord, 
							plan.getPerson(), nextLeg.getDepartureTime(), nextLeg.getMode());
					nextLeg.setTravelTime(travelTime);
					if (nextLeg.getRoute() != null) 
						nextLeg.getRoute().setTravelTime(travelTime);
					time += travelTime;
					//if the swaped activities are after each other we don;t have to do anything,
					//otherwise adapt the travel time of the leg before the activity with a larger index
					if (innerIndex > outerIndex + 1) {
						
						for (int i = outerIndex + 1; i < innerIndex; i++) {
							
							Activity currentActivity = tNew.get(i);
							double durationCurrent = currentActivity.getEndTime() - currentActivity.getStartTime();
							
							currentActivity.setEndTime(time + durationCurrent);
							time += durationCurrent;
							
							Leg leg = (Leg) newPlan.getPlanElements().get(newPlan.getPlanElements().indexOf(currentActivity) + 1);
							double ttLeg = leg.getTravelTime();
							time += ttLeg;
						}						
						
						startCoord = ((Activity)newPlan.getPlanElements().get(index2 - 2)).getCoord();
						endCoord = act1.getCoord();
						previousLeg = (Leg) newPlan.getPlanElements().get(index2 - 1);
						travelTime = estimateTravelTime(startCoord, endCoord, 
								plan.getPerson(), previousLeg.getDepartureTime(), previousLeg.getMode());
						previousLeg.setTravelTime(travelTime);
						if (previousLeg.getRoute() != null) 
							previousLeg.getRoute().setTravelTime(travelTime);
						time += travelTime;
					}
							
					act1.setEndTime(time + duration1);
					startCoord = act1.getCoord();
					endCoord = tNew.get(innerIndex + 1).getCoord();
					
					//update the time after the activity with a higher index.
					nextLeg = (Leg) newPlan.getPlanElements().get(index2 + 1);
					travelTime = estimateTravelTime(startCoord, endCoord, 
							plan.getPerson(), nextLeg.getDepartureTime(), nextLeg.getMode());
					nextLeg.setTravelTime(travelTime);
					if (nextLeg.getRoute() != null) 
						nextLeg.getRoute().setTravelTime(travelTime);
					time += travelTime;
					for (int i = innerIndex + 1; i < numberOfActivities - 1; i ++) {
						Activity currentActivity = tNew.get(i);
						double durationCurrent = currentActivity.getEndTime() - currentActivity.getStartTime();
						
						currentActivity.setEndTime(time + durationCurrent);
						time += durationCurrent;
						
						Leg leg = (Leg) newPlan.getPlanElements().get(newPlan.getPlanElements().indexOf(currentActivity) + 1);
						double ttLeg = leg.getTravelTime();
						time += ttLeg;
						
					}					
					newPlans.add(newPlan);
				}				
			}			
		}
		return newPlans;		
	}
	
	private List<Plan> getAllChainsWthInserting(Plan plan) {

		List<Plan> newPlans = new LinkedList<Plan>();
		PopulationFactory pf = scenario.getPopulation().getFactory();
		NetworkImpl network = (NetworkImpl)scenario.getNetwork();
		Person person = plan.getPerson();

		//get all the activity types that this person would like to do during the day
		// that are not mandatory activities (work, education)
		String actTypes = (String) this.scenario.getPopulation().getPersonAttributes().getAttribute(plan.getPerson().getId().toString(),
				"activities");
		String[] allActTypes = actTypes.split(",");	

		List<Activity> t = TripStructureUtils.getActivities(plan, this.stageActivityTypes);

		for (int index = 1; index < t.size() - 1; index++) {

			int actIndex = plan.getPlanElements().indexOf(t.get(index));

			for (String actType:allActTypes) {
				Plan newPlan = PlanUtils.createCopy(plan);
				newPlan.setPerson(plan.getPerson());
				
				Activity newActivity = null;
				Link startLink = null;
				Coord newActivityCoord = null;
				Activity previousActivity = t.get(index - 1);
				double arrivalTime = previousActivity.getEndTime();
				
				if (actType.equals("home")) {					
					Activity primaryActivity = getPersonHomeLocation(t);					
					startLink = network.getLinks().get(primaryActivity.getLinkId());
					newActivityCoord = primaryActivity.getCoord();					
					Id<ActivityFacility> facilityId = primaryActivity.getFacilityId();
					Id<Link> startLinkId = primaryActivity.getLinkId();
					newActivity = createNewActivity(actType, newActivityCoord, facilityId, startLinkId, person, arrivalTime);									
				}
				else {					
					ActivityFacility actFacility = findActivityLocation(actType, 
							t.get(index - 1).getCoord(), t.get(index).getCoord());					
					
					startLink = network.getNearestLinkExactly(actFacility.getCoord());
					newActivityCoord = actFacility.getCoord();
					Id<ActivityFacility> facilityId = actFacility.getId();
					Id<Link> startLinkId =startLink.getId();
					newActivity = createNewActivity(actType, newActivityCoord, facilityId, startLinkId, person, arrivalTime);					
				}	
				//place the new activity in the newPlan and the right position
				newPlan.getPlanElements().add(actIndex, newActivity);
				
				Leg legAfterInsertedActivity = ( (Leg) newPlan.getPlanElements().get(actIndex + 2) );
				String modeOfTheLegToBeInserted = legAfterInsertedActivity.getMode();
				
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
				
				//adding the leg between the inserted activity and the next one
				Leg newLeg = pf.createLeg(modeOfTheLegToBeInserted);
				Coord nextActivityCoord = t.get(index).getCoord();
				
				double startOfThePreviousLeg = now;
				now = newActivity.getEndTime(); 
				
				double estimatedTravelTime = estimateTravelTime(newActivityCoord, nextActivityCoord, person, now, modeOfTheLegToBeInserted);
				newLeg.setDepartureTime(now);
				newLeg.setTravelTime(estimatedTravelTime);
				newPlan.getPlanElements().add(actIndex + 1, newLeg);
				
				//adapting the travel time of the leg before the inserted activity 				
				Coord previousActivityCoord = t.get(index - 1).getCoord();
				Leg previousLeg = (Leg) newPlan.getPlanElements().get(actIndex - 1);
				String modeOfPreviousLeg = previousLeg.getMode();
				double newPreviousLegTravelTime = estimateTravelTime(previousActivityCoord, newActivityCoord, person,
						startOfThePreviousLeg, modeOfPreviousLeg);
				if (previousLeg.getRoute() != null)
					previousLeg.getRoute().setTravelTime(newPreviousLegTravelTime);
				previousLeg.setTravelTime(newPreviousLegTravelTime);
				
				//adapting once again the end time of the new activity
				//in order to have minimum duration
				newActivity.setEndTime(newActivity.getEndTime() + newPreviousLegTravelTime); 
				newPlans.add(newPlan);
			}			
		}			
		return newPlans;		
	}
		
	private Activity createNewActivity(String actType, Coord coord, Id<ActivityFacility> facilityId,
			Id<Link> startLinkId, Person person, double arrivalTime) {
		PopulationFactory pf = scenario.getPopulation().getFactory() ;
		CharyparNagelScoringParameters params = parametersForPerson.getScoringParameters( person );

		Activity newActivity = pf.createActivityFromCoord(actType,
				coord);	
		
		newActivity.setEndTime(arrivalTime + params.utilParams.get(actType).getMinimalDuration());

		newActivity.setFacilityId(facilityId);
		newActivity.setLinkId(startLinkId);
		
		return newActivity;
	}

	private List<Plan> getAllChainsWthRemoving(Plan plan) {
		List<Plan> newPlans = new LinkedList<Plan>();
		
		List<Activity> t = TripStructureUtils.getActivities(plan, this.stageActivityTypes);

		//nothing to remove
		if (t.size() == 1 || t.size() == 2)
			return newPlans;
		
		//look at all the activities (not including first and last) and try to remove it
		for (int index = 1; index < t.size() - 2; index++) {
		
			Plan newPlan = PlanUtils.createCopy(plan);
			
			newPlan.setPerson(plan.getPerson());
			
			if ((t.get(index).getType().startsWith("work") || t.get(index).getType().startsWith("education")))
				//don't remove mandatory activities
				continue;
			int actIndex = plan.getPlanElements().indexOf(t.get(index));
			double durationRemovedActivity = 0.0;
			if (t.get(index).getMaximumDuration() != Time.UNDEFINED_TIME)
				durationRemovedActivity = t.get(index).getMaximumDuration();
			else
				durationRemovedActivity = t.get(index).getEndTime() - t.get(index - 1).getEndTime();
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
			
			//check if after removing we have two work activities next to each other
			if (t.get(index - 1).getType().startsWith("work") 
					&& t.get(index + 1).getType().startsWith("work")) {
				
				double initialEndTime = t.get(index + 1).getEndTime();
				newPlan.getPlanElements().remove(actIndex);
				newPlan.getPlanElements().remove(actIndex);
				((Activity)newPlan.getPlanElements().get(actIndex - 2)).setEndTime(initialEndTime - durationRemovedActivity);
			}
			//update traveltimes of the legs
			int i = 0;
			for (PlanElement pe : newPlan.getPlanElements()) {
				
				if (pe instanceof Leg) {
					
					Coord startCoord =  ((Activity)newPlan.getPlanElements().get(i - 1)).getCoord();
					Coord endCoord = ((Activity)newPlan.getPlanElements().get(i + 1)).getCoord();
					double trTime = estimateTravelTime(startCoord, endCoord, plan.getPerson(), ((Leg) pe).getDepartureTime(), ((Leg) pe).getMode());
					((Leg) pe).setTravelTime(trTime);
					((Leg) pe).getRoute().setTravelTime(trTime);
				}
				i++;
			}
			newPlans.add(newPlan);
		}
		return newPlans;		
	}
	/**
	 * Estimation of the travel times, using car as a network mode and
	 * teleportation for other modes.
	 * 
	 * @param startCoord
	 * @param endCoord
	 * @param person
	 * @param now
	 * @param mode
	 * @return estimated travel time
	 */
	private double estimateTravelTime(Coord startCoord, Coord endCoord, Person person, double now, String mode) {
		
		double travelTime = 0.0;
		if (mode.equals("car")) {
			NetworkImpl network = (NetworkImpl)scenario.getNetwork();
			Link startLink = network.getNearestLinkExactly(startCoord);
			Link endLink = network.getNearestLinkExactly(endCoord);
			Path path = this.pathCalculator.calcLeastCostPath(startLink.getToNode(), endLink.getFromNode(), 
					now, person, null ) ;
			travelTime = path.travelTime;
		}
		//estimate other modes travel time as they were teleportation
		else {
			
			double beelineFactor = scenario.getConfig().plansCalcRoute().getBeelineDistanceFactors().get(mode);
			double modeSpeed = scenario.getConfig().plansCalcRoute().getTeleportedModeSpeeds().get(mode);
			
			double distance = CoordUtils.calcEuclideanDistance(startCoord, endCoord);
			
			travelTime = distance * beelineFactor / modeSpeed;
			
		}
		return travelTime;
	}

	/**
	 * Scoring all the possible plans.
	 * 
	 * @param plansToScore list of all plans
	 */
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
	/**
	 * Approximate the location of the new activity by choosing the closest location
	 * at the middle point between the neighbouring activities.
	 * 
	 * @param actType type of the activity
	 * @param coordStart coordinate of the activity before
	 * @param coordEnd coordinate of the activity after
	 * @return ActivityFacility
	 */
	private ActivityFacility findActivityLocation(String actType, Coord coordStart, Coord coordEnd) {		
		
		Coord coord = CoordUtils.createCoord(( coordStart.getX() + coordEnd.getX() ) / 2.0,
				( coordStart.getY() + coordEnd.getY() ) / 2.0);
		if (actType.equals("leisure"))
			return (ActivityFacility)leisureFacilityQuadTree.getClosest(coord.getX(), coord.getY());		

		else if (actType.equals("shop"))
		
			return (ActivityFacility)shopFacilityQuadTree.getClosest(coord.getX(), coord.getY());		
		else 
			throw new NullPointerException("The activity type: " + actType + " ,is not known!");
		
	}
	
}
