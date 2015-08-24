/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,     *
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

package playground.southafrica.sandboxes.ndekoker.test;

import org.apache.log4j.Logger;

import playground.southafrica.utilities.Header;


public class TestHeader {
	private final static Logger LOG = Logger.getLogger(TestHeader.class);

	public static void main(String[] args) {
		Header.printHeader(TestHeader.class.toString(), args);
		
		LOG.info("First line...");
		LOG.info("My first log message.");
		
		LOG.warn("Oops, something changed.");
		
		Header.printFooter();
	}

}
