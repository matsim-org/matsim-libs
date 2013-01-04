/* *********************************************************************** *
 * project: org.matsim.*
 * MzWegFactory2000.java
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

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.basic.v01.IdImpl;

import playground.thibautd.initialdemandgeneration.old.activitychainsextractor.MzWeg.Purpose;

/**
 * @author thibautd
 */
public class MzWegFactory2000 implements MzWegFactory {
	private static final String PERSON_NAME = "INTNR";
	private static final String WEGNR_NAME = "WEG";
	private static final String DEPARTURE_TIME_NAME = "WVON";
	private static final String ARRIVAL_TIME_NAME = "WBIS";
	private static final String DISTANCE_NAME = "WEGDIST";
	private static final String DURATION_NAME = "WDAUER2";
	private static final String PURPOSE_NAME = "WZWECK2";
	private static final String START_ORT_NAME = "W61201";
	private static final String START_STREET_NAME = "W61202";
	private static final String START_STREET_NR_NAME = "W61203";
	private static final String END_ORT_NAME = "W61601";
	private static final String END_STREET_NAME = "W61602";
	private static final String END_STREET_NR_NAME = "W61603";


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

	public MzWegFactory2000( final String headLine ) {
		String[] names = headLine.split("\t");
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

		for (int i=0; i < names.length; i++) {
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
		}

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
	}

	@Override
	public MzWeg createMzWeg(final String line) {
		String[] lineArray = line.split( "\t" );
		return new MzWeg(
				new IdImpl( lineArray[ personIndex ].trim() ),
				new IdImpl( lineArray[ wegnrIndex ].trim() ),
				time( lineArray[ departureTimeIndex ] ),
				time( lineArray[ arrivalTimeIndex ] ),
				distance( lineArray[ distanceIndex ] ),
				time( lineArray[ durationIndex ] ),
				MzAdress.createAdress(
						lineArray[ startOrtIndex ],
						lineArray[ startStreetIndex ],
						lineArray[ startStreetNrIndex ] ),
				MzAdress.createAdress(
						lineArray[ endOrtIndex ],
						lineArray[ endStreetIndex ],
						lineArray[ endStreetNrIndex ] ),
				purpose2000( lineArray[ purposeIndex ] ),
				null);
	}

	public static String mode( final String mode ) {
		int i = Integer.parseInt( mode.trim() );

		switch ( i ) {
			case 1: //"zu Fuss"
				return TransportMode.walk;
			case 2: //"Velo"
				return TransportMode.bike;
			case 5: //"Auto"
				return TransportMode.car;
			case 6: //"Bahn"
			case 7: //"Postauto"
			case 8: //"Bus und Tram"
				return TransportMode.pt;
			case 9: //"andere"
			case 3: //"Mofa"
			case 4: //"Moto"
			default:
				return "unknown";
		}
	}

	public static double time( final String value ) {
		// min -> secs
		return Double.parseDouble( value ) * 60d;
	}

	public static double distance( final String value ) {
		// km -> m
		return Double.parseDouble( value ) * 1000d;
	}

	private static Purpose purpose2000( final String value ) {
		int i = Integer.parseInt( value.trim() );

		switch ( i ) {
			case 0: // Umsteigen / Verkehrsmittelwechsel
				return Purpose.transitTransfer;
			case 1: // Arbeit
				return Purpose.work;
			case 2: // Ausbildung
				return Purpose.educ;
			case 3: // Einkauf / Besorgungen
				return Purpose.shop;
			case 4: // Geschäftliche Tätigkkeit
				return Purpose.commercialActivity;
			case 5: // Dienstfahrt
				return Purpose.useService;
			case 6: // Freizeit
				return Purpose.leisure;
			case 7: // Serviceweg
				return Purpose.servePassengerRide;
			case 8: // Begleitweg
				return Purpose.accompany;
			case 9: // keine Angabe
			default:
				return Purpose.unknown;
		}
	}
}

