/* *********************************************************************** *
 * project: org.matsim.*
 * HomeSweetHome.java
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
package playground.thibautd.geocoding;

/**
 * This is a test, but I do not want to use a unit test,
 * as it would eat my request limit, as well as fail in case
 * of internet connection problem.
 * @author thibautd
 */
public class HomeSweetHome {
	public static void main(final String[] args) {
		final Address address = new Address();
		address.setStreet( "Langwiesstrasse" );
		address.setNumber( "29" );
		address.setZipcode( "8050" );
		address.setMunicipality( "Zürich" );
		address.setCountry( "Schweiz" );

		final GoogleAPIResult result = new GoogleAPIUtils().getLocation( address );
		System.out.println( "Status "+result.getStatus() );
		System.out.println( "LocationType="+result.getLocationType() );
		System.out.println( "lat="+result.getLatitude() );
		System.out.println( "lng="+result.getLongitude() );
		System.out.println( "formattedAddress="+result.getFormattedAddress() );
	}
}

