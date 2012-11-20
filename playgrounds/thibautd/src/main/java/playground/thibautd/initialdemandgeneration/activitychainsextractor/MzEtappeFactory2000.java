/* *********************************************************************** *
 * project: org.matsim.*
 * MzEtappeFactory2000.java
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

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.basic.v01.IdImpl;

/**
 * @author thibautd
 */
public class MzEtappeFactory2000 implements MzEtappeFactory {
	private static final String PERSON_NAME = "INTNR";
	private static final String WEGNR_NAME = "WEG";
	private static final String ETAPPENR_NAME = "ETAPPE";
	private static final String DISTANCE_NAME = "F61500";
	private static final String MODE_NAME = "F61300";

	private final int modeIndex;
	private final int personIndex;
	private final int wgnrIndex;
	private final int etappenrIndex;
	private final int distanceIndex;

	public MzEtappeFactory2000(final String headLine) {
		String[] names = headLine.split("\t");

		int modeIndex = -1;
		int personIndex = -1;
		int wgnrIndex = -1;
		int etappenrIndex = -1;
		int distanceIndex = -1;

		for (int i=0; i < names.length; i++) {
			if (names[ i ].equals( PERSON_NAME )) {
				personIndex = i;
			}
			if (names[ i ].equals( WEGNR_NAME )) {
				wgnrIndex = i;
			}
			if (names[ i ].equals( ETAPPENR_NAME )) {
				etappenrIndex = i;
			}
			if (names[ i ].equals( DISTANCE_NAME )) {
				distanceIndex = i;
			}
			if (names[ i ].equals( MODE_NAME )) {
				modeIndex = i;
			}
		}

		this.modeIndex = modeIndex;
		this.personIndex = personIndex;
		this.wgnrIndex = wgnrIndex;
		this.etappenrIndex = etappenrIndex;
		this.distanceIndex = distanceIndex;
	}

	@Override
	public MzEtappe createMzEtappe(final String line) {
		String[] lineArray = line.split( "\t" );

		return new MzEtappe(
			new IdImpl( lineArray[ personIndex ].trim() ),
			new IdImpl( lineArray[ wgnrIndex ].trim() ),
			new IdImpl( lineArray[ etappenrIndex ].trim() ),
			distance( lineArray[ distanceIndex ].trim() ),
			mode( lineArray[ modeIndex ].trim() ));
	}

	// TODO: group with methods weg?
	private String mode( final String value ) {
		int i = Integer.parseInt( value );

		switch (i) {
			case 1: // Zu Fuss
				return TransportMode.walk;
			case 2: // Velo
				return TransportMode.bike;
			case 6: // Auto als Fahrer
				return TransportMode.car;
			case 7: // Auto als Mitfahrer
				return TransportMode.ride;
			case 8: // Bahn
			case 9: // Postauto
			case 10: // Bus
			case 11: // Tram
			case 12: // Taxi
			case 13: // Reisecar
			case 14: // Lastwagen
			case 15: // Schiff
			case 16: // Flugzeug
			case 17: // Zahnradbahn, Seilbahn, Standseilbahn, Sessellift, Skilift
				return TransportMode.pt;
			case 23: // Kleinmotorrad
			case 3: // Mofa (Motorfahrrad)
			case 4: // Motorrad als Fahrer
			case 5: // Motorrad als Mitfahrer
			default: return "unknown";
		}
	}

	private double distance( final String value ) {
		// km -> m
		return Double.parseDouble( value ) * 1000d;
	}
}

