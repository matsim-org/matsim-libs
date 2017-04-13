/* *********************************************************************** *
 * project: org.matsim.*
 * GroupCompositionPenalizer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package org.matsim.contrib.socnetsim.framework.scoring;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.contrib.socnetsim.framework.events.CourtesyEvent;
import org.matsim.core.scoring.SumScoringFunction.ArbitraryEventScoring;
import org.matsim.core.utils.misc.Time;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * Scoring element that computes a penalty for "joint activities"
 * if they do not fulfill the desired number of participants.
 * Each agent has preferences on the number of social contacts with whom it wants
 * to perform a joint activity with.
 * A penalty of time, dependent on the number of participants, is applied.
 *
 * @author thibautd
 */
public class GroupCompositionPenalizer implements ArbitraryEventScoring {
	private static final Logger log = Logger.getLogger( GroupCompositionPenalizer.class );
	private final UtilityOfTimeCalculator utilityCalculator;

	private final String activityType;

	private double lastChangeInNCoparticipants = -1;
	private int currentNCoparticipants = 0;

	private double score = 0;

	public GroupCompositionPenalizer(
			final String activityType,
			final UtilityOfTimeCalculator utilityCalculator ) {
		this.activityType = activityType;
		this.utilityCalculator = utilityCalculator;
	}

	@Override
	public void finish() {}

	@Override
	public double getScore() {
		return score;
	}

	@Override
	public void handleEvent( final Event event ) {
		if ( event instanceof ActivityStartEvent ) {
			startActivity( (ActivityStartEvent) event );
		}
		if ( event instanceof ActivityEndEvent ) {
			endActivity( (ActivityEndEvent) event );
		}
		if ( event instanceof CourtesyEvent ) {
			handleCourtesy( (CourtesyEvent) event );
		}
	}

	private void startActivity( final ActivityStartEvent event ) {
		if ( !event.getActType().equals( activityType ) ) {
			assert currentNCoparticipants == 0 : currentNCoparticipants;
			return;
		}

		if ( log.isTraceEnabled() ) {
			log.trace( "Starting activity at time "+Time.writeTime( event.getTime() ) );
		}

		lastChangeInNCoparticipants = event.getTime();
	}

	private void handleCourtesy( final CourtesyEvent event ) {
		if ( !event.getActType().equals( activityType ) ) return;

		if ( log.isTraceEnabled() ) {
			log.trace( "Handle "+event.getType()+" at time "+Time.writeTime( event.getTime() ) );
		}

		updateScore( event.getTime() );

		lastChangeInNCoparticipants = event.getTime();
		switch ( event.getType() ) {
			case sayGoodbyeEvent:
				currentNCoparticipants--;
				break;
			case sayHelloEvent:
				currentNCoparticipants++;
				break;
			default:
				throw new RuntimeException( event.getType()+"?" );
		}

		if ( log.isTraceEnabled() ) {
			log.trace( "Now "+currentNCoparticipants+" participants" );
		}

		assert currentNCoparticipants >= 0 : event +" -> "+ currentNCoparticipants;
	}

	private void endActivity( final ActivityEndEvent event ) {
		if ( !event.getActType().equals( activityType ) ) return;

		if ( log.isTraceEnabled() ) {
			log.trace( "Ending activity at time "+Time.writeTime( event.getTime() ) );
		}

		updateScore( event.getTime() );
		lastChangeInNCoparticipants = -1;
	}

	private void updateScore( final double time ) {
		if (lastChangeInNCoparticipants < 0) return;
		assert time >= lastChangeInNCoparticipants : time +" < "+ lastChangeInNCoparticipants;

		this.score += utilityCalculator.getUtilityOfTime( currentNCoparticipants ) *
			(time - lastChangeInNCoparticipants );

		if ( log.isTraceEnabled() ) {
			log.trace( "update score "+Time.writeTime( lastChangeInNCoparticipants )+"->"+Time.writeTime( time )+
					": currently "+currentNCoparticipants+" participants, score="+score );
		}
	}

	public interface UtilityOfTimeCalculator {
		double getUtilityOfTime( int nCoParticipants );
	}

	public static class MinGroupSizeLinearUtilityOfTime implements UtilityOfTimeCalculator {
		private final int minGroupSize;
		private final double utilOfMissingContact;

		public MinGroupSizeLinearUtilityOfTime(
				final int minGroupSize,
				final double utilOfMissingContact ) {
			this.minGroupSize = minGroupSize;

			if ( utilOfMissingContact > 0 ) throw new IllegalArgumentException( "util of missing contact expected to be negative. Was "+utilOfMissingContact );
			this.utilOfMissingContact = utilOfMissingContact;
		}

		@Override
		public double getUtilityOfTime( final int nCoParticipants ) {
			if ( nCoParticipants >= minGroupSize ) return 0;

			final double util = (minGroupSize - nCoParticipants) * utilOfMissingContact;
			assert util <= 0 : util;

			return util;
		}
	}
}

