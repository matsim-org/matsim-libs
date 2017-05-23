package playground.balac.induceddemand.strategies.activitychainmodifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.locationchoice.utils.PlanUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.router.ActivityWrapperFacility;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.Facility;

/**
 * 
 * @author balacm
 *
 */

public class NeighboursCreator {
	private final static Set<String> priamryActivities = new HashSet<String>(Arrays.asList("work", "work_sector3",
			"home_1", "home_2", "education_primary", "education_secondary", "education_kindergarten", "education_higher", "secondary"));
	private final StageActivityTypes stageActivityTypes;
	private QuadTree<ActivityFacility> shopFacilityQuadTree;
	private QuadTree<ActivityFacility> leisureFacilityQuadTree;
	private Scenario scenario;
	private LeastCostPathCalculator pathCalculator;
	private ScoringFunctionFactory scoringFunctionFactory;
	private final static  Logger logger = Logger.getLogger(NeighboursCreator.class);
	private ScoringParametersForPerson parametersForPerson;
	private final TripRouter routingHandler;
	private final ActivityFacilities facilities;

	private boolean allowSplittingWorkActivity = false;
	private HashMap scoreChange;
	private Map<Id<Person>, Set<String>> perPersonAllActivities;

	private int level = 1;
	
	public NeighboursCreator(StageActivityTypes stageActivityTypes,
			QuadTree<ActivityFacility> shopFacilityQuadTree, QuadTree<ActivityFacility> leisureFacilityQuadTree,
			Scenario scenario, LeastCostPathCalculator pathCalculator, 
			ScoringFunctionFactory scoringFunctionFactory, HashMap scoreChange, 
			ScoringParametersForPerson parametersForPerson, final TripRouter routingHandler, final ActivityFacilities facilities){
		this.shopFacilityQuadTree = shopFacilityQuadTree;
		this.leisureFacilityQuadTree = leisureFacilityQuadTree;
		this.scenario = scenario;
		this.stageActivityTypes = stageActivityTypes;
		this.pathCalculator = pathCalculator;
		this.scoringFunctionFactory = scoringFunctionFactory;
		this.scoreChange = scoreChange;
		this.parametersForPerson = parametersForPerson;
		this.routingHandler = routingHandler;
		this.facilities = facilities;
	}

