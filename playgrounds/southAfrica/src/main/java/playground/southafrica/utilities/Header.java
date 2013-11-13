/* *********************************************************************** *
 * project: org.matsim.*
 * Header.java
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

package playground.southafrica.utilities;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.Gbl;

public class Header {
	private final static Logger LOG = Logger.getLogger(Header.class);
	
	public static void printHeader(String classString, String[] args){
		LOG.info("======================================================================");
		LOG.info(classString);
		LOG.info("----------------------------------------------------------------------");
		if ( args != null ) {
			for(int i = 0; i < args.length; i++){
				LOG.info("args[" + i + "]: " + args[i]);
			}
		}
		LOG.info("----------------------------------------------------------------------");
		Gbl.printSystemInfo();
		Gbl.startMeasurement();
	}

	public static void printFooter(){
		Gbl.printElapsedTime();
		LOG.info("----------------------------------------------------------------------");
		LOG.info("                               Done");
		LOG.info("======================================================================");
	}

}

