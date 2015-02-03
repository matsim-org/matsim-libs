package playground.artemc.scoring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction.ActivityScoring;
import org.matsim.core.scoring.SumScoringFunction.AgentStuckScoring;
import org.matsim.core.scoring.SumScoringFunction.ArbitraryEventScoring;
import org.matsim.core.scoring.SumScoringFunction.BasicScoring;
import org.matsim.core.scoring.SumScoringFunction.LegScoring;
import org.matsim.core.scoring.SumScoringFunction.MoneyScoring;
import org.matsim.core.utils.misc.Time;

import playground.artemc.scoring.functions.PersonalScoringParameters;

public class DisaggregatedSumScoringFunction implements ScoringFunction {
	
	private static Logger log = Logger.getLogger(DisaggregatedSumScoringFunction.class);
	
	private PersonalScoringParameters params = null;
	private ArrayList<BasicScoring> basicScoringFunctions = new ArrayList<BasicScoring>();
	private ArrayList<ActivityScoring> activityScoringFunctions = new ArrayList<ActivityScoring>();
	private ArrayList<MoneyScoring> moneyScoringFunctions = new ArrayList<MoneyScoring>();
	private Map<String, LegScoring> legScoringFunctions = new HashMap<String, LegScoring>();
	private ArrayList<AgentStuckScoring> agentStuckScoringFunctions = new ArrayList<AgentStuckScoring>();
	private ArrayList<ArbitraryEventScoring> arbitraryEventScoringFunctions = new ArrayList<ArbitraryEventScoring>() ;
	
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
		legScoringFunctions.get(leg.getMode()).handleLeg(leg);
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
	public void handleEvent(Event event) {
		for ( ArbitraryEventScoring eventScoringFunction : this.arbitraryEventScoringFunctions ) {
			eventScoringFunction.handleEvent(event) ;
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


}
