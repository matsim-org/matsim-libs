package playground.artemc.heterogeneity.scoring;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction.*;
import org.matsim.core.utils.misc.Time;
import playground.artemc.crowding.events.CrowdedPenaltyEvent;
import playground.artemc.crowding.events.PersonCrowdednessEvent;
import playground.artemc.crowding.newScoringFunctions.PersonScore;
import playground.artemc.crowding.newScoringFunctions.ScoreTracker;
import playground.artemc.crowding.newScoringFunctions.VehicleScore;
import playground.artemc.heterogeneity.scoring.functions.PersonalScoringParameters;

import java.util.*;
import java.util.Map.Entry;

public class DisaggregatedSumScoringFunctionForCrowding implements ScoringFunction {
	
	private static Logger log = Logger.getLogger(DisaggregatedSumScoringFunctionForCrowding.class);
	
	private PersonalScoringParameters params = null;
	private ArrayList<BasicScoring> basicScoringFunctions = new ArrayList<BasicScoring>();
	private ArrayList<ActivityScoring> activityScoringFunctions = new ArrayList<ActivityScoring>();
	private ArrayList<MoneyScoring> moneyScoringFunctions = new ArrayList<MoneyScoring>();
	private Map<String, LegScoring> legScoringFunctions = new HashMap<String, LegScoring>();
	private ArrayList<AgentStuckScoring> agentStuckScoringFunctions = new ArrayList<AgentStuckScoring>();
	private ArrayList<ArbitraryEventScoring> arbitraryEventScoringFunctions = new ArrayList<ArbitraryEventScoring>() ;

	private final static double loadFactorBeta = ((0.66 + 0.48) * 6.0 / 11.0) / 3600.0; //beta_lf_b, Tirachini, p.44, ratio of travel time and crowded travel time: 6/11

	private double score;
	private ScoreTracker scoreTracker;
	private EventsManager events;
	private Id personId;
	private boolean internalization;

	double marginalUtilityOfMoney;
	double opportunityCostOfPtTravel;

	public DisaggregatedSumScoringFunctionForCrowding(EventsManager events, ScoreTracker scoreTracker, Scenario scenario, boolean internalizationOfComfortDisutility) {
		this.events = events;
		this.scoreTracker = scoreTracker;
		this.opportunityCostOfPtTravel = - scenario.getConfig().planCalcScore().getModes().get(TransportMode.pt).getMarginalUtilityOfTraveling() + scenario.getConfig().planCalcScore().getPerforming_utils_hr();
		this.marginalUtilityOfMoney = scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney();
		this.internalization = internalizationOfComfortDisutility;
		this.score = 0.0;
	}


	@Override
	public final void handleActivity(Activity activity) {
		double startTime = activity.getStartTime();
		double endTime = activity.getEndTime();
        if (startTime == Time.UNDEFINED_TIME && endTime != Time.UNDEFINED_TIME) {
        	for (ActivityScoring activityScoringFunction : activityScoringFunctions) {
    			activityScoringFunction.handleFirstActivity(activity);
    		}
        } else if (startTime != Time.UNDEFINED_TIME && endTime != Time.UNDEFINED_TIME) {
        	for (ActivityScoring activityScoringFunction : activityScoringFunctions) {
    			activityScoringFunction.handleActivity(activity);
    		}
        } else if (startTime != Time.UNDEFINED_TIME && endTime == Time.UNDEFINED_TIME) {
        	for (ActivityScoring activityScoringFunction : activityScoringFunctions) {
    			activityScoringFunction.handleLastActivity(activity);
    		}
        } else {
        	throw new RuntimeException("Trying to score an activity without start or end time. Should not happen."); 	
        }
    }

	@Override
    public final void handleLeg(Leg leg) {
		if(legScoringFunctions.containsKey(leg.getMode())) {
			legScoringFunctions.get(leg.getMode()).handleLeg(leg);
		}
		else{
			legScoringFunctions.get(TransportMode.other).handleLeg(leg);
		}
    }
	
	@Override
	public void addMoney(double amount) {
		for (MoneyScoring moneyScoringFunction : moneyScoringFunctions) {
			moneyScoringFunction.addMoney(amount);
		}
	}

	@Override
	public void agentStuck(double time) {
		for (AgentStuckScoring agentStuckScoringFunction : agentStuckScoringFunctions) {
			agentStuckScoringFunction.agentStuck(time);
		}
	}

	@Override
	public void finish() {
		for (BasicScoring basicScoringFunction : basicScoringFunctions) {
			basicScoringFunction.finish();
		}
	}

	/**
	 * Add the score of all functions.
	 */
	@Override
	public double getScore() {
		double score = 0.0;
		for (BasicScoring basicScoringFunction : basicScoringFunctions) {
            double contribution = basicScoringFunction.getScore();
//			if (log.isTraceEnabled()) {
//				log.trace("Contribution of scoring function: " + basicScoringFunction.getClass().getName() + " is: " + contribution);
//			}
            score += contribution;
		}

		score = score + this.score;

		return score;
	}
	
