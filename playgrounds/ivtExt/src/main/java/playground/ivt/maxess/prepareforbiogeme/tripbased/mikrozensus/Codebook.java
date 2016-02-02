/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.ivt.maxess.prepareforbiogeme.tripbased.mikrozensus;

import org.matsim.core.utils.collections.MapUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author thibautd
 */
public class Codebook {
	private Map<String, Codepage> pages = new LinkedHashMap<>();

	private Codepage currentPage = null;
	private String meaning = null;
	private Number coding = null;

	public void openPage( final String name ) {
		currentPage = pages.get( name );

		if ( currentPage == null ) {
			currentPage = new Codepage( name );
			pages.put( name, currentPage );
		}
	}

	public void writeMeaning( final String meaning ) {
		this.meaning = meaning;
	}

	public void writeCoding( final Number coding ) {
		this.coding = coding;
	}

	public void closePage() {
		currentPage.add( meaning, coding );
		currentPage = null;
		meaning = null;
		coding = null;
	}

	public Map<String, Codepage> getPages() {
		return Collections.unmodifiableMap( pages );
	}

	public static class Codepage {
		private final String variableName;

		private final Map<Number, String> codingToMeaning = new TreeMap<>(  );
		private final Map<Number, Integer> codingToCount = new TreeMap<>(  );

		private Codepage( String variableName ) {
			this.variableName = variableName;
		}

		private void add( final String meaning , final Number coding ) {
			codingToMeaning.put( coding , meaning );
			MapUtils.addToInteger( coding , codingToCount , 0 , 1 );
		}

		public String getVariableName() {
			return variableName;
		}

		public Map<Number, String> getCodingToMeaning() {
			return Collections.unmodifiableMap( codingToMeaning );
		}

		public Map<Number, Integer> getCodingToCount() {
			return Collections.unmodifiableMap( codingToCount );
		}

	}
}
