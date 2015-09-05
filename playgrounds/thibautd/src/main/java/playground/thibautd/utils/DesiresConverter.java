/* *********************************************************************** *
 * project: org.matsim.*
 * DesiresReadableConverter.java
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
package playground.thibautd.utils;

import java.util.Map;

import org.apache.log4j.Logger;

import org.matsim.core.utils.misc.Time;
import playground.ivt.utils.Desires;
import org.matsim.utils.objectattributes.AttributeConverter;

/**
 * desires in form type1=dur1;type2=dur2"
 * @author thibautd
 */
public class DesiresConverter implements AttributeConverter<Desires> {
	private static final Logger log =
		Logger.getLogger(DesiresConverter.class);

	private static final String SEP_1 = ";";
	private static final String SEP_2 = "=";

	@Override
	public Desires convert(final String value) {

		final Desires desires = new Desires( null );

		for ( String typedur : value.split( SEP_1 ) ) {
			final String[] arr = typedur.split( SEP_2 );
			if ( arr.length != 2 ) throw new IllegalArgumentException( value );

			final String type = arr[ 0 ];
			final double dur = Time.parseTime( arr[ 1 ] );

			desires.putActivityDuration( type , dur );
		}

		if ( log.isTraceEnabled() ) {
			log.trace( value+" converted to desires "+desires );
		}

		return desires;
	}

	@Override
	public String convertToString(final Object o) {
		final Desires desires = (Desires) o;

		final StringBuilder builder = new StringBuilder();
		for ( Map.Entry<String, Double> e : desires.getActivityDurations().entrySet() ) {
			final String type = e.getKey();
			final double dur = e.getValue().doubleValue();

			if ( builder.length() > 0 ) builder.append( SEP_1 );
			builder.append( type + SEP_2 + Time.writeTime( dur ) );
		}

		if ( log.isTraceEnabled() ) {
			log.trace( desires+" converted to string "+builder );
		}

		return builder.toString();
	}
}