	public double getActivityTotalScore() {
		double score = 0.0;
		for (ActivityScoring scoringFunction : activityScoringFunctions) {
            double contribution = scoringFunction.getScore();
//			if (log.isTraceEnabled()) {
//				log.trace("Contribution of activity scoring function: " + scoringFunction.getClass().getName() + " is: " + contribution);
//			}
            score += contribution;
		}
		return score;
	}
	
	public double getLegTotalScore() {
		double score = 0.0;
		for(LegScoring legScoring:legScoringFunctions.values()) {
			double contribution = legScoring.getScore();
//			if (log.isTraceEnabled()) {
//				log.trace("Contribution of leg scoring function: " + legScoring.getClass().getName() + " is: " + contribution);
//			}
            score += contribution;
		}
		score = score + this.score;
		return score;
	}
	
	public double getMoneyTotalScore() {
		double score = 0.0;
		for (MoneyScoring scoringFunction : moneyScoringFunctions) {
            double contribution = scoringFunction.getScore();
//			if (log.isTraceEnabled()) {
//				log.trace("Contribution of money scoring function: " + scoringFunction.getClass().getName() + " is: " + contribution);
//			}
            score += contribution;
		}
		return score;
	}
	
	public double getStuckScore() {
		double score = 0.0;
		for (AgentStuckScoring scoringFunction : agentStuckScoringFunctions) {
            double contribution = scoringFunction.getScore();
//			if (log.isTraceEnabled()) {
//				log.trace("Contribution of stuck scoring function: " + scoringFunction.getClass().getName() + " is: " + contribution);
//			}
            score += contribution;
		}
		return score;
	}
	
	public Map<String, Double> getLegScores() {
		Map<String, Double> scoreMap = new HashMap<String, Double>();
		for(Entry<String, LegScoring> modeEntry:legScoringFunctions.entrySet()) {
			double contribution = modeEntry.getValue().getScore();
//			if (log.isTraceEnabled()) {
//				log.trace("Contribution of leg scoring function: " + legScoringFunctions.get(modeEntry.getKey()).getClass().getName() + " is: " + contribution);
//			}
            scoreMap.put(modeEntry.getKey(), contribution);
		}
		return scoreMap;
	}

	/**
	 * add the scoring function the list of functions, it implemented the
	 * interfaces.
	 * 
	 * @param scoringFunction
	 */
	public void addScoringFunction(BasicScoring scoringFunction) {
		basicScoringFunctions.add(scoringFunction);

		if (scoringFunction instanceof ActivityScoring) {
			activityScoringFunctions.add((ActivityScoring) scoringFunction);
		}

		if (scoringFunction instanceof AgentStuckScoring) {
			agentStuckScoringFunctions.add((AgentStuckScoring) scoringFunction);
		}

		if (scoringFunction instanceof MoneyScoring) {
			moneyScoringFunctions.add((MoneyScoring) scoringFunction);
		}
		
		if (scoringFunction instanceof ArbitraryEventScoring ) {
			this.arbitraryEventScoringFunctions.add((ArbitraryEventScoring) scoringFunction) ;
		}

	}
	
	public void addLegScoringFunction(String mode, LegScoring scoringFunction) {
		basicScoringFunctions.add(scoringFunction);
		legScoringFunctions.put(mode, scoringFunction);
	}

	public PersonalScoringParameters getParams() {
		return params;
	}

	public void setParams(PersonalScoringParameters params) {
		this.params = params;
	}

