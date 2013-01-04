/* *********************************************************************** *
 * project: org.matsim.*
 * MzPersonFactory2010.java
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

import org.matsim.core.basic.v01.IdImpl;

import playground.thibautd.initialdemandgeneration.old.activitychainsextractor.MzConfig.Statistics;

/**
 * @author thibautd
 */
public class MzPersonFactory2010  implements MzPersonFactory {
	// some innovation this year: questions for which several answer
	// may be possible (example: being both a student and living on a rent)
	// apear several times, with names CODEa CODEb etc.
	// NOTE: there are also fields:
	// -AMSTAT (arbeitsmarketstatus)
	// -ESTATUS (nichterwerbstatus)
	// -BSTELL (beruflichestellung)
	private static final String NON_EMPLOYED_ACT_REGEXP = "f41001*";
	private static final String EMPLOYED_ADITIONAL_OCCUPATION_REGEXP = "f41000*";

	private static final String ARBEIT_MARKET_STATUS_NAME = "AMSTAT";

	// actually, "did you work last week"
	//private static final String EMPLOYED_NAME = "f40500";
	private static final String ZPNR_NAME = "ZIELPNR";
	private static final String HHNR_NAME = "HHNR";
	private static final String DOW_NAME = "tag";
	//private static final String CAR_AVAIL_NAME = "f42100e";
	private static final String CAR_LICENCE_NAME = "f20400a";
	private static final String MOTORBIKE_LICENCE_NAME = "f20400b";
	private static final String AGE_NAME = "alter";
	private static final String WEIGHT_NAME = "WP";
	private static final String GENDER_NAME = "gesl";

	// rather than detecting the fields (which becomes messy with
	// reg exps and array), we hard code them and just check the column names
	// at initialisation.
	private final int employedIndex = 177;
	private final int[] nonWorkActivitiesIndices = {36,37,38,39,40,41};
	private final int personIndex = 2;
	private final int hhIndex = 1;
	private final int dayOfWeekIndex = 11;
	private final int carLicenceIndex = 194;
	private final int motorbikeLicenceIndex = 195;
	private final int ageIndex = 189;
	private final int weightIndex = 3;
	private final int genderIndex = 191;

	private final Statistics stats;

	public MzPersonFactory2010(final Statistics stats , final String titleLine) {
		this.stats = stats;
		String[] names = titleLine.split("\t");

		checkEquals( employedIndex , names , ARBEIT_MARKET_STATUS_NAME );
		checkEquals( personIndex , names , ZPNR_NAME );
		checkEquals( hhIndex , names , HHNR_NAME );
		checkEquals( dayOfWeekIndex , names , DOW_NAME );
		checkEquals( carLicenceIndex , names , CAR_LICENCE_NAME );
		checkEquals( motorbikeLicenceIndex , names , MOTORBIKE_LICENCE_NAME );
		checkEquals( ageIndex , names , AGE_NAME );
		checkEquals( weightIndex , names , WEIGHT_NAME );
		checkEquals( genderIndex , names , GENDER_NAME );
		for (int ind : nonWorkActivitiesIndices) {
			checkRegExp( ind , names , EMPLOYED_ADITIONAL_OCCUPATION_REGEXP+"|"+NON_EMPLOYED_ACT_REGEXP );
		}
	}

	private static void checkEquals( final int index, final String[] titles, final String expected ) {
		if (!titles[ index ].equals( expected )) {
			throw new RuntimeException( "expected to find "+expected+" at index "+index+". found "+titles[ index ] );
		}
	}

	private static void checkRegExp( final int index, final String[] titles, final String expected ) {
		if (!titles[ index ].matches( expected )) {
			throw new RuntimeException( "expected to find a match for "+expected+" at index "+index+". found "+titles[ index ] );
		}
	}

	@Override
	public MzPerson createMzPersonFromZpLine(final String zpDataLine) {
		String[] lineArray = zpDataLine.split("\t");
		int age = Integer.parseInt( lineArray[ ageIndex ] );
		return new MzPerson(
				stats,
				MzPersonFactory1994.id94( lineArray[ personIndex ].trim() , lineArray[ hhIndex ].trim() ),
				booleanField( lineArray[ employedIndex ] ),
				education( lineArray ),
				dayOfWeek( lineArray[ dayOfWeekIndex ] ),
				licence( age , lineArray[ carLicenceIndex ] , lineArray[ motorbikeLicenceIndex ] ),
				age,
				Double.parseDouble( lineArray[ weightIndex ] ),
				gender( lineArray[ genderIndex] ));
	}

	// /////////////////////////////////////////////////////////////////////////
	// helpers
	public static Boolean licence(final int age, final String car, final String bike) {
		// for children, no value
		if ( age < 18 ) return false;
		return booleanField( car ) || booleanField( bike );
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

	private boolean education( final String[] line ) {
		for (int ind : nonWorkActivitiesIndices) {
			if ( line[ ind ].trim().equals( "32" ) ) {
				return true;
			}
		}
		return false;
	}
}
