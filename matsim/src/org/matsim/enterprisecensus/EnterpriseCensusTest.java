/* *********************************************************************** *
 * project: org.matsim.*
 * EnterpriseCensusTest.java
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

package org.matsim.enterprisecensus;

import org.matsim.gbl.Gbl;

public class EnterpriseCensusTest {

	public EnterpriseCensusTest() {
		super();
	}

	public static void readWriteTest() {

		System.out.println("  creating EnterpriseCensus object... ");
		EnterpriseCensus myCensus = new EnterpriseCensus();
		System.out.println("  done.");

		System.out.println("  reading enterprise census files into EnterpriseCensus object... ");
		EnterpriseCensusParser myCensusParser = new EnterpriseCensusParser(myCensus);
		myCensusParser.parse(myCensus);
		System.out.println("  done.");

		System.out.println("  writing EnterpriseCensus object to output file... ");
		EnterpriseCensusWriter myCensusWriter = new EnterpriseCensusWriter();
		myCensusWriter.write(myCensus);
		System.out.println("  done.");
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Gbl.createConfig(args);
		Gbl.createWorld();
		
		readWriteTest();

	}

}
