/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package playground.boescpa.projects.netcap.preparation;

import playground.boescpa.lib.tools.scenarioUtils.UsedFacilitiesExtraction;

/**
 * If there are no intentions to use location choice this file reduces all facilities to the only used ones.
 * For the creation of the f2l, the network should only contain the car-links...
 *
 * @author boescpa
 */
public class NCFacilities {

	public static void main(String[] args) {
        UsedFacilitiesExtraction.main(args);
	}

}