	public void findBestNeighbour(Plan plan) {
		List<Plan> newPlans = new LinkedList<Plan>();
		
		newPlans.addAll(getAllChainsWthRemoving(plan));
		//newPlans.addAll(getAllChainsWthSwapping(plan));
		newPlans.addAll(getAllChainsWthInserting(plan));
		
		if (plan.getPerson().getId().toString().equals("10355_2"))
			System.out.println("");
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
				//this is used for the analysis later on the estimation precision
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
		if (plan.getPlanElements().size() != 1)
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
			
			if (plan.getPlanElements().size() == 1) {
				((Activity)plan.getPlanElements().get(0)).setStartTime(0.0);
				((Activity)plan.getPlanElements().get(0)).setEndTime(24.0 * 3600.0);

			}
			else {
			
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
							//time += 1.0;
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
	}
	

	/**
	 * Creates all chains with one swap of the activities in the original plan.
	 * Not allowing swaps if it leads to having same consecutive activities.
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
				
				//don't swap if the activities are the same or if it would lead to 
				//having same consecutive activities
				
				
				if (!act1.getType().equals(act2.getType()) &&  
						!( act1.getType().equals( tNew.get(innerIndex + 1).getType() ) &&
								NeighboursCreator.priamryActivities.contains( act1.getType() ) ) &&
						!( act1.getType().equals( tNew.get(innerIndex - 1).getType() ) &&
								NeighboursCreator.priamryActivities.contains(act1.getType() ) ) && 
						!( act2.getType().equals( tNew.get(outerIndex + 1).getType() )  &&
								NeighboursCreator.priamryActivities.contains(act2.getType() ) ) && 
						!( act2.getType().equals( tNew.get(outerIndex - 1).getType() ) &&
								NeighboursCreator.priamryActivities.contains(act2.getType() ) ) ) {
				
					double time = 0.0;				
					
					int index1 = newPlan.getPlanElements().indexOf(act1);
					int index2 = newPlan.getPlanElements().indexOf(act2);
					
					newPlan.getPlanElements().set(index1, act2);
					newPlan.getPlanElements().set(index2, act1);					
					
					Activity previousActivity = tNew.get(outerIndex - 1);				
					
					double duration1 = act1.getEndTime() - previousActivity.getEndTime()
							- ((Leg)newPlan.getPlanElements().get(index1 - 1)).getTravelTime(); 
					double duration2 = act2.getEndTime() - tNew.get(innerIndex - 1).getEndTime() 
							- ((Leg)newPlan.getPlanElements().get(index2 - 1)).getTravelTime();					
					
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
					
					double prevTime = previousActivity.getEndTime() + travelTime +  duration1;
					act2.setEndTime(time);					
					
					startCoord = act2.getCoord();
					endCoord = ((Activity)newPlan.getPlanElements().get(index1 + 2)).getCoord();
					//the leg that is after the activity with a lower index
					Leg nextLeg = (Leg) newPlan.getPlanElements().get(index1 + 1);
					prevTime += nextLeg.getTravelTime();
					travelTime = estimateTravelTime(startCoord, endCoord, 
							plan.getPerson(), nextLeg.getDepartureTime(), nextLeg.getMode());
					nextLeg.setTravelTime(travelTime);
					if (nextLeg.getRoute() != null) 
						nextLeg.getRoute().setTravelTime(travelTime);
					time += travelTime;
					
					//if the swapped activities are after each other we don't have to do anything,
					//otherwise adapt the travel time of the leg before the activity with a larger index
					//also adapt the end times of the activities in between 
					if (innerIndex > outerIndex + 1) {
						
						for (int i = outerIndex + 1; i < innerIndex; i++) {
							
							Activity currentActivity = tNew.get(i);
							double durationCurrent = currentActivity.getEndTime() - prevTime;
							prevTime += currentActivity.getEndTime();
							currentActivity.setEndTime(time + durationCurrent);
							time += durationCurrent;
							
							Leg leg = (Leg) newPlan.getPlanElements().get(newPlan.getPlanElements().indexOf(currentActivity) + 1);
							double ttLeg = leg.getTravelTime();
							prevTime += ttLeg;
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
				
					newPlans.add(newPlan);
				}				
			}			
		}
		return newPlans;		
	}
	
	private Set<String> findPossibleInsertActivities(Plan plan) {
		//String actTypes = (String) this.scenario.getPopulation().getPersonAttributes().getAttribute(plan.getPerson().getId().toString(),
		//		"activities");
		String[] allActTypes = new String[1];			
		allActTypes[0] = "shopping";
		//allActTypes[1] = "secondary";

		Set<String> possibleActivities = new HashSet<>(Arrays.asList(allActTypes));
		/*for (PlanElement pe : plan.getPlanElements()) {
			
			if (pe instanceof Activity) {
				possibleActivities.remove(((Activity) pe).getType());
			}			
		}*/
		
		return possibleActivities;		
	}
	
