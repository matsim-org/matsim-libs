package playground.kai.usecases.scoringfunctionlistener;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction.ActivityScoring;
import org.matsim.core.scoring.SumScoringFunction.AgentStuckScoring;
import org.matsim.core.scoring.SumScoringFunction.ArbitraryEventScoring;
import org.matsim.core.scoring.SumScoringFunction.BasicScoring;
import org.matsim.core.scoring.SumScoringFunction.LegScoring;
import org.matsim.core.scoring.SumScoringFunction.MoneyScoring;
import org.matsim.core.utils.misc.Time;

public final class MySumScoringFunction implements ScoringFunction {

	private static Logger log = Logger.getLogger(MySumScoringFunction.class);

	private ArrayList<BasicScoring> basicScoringFunctions = new ArrayList<BasicScoring>();
	private ArrayList<ActivityScoring> activityScoringFunctions = new ArrayList<ActivityScoring>();
	private ArrayList<MoneyScoring> moneyScoringFunctions = new ArrayList<MoneyScoring>();
	private ArrayList<LegScoring> legScoringFunctions = new ArrayList<LegScoring>();
	private ArrayList<AgentStuckScoring> agentStuckScoringFunctions = new ArrayList<AgentStuckScoring>();
	private ArrayList<ArbitraryEventScoring> arbtraryEventScoringFunctions = new ArrayList<ArbitraryEventScoring>() ;

	private KNScoringFunctionListener listener;

	MySumScoringFunction( KNScoringFunctionListener listener ) {
		this.listener = listener ;
	}

	public MySumScoringFunction(Person person, MyScoringFunctionListener listener2) {
			// TODO Auto-generated constructor stub
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
		for (LegScoring legScoringFunction : legScoringFunctions) {
			legScoringFunction.handleLeg(leg);
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
	public void handleEvent(Event event) {
		for ( ArbitraryEventScoring eventScoringFunction : this.arbtraryEventScoringFunctions ) {
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
			if (log.isTraceEnabled()) {
				log.trace("Contribution of scoring function: " + basicScoringFunction.getClass().getName() + " is: " + contribution);
			}
			if ( Double.isNaN( contribution ) ) {
				// I consider this dangerous enough to justify a crash. If somebody has strong arguments for NaN scores,
				// one might change this to "log.error(...)". td june 15
				throw new RuntimeException( "Contribution of scoring function: " + basicScoringFunction.getClass().getName() + " is NaN! Behavior with NaN scores is undefined." );
			}
			score += contribution;
		}
		return score;
	}

	public void addScoringFunction(BasicScoring scoringFunction) {
		basicScoringFunctions.add(scoringFunction);

		if (scoringFunction instanceof ActivityScoring) {
			activityScoringFunctions.add((ActivityScoring) scoringFunction);
		}

		if (scoringFunction instanceof AgentStuckScoring) {
			agentStuckScoringFunctions.add((AgentStuckScoring) scoringFunction);
		}

		if (scoringFunction instanceof LegScoring) {
			legScoringFunctions.add((LegScoring) scoringFunction);
		}

		if (scoringFunction instanceof MoneyScoring) {
			moneyScoringFunctions.add((MoneyScoring) scoringFunction);
		}

		if (scoringFunction instanceof ArbitraryEventScoring ) {
			this.arbtraryEventScoringFunctions.add((ArbitraryEventScoring) scoringFunction) ;
		}

	}


}
