package playground.artemc.analysis;

import org.apache.log4j.Logger;
import org.jfree.util.Log;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.PtConstants;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import playground.artemc.heterogeneity.scoring.DisaggregatedSumScoringFunction;
import playground.artemc.heterogeneity.scoring.PersonalScoringFunctionFactory;
import playground.artemc.heterogeneity.scoring.functions.ActivityUtilityParameters;
import playground.artemc.heterogeneity.scoring.functions.PersonalScoringParameters;
import playground.artemc.heterogeneity.scoring.functions.PersonalScoringParameters.Mode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;



public class ScheduleDelayCostHandler implements ActivityStartEventHandler, ActivityEndEventHandler, PersonArrivalEventHandler, PersonDepartureEventHandler, PersonEntersVehicleEventHandler, PersonStuckEventHandler{

	private final static Logger log = Logger.getLogger(TripAnalysisHandler.class);
	
	private double headway = 300.0;
	
	private HashMap<Id<Person>, LinkedList<String>> personActivities;
	private HashMap<Id<Person>, Activity> activities;
	private HashMap<Id<Person>, Journey> journeys;
	private HashMap<Id<Person>, Double> sdc_morning;
	private HashMap<Id<Person>, Double> sdc_evening;
	private HashMap<Id<Person>, Double> ttc_morning;
	private HashMap<Id<Person>, Double> ttc_evening;
	private HashMap<Id<Person>, LinkedList<String>> modes;
	private HashMap<Id<Person>, PersonalScoringParameters>  scoringParameters;
	private Controler controler;
	private HashSet<String> usedModes;
	private PersonalScoringFunctionFactory disScoringFactory = null;
	private Journey currentJourney;
	private ArrayList<Id<Person>> stuckedAgents;

	public ScheduleDelayCostHandler(){
		this.personActivities = new HashMap<Id<Person>, LinkedList<String>>();
		this.activities = new HashMap<Id<Person>, Activity>();
		this.journeys = new HashMap<Id<Person>, ScheduleDelayCostHandler.Journey>();
		this.ttc_morning = new HashMap<Id<Person>, Double>();
		this.ttc_evening = new HashMap<Id<Person>, Double>();
		this.sdc_morning = new HashMap<Id<Person>, Double>();
		this.sdc_evening = new HashMap<Id<Person>, Double>();
		this.modes = new HashMap<Id<Person>, LinkedList<String>>();
		this.scoringParameters = new HashMap<Id<Person>, PersonalScoringParameters>();
		this.usedModes = new HashSet<String>();
		this.stuckedAgents = new ArrayList<Id<Person>>();
	}

	public class Journey{
		private boolean nextEnterVehicleIsFirstOfTrip = true ;
		private boolean nextStartPtLegIsFirstOfTrip = true ;
		private boolean currentLegIsPtLeg = false;
		private double lastActivityEndTime = Time.UNDEFINED_TIME ;
		private double score;
		private double noDelayScore;
		private Leg currentLeg;
		private Leg currentLegFromPlan;
		private String mode = "";

	}