	private List<Plan> getAllChainsWthInserting(Plan plan) {

		List<Plan> newPlans = new LinkedList<Plan>();
		PopulationFactory pf = scenario.getPopulation().getFactory();
		Network network = (Network)scenario.getNetwork();
		Person person = plan.getPerson();

		//get all the activity types that this person would like to do during the day
		// that are not mandatory activities (work, education)
		Set<String> allActTypes = findPossibleInsertActivities(plan);

		List<Activity> t = TripStructureUtils.getActivities(plan, this.stageActivityTypes);

		for (int index = 1; index < t.size(); index++) {

			int actIndex = plan.getPlanElements().indexOf(t.get(index));

			for (String actType:allActTypes) {
				
				//===no point to insert a mandatory activity next to another mandatory activity===
				
				if ( (t.get(index - 1).getType().equals(actType) && 
						NeighboursCreator.priamryActivities.contains(actType) ) ||
						(t.get(index).getType().equals(actType) && 
						NeighboursCreator.priamryActivities.contains(actType) ) )
					continue;
				
				
				Plan newPlan = PlanUtils.createCopy(plan);
				newPlan.setPerson(plan.getPerson());
				
				Activity newActivity = null;
				Link startLink = null;
				Coord newActivityCoord = null;
				Activity previousActivity = t.get(index - 1);
				
				//TODO: find a better way to get arrivalTime
				
				double arrivalTime = previousActivity.getEndTime();
				double durationNewActivity = 0.0;
				if (actType.startsWith("home")) {					
					Activity primaryActivity = getPersonHomeLocation(t);					
					startLink = network.getLinks().get(primaryActivity.getLinkId());
					newActivityCoord = primaryActivity.getCoord();					
					Id<ActivityFacility> facilityId = primaryActivity.getFacilityId();
					Id<Link> startLinkId = primaryActivity.getLinkId();
					durationNewActivity = getDurationOfNewActivity(null);
					newActivity = createNewActivity(actType, newActivityCoord, facilityId, startLinkId, person, arrivalTime, durationNewActivity);
					
				}
				else {					
					ActivityFacility actFacility = findActivityLocation(actType, 
							t.get(index - 1).getCoord(), t.get(index).getCoord());					
					
					startLink = NetworkUtils.getNearestLinkExactly(network,actFacility.getCoord());
					newActivityCoord = actFacility.getCoord();
					Id<ActivityFacility> facilityId = actFacility.getId();
					Id<Link> startLinkId =startLink.getId();
					durationNewActivity = getDurationOfNewActivity(null);
					newActivity = createNewActivity(actType, newActivityCoord, facilityId, startLinkId, person, arrivalTime, durationNewActivity);
				}	
				//place the new activity in the newPlan at the right position
				newPlan.getPlanElements().add(actIndex, newActivity);
				
				Leg legBeforeInsertedActivity = ( (Leg) newPlan.getPlanElements().get(actIndex - 1) );
				String modeOfTheLegToBeInserted = legBeforeInsertedActivity.getMode();
								
				//adding the leg between the inserted activity and the next one
				Leg newLeg = pf.createLeg(modeOfTheLegToBeInserted);
				newPlan.getPlanElements().add(actIndex + 1, newLeg);

				int sizeNewPlan = newPlan.getPlanElements().size();
				
				//TODO: time move should also include the travel times of the legs
				
				for (int i = actIndex + 1; i < sizeNewPlan - 2; i++) {
					
					if (newPlan.getPlanElements().get(i) instanceof Activity) {
						((Activity)newPlan.getPlanElements().get(i)).setEndTime(((Activity)newPlan.getPlanElements().get(i)).getEndTime() + durationNewActivity);
					}
					
				}
				
				final List<Trip> trips = TripStructureUtils.getTrips( newPlan , routingHandler.getStageActivityTypes() );

				for (Trip oldTrip : trips) {
					
					final List<? extends PlanElement> newTrip =
							routingHandler.calcRoute(
									routingHandler.getMainModeIdentifier().identifyMainMode( oldTrip.getTripElements() ),
									toFacility( oldTrip.getOriginActivity() ),
									toFacility( oldTrip.getDestinationActivity() ),
									calcEndOfActivity( oldTrip.getOriginActivity() , newPlan ),
									newPlan.getPerson() );
					//putVehicleFromOldTripIntoNewTripIfMeaningful(oldTrip, newTrip);
						TripRouter.insertTrip(
								newPlan, 
								oldTrip.getOriginActivity(),
								newTrip,
								oldTrip.getDestinationActivity());
					
				}
				
				newPlans.add(newPlan);
			}			
		}			
		return newPlans;		
	}
		
	private Activity createNewActivity(String actType, Coord coord, Id<ActivityFacility> facilityId,
			Id<Link> startLinkId, Person person, double arrivalTime, double duration) {
		PopulationFactory pf = scenario.getPopulation().getFactory() ;
		ScoringParameters params = parametersForPerson.getScoringParameters( person );

		Activity newActivity = pf.createActivityFromCoord(actType,
				coord);	
		
		newActivity.setEndTime(arrivalTime + duration);

		newActivity.setFacilityId(facilityId);
		newActivity.setLinkId(startLinkId);
		
		return newActivity;
	}
	
