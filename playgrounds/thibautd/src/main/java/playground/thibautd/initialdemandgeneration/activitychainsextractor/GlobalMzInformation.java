/* *********************************************************************** *
 * project: org.matsim.*
 * GlobalMzInformation.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

import org.apache.log4j.Logger;

/**
 * class providing static methods to get information about the settings
 * for the current session.
 *
 * Settings are for the moment the MZ year.
 *
 * @author thibautd
 */
public class GlobalMzInformation {
	private static final Logger log =
		Logger.getLogger(GlobalMzInformation.class);

	private GlobalMzInformation() {}

	private static int internalYear = -1;

	/**
	 * @throws IllegalStateException if the year has already been set
	 * @throws IllegalArgumentException if the year is negative
	 */
	public static void setMzYear(final int year) {
		if (internalYear > 0) {
			throw new IllegalStateException( "The year is already set" );
		}
		else if (year <= 0) {
			throw new IllegalArgumentException( "Really? A swiss microcesus in "+(-year)+" B.C.?" );
		}
		else {
			log.info( "setting MZ year to "+year );
			internalYear = year;
		}
	}

	/**
	 * @throws IllegalStateException if the year is not yet set
	 */
	public static int getMzYear() {
		if (internalYear <= 0) {
			throw new IllegalStateException( "The year not yet set" );
		}
		return internalYear;
	}
}