	@Override
	public void reset(int iteration) {
		personActivities.clear();
		activities.clear();
		journeys.clear();
		sdc_morning.clear();
		sdc_evening.clear();
		ttc_morning.clear();
		ttc_evening.clear();
		modes.clear();
		stuckedAgents.clear();
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {

		Id<Person> personId = event.getPersonId();
		Journey thisJourney = journeys.get(personId);

		if(!personId.toString().startsWith("pt")){

			//Create new leg for the person
			journeys.get(personId).currentLeg = new LegImpl(event.getLegMode());
			journeys.get(personId).currentLeg.setDepartureTime(event.getTime());

			if(!journeys.get(personId).mode.equals(TransportMode.pt)){
				journeys.get(personId).mode = event.getLegMode();
			}

			//Calcualte free flow travel time for the corresponding Leg from the selected plan 
			int numberOfActivities = personActivities.get(personId).size();
            PlanElement planElement = controler.getScenario().getPopulation().getPersons().get(personId).getSelectedPlan().getPlanElements().get(2*numberOfActivities-1);
			if(planElement instanceof Leg){
				journeys.get(personId).currentLegFromPlan = (Leg) planElement;	
				journeys.get(personId).currentLegFromPlan.setTravelTime(getFreeSpeedTravelTime((Leg) planElement));
				journeys.get(personId).currentLegFromPlan.setDepartureTime(journeys.get(personId).lastActivityEndTime);
				journeys.get(personId).currentLeg.setRoute(((Leg) planElement).getRoute());
			}
			else{
				log.error("This shouldn't happen! Something worg with getting current Leg-Element from Selected Plan!");
			}		

			//Keep track of transfers and pt-constant (same as CharyparNagelLegScoring.java)
			journeys.get(personId).currentLegIsPtLeg = TransportMode.pt.equals(event.getLegMode());
			if (journeys.get(personId).currentLegIsPtLeg) {
				if (!journeys.get(personId).nextStartPtLegIsFirstOfTrip ) {
					journeys.get(personId).score -= scoringParameters.get(personId).modeParams.get(TransportMode.pt).constant ;
					// (yyyy deducting this again, since is it wrongly added above.  should be consolidated; this is so the code
					// modification is minimally invasive.  kai, dec'12)
				}
				journeys.get(personId).nextStartPtLegIsFirstOfTrip = false ;
			}
		}
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		Id<Person> personId = event.getPersonId();
		Journey thisJourney = journeys.get(personId);

		if(!personId.toString().startsWith("pt")){

			Double bestArrivalTime = journeys.get(personId).currentLegFromPlan.getDepartureTime() + journeys.get(personId).currentLegFromPlan.getTravelTime();
			Double legScore = calcLegScore(journeys.get(personId).currentLeg.getDepartureTime(),event.getTime(), journeys.get(personId).currentLeg, personId);
			Double legNoDelayScore = calcLegScore(journeys.get(personId).currentLegFromPlan.getDepartureTime(),bestArrivalTime , journeys.get(personId).currentLegFromPlan, personId);

			if(legScore>legNoDelayScore){
				legNoDelayScore = legScore;
			}
			
			if((event.getLegMode().equals(TransportMode.transit_walk) || event.getLegMode().equals(TransportMode.walk)) && !legScore.equals(legNoDelayScore)){
				log.warn("Weired!!! This shouldn't happen! Walk cost are always as planned. ");			
			}

			if(legScore==null || legNoDelayScore==null){
				log.warn("Some score is NULL! This shouldn't happen! ");	
			}

			journeys.get(personId).score += legScore;
			journeys.get(personId).noDelayScore += legNoDelayScore;
			//System.out.println(journeys.get(perosnId).currentLeg.getMode()+","+legScore+";"+legNoDelayScore);
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		Id<Person> perosnId = event.getPersonId();
		if(!perosnId.toString().startsWith("pt")){
			if(journeys.get(perosnId).currentLegIsPtLeg) 
				if ( !journeys.get(perosnId).nextEnterVehicleIsFirstOfTrip ) {
					// all vehicle entering after the first triggers the disutility of line switch:
					journeys.get(perosnId).score  += scoringParameters.get(perosnId).utilityOfLineSwitch ;
					journeys.get(perosnId).noDelayScore  += scoringParameters.get(perosnId).utilityOfLineSwitch ;
				}
			journeys.get(perosnId).nextEnterVehicleIsFirstOfTrip = false ;
			// add score of waiting, _minus_ score of travelling (since it is added in the legscoring above):
			journeys.get(perosnId).score += (event.getTime() - journeys.get(perosnId).lastActivityEndTime) * (scoringParameters.get(perosnId).marginalUtilityOfWaitingPt_s - scoringParameters.get(perosnId).modeParams.get(TransportMode.pt).marginalUtilityOfTraveling_s); 
			
			if((event.getTime() - journeys.get(perosnId).lastActivityEndTime)<(headway/2)){
				journeys.get(perosnId).noDelayScore += (event.getTime() - journeys.get(perosnId).lastActivityEndTime) * (scoringParameters.get(perosnId).marginalUtilityOfWaitingPt_s);
			}
			else{
				journeys.get(perosnId).noDelayScore += (headway/2) * (scoringParameters.get(perosnId).marginalUtilityOfWaitingPt_s);
			}
		}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		Id<Person> perosnId = event.getPersonId();
		Journey thisJourney = journeys.get(perosnId);
		if(event.getActType().equals("work")){
			ActivityImpl activity = new ActivityImpl(event.getActType(), event.getLinkId());
			activity.setFacilityId(event.getFacilityId());
			activity.setStartTime(event.getTime());
			activities.put(perosnId, activity);
		}

		if (!PtConstants.TRANSIT_ACTIVITY_TYPE.equals(event.getActType())) {

			modes.get(perosnId).add(journeys.get(perosnId).mode);		
			if(!usedModes.contains(journeys.get(perosnId).mode)){
				usedModes.add(journeys.get(perosnId).mode);
			}

			if(event.getActType().equals("work")){
				this.ttc_morning.put(perosnId, (journeys.get(perosnId).score - journeys.get(perosnId).noDelayScore));			
			}else if(event.getActType().equals("home2") || event.getActType().equals("home")){
				this.ttc_evening.put(perosnId, (journeys.get(perosnId).score - journeys.get(perosnId).noDelayScore));		
			}
			else{
				Log.error("Unknown activity type! Works only for home and work activities! ");	
			}
			journeys.remove(perosnId);
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {

		Id<Person> perosnId = event.getPersonId();

		//Save personal scoring parameters, if the person appears for the first time
		if(disScoringFactory==null){
			disScoringFactory = (PersonalScoringFunctionFactory) controler.getScoringFunctionFactory();
		}

		if(!scoringParameters.containsKey(perosnId)){
			DisaggregatedSumScoringFunction sf = (DisaggregatedSumScoringFunction) disScoringFactory.getPersonScoringFunctions().get(perosnId);
			scoringParameters.put(perosnId, sf.getParams());
		}

		//Keep track of persons activities incl. pt-interactions
		if(!personActivities.containsKey(perosnId)){
			personActivities.put(perosnId, new LinkedList<String>());
		}
		personActivities.get(perosnId).add(event.getActType());	

		if(!modes.containsKey(perosnId)){
			modes.put(perosnId, new LinkedList<String>());
		}

		if ( !PtConstants.TRANSIT_ACTIVITY_TYPE.equals(event.getActType())) {
			journeys.put(perosnId, new Journey());
			journeys.get(perosnId).nextEnterVehicleIsFirstOfTrip  = true ;
			journeys.get(perosnId).nextStartPtLegIsFirstOfTrip = true ;

		}

		journeys.get(perosnId).lastActivityEndTime = event.getTime();

		if(activities.containsKey(perosnId)){
			activities.get(perosnId).setEndTime(event.getTime());
			calcSDC(activities.get(perosnId),perosnId);
			activities.remove(perosnId);
		}
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		this.stuckedAgents.add(event.getPersonId());
	}

	private static int ccc=0 ;
	protected double calcLegScore(final double departureTime, final double arrivalTime, final Leg leg, Id<Person> personId) {
		double legScore = 0.0;
		double travelTime = arrivalTime - departureTime; // travel time in seconds	
		Mode modeParams = scoringParameters.get(personId).modeParams.get(leg.getMode());
		if (modeParams == null) {
			if (leg.getMode().equals(TransportMode.transit_walk)) {
				modeParams = scoringParameters.get(personId).modeParams.get(TransportMode.walk);
			} else {
				modeParams = scoringParameters.get(personId).modeParams.get(TransportMode.other);
			}
		}
		legScore += travelTime * (modeParams.marginalUtilityOfTraveling_s - scoringParameters.get(personId).marginalUtilityOfPerforming_s);
		if (modeParams.marginalUtilityOfDistance_m != 0.0
				|| modeParams.monetaryDistanceCostRate != 0.0) {
			Route route = leg.getRoute();
			double dist = route.getDistance(); // distance in meters
			if ( Double.isNaN(dist) ) {
				if ( ccc<10 ) {
					ccc++ ;
					Logger.getLogger(this.getClass()).warn("distance is NaN. Will make score of this plan NaN. Possible reason: Simulation does not report " +
							"a distance for this trip. Possible reason for that: mode is teleported and router does not " +
							"write distance into plan.  Needs to be fixed or these plans will die out.") ;
					if ( ccc==10 ) {
						Logger.getLogger(this.getClass()).warn(Gbl.FUTURE_SUPPRESSED) ;
					}
				}
			}
			legScore += modeParams.marginalUtilityOfDistance_m * dist;
			legScore += modeParams.monetaryDistanceCostRate * scoringParameters.get(personId).marginalUtilityOfMoney * dist;
		}
		legScore += modeParams.constant;
		// (yyyy once we have multiple legs without "real" activities in between, this will produce wrong results.  kai, dec'12)
		// (yy NOTE: the constant is added for _every_ pt leg.  This is not how such models are estimated.  kai, nov'12)
		return legScore;
	}


	protected void calcSDC(final Activity act, Id<Person> personId) {

		double arrivalTime= act.getStartTime();
		double departureTime = act.getEndTime();
		double activityStart = arrivalTime;
		double activityEnd = departureTime;

		ActivityUtilityParameters actParams = scoringParameters.get(personId).utilParams.get(act.getType());
		if (actParams == null) {
			throw new IllegalArgumentException("acttype \"" + act.getType() + "\" is not known in utility parameters " +
					"(module name=\"planCalcScore\" in the config file).");
		}

		double openingTime = actParams.getOpeningTime();
		double closingTime = actParams.getClosingTime();

		double scheduleDelayMorning = 0.0;
		double scheduleDelayEvening = 0.0;

		if (actParams.isScoreAtAll()) {
			/* Calculate the times the agent actually performs the
			 * activity.  The facility must be open for the agent to
			 * perform the activity.  If it's closed, but the agent is
			 * there, the agent must wait instead of performing the
			 * activity (until it opens).
			 *
			 *                                             Interval during which
			 * Relationship between times:                 activity is performed:
			 *
			 *      O________C A~~D  ( 0 <= C <= A <= D )   D...D (not performed)
			 * A~~D O________C       ( A <= D <= O <= C )   D...D (not performed)
			 *      O__A+++++C~~D    ( O <= A <= C <= D )   A...C
			 *      O__A++D__C       ( O <= A <= D <= C )   A...D
			 *   A~~O++++++++C~~D    ( A <= O <= C <= D )   O...C
			 *   A~~O+++++D__C       ( A <= O <= D <= C )   O...D
			 *
			 * Legend:
			 *  A = arrivalTime    (when agent gets to the facility)
			 *  D = departureTime  (when agent leaves the facility)
			 *  O = openingTime    (when facility opens)
			 *  C = closingTime    (when facility closes)
			 *  + = agent performs activity
			 *  ~ = agent waits (agent at facility, but not performing activity)
			 *  _ = facility open, but agent not there
			 *
			 * assume O <= C
			 * assume A <= D
			 */


			if ((openingTime >=  0) && (arrivalTime < openingTime)) {
				activityStart = openingTime;
			}
			if ((closingTime >= 0) && (closingTime < departureTime)) {
				activityEnd = closingTime;
			}
			if ((openingTime >= 0) && (closingTime >= 0)
					&& ((openingTime > departureTime) || (closingTime < arrivalTime))) {
				// agent could not perform action
				activityStart = departureTime;
				activityEnd = departureTime;
			}
			double duration = activityEnd - activityStart;

			// disutility if too early
			if (arrivalTime < activityStart) {
				// agent arrives to early, has to wait
				scheduleDelayMorning += (scoringParameters.get(personId).marginalUtilityOfWaiting_s - scoringParameters.get(personId).marginalUtilityOfPerforming_s) * (activityStart - arrivalTime);
			}

			// disutility if too late
			double latestStartTime = actParams.getLatestStartTime();
			if ((latestStartTime >= 0) && (activityStart > latestStartTime)) {
				scheduleDelayMorning += scoringParameters.get(personId).marginalUtilityOfLateArrival_s * (activityStart - latestStartTime);
			}


			// disutility if stopping too early
			double earliestEndTime = actParams.getEarliestEndTime();
			if ((earliestEndTime >= 0) && (activityEnd < earliestEndTime)) {
				scheduleDelayEvening += scoringParameters.get(personId).marginalUtilityOfEarlyDeparture_s * (earliestEndTime - activityEnd);
			}

			// disutility if going to away to late
			if (activityEnd < departureTime) {
				scheduleDelayEvening += (scoringParameters.get(personId).marginalUtilityOfWaiting_s - scoringParameters.get(personId).marginalUtilityOfPerforming_s) *  (departureTime - activityEnd);
			}

			//			// disutility if duration was too short
			//			double minimalDuration = actParams.getMinimalDuration();
			//			if ((minimalDuration >= 0) && (duration < minimalDuration)) {
			//				tmpScore += sf.getParams().marginalUtilityOfEarlyDeparture_s * (minimalDuration - duration);
			//			}
		}

		this.sdc_morning.put(personId,scheduleDelayMorning);
		this.sdc_evening.put(personId,scheduleDelayEvening);
	}

	public double getFreeSpeedTravelTime(Leg leg) {
		Double freeSpeedTravelTime = 0.0;
		if(leg.getMode().equals(TransportMode.car)){
			NetworkRoute route = (NetworkRoute) leg.getRoute();
			for(Id<Link> link:route.getLinkIds()){
                freeSpeedTravelTime += (controler.getScenario().getNetwork().getLinks().get(link).getLength() / controler.getScenario().getNetwork().getLinks().get(link).getFreespeed());
			}
		}
		else if(leg.getMode().equals(TransportMode.pt)){
			ExperimentalTransitRoute route = (ExperimentalTransitRoute) leg.getRoute();	

			TransitStopFacility accessStop = controler.getScenario().getTransitSchedule().getFacilities().get(route.getAccessStopId());
			TransitStopFacility egressStop = controler.getScenario().getTransitSchedule().getFacilities().get(route.getEgressStopId());

			double scheduleDeparture = controler.getScenario().getTransitSchedule().getTransitLines().get(route.getLineId()).getRoutes().get(route.getRouteId()).getStop(accessStop).getDepartureOffset();
			double scheduleArrival = controler.getScenario().getTransitSchedule().getTransitLines().get(route.getLineId()).getRoutes().get(route.getRouteId()).getStop(egressStop).getArrivalOffset();
			freeSpeedTravelTime = scheduleArrival - scheduleDeparture;
		}
		else if(leg.getMode().equals(TransportMode.transit_walk) || leg.getMode().equals(TransportMode.walk)){
			freeSpeedTravelTime = Math.ceil(leg.getTravelTime());
		}
		return freeSpeedTravelTime;
	}

	public HashMap<Id<Person>, Double> getTTC_morning() {
		return ttc_morning;
	}

	public HashMap<Id<Person>, Double> getTTC_evening() {
		return ttc_evening;
	}

	public HashMap<Id<Person>,Double> getSDC_morning() {
		return sdc_morning;
	}

	public HashMap<Id<Person>,Double> getSDC_evening() {
		return sdc_evening;
	}

	public HashMap<Id<Person>, LinkedList<String>> getModes() {
		return modes;
	}

	public void setControler(Controler controler) {
		this.controler = controler;
	}

	public HashSet<String> getUsedModes() {
		return usedModes;
	}

	public ArrayList<Id<Person>> getStuckedAgents() {
		return stuckedAgents;
	}
	
	public static Logger getLog() {
		return log;
	}


}
