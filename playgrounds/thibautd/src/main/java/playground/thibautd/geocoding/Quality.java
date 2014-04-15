/* *********************************************************************** *
 * project: org.matsim.*
 * Quality.java
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
 * @author thibautd
 */
public class Quality implements Comparable<Quality> {
	private boolean countryMatch = false;
	private boolean zipMatch = false;
	private boolean municipalityMatch = false;
	private boolean streetMatch = false;
	private boolean numberMatch = false;

	@Override
	public int compareTo(final Quality o) {
		// "lexicographic" order
	
		if ( countryMatch && !o.countryMatch ) return 1;
		if ( !countryMatch && o.countryMatch ) return -1;

		if ( municipalityMatch && !o.municipalityMatch ) return 1;
		if ( !municipalityMatch && o.municipalityMatch ) return -1;

		if ( zipMatch && !o.zipMatch ) return 1;
		if ( !zipMatch && o.zipMatch ) return -1;

		if ( streetMatch && !o.streetMatch ) return 1;
		if ( !streetMatch && o.streetMatch ) return -1;

		if ( numberMatch && !o.numberMatch ) return 1;
		if ( !numberMatch && o.numberMatch ) return -1;

		assert equals( o );
		return 0;
	}

	@Override
	public boolean equals(final Object o) {
		if ( !o.getClass().equals( getClass() ) ) return false;

		final Quality other = (Quality) o;
		return other.zipMatch == zipMatch &&
			other.municipalityMatch == municipalityMatch &&
			other.streetMatch == streetMatch &&
			other.numberMatch == numberMatch &&
			other.countryMatch == countryMatch;
	}

	@Override
	public int hashCode() {
		int h = 0;
		if ( zipMatch ) h+=1;
		if ( municipalityMatch ) h+=10;
		if ( streetMatch ) h+=100;
		if ( numberMatch ) h+=1000;
		if ( countryMatch ) h+=10000;
		return h;
	}

	@Override
	public String toString() {
		final StringBuilder b =  new StringBuilder( "q" );

		if ( countryMatch ) b.append( 'C' );
		if ( zipMatch ) b.append( 'Z' );
		if ( municipalityMatch ) b.append( 'M' );
		if ( streetMatch ) b.append( 'S' );
		if ( numberMatch ) b.append( 'N' );

		return b.toString();
	}

	public boolean isZipMatch() {
		return zipMatch;
	}
	public void setZipMatch(boolean zipMatch) {
		this.zipMatch = zipMatch;
	}
	public boolean isMunicipalityMatch() {
		return municipalityMatch;
	}
	public void setMunicipalityMatch(boolean municipalityMatch) {
		this.municipalityMatch = municipalityMatch;
	}
	public boolean isStreetMatch() {
		return streetMatch;
	}
	public void setStreetMatch(boolean streetMatch) {
		this.streetMatch = streetMatch;
	}
	public boolean isNumberMatch() {
		return numberMatch;
	}
	public void setNumberMatch(boolean numberMatch) {
		this.numberMatch = numberMatch;
	}
	public boolean isCountryMatch() {
		return countryMatch;
	}
	public void setCountryMatch(boolean countryMatch) {
		this.countryMatch = countryMatch;
	}
}

