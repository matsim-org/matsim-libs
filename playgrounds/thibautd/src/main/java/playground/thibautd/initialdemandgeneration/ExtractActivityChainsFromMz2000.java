/* *********************************************************************** *
 * project: org.matsim.*
 * ExtractActivityChainsFromMz2000.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.initialdemandgeneration;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.population.PopulationWriter;

/**
 * executable class to extract activity chains from MZ2000
 * @author thibautd
 */
public class ExtractActivityChainsFromMz2000 {
	/**
	 * usage: ExtractActivityChainsFromMz2000 zpFile wgFile etFile outFile
	 */
	public static void main(final String[] args) {
		Mz2000ActivityChainsExtractor extractor = new Mz2000ActivityChainsExtractor();
		Scenario scen = extractor.run( args[ 0 ] , args[ 1 ] , args[ 2 ] );
		(new PopulationWriter(scen.getPopulation(),
							  scen.getNetwork())).write( args[ 3 ] );
	}
}

