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

import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.contrib.socnetsim.framework.events.CourtesyEvent;
import org.matsim.core.scoring.SumScoringFunction.ArbitraryEventScoring;

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
	private final UtilityOfTimeCalculator utilityCalculator;

	private final String activityType;

	private boolean inAct = false;
	private double lastChangeInNCoparticipants = Double.NaN;
	private int currentNCoparticipants = 0;

	private double score = 0;
	private final EventBuffer buffer = new EventBuffer();

	public GroupCompositionPenalizer(
			final String activityType,
			final UtilityOfTimeCalculator utilityCalculator ) {
		this.activityType = activityType;
		this.utilityCalculator = utilityCalculator;
	}

	@Override
	public void finish() {
		buffer.moveToBuffer();
	}

	@Override
	public double getScore() {
		bufferToScore();
		return score;
	}

	@Override
	public void handleEvent( final Event event ) {
		if ( !(event instanceof ActivityStartEvent) && !(event instanceof ActivityEndEvent) && !(event instanceof CourtesyEvent) ) return;
		if ( !event.getAttributes().get( "actType" ).equals( activityType ) ) return;
		buffer.addEvent( event );
		bufferToScore();
	}

	private void bufferToScore() {
		while ( !buffer.outBuffer.isEmpty() ) {
			final Event event = buffer.outBuffer.poll();
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
		buffer.outBuffer.clear();
	}

	private void startActivity( final ActivityStartEvent event ) {
		if ( !event.getActType().equals( activityType ) ) return;

		inAct = true;
		lastChangeInNCoparticipants = event.getTime();
		currentNCoparticipants = 0;
	}

	private void handleCourtesy( final CourtesyEvent event ) {
		if ( !event.getActType().equals( activityType ) ) return;

		if ( !inAct ) return;
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
		assert currentNCoparticipants >= 0 : event +" -> "+ currentNCoparticipants;
	}

	private void endActivity( final ActivityEndEvent event ) {
		if ( !event.getActType().equals( activityType ) ) return;

		updateScore( event.getTime() );
		inAct = false;
	}

	private void updateScore( final double time ) {
		assert time >= lastChangeInNCoparticipants;
		this.score += utilityCalculator.getUtilityOfTime( currentNCoparticipants ) *
			(time - lastChangeInNCoparticipants );
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

	/**
	 * ensures the events of a time step are properly ordered by type
	 */
	private static class EventBuffer {
		private final List<Event> inBuffer = new ArrayList<>();

		private final Queue<Event> outBuffer = new PriorityQueue<>( 11 , new EventComparator() );

		private double lastTime = -1;

		void addEvent( final Event event ) {
			if ( event.getTime() > lastTime ) moveToBuffer();

			inBuffer.add( event );
			lastTime = event.getTime();
		}

		void moveToBuffer() {
			outBuffer.addAll( inBuffer );
			inBuffer.clear();
		}
	}

	private static class EventComparator implements Comparator<Event> {
		@Override
		public int compare( final Event o1, final Event o2 ) {
			if ( o1.getTime() != o2.getTime() ) return Double.compare( o1.getTime() , o2.getTime() );
			return getTypeOrder( o1.getEventType() ) - getTypeOrder( o2.getEventType() );
		}

		private int getTypeOrder( final String eventType  ) {
			switch ( eventType ) {
				case "actstart": return 1;
				case "sayHelloEvent": return 2;
				case "sayGoodbyeEvent": return 3;
				case "actend": return 4;
			}

			throw new RuntimeException( eventType );
		}
	}
}

