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
package playground.yu.analysis.forMuc;

/**
 * keep some general tools for Analysis for Zurich and respectively Kanton
 * Zurich
 * 
 * @author yu
 * 
 */
public interface Analysis4Muc {
	public enum ActType {
		unknown("unknown"), work("work"), education("education"), business(
				"business"), shopping("shopping"), private_("private"), leisure(
				"leisure"), sports("sports"), home("home"), friends("friends"), pickup(
				"pickup"), with_adult("with adult"), other("other");

		private final String actTypeName;

		public String getActTypeName() {
			return actTypeName;
		}

		ActType(String actTypeName) {
			this.actTypeName = actTypeName;
		}
	}
}
