/* *********************************************************************** *
 * project: org.matsim.*
 * DgEuropeCountryFilter
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
package air.scenario.countryfilter;

import java.util.HashSet;
import java.util.Set;


/**
 * @author dgrether
 *
 */
public class DgEuropeCountryFilter implements DgCountryFilter {

	private final static String[] EURO_COUNTRIES = {
		"AT",
		"BE",
		"BG",
		"CY",
		"CZ",
		"DE",
		"DK",
		"EE",
		"EL",
		"ES",
		"FI",
		"FR",
		"GB",
		"GR",
		"HU",
		"IE",
		"IT",
		"LV",
		"LT",
		"LU",
		"MT",
		"NL",
		"PL",
		"PT",
		"RO",
		"SE",
		"SI",
		"SK",
		"UK",
		"CR", //candidates
		"ME",
		"IS",
		"RS",
		"TR"
	}; 
	
	private Set<String> countryFilter;

	private boolean doAndFilter = false;
	
	public DgEuropeCountryFilter(boolean doAndfilter) {
		this.doAndFilter  = doAndfilter;
		countryFilter = new HashSet<String>();
		for (String s : EURO_COUNTRIES){
			this.countryFilter.add(s.toUpperCase());
		}
	}

	@Override
	public boolean isCountryOfInterest(String originCountry, String destinationCountry) {
		if (doAndFilter){
			return countryFilter.contains(originCountry) && countryFilter.contains(destinationCountry);
		}
		else {
			return countryFilter.contains(originCountry) || countryFilter.contains(destinationCountry);
		}
	}

}
