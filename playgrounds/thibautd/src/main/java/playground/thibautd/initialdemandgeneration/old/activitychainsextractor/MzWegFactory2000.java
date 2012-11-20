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

	public MzWegFactory2000( final String headLine ) {
		String[] names = headLine.split("\t");
		int hhIndex = -1;
		int personIndex = -1;
		int wegnrIndex = -1;
		int departureTimeIndex = -1;
		int arrivalTimeIndex = -1;
		int distanceIndex = -1;
		int durationIndex = -1;
		int purposeIndex = -1;
		int startOrtIndex = -1;
		int startStreetIndex = -1;
		int startStreetNrIndex = -1;
		int endOrtIndex = -1;
		int endStreetIndex = -1;
		int endStreetNrIndex = -1;
		int modeIndex = -1;

		for (int i=0; i < names.length; i++) {
			if (names[ i ].equals( PERSON_NAME )) {
				personIndex = i;
			}
			if (names[ i ].equals( WEGNR_NAME )) {
				wegnrIndex = i;
			}
			if (names[ i ].equals( DEPARTURE_TIME_NAME )) {
				departureTimeIndex = i;
			}
			if (names[ i ].equals( ARRIVAL_TIME_NAME )) {
				arrivalTimeIndex = i;
			}
			if (names[ i ].equals( DISTANCE_NAME )) {
				distanceIndex = i;
			}
			if (names[ i ].equals( DURATION_NAME )) {
				durationIndex = i;
			}
			if (names[ i ].equals( PURPOSE_NAME )) {
				purposeIndex = i;
			}
			if (names[ i ].equals( START_ORT_NAME )) {
				startOrtIndex = i;
			}
			if (names[ i ].equals( START_STREET_NAME )) {
				startStreetIndex = i;
			}
			if (names[ i ].equals( START_STREET_NR_NAME )) {
				startStreetNrIndex = i;
			}
			if (names[ i ].equals( END_ORT_NAME )) {
				endOrtIndex = i;
			}
			if (names[ i ].equals( END_STREET_NAME )) {
				endStreetIndex = i;
			}
			if (names[ i ].equals( END_STREET_NR_NAME )) {
				endStreetNrIndex = i;
			}
		}

		this.hhIndex = hhIndex;
		this.personIndex = personIndex;
		this.wegnrIndex = wegnrIndex;
		this.departureTimeIndex = departureTimeIndex;
		this.arrivalTimeIndex = arrivalTimeIndex;
		this.distanceIndex = distanceIndex;
		this.durationIndex = durationIndex;
		this.purposeIndex = purposeIndex;
		this.startOrtIndex = startOrtIndex;
		this.startStreetIndex = startStreetIndex;
		this.startStreetNrIndex = startStreetNrIndex;
		this.endOrtIndex = endOrtIndex;
		this.endStreetIndex = endStreetIndex;
		this.endStreetNrIndex = endStreetNrIndex;
		this.modeIndex = modeIndex;
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

