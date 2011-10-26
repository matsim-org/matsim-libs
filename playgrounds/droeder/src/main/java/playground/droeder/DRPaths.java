/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.droeder;


/**
 * @author droeder
 *
 */
public interface DRPaths {
	
	final String VSP =  "D:/VSP/";
	
	final String SHAREDSVN = VSP + "shared-svn/";
		final String BVG09 = SHAREDSVN + "bvg09_urdaten/";
		
	final String STUDIES = SHAREDSVN + "studies/";
		final String DASTUDIES = STUDIES + "_droeder/";
			final String STUDIESSKETCH = DASTUDIES + "sketchPlanning/";
			
		final String BERLIN = STUDIES + "countries/de/berlin/";
			final String B_COUNTS = BERLIN + "_counts/";
			final String B_NETWORK = BERLIN + "_network/";

	final String PROJECTS = VSP + "projects/";
		final String SIM2D = VSP + "2D_Sim/";
		final String SKETCH = PROJECTS + "sketchPlanning/";
			
	
	
	



}
