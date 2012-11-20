/* *********************************************************************** *
 * project: org.matsim.*
 * SimplifyObjectAttributes.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.initialdemandgeneration.MZ2010;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.misc.Counter;
import org.matsim.utils.objectattributes.AttributeConverter;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import org.xml.sax.Attributes;

import playground.acmarmol.matsim2030.microcensus2010.MZConstants;
import playground.acmarmol.matsim2030.microcensus2010.objectAttributesConverters.CoordConverter;

/**
 * Converts the string based object attributes from Alejandro to
 * a usable format.
 * @author thibautd
 */
public class SimplifyObjectAttributes {
	public final static String AGE = "age";
	public final static String IS_EMPLOYED = "isEmployed";
	public final static String IS_IN_EDUCATION = "isInEducation";
	public final static String GENDER = "gender";
	public final static String LICENSE = "hasDrivingLicense";
	public final static String CAR_AVAIL = "isCarAvail";
	public final static String WEIGHT = "weight";
	public final static String DOW = "dayOfWeek";

	/**
	 * @author thibautd
	 *
	 */
	public static class GenderConverter implements AttributeConverter<Gender> {
		@Override
		public Gender convert(final String value) {
			if ( value.equals( MZConstants.MALE ) ) return Gender.MALE;
			if ( value.equals( MZConstants.FEMALE ) ) return Gender.FEMALE;
			throw new IllegalArgumentException( value );
		}

		@Override
		public String convertToString(final Object o) {
			if ( Gender.MALE.equals( o ) ) return MZConstants.MALE;
			if ( Gender.FEMALE.equals( o ) ) return MZConstants.FEMALE;
			throw new IllegalArgumentException( ""+o );
		}
	}

	public static void main(final String[] args) {
		final String inputFile = args[ 0 ];
		final String outputFile = args[ 1 ];

		List<String> ids = getIds( inputFile );
		ObjectAttributes oldAttributes = read( inputFile );
		ObjectAttributes newAttributes = convert( ids , oldAttributes );
		write( newAttributes , outputFile );
	}

	private static void write(
			final ObjectAttributes newAttributes,
			final String outputFile) {
		ObjectAttributesXmlWriter writer = new ObjectAttributesXmlWriter( newAttributes );
		writer.putAttributeConverter( Gender.class , new GenderConverter() );
		writer.writeFile( outputFile );
	}

	private static List<String> getIds(final String inputFile) {
		final List<String> ids = new ArrayList<String>();
		final Counter count = new Counter( "read id # " );

		(new MatsimXmlParser( false ) {
			@Override
			public void startTag(
				final String name,
				final Attributes atts,
				final Stack<String> context) {
				if (name.equals( "object" )) {
					count.incCounter();
					ids.add( atts.getValue( "id" ) );
				}
			}

			@Override
			public void endTag(String name, String content,
					Stack<String> context) {}
		}).parse( inputFile );

		count.printCounter();

		return ids;
	}

	private static ObjectAttributes convert(
			final List<String> ids,
			final ObjectAttributes old) {
		ObjectAttributes newAtts = new ObjectAttributes();

		GenderConverter genderConverter = new GenderConverter();
		for ( String id : ids ) {
			String age = (String) old.getAttribute( id , MZConstants.AGE );
			newAtts.putAttribute( id , AGE , Double.parseDouble( age ) );

			String gender = (String) old.getAttribute( id , MZConstants.GENDER );
			newAtts.putAttribute(
					id,
					GENDER,
					genderConverter.convert( gender ) );

			newAtts.putAttribute(
					id,
					IS_EMPLOYED,
					hasWork( id , old ) );
			newAtts.putAttribute(
					id,
					IS_IN_EDUCATION,
					hasEducation( id , old ) );
			
			newAtts.putAttribute(
					id,
					LICENSE,
					old.getAttribute(
						id,
						MZConstants.DRIVING_LICENCE).equals( MZConstants.YES ));

			newAtts.putAttribute(
					id,
					CAR_AVAIL,
					old.getAttribute(
						id,
						MZConstants.CAR_AVAILABILITY ).equals( MZConstants.ALWAYS ) );

			newAtts.putAttribute(
					id,
					WEIGHT,
					Double.parseDouble( (String)
						old.getAttribute( id , MZConstants.PERSON_WEIGHT )));

			newAtts.putAttribute(
					id,
					DOW,
					getDow( id , old ));
		}

		return newAtts;
	}

	private static Object getDow(
			final String id,
			final ObjectAttributes old) {
		Object dow = old.getAttribute( id , MZConstants.DAY_OF_WEEK );
		if (dow.equals( MZConstants.MONDAY )) return 1;
		if (dow.equals( MZConstants.TUESDAY )) return 2;
		if (dow.equals( MZConstants.WEDNESDAY )) return 3;
		if (dow.equals( MZConstants.THURSDAY )) return 4;
		if (dow.equals( MZConstants.FRIDAY )) return 5;
		if (dow.equals( MZConstants.SATURDAY )) return 6;
		if (dow.equals( MZConstants.SUNDAY )) return 7;
		throw new IllegalArgumentException( ""+dow );
	}

	private static final List<String> employed = Arrays.asList(
			MZConstants.EMPLOYEE , MZConstants.INDEPENDENT , MZConstants.MITARBEITENDES,
			MZConstants.TRAINEE );
	private static Boolean hasWork(
			final String id,
			final ObjectAttributes atts) {
		String att = (String) atts.getAttribute( id , MZConstants.EMPLOYMENT_STATUS );
		// TODO: check that value is correct
		return employed.contains( att );
	}

	private static Boolean hasEducation(
			final String id,
			final ObjectAttributes atts) {
		String att = (String) atts.getAttribute( id , MZConstants.EMPLOYMENT_STATUS );
		if ( att.equals( MZConstants.EDUCATION ) ) return true;

		att = (String) atts.getAttribute( id , MZConstants.OTHER_ACTIVITY1 );
		if ( att.equals( MZConstants.EDUCATION ) ) return true;

		att = (String) atts.getAttribute( id , MZConstants.OTHER_ACTIVITY2 );
		if ( att.equals( MZConstants.EDUCATION ) ) return true;

		att = (String) atts.getAttribute( id , MZConstants.OTHER_ACTIVITY3 );
		return att.equals( MZConstants.EDUCATION );
	}

	private static ObjectAttributes read(final String inputFile) {
		ObjectAttributes atts = new ObjectAttributes();
		ObjectAttributesXmlReader r = new ObjectAttributesXmlReader( atts );
		r.putAttributeConverter( CoordImpl.class , new CoordConverter() );
		r.parse( inputFile );
		return atts;
	}
}

