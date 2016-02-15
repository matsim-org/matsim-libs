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
import org.matsim.core.population.PersonUtils;
import org.matsim.core.utils.misc.Time;
import playground.ivt.maxess.prepareforbiogeme.framework.ChoiceDataSetWriter.ChoiceSetRecordFiller;
import playground.ivt.maxess.prepareforbiogeme.framework.ChoiceSet;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author thibautd
 */
public class BasicTripChoiceSetRecordFiller implements ChoiceSetRecordFiller<Trip> {
	private static final Logger log = Logger.getLogger( BasicTripChoiceSetRecordFiller.class );

	@Override
	public Map<String,Number> getFieldValues(final ChoiceSet<Trip> cs) {
		final Map<String,Number> values = new LinkedHashMap<>();

		// This is awful, but BIOGEME does not understand anything else than numbers...
		values.put("P_ID", getId( cs ) );
		values.put("P_AGE", getAge(cs.getDecisionMaker()));
		values.put("P_GENDER", getGender(cs.getDecisionMaker()));
		values.put("P_CARAVAIL", getCarAvailability(cs.getDecisionMaker()));

		values.put("C_CHOICE", getChoice(cs));

		for ( Map.Entry<String,Trip> alt : cs.getNamedAlternatives().entrySet() ) {
			values.put("A_" + alt.getKey() + "_TT", getTravelTime( alt.getValue() ));
		}

		return values;
	}

	private final TLongObjectMap<Id<Person>> idMappings = new TLongObjectHashMap<>();
	private long getId( final ChoiceSet<Trip> cs ) {
		final Id<Person> id = cs.getDecisionMaker().getId();
		final Long longId = Long.getLong( id.toString() );

		if ( longId != null ) return longId;

		// TODO store table number to id
		// (could also be done offline, as String hashCode should be stable according to documentation)
		long value = id.toString().hashCode();

		while ( idMappings.containsKey( value ) && !idMappings.get( value ).equals( id ) ) {
			log.warn( "Already a numerical ID "+value+" (for id "+idMappings.get( value )+") when adding "+id );
			value++;
		}

		idMappings.put( value , id );
		return value;
	}

	private int getChoice(final ChoiceSet<Trip> cs) {
		// Assuming the chosen destination is the first listed one, the choice index will always be between
		// 0 and n_modes. So linear search sould actually not be that bad for large number of alternatives,
		// and actually scale BETTER than "smart" methods such as binary search...
		int index = 0;
		for ( String alt : cs.getNamedAlternatives().keySet() ) {
			if ( alt.equals( cs.getChosenName() ) ) return index;
			index++;
		}
		throw new RuntimeException( cs.getChosenName()+" not found in "+cs.getNamedAlternatives() );
	}

	private int getAge(final Person decisionMaker) {
		return PersonUtils.getAge(decisionMaker);
	}

	private short getGender(final Person decisionMaker) {
		final String sex = PersonUtils.getSex(decisionMaker).toLowerCase().trim();

		switch ( sex ) {
			case "f":
				return 0;
			case "m":
				return 1;
			default:
				throw new IllegalArgumentException( "unhandled sex "+sex );
		}
	}

	private short getCarAvailability(final Person decisionMaker) {
		final String avail = PersonUtils.getCarAvail(decisionMaker);

		if ( "never".equals( avail ) ) return 0;

		final String license = PersonUtils.getLicense( decisionMaker );

		if ( "no".equals( license ) ) return 0;

		return 1;
	}

	private double getTravelTime(final Trip alternative) {
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


}
