/* *********************************************************************** *
 * project: org.matsim.*
 * MzPersonFactory1994.java
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
package playground.thibautd.initialdemandgeneration.old.activitychainsextractor;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;

import playground.thibautd.initialdemandgeneration.old.activitychainsextractor.MzConfig.Statistics;

/**
 * @author thibautd
 */
public class MzPersonFactory1994 implements MzPersonFactory {
	private static final String EDUCATION_NAME = "ZP04";
	private static final String EMPLOYED_NAME = "ZP03";
	private static final String PERSON_NAME = "PERSON";
	private static final String HH_NAME = "HAUSHALT";
	private static final String DOW_NAME = "ZP_WTAGF";
	private static final String LICENCE_NAME = "ZP05";
	private static final String AGE_NAME = "ZP01";
	private static final String WEIGHT_NAME = "WP";
	private static final String GENDER_NAME = "ZP02";

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

	public MzPersonFactory1994(final Statistics stats , final String titleLine) {
		this.stats = stats;
		String[] names = titleLine.split("\t");
		int employedIndex1 = -1;
		int educationIndex1 = -1;
		int personIndex1 = -1;
		int hhIndex1 = -1;
		int dayOfWeekIndex1 = -1;
		int licenceIndex1 = -1;
		int ageIndex1 = -1;
		int weightIndex1 = -1;
		int genderIndex1 = -1;

		for (int i=0; i < names.length; i++) {
			if (names[ i ].equals( EMPLOYED_NAME )) {
				employedIndex1 = i;
			}
			if (names[ i ].equals( EDUCATION_NAME )) {
				educationIndex1 = i;
			}
			else if (names[ i ].equals( PERSON_NAME )) {
				personIndex1  = i;
			}
			else if (names[ i ].equals( HH_NAME )) {
				hhIndex1  = i;
			}
			else if (names[ i ].equals( DOW_NAME )) {
				dayOfWeekIndex1 = i;
			}
			else if (names[ i ].equals( LICENCE_NAME )) {
				licenceIndex1 = i;
			}
			else if (names[ i ].equals( AGE_NAME )) {
				ageIndex1 = i;
			}
			else if (names[ i ].equals( WEIGHT_NAME )) {
				weightIndex1 = i;
			}
			else if (names[ i ].equals( GENDER_NAME )) {
				genderIndex1 = i;
			}
		}

		this.employedIndex = employedIndex1;
		this.educationIndex = educationIndex1;
		this.personIndex = personIndex1;
		this.hhIndex = hhIndex1;
		this.dayOfWeekIndex = dayOfWeekIndex1;
		this.licenceIndex = licenceIndex1;
		this.ageIndex = ageIndex1;
		this.weightIndex = weightIndex1;
		this.genderIndex = genderIndex1;
	}

	@Override
	public MzPerson createMzPersonFromZpLine(final String zpDataLine) {
		String[] lineArray = zpDataLine.split("\t");
		int age = Integer.parseInt( lineArray[ ageIndex ] );
		return new MzPerson(
				stats,
				id94( lineArray[ personIndex ].trim() , lineArray[ hhIndex ] ),
				MzPersonFactory2000.booleanField( lineArray[ employedIndex ] ),
				MzPersonFactory2000.booleanField( lineArray[ educationIndex ] ),
				MzPersonFactory2000.dayOfWeek( lineArray[ dayOfWeekIndex ] ),
				MzPersonFactory2000.licence( age , lineArray[ licenceIndex ] ),
				age,
				Double.parseDouble( lineArray[ weightIndex ] ),
				MzPersonFactory2000.gender( lineArray[ genderIndex] ));
	}

	public static Id id94(final String pers , final String hh) {
		return new IdImpl( pers.trim() + "-" + hh.trim() );
	}
}

