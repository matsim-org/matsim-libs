/* *********************************************************************** *
 * project: org.matsim.*
 * PlaygroundTest.java
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

package playground.david;

import org.matsim.core.basic.v01.IdImpl;

public class PlaygroundTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		IdImpl i1 = new IdImpl("20");
		IdImpl i2 = new IdImpl("20");
		String s1 = "20";
		String s2 = "20";
		boolean erg = i1.equals(i2);
		boolean serg = s1.equals(s2);
		System.out.println(erg);
	}

}
