/* *********************************************************************** *
 * project: org.matsim.*
 * MzWegFactory1994.java
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

import playground.thibautd.initialdemandgeneration.old.activitychainsextractor.MzWeg.Purpose;

/**
 * @author thibautd
 */
public class MzWegFactory1994 implements MzWegFactory {
	private static final String HH_NAME = "HAUSHALT";
	private static final String PERSON_NAME = "PERSON";
	private static final String WEGNR_NAME = "WEG";
	private static final String DEPARTURE_TIME_NAME = "WG01";
	private static final String ARRIVAL_TIME_NAME = "WG02";
	private static final String DISTANCE_NAME = "WG_DIST";
	private static final String DURATION_NAME = "WG_DAUER";
	private static final String PURPOSE_NAME = "WG03";
	private static final String START_ORT_NAME = "WG_ORT";
	private static final String START_STREET_NAME = "WG_STR";
	private static final String START_STREET_NR_NAME = "WG_HAUS";
	private static final String END_ORT_NAME = "WG_ZORT";
	private static final String END_STREET_NAME = "WG_ZSTR";
	private static final String END_STREET_NR_NAME = "WG_ZHAUS";
	private static final String MODE_NAME = "WG_WMITT";

	private final int hhIndex;
	private final int personIndex;
	private final int wegnrIndex;
	private final int departureTimeIndex;
	private final int arrivalTimeIndex;
	private final int distanceIndex;
	private final int durationIndex;
	private final int purposeIndex;
	private final int startOrtIndex;
	private final int startStreetIndex;
	private final int startStreetNrIndex;
	private final int endOrtIndex;
	private final int endStreetIndex;
	private final int endStreetNrIndex;
	private final int modeIndex;

	public MzWegFactory1994( final String headLine ) {
		String[] names = headLine.split("\t");
		int hhIndex1 = -1;
		int personIndex1 = -1;
		int wegnrIndex1 = -1;
		int departureTimeIndex1 = -1;
		int arrivalTimeIndex1 = -1;
		int distanceIndex1 = -1;
		int durationIndex1 = -1;
		int purposeIndex1 = -1;
		int startOrtIndex1 = -1;
		int startStreetIndex1 = -1;
		int startStreetNrIndex1 = -1;
		int endOrtIndex1 = -1;
		int endStreetIndex1 = -1;
		int endStreetNrIndex1 = -1;
		int modeIndex1 = -1;

		for (int i=0; i < names.length; i++) {
			if (names[ i ].equals( HH_NAME )) {
				hhIndex1 = i;
			}
			if (names[ i ].equals( PERSON_NAME )) {
				personIndex1 = i;
			}
			if (names[ i ].equals( WEGNR_NAME )) {
				wegnrIndex1 = i;
			}
			if (names[ i ].equals( DEPARTURE_TIME_NAME )) {
				departureTimeIndex1 = i;
			}
			if (names[ i ].equals( ARRIVAL_TIME_NAME )) {
				arrivalTimeIndex1 = i;
			}
			if (names[ i ].equals( DISTANCE_NAME )) {
				distanceIndex1 = i;
			}
			if (names[ i ].equals( DURATION_NAME )) {
				durationIndex1 = i;
			}
			if (names[ i ].equals( PURPOSE_NAME )) {
				purposeIndex1 = i;
			}
			if (names[ i ].equals( START_ORT_NAME )) {
				startOrtIndex1 = i;
			}
			if (names[ i ].equals( START_STREET_NAME )) {
				startStreetIndex1 = i;
			}
			if (names[ i ].equals( START_STREET_NR_NAME )) {
				startStreetNrIndex1 = i;
			}
			if (names[ i ].equals( END_ORT_NAME )) {
				endOrtIndex1 = i;
			}
			if (names[ i ].equals( END_STREET_NAME )) {
				endStreetIndex1 = i;
			}
			if (names[ i ].equals( END_STREET_NR_NAME )) {
				endStreetNrIndex1 = i;
			}
			if (names[ i ].equals( MODE_NAME )) {
				modeIndex1 = i;
			}
		}

		this.hhIndex = hhIndex1;
		this.personIndex = personIndex1;
		this.wegnrIndex = wegnrIndex1;
		this.departureTimeIndex = departureTimeIndex1;
		this.arrivalTimeIndex = arrivalTimeIndex1;
		this.distanceIndex = distanceIndex1;
		this.durationIndex = durationIndex1;
		this.purposeIndex = purposeIndex1;
		this.startOrtIndex = startOrtIndex1;
		this.startStreetIndex = startStreetIndex1;
		this.startStreetNrIndex = startStreetNrIndex1;
		this.endOrtIndex = endOrtIndex1;
		this.endStreetIndex = endStreetIndex1;
		this.endStreetNrIndex = endStreetNrIndex1;
		this.modeIndex = modeIndex1;
	}

	@Override
	public MzWeg createMzWeg(final String line) {
		String[] lineArray = line.split( "\t" );
		return new MzWeg(
				MzPersonFactory1994.id94( lineArray[ personIndex ] , lineArray[ hhIndex ] ),
				new IdImpl( lineArray[ wegnrIndex ].trim() ),
				MzWegFactory2000.time( lineArray[ departureTimeIndex ] ),
				MzWegFactory2000.time( lineArray[ arrivalTimeIndex ] ),
				MzWegFactory2000.distance( lineArray[ distanceIndex ] ),
				MzWegFactory2000.time( lineArray[ durationIndex ] ),
				MzAdress.createAdress(
						lineArray[ startOrtIndex ],
						lineArray[ startStreetIndex ],
						lineArray[ startStreetNrIndex ] ),
				MzAdress.createAdress(
						lineArray[ endOrtIndex ],
						lineArray[ endStreetIndex ],
						lineArray[ endStreetNrIndex ] ),
				purpose1994( lineArray[ purposeIndex ] ),
				MzWegFactory2000.mode( lineArray[ modeIndex ] ));
	}

	private static Purpose purpose1994( final String value ) {
		int i = Integer.parseInt( value.trim() );

		switch ( i ) {
			case 1: // Arbeit
				return Purpose.work;
			case 2: // Ausbildung
				return Purpose.educ;
			case 3: // Einkauf / Besorgungen
				return Purpose.shop;
			case 5: // Geschäftliche Tätigkkeit
				return Purpose.commercialActivity;
			case 4: // Freizeit
				return Purpose.leisure;
			case 9: // keine Angabe
			default:
				return Purpose.unknown;
		}
	}
}

