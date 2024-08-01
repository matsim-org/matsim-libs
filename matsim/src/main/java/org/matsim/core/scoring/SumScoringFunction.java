
/* *********************************************************************** *
 * project: org.matsim.*
 * SumScoringFunction.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

 package org.matsim.core.scoring;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.router.TripStructureUtils.Trip;

public final class SumScoringFunction implements ScoringFunction {

	public interface BasicScoring {
		void finish();
		double getScore();

		/**
		 * @see ScoringFunction#explainScore(StringBuilder).
		 */
		default void explainScore(StringBuilder out) {
		}
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

	public interface ScoreScoring extends BasicScoring {
		void addScore(final double amount);
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

	private static final  Logger log = LogManager.getLogger(SumScoringFunction.class);

	private final List<BasicScoring> basicScoringFunctions = new ArrayList<>();
	private final List<ActivityScoring> activityScoringFunctions = new ArrayList<>();
	private final List<MoneyScoring> moneyScoringFunctions = new ArrayList<>();
	private final List<ScoreScoring> scoreScoringFunctions = new ArrayList<>();
	private final List<LegScoring> legScoringFunctions = new ArrayList<>();
	private final List<TripScoring> tripScoringFunctions = new ArrayList<>();
	private final List<AgentStuckScoring> agentStuckScoringFunctions = new ArrayList<>();
	private final List<ArbitraryEventScoring> arbitraryEventScoringFunctions = new ArrayList<>() ;

	@Override
	public final void handleActivity(Activity activity) {
		if (activity.getStartTime().isUndefined() && activity.getEndTime().isDefined()) {
			for (ActivityScoring activityScoringFunction : this.activityScoringFunctions) {
				activityScoringFunction.handleFirstActivity(activity);
			}
		} else if (activity.getStartTime().isDefined() && activity.getEndTime().isDefined()) {
			for (ActivityScoring activityScoringFunction : this.activityScoringFunctions) {
				activityScoringFunction.handleActivity(activity);
			}
		} else if (activity.getStartTime().isDefined() && activity.getEndTime().isUndefined()) {
			for (ActivityScoring activityScoringFunction : this.activityScoringFunctions) {
				activityScoringFunction.handleLastActivity(activity);
			}
		} else {
			throw new RuntimeException(
					"Trying to score an activity without start or end time. Should not happen. Activity=" + activity);
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
	public void addScore(double amount) {
		for (ScoreScoring scoreScoringFunction : this.scoreScoringFunctions) {
			scoreScoringFunction.addScore(amount);
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


	@Override
	public void explainScore(StringBuilder out) {

		for (BasicScoring s : basicScoringFunctions) {

			// If something was already written, a delimiter needs to be placed
			if (!out.isEmpty())
				out.append(SCORE_DELIMITER);

			s.explainScore(out);
		}
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

		if (scoringFunction instanceof ScoreScoring) {
			this.scoreScoringFunctions.add((ScoreScoring) scoringFunction);
		}

		if (scoringFunction instanceof ArbitraryEventScoring) {
			this.arbitraryEventScoringFunctions.add((ArbitraryEventScoring) scoringFunction);
		}

	}


}
