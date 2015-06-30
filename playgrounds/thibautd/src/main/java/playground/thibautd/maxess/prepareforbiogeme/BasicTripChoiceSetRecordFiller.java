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
package playground.thibautd.maxess.prepareforbiogeme;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.utils.misc.Time;
import playground.thibautd.maxess.prepareforbiogeme.ChoiceDataSetWriter.ChoiceSetRecordFiller;

import java.util.ArrayList;
import java.util.List;

/**
 * @author thibautd
 */
public class BasicTripChoiceSetRecordFiller implements ChoiceSetRecordFiller<TripStructureUtils.Trip> {
	private static final Logger log = Logger.getLogger( BasicTripChoiceSetRecordFiller.class );

	private final List<String> alternativeNames;

	public BasicTripChoiceSetRecordFiller(final List<String> alternativeNames) {
		this.alternativeNames = alternativeNames;
	}

	@Override
	public List<String> getFieldNames() {
		final List<String> fieldNames = new ArrayList<String>();

		fieldNames.add( "P_ID" );
		fieldNames.add( "P_AGE" );
		fieldNames.add( "P_GENDER" );
		fieldNames.add( "P_CARAVAIL" );

		fieldNames.add( "C_CHOICE" );

		for ( String alt : alternativeNames ) {
			fieldNames.add( "A_"+alt+"_TT" );
		}

		return fieldNames;
	}

	@Override
	public List<Number> getFieldValues(final ChoiceSet<Trip> cs) {
		final List<Number> values = new ArrayList<>( 5 + alternativeNames.size() );

		// This is awful, but BIOGEME does not understand anything else than numbers...
		values.add( Long.getLong(cs.getDecisionMaker().getId().toString()) );
		values.add( getAge(cs.getDecisionMaker()) );
		values.add( getGender(cs.getDecisionMaker()) );
		values.add( getCarAvailability(cs.getDecisionMaker()) );

		values.add( getChoice( cs ) );

		for ( String name : alternativeNames ) {
			values.add( getTravelTime( cs.getAlternative( name ) ) );
		}

		return values;
	}

	private int getChoice(final ChoiceSet<Trip> cs) {
		// Assuming the chosen destination is the first listed one, the choice index will always be between
		// 0 and n_modes. So linear search sould actually not be that bad for large number of alternatives,
		// and actually scale BETTER than "smart" methods such as binary search...
		final int index = alternativeNames.indexOf( cs.getChosenName() );
		if ( index < 0 ) throw new RuntimeException( cs.getChosenName()+" not found in "+alternativeNames );
		return index;
	}

	private int getAge(final Person decisionMaker) {
		return ((PersonImpl) decisionMaker).getAge();
	}

	private short getGender(final Person decisionMaker) {
		final String sex = ((PersonImpl) decisionMaker).getSex().toLowerCase().trim();

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
		final String avail = ((PersonImpl) decisionMaker).getCarAvail().toLowerCase().trim();

		switch ( avail ) {
			case "sometimes":
			case "always":
				return 1;
			case "never":
				return 0;
			default:
				throw new IllegalArgumentException( "unhandled availability "+avail );
		}
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
