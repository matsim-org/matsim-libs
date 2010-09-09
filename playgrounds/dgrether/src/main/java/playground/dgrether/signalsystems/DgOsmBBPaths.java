/* *********************************************************************** *
 * project: org.matsim.*
 * DgOsmBBPaths
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.dgrether.signalsystems;

import playground.dgrether.DgPaths;


/**
 * @author dgrether
 *
 */
public interface DgOsmBBPaths {
	
	public static final String BASE_IN_DIR = DgPaths.SHAREDSVN + "studies/countries/de/osm_berlinbrandenburg/workingset/";

	public static final String BASE_OUT_DIR = DgPaths.STUDIESDG + "osmBerlinSzenario/";
	
	public static final String NETWORK_GENERATED = BASE_OUT_DIR + "osm_bb_network.xml";
	
	
	
}
