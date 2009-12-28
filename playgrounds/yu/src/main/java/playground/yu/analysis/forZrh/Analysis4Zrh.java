/* *********************************************************************** *
 * project: org.matsim.*
 * Analysis4Zrh.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.yu.analysis.forZrh;

/**
 * keep some general tools for Analysis for Zurich and respectively Kanton
 * Zurich
 * 
 * @author yu
 * 
 */
public interface Analysis4Zrh {
	static final String KANTON_ZURICH = "Kanton_Zurich", ZURICH = "Zurich";

	public enum ActType {
		home("h"), work("w"), shopping("s"), education("e"), leisure("l"), others(
				"o");
		private final String firstLetter;

		public String getFirstLetter() {
			return firstLetter;
		}

		ActType(String firstLetter) {
			this.firstLetter = firstLetter;
		}
	}
}