	@Override
	public void handleEvent(Event event) {
		/*From original code*/
		for ( ArbitraryEventScoring eventScoringFunction : this.arbitraryEventScoringFunctions ) {
			eventScoringFunction.handleEvent(event) ;
		}

		/*Crowding implementation*/
		if( event instanceof ActivityEndEvent && personId == null ){
			ActivityEndEvent aee = (ActivityEndEvent) event;
			personId = aee.getPersonId();
		}

		if ( event instanceof PersonCrowdednessEvent) {
			PersonCrowdednessEvent pce = (PersonCrowdednessEvent) event;

			if(!scoreTracker.getPersonScores().containsKey(pce.getPersonId())){
				scoreTracker.getPersonScores().put(pce.getPersonId(), new PersonScore(pce.getPersonId()));
			}

			if(!scoreTracker.getVehicleExternalities().containsKey(pce.getVehicleId())){
				scoreTracker.getVehicleExternalities().put(pce.getVehicleId(), new VehicleScore(pce.getVehicleId()));
			}


			Id facilityId = pce.getFacilityId();
			Id vehicleId = pce. getVehicleId();
			Id personId = pce.getPersonId();

			double scoringTime = pce.getTime();
			double travelTime = pce.getTravelTime();
			double dwellTime = pce.getDwellTime();
			boolean isSitting = pce.isSitting();
			int sitters = pce.getSitters();
			int standees = pce.getStandees();
			double loadFactor = pce.getloadFactor();

			double penalty = 0;
			double standingPenalty = 0;
			double seatingPenalty = 0;
			double externality = 0;
			double monetaryExternality = 0;

			// Use of the Tirachini's formula

			// The crowding disutilities arise when the load factor reach 60%
			seatingPenalty += (travelTime + dwellTime) * loadFactorBeta * Math.max(loadFactor - 0.6, 0);

			standingPenalty += (travelTime + dwellTime) * loadFactorBeta * Math.max(loadFactor - 0.6, 0);

			if (isSitting)
			{
				penalty = seatingPenalty;

			}
			else
			{
				penalty = standingPenalty;

			}
			score -= penalty;

			// Calculate Externalities
			externality = ((standingPenalty * standees + seatingPenalty * sitters) / (standees + sitters));
			monetaryExternality = externality / marginalUtilityOfMoney;

			if (events != null)
			{
				// If we have an EventManager, report the penalty calculated to it
				// "getTime", "getVehicleId" and "externality" have beed added at the initial class
				events.processEvent(new CrowdedPenaltyEvent(pce.getTime(), personId, vehicleId, penalty, externality));

				if(internalization){
					//Paying for externality
					PersonMoneyEvent e = new PersonMoneyEvent(pce.getTime(), personId, -monetaryExternality);
					events.processEvent(e);
				}

			}

			// FILLING THE SCORETRACKER
			// Set the total crowdedness and the sum of the externalities produced during the simulation
			scoreTracker.setTotalCrowdednessUtility(scoreTracker.getTotalCrowdednessUtility()-penalty);
			scoreTracker.setTotalCrowdednessExternalityCharges(scoreTracker.getTotalCrowdednessExternalityCharges() - monetaryExternality);
			scoreTracker.setTotalMoneyPaid(scoreTracker.getTotalMoneyPaid() - monetaryExternality);

			// Set an objet PersonScore() per agent (score, time when scoring, trip duration, facility of alighting,
			// crowding utility, externalitites and money paid)
			PersonScore personScore = scoreTracker.getPersonScores().get(personId);
			personScore.setScoringTime(scoringTime);
			personScore.setTripDuration(personScore.getTripDuration() + (travelTime + dwellTime));
			personScore.setFacilityOfAlighting(facilityId);
			personScore.setCrowdingUtility(personScore.getCrowdingUtility()-penalty);
			personScore.setCrowdednessExternalityCharge(personScore.getCrowdednessExternalityCharge() - monetaryExternality);
			personScore.setMoneyPaid(personScore.getMoneyPaid() - monetaryExternality);

			// Set an objet VehicleExternalities() per vehicle (facilities with boarding/alighting, crowding penalty pro facility,
			// externalities pro facility)
			VehicleScore vehicleScore = scoreTracker.getVehicleExternalities().get(vehicleId);
			if(!vehicleScore.getFacilityId().containsKey(vehicleId)) {
				Set<Id> facilities = new HashSet<Id>();
				facilities.add(facilityId);
				scoreTracker.getVehicleExternalities().get(vehicleId).getFacilityId().put(vehicleId, facilities);
				vehicleScore.getFacilityTime().put(facilityId, scoringTime);
				vehicleScore.getDwellTime().put(facilityId, dwellTime);
				vehicleScore.getVehicleCrowdingCost().put(facilityId, -penalty);
				vehicleScore.getVehicleCrowdednessExternalityCharge().put(facilityId, monetaryExternality);
				vehicleScore.getVehicleMoneyPaid().put(facilityId, monetaryExternality);
			}

			else if(!vehicleScore.getVehicleCrowdingCost().containsKey(facilityId)){
				vehicleScore.getFacilityId().get(vehicleId).add(facilityId);
				vehicleScore.getFacilityTime().put(facilityId, scoringTime);
				vehicleScore.getDwellTime().put(facilityId, dwellTime);
				vehicleScore.getVehicleCrowdingCost().put(facilityId, -penalty);
				vehicleScore.getVehicleCrowdednessExternalityCharge().put(facilityId, monetaryExternality);
				vehicleScore.getVehicleMoneyPaid().put(facilityId, monetaryExternality);
			}

			else {
				vehicleScore.getFacilityTime().put(facilityId, scoringTime);
				vehicleScore.getDwellTime().put(facilityId, dwellTime);
				vehicleScore.getVehicleCrowdingCost().put(facilityId, vehicleScore.getVehicleCrowdingCost().get(facilityId)-penalty);
				vehicleScore.getVehicleCrowdednessExternalityCharge().put(facilityId, vehicleScore.getVehicleCrowdednessExternalityCharge().get(facilityId) + monetaryExternality);
				vehicleScore.getVehicleMoneyPaid().put(facilityId, vehicleScore.getVehicleMoneyPaid().get(facilityId) + monetaryExternality);
			}
		}

	}

	public ScoreTracker getScoreTracker() {
		return scoreTracker;
	}

}
