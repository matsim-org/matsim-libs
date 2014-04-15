/* *********************************************************************** *
 * project: org.matsim.*
 * Adress.java
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
public class Address {
	// note: this is not final, because otherwise it leads to a confusing constructor
	private String street;
	private String number;
	private String zipcode;
	private String municipality;
	private String country;

	private String id;

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		if ( this.id != null ) throw new IllegalStateException();
		this.id = id;
	}
	
	public String getStreet() {
		return street;
	}
	public String getNumber() {
		return number;
	}
	public String getZipcode() {
		return zipcode;
	}
	public String getMunicipality() {
		return municipality;
	}
	public String getCountry() {
		return country;
	}
	public void setStreet(String street) {
		if ( this.street != null ) throw new IllegalStateException();
		this.street = street;
	}
	public void setNumber(String number) {
		if ( this.number != null ) throw new IllegalStateException();
		this.number = number;
	}
	public void setZipcode(String zipcode) {
		if ( this.zipcode != null ) throw new IllegalStateException();
		this.zipcode = zipcode;
	}
	public void setMunicipality(String municipality) {
		if ( this.municipality != null ) throw new IllegalStateException();
		this.municipality = municipality;
	}
	public void setCountry(String country) {
		if ( this.country != null ) throw new IllegalStateException();
		this.country = country;
	}

	@Override
	public String toString() {
 		return "{Adress id: "+id+"; "+
 			"street="+street+"; "+
			"number="+number+"; "+
			"zipcode="+zipcode+"; "+
			"municipality="+municipality+"; "+
			"country="+country+"}";
	}
}