	private double getDurationOfNewActivity(Activity activity) {
		
		return MatsimRandom.getRandom().nextDouble() * 3600.0;		
	}

	private List<Plan> getAllChainsWthRemoving(Plan plan) {
		List<Plan> newPlans = new LinkedList<Plan>();
		
		List<Activity> t = TripStructureUtils.getActivities(plan, this.stageActivityTypes);

		//nothing to remove
		if (t.size() == 1 || t.size() == 2)
			return newPlans;
		
		//look at all the activities (not including first and last) and try to remove it
		for (int index = 1; index < t.size() - 1; index++) {
		
			Plan newPlan = PlanUtils.createCopy(plan);
			
			newPlan.setPerson(plan.getPerson());
			
			if (NeighboursCreator.priamryActivities.contains(t.get(index).getType()))
				//===don't remove primary activities
				continue;
			
			int actIndex = plan.getPlanElements().indexOf(t.get(index));
			double durationRemovedActivity = 0.0;
			//if (t.get(index).getMaximumDuration() != Time.UNDEFINED_TIME)
			//	durationRemovedActivity = t.get(index).getMaximumDuration();
			//else
			Leg previousLeg = ((Leg) newPlan.getPlanElements().get(actIndex - 1));

			durationRemovedActivity = t.get(index).getEndTime() - t.get(index - 1).getEndTime() - previousLeg.getRoute().getTravelTime();
			if (durationRemovedActivity < 0.0)
				durationRemovedActivity = 0.0;
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
			boolean ind = true;
			while (ind) {
				
				if (newPlan.getPlanElements().get(actIndex) instanceof Activity) {
					
					if (!((Activity)newPlan.getPlanElements().get(actIndex)).getType().equals("pt interaction"))
						ind = false;
					else
						newPlan.getPlanElements().remove(actIndex);			

				}
				else
					newPlan.getPlanElements().remove(actIndex);					
			}
			
			//setting all trips to the same mode in order to save some time figuring out which mode can be used here
			if (!previousLeg.getMode().equals(nextLeg.getMode()) && !(!previous && !next)) {
				String previousLegMode = previousLeg.getMode();
				for(PlanElement pe : newPlan.getPlanElements()) {
					
					if (pe instanceof Leg)
						((Leg) pe).setMode(previousLegMode);
				}
				
			}		
			
			/*check if after removing we have two same activities next to each other
			 * that are of type work, education or home
			 */
			/*if (t.get(index - 1).getType().equals(t.get(index + 1).getType()) 
					 && (NeighboursCreator.priamryActivities.contains(t.get(index - 1).getType()))) {
				
				double initialEndTime = t.get(index + 1).getEndTime();
				newPlan.getPlanElements().remove(actIndex);
				if (index != t.size() - 2)
					newPlan.getPlanElements().remove(actIndex);
				else
					newPlan.getPlanElements().remove(actIndex - 1);

				if (initialEndTime != Time.UNDEFINED_TIME)
					((Activity)newPlan.getPlanElements().get(actIndex - 2)).setEndTime(initialEndTime - durationRemovedActivity);
				else
					((Activity)newPlan.getPlanElements().get(actIndex - 2)).setEndTime(Time.UNDEFINED_TIME);
				
			}*/
			//update traveltimes of the legs
			final List<Trip> trips = TripStructureUtils.getTrips( newPlan , routingHandler.getStageActivityTypes() );

			for (Trip oldTrip : trips) {
				
				final List<? extends PlanElement> newTrip =
						routingHandler.calcRoute(
								routingHandler.getMainModeIdentifier().identifyMainMode( oldTrip.getTripElements() ),
								toFacility( oldTrip.getOriginActivity() ),
								toFacility( oldTrip.getDestinationActivity() ),
								calcEndOfActivity( oldTrip.getOriginActivity() , newPlan ),
								newPlan.getPerson() );
				//putVehicleFromOldTripIntoNewTripIfMeaningful(oldTrip, newTrip);
					TripRouter.insertTrip(
							newPlan, 
							oldTrip.getOriginActivity(),
							newTrip,
							oldTrip.getDestinationActivity());
				
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
			Network network = (Network)scenario.getNetwork();
			final Coord coord = startCoord;
			Link startLink = NetworkUtils.getNearestLinkExactly(network,coord);
			final Coord coord1 = endCoord;
			Link endLink = NetworkUtils.getNearestLinkExactly(network,coord1);
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
	
	
	private void createRoute(Activity originActivity, Activity destinationActivity, Person person, double now, Trip oldTrip, Plan plan) {
		
		Route r;
		
		double travelTime = 0.0;
	
		final List<? extends PlanElement> newTrip =
				routingHandler.calcRoute(
						routingHandler.getMainModeIdentifier().identifyMainMode( oldTrip.getTripElements() ),
						toFacility( originActivity ),
						toFacility( destinationActivity ),
						originActivity.getEndTime(),
						person );

		TripRouter.insertTrip(
				plan, 
				oldTrip.getOriginActivity(),
				newTrip,
				oldTrip.getDestinationActivity());	
		
	}
	
	private Facility toFacility(final Activity act) {
		if (  (act.getLinkId() == null && act.getCoord() == null)  // yyyy this used to be || instead of && --???  kai, jun'16
				&& facilities != null
				&& !facilities.getFacilities().isEmpty()) {
			// use facilities only if the activity does not provide the required fields.
			// yyyyyy Seems to me that the Access/EgressRoutingModule only needs either link or coord to start from.  So we only go
			// to facilities if neither is provided.  --  This may, however, be at odds of how it is done in the AERoutingModule, so we 
			// need to conceptually sort this out!!  kai, jun'16
			return facilities.getFacilities().get( act.getFacilityId() );
		}
		return new ActivityWrapperFacility( act );
	}

	private static double calcEndOfActivity(
			final Activity activity,
			final Plan plan) {
		if (activity.getEndTime() != Time.UNDEFINED_TIME) return activity.getEndTime();

		// no sufficient information in the activity...
		// do it the long way.
		// XXX This is inefficient! Using a cache for each plan may be an option
		// (knowing that plan elements are iterated in proper sequence,
		// no need to re-examine the parts of the plan already known)
		double now = 0;

		for (PlanElement pe : plan.getPlanElements()) {
			now = updateNow( now , pe );
			if (pe == activity) return now;
		}

		throw new RuntimeException( "activity "+activity+" not found in "+plan.getPlanElements() );
	}
	
	private static double updateNow(
			final double now,
			final PlanElement pe) {
		if (pe instanceof Activity) {
			Activity act = (Activity) pe;
			double endTime = act.getEndTime();
			double startTime = act.getStartTime();
			double dur = (act instanceof Activity ? act.getMaximumDuration() : Time.UNDEFINED_TIME);
			if (endTime != Time.UNDEFINED_TIME) {
				// use fromAct.endTime as time for routing
				return endTime;
			}
			else if ((startTime != Time.UNDEFINED_TIME) && (dur != Time.UNDEFINED_TIME)) {
				// use fromAct.startTime + fromAct.duration as time for routing
				return startTime + dur;
			}
			else if (dur != Time.UNDEFINED_TIME) {
				// use last used time + fromAct.duration as time for routing
				return now + dur;
			}
			else {
				throw new RuntimeException("activity has neither end-time nor duration." + act);
			}
		}
		double tt = ((Leg) pe).getTravelTime();
		return now + (tt != Time.UNDEFINED_TIME ? tt : 0);
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
				
				else {
					if (!((Activity)pe).getType().equals("pt interaction"))
				
						scoringFunction.handleActivity((Activity) pe);
				}
				
			}
			scoringFunction.finish();
			double score = scoringFunction.getScore();
			if (plan.getPlanElements().size() == 1)
				plan.setScore(0.0);
			else
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
		if (actType.startsWith("secondary"))
			return (ActivityFacility)leisureFacilityQuadTree.getClosest(coord.getX(), coord.getY());		

		else if (actType.startsWith("shop"))
		
			return (ActivityFacility)shopFacilityQuadTree.getClosest(coord.getX(), coord.getY());		
		else 
			throw new NullPointerException("The activity type: " + actType + " ,is not known!");
		
	}
	
}
