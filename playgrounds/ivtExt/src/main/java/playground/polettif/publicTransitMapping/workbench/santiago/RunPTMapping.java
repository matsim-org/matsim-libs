/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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


package playground.polettif.publicTransitMapping.workbench.santiago;

import playground.polettif.publicTransitMapping.mapping.PTMapperModesFilterAndMerge;
import playground.polettif.publicTransitMapping.mapping.PTMapperThreaded;

public class RunPTMapping {

	public static void main(String[] args) {
		String base = "E:/data/santiago/";
//		PTMapperModesFilterAndMerge.run(base+"mts/mappingConfig.xml");
		PTMapperThreaded.run(base+"mts/mappingConfig.xml");
	}
}