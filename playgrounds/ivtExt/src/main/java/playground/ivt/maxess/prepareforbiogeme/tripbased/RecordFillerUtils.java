/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.ivt.maxess.prepareforbiogeme.tripbased;

import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.misc.Time;
import playground.ivt.maxess.prepareforbiogeme.framework.ChoiceSet;
import playground.ivt.maxess.prepareforbiogeme.tripbased.mikrozensus.MZ2010ExportChoiceSetRecordFiller;

/**
 * @author thibautd
 */
public class RecordFillerUtils {
	private static final Logger log = Logger.getLogger( RecordFillerUtils.class );
	private static final TLongObjectMap<Id<Person>> idMappings = new TLongObjectHashMap<>();

	public static double getTravelTime(final Trip alternative) {
		double tt = 0;

		for ( Leg l : alternative.getLegsOnly() ) {
			if ( l.getTravelTime() == Time.UNDEFINED_TIME ) {
				log.warn("undefined travel time for " + alternative);
				return -99;
			}
			tt += l.getTravelTime();
		}

		return tt;
	}

	public static int getChoice( final ChoiceSet<Trip> cs ) {
		// Assuming the chosen destination is the first listed one, the choice index will always be between
		// 0 and n_modes. So linear search should actually not be that bad for large number of alternatives,
		// and actually scale BETTER than "smart" methods such as binary search...
		int index = 0;
		for ( String alt : cs.getNamedAlternatives().keySet() ) {
			if ( alt.equals( cs.getChosenName() ) ) return index;
			index++;
		}
		throw new RuntimeException( cs.getChosenName()+" not found in "+cs.getNamedAlternatives() );
	}

	public static long getId( final ChoiceSet<Trip> cs ) {
		final Id<Person> id = cs.getDecisionMaker().getId();
		try {
			return Long.valueOf( id.toString() );
		}
		catch ( NumberFormatException e ) {
			// TODO store table number to id
			// (could also be done offline, as String hashCode should be stable according to documentation)
			long value = id.toString().hashCode();

			while ( idMappings.containsKey( value ) && !idMappings.get( value ).equals( id ) ) {
				log.warn( "Already a numerical ID " + value + " (for id " + idMappings.get( value ) + ") when adding " + id );
				value++;
			}

			idMappings.put( value, id );
			return value;
		}
	}

	public static double getDistance( Trip trip ) {
		double d = 0;

		for ( Leg l : trip.getLegsOnly() ) {
			if ( Double.isNaN( l.getRoute().getDistance() ) ) {
				throw new IllegalArgumentException( "undefined distance in "+trip );
			}
			d += l.getRoute().getDistance();
		}

		return d;
	}
}
