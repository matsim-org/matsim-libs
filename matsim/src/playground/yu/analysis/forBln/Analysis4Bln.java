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
package playground.yu.analysis.forBln;

/**
 * keep some general tools for Analysis for Zurich and respectively Kanton
 * Zurich
 * 
 * @author yu
 * 
 */
public interface Analysis4Bln {
	public enum ActType {
		home("home"), work("work"), shopping("shopping"), education("education"), leisure(
				"leisure"), other("other"), not_specified("not specified"), business(
				"business"), Einkauf_sonstiges("Einkauf sonstiges"), Freizeit_sonstiges_incl_Sport(
				"Freizeit (sonstiges incl.Sport)"), see_a_doctor("see a doctor"), holiday_journey(
				"holiday / journey"), multiple("multiple");
		private final String actTypeName;

		public String getActTypeName() {
			return actTypeName;
		}

		ActType(String actTypeName) {
			this.actTypeName = actTypeName;
		}
	}
}
