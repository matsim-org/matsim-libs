/* *********************************************************************** *
 * project: org.matsim.*
 * SfEuropeCountryFilter
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
package air.scenario;

import java.util.HashSet;
import java.util.Set;

import air.scenario.countryfilter.DgCountryFilter;

/**
 * @author dgrether
 * 
 */
public class SfEuropeCountryFilter implements DgCountryFilter {

	private final static String[] EURO_COUNTRIES = { "AD", "AL", "AM", "AT", "AX", "AZ", "BA", "BE",
			"BG", "BY", "CH", "CY", "CZ", "DE", "DK", "EE", "ES", "FI", "FO", "FR", "GB", "GI", "GE",
			"GG", "GR", "HR", "HU", "IE", "IM", "IS", "IT", "JE", "KZ", "LI", "LT", "LU", "LV", "MC",
			"MD", "ME", "MK", "MT", "NL", "NO", "PL", "PT", "RO", "RS", "RU", "SE", "SI", "SJ", "SK",
			"SM", "TR", "UA", "VA" };

	private Set<String> countryFilter;

	public SfEuropeCountryFilter() {
		countryFilter = new HashSet<String>();
		for (String s : EURO_COUNTRIES) {
			this.countryFilter.add(s.toUpperCase());
		}
	}

	@Override
	public boolean isCountryOfInterest(String originCountry, String destinationCountry) {
		if (countryFilter.contains(originCountry) && countryFilter.contains(destinationCountry)) {
			return true;
		}
		return false;
	}

}
