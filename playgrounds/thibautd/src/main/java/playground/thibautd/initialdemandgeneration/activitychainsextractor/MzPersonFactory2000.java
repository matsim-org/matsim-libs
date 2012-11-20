/* *********************************************************************** *
 * project: org.matsim.*
 * MzPersonFactory2000.java
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
package playground.thibautd.initialdemandgeneration.activitychainsextractor;

import org.matsim.core.basic.v01.IdImpl;

import playground.thibautd.initialdemandgeneration.activitychainsextractor.MzConfig.Statistics;

/**
 * Creates MzPersons for the 2000 MZ
 * @author thibautd
 */
public class MzPersonFactory2000 implements MzPersonFactory {
	private static final String EDUCATION_NAME = "F50004";
	private static final String EMPLOYED_NAME = "F50003";
	private static final String ID_NAME = "INTNR";
	private static final String DOW_NAME = "DAYSTTAG";
	private static final String LICENCE_NAME = "F50005";
	private static final String AGE_NAME = "F50001";
	private static final String WEIGHT_NAME = "WP";
	private static final String GENDER_NAME = "F50002";

	private final int employedIndex;
	private final int educationIndex;
	private final int personIndex;
	private final int hhIndex;
	private final int dayOfWeekIndex;
	private final int licenceIndex;
	private final int ageIndex;
	private final int weightIndex;
	private final int genderIndex;

	private final Statistics stats;

	public MzPersonFactory2000(final Statistics stats , final String titleLine) {
		this.stats = stats;
		String[] names = titleLine.split("\t");
		int employedIndex = -1;
		int educationIndex = -1;
		int personIndex = -1;
		int hhIndex = -1;
		int dayOfWeekIndex = -1;
		int licenceIndex = -1;
		int ageIndex = -1;
		int weightIndex = -1;
		int genderIndex = -1;
		for (int i=0; i < names.length; i++) {
			if (names[ i ].equals( EMPLOYED_NAME )) {
				employedIndex = i;
			}
			if (names[ i ].equals( EDUCATION_NAME )) {
				educationIndex = i;
			}
			else if (names[ i ].equals( ID_NAME )) {
				personIndex  = i;
			}
			else if (names[ i ].equals( DOW_NAME )) {
				dayOfWeekIndex = i;
			}
			else if (names[ i ].equals( LICENCE_NAME )) {
				licenceIndex = i;
			}
			else if (names[ i ].equals( AGE_NAME )) {
				ageIndex = i;
			}
			else if (names[ i ].equals( WEIGHT_NAME )) {
				weightIndex = i;
			}
			else if (names[ i ].equals( GENDER_NAME )) {
				genderIndex = i;
			}
		}
		this.employedIndex = employedIndex;
		this.educationIndex = educationIndex;
		this.personIndex = personIndex;
		this.hhIndex = hhIndex;
		this.dayOfWeekIndex = dayOfWeekIndex;
		this.licenceIndex = licenceIndex;
		this.ageIndex = ageIndex;
		this.weightIndex = weightIndex;
		this.genderIndex = genderIndex;
	}

	@Override
	public MzPerson createMzPersonFromZpLine(final String zpDataLine) {
		String[] lineArray = zpDataLine.split("\t");
		int age = Integer.parseInt( lineArray[ ageIndex ] );
		return new MzPerson(
				stats,
				new IdImpl( lineArray[ personIndex ].trim() ),
				booleanField( lineArray[ employedIndex ] ),
				booleanField( lineArray[ educationIndex ] ),
				dayOfWeek( lineArray[ dayOfWeekIndex ] ),
				licence( age , lineArray[ licenceIndex ] ),
				age,
				Double.parseDouble( lineArray[ weightIndex ] ),
				gender( lineArray[ genderIndex] ));
	}

	// /////////////////////////////////////////////////////////////////////////
	// helpers
	public static Boolean licence(final int age, final String value) {
		// for children, no value
		if ( age < 18 ) return false;
		return booleanField( value );
	}

	public static Boolean booleanField( final String value ) {
		int intValue = Integer.parseInt( value );

		return intValue == 1 ? true :
			(intValue == 2 ? false : null);
	}

	public static int dayOfWeek( final String value ) {
		int intValue = Integer.parseInt( value );

		if (intValue < 1 || intValue > 7) throw new IllegalArgumentException( "unknown day "+value );

		return intValue;
	}

	public static String gender( final String value ) {
		int intValue = Integer.parseInt( value );

		return intValue == 1 ? "m" : (intValue == 2 ? "f" : null);
	}
}

