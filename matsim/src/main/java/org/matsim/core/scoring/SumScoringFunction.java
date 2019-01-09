package org.matsim.core.scoring;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.utils.misc.Time;

import java.util.ArrayList;

public final class SumScoringFunction implements ScoringFunction {

	public interface BasicScoring {
		void finish();
		double getScore();
	}

	public interface ActivityScoring extends BasicScoring {
		void handleFirstActivity(final Activity act);
		void handleActivity(final Activity act);
		void handleLastActivity(final Activity act);
	}

	public interface LegScoring extends BasicScoring {
		void handleLeg(final Leg leg);
	}

	public interface TripScoring extends BasicScoring{
		void handleTrip(final Trip trip) ;
	}

	public interface MoneyScoring extends BasicScoring {
		void addMoney(final double amount);
	}

	public interface AgentStuckScoring extends BasicScoring {
		void agentStuck(final double time);
	}

	/**
	 * NOTE: Despite its somewhat misleading name, only Events that at the same time implement HasPersonId are passed 
	 * through this interface.  This excludes, in particular, LinkEnterEvent and LinkLeaveEvent.  This was done for performance reasons,
	 * since passing those events to all handlers imposes a significant additional burden.  See comments in 
	 * and implementation of the handleEvent 
	 * method in {@link EventsToScore}. (yyyyyy Those comments, and possibly also the events filtering, have gone
	 * from EventsToScore in commit e718809884cac6a86bdc35ea2a03c10195d37c69 .  I don't know if the performance consequences
	 * were tested.) 
	 * 
	 * @author nagel
	 */
	public interface ArbitraryEventScoring extends BasicScoring {
		void handleEvent( final Event event ) ;
	}

	private static Logger log = Logger.getLogger(SumScoringFunction.class);

	private ArrayList<BasicScoring> basicScoringFunctions = new ArrayList<>();
	private ArrayList<ActivityScoring> activityScoringFunctions = new ArrayList<>();
	private ArrayList<MoneyScoring> moneyScoringFunctions = new ArrayList<>();
	private ArrayList<LegScoring> legScoringFunctions = new ArrayList<>();
	private ArrayList<TripScoring> tripScoringFunctions = new ArrayList<>();
	private ArrayList<AgentStuckScoring> agentStuckScoringFunctions = new ArrayList<>();
	private ArrayList<ArbitraryEventScoring> arbitraryEventScoringFunctions = new ArrayList<>() ;

	@Override
	public final void handleActivity(Activity activity) {
		double startTime = activity.getStartTime();
		double endTime = activity.getEndTime();
		if (Time.isUndefinedTime(startTime) && !Time.isUndefinedTime(endTime)) {
			for (ActivityScoring activityScoringFunction : this.activityScoringFunctions) {
				activityScoringFunction.handleFirstActivity(activity);
			}
		} else if (!Time.isUndefinedTime(startTime) && !Time.isUndefinedTime(endTime)) {
			for (ActivityScoring activityScoringFunction : this.activityScoringFunctions) {
				activityScoringFunction.handleActivity(activity);
			}
		} else if (!Time.isUndefinedTime(startTime) && Time.isUndefinedTime(endTime)) {
			for (ActivityScoring activityScoringFunction : this.activityScoringFunctions) {
				activityScoringFunction.handleLastActivity(activity);
			}
		} else {
			throw new RuntimeException("Trying to score an activity without start or end time. Should not happen."); 	
		}
	}

	@Override
	public final void handleLeg(Leg leg) {
		for (LegScoring legScoringFunction : this.legScoringFunctions) {
			legScoringFunction.handleLeg(leg);
		}
	}

	@Override
	public final void handleTrip(Trip trip) {
		for (TripScoring tripScoringFunction : this.tripScoringFunctions) {
			tripScoringFunction.handleTrip(trip);
		}
	}

	@Override
	public void addMoney(double amount) {
		for (MoneyScoring moneyScoringFunction : this.moneyScoringFunctions) {
			moneyScoringFunction.addMoney(amount);
		}
	}

	@Override
	public void agentStuck(double time) {
		for (AgentStuckScoring agentStuckScoringFunction : this.agentStuckScoringFunctions) {
			agentStuckScoringFunction.agentStuck(time);
		}
	}

	@Override
	public void handleEvent(Event event) {
		for (ArbitraryEventScoring eventScoringFunction : this.arbitraryEventScoringFunctions) {
			eventScoringFunction.handleEvent(event) ;
		}
	}

	@Override
	public void finish() {
		for (BasicScoring basicScoringFunction : this.basicScoringFunctions) {
			basicScoringFunction.finish();
		}
	}

	/**
	 * Add the score of all functions.
	 */
	@Override
	public double getScore() {
		double score = 0.0;
		for (BasicScoring basicScoringFunction : this.basicScoringFunctions) {
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
		this.basicScoringFunctions.add(scoringFunction);

		if (scoringFunction instanceof ActivityScoring) {
			this.activityScoringFunctions.add((ActivityScoring) scoringFunction);
		}

		if (scoringFunction instanceof AgentStuckScoring) {
			this.agentStuckScoringFunctions.add((AgentStuckScoring) scoringFunction);
		}

		if (scoringFunction instanceof LegScoring) {
			this.legScoringFunctions.add((LegScoring) scoringFunction);
		}

		if (scoringFunction instanceof TripScoring) {
			this.tripScoringFunctions.add((TripScoring) scoringFunction);
		}

		if (scoringFunction instanceof MoneyScoring) {
			this.moneyScoringFunctions.add((MoneyScoring) scoringFunction);
		}

		if (scoringFunction instanceof ArbitraryEventScoring) {
			this.arbitraryEventScoringFunctions.add((ArbitraryEventScoring) scoringFunction);
		}

	}


}
