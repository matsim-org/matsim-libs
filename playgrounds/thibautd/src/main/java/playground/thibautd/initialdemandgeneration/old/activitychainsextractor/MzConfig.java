/* *********************************************************************** *
 * project: org.matsim.*
 * MzConfig.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.initialdemandgeneration.old.activitychainsextractor;

import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * class providing static methods to get information about the settings
 * for the current session.
 *
 * Settings are for the moment the MZ year.
 *
 * @author thibautd
 */
public class MzConfig {
	private static final Logger log =
		Logger.getLogger(MzConfig.class);

	private static final List<Integer> knownYears = Arrays.asList( 1994 , 2000 );
	private final int year;
	private final Statistics stats = new Statistics();

	public MzConfig(
			final int year) {
		if (year <= 0) {
			throw new IllegalArgumentException( "Really? A swiss microcensus in "+(-year)+" B.C.?" );
		}
		else if (!knownYears.contains( year )) {
			throw new IllegalArgumentException( "Cannot handle the "+year+" MZ. Handled years are "+knownYears );
		}
		else {
			log.info( "setting MZ year to "+year );
			this.year = year;
		}
	}

	public int getMzYear() {
		return year;
	}

	public MzPersonFactory createMzPersonFactory(final String titleLineZp ) {
		if (year == 1994) {
			return new MzPersonFactory1994( stats , titleLineZp );
		}
		else if (year == 2000) {
			return new MzPersonFactory2000( stats , titleLineZp );
		}
		else {
			throw new IllegalArgumentException( "no settings known for year "+year );
		}
	}

	public MzWegFactory createMzWegFactory(final String titleLineWege ) {
		if (year == 1994) {
			return new MzWegFactory1994( titleLineWege );
		}
		else if (year == 2000) {
			return new MzWegFactory2000( titleLineWege );
		}
		else {
			throw new IllegalArgumentException( "no settings known for year "+year );
		}
	}

	public MzEtappeFactory createMzEtappeFactory(final String titleLineWege ) {
		if (year == 1994) {
			return null;
		}
		else if (year == 2000) {
			return new MzEtappeFactory2000( titleLineWege );
		}
		else {
			throw new IllegalArgumentException( "no settings known for year "+year );
		}
	}

	public void printStats() {
		stats.printStats();
	}

	public static class Statistics {
		private int addressInconsistencies = 0;
		private int timeSequenceInconsistencies = 0;
		private int tripDurationInconsistencies = 0;
		private int totalPersonsProcessed = 0;
		private int totallyProcessedPersons = 0;
		private int roundTrips = 0;
		private int unhandledActivityType = 0;
		private int nonHomeBased = 0;
		private int longServePassengerAct = 0;

		public void longServePassengerAct() {
			longServePassengerAct++;
		}

		public void nonHomeBased() {
			nonHomeBased++;
		}

		public void roundTrip() {
			roundTrips++;
		}

		public void addressInconsistency() {
			addressInconsistencies++;
		}

		public void timeSequenceInconsistency() {
			timeSequenceInconsistencies++;
		}

		public void tripDurationInconsistency() {
			tripDurationInconsistencies++;
		}

		public void totalPersonsProcessed() {
			totalPersonsProcessed++;
		}

		public void totallyProcessedPerson() {
			totallyProcessedPersons++;
		}

		public void unhandledActivityType() {
			unhandledActivityType++;
		}

		public void printStats() {
			log.info( "-----------> statistics on the processing." );
			log.info( "beware: the counts count the reason for which plan construction was aborted" );
			log.info( "Actual number of interviews of each type may  be bigger." );
			log.info( "" );
			log.info( "round trips: "+roundTrips );
			log.info( "adress inconsistencies: "+addressInconsistencies );
			log.info( "time sequence inconsistencies: "+timeSequenceInconsistencies );
			log.info( "trip duration inconsistencies: "+tripDurationInconsistencies );
			log.info( "plans with unhandled activity types: "+unhandledActivityType );
			log.info( "plans with long serve passenger activities: "+longServePassengerAct );
			// log.info( "plans with numerous serve passenger legs: "+numerousServePassengersLegs );
			log.info( "non home based plans: "+nonHomeBased );
			log.info( "total number of persons: "+totalPersonsProcessed );
			log.info( "number of persons retained: "+totallyProcessedPersons );
			MzAdress.printStatistics();
			log.info( "<----------- end of statistics on the processing" );
		}
	}
}

