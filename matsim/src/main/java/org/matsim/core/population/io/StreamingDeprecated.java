/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.core.population.io;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.algorithms.PersonAlgorithm;

/**
 * @author nagel
 *
 */
public final class StreamingDeprecated {
	private static final Logger log = LogManager.getLogger( StreamingDeprecated.class ) ;
	
	@SuppressWarnings("unused")
	public static void addAlgorithm( Population pop, PersonAlgorithm algo ) {
		printError();
	}
	static void printError() {
		log.fatal("This does not work any more after change of the streaming api.  You will need something like");
		log.fatal("StreamingPopulationReader reader = new StreamingPopulationReader( scenario ); " ) ;
		log.fatal("reader.addAlgorithm(...);") ;
		log.fatal("reader.readFile(...)" ) ;
		throw new RuntimeException("This does not work any more after change of the streaming api. ") ;
	}
	@SuppressWarnings("unused")
	public static void runAlgorithms( Population pop ) {
		printError() ;
	}

	@SuppressWarnings("unused")
	public static void setIsStreaming( Population pop, boolean isStreaming ) {
		throw new RuntimeException("you cannot set streaming any more at the regular Population(Impl).  "
				+ "Use StreamingPopulationReader, or talk to us if you need something else. kai, jul'16" ) ;
	}
	@SuppressWarnings("unused")
	public static void setIsStreaming( StreamingPopulationReader reader, boolean isStreaming ) {
		throw new RuntimeException("Code around this statement was manually adapted to revised streaming API; "
				+ "pls chk if everything is ok and then remove the call to this method from your code.  "
				+ "Pls tlk to us if you have problems.  kai, jul'16") ;
	}

}
