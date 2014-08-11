/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.boescpa.converters.vissim.tools;

import org.matsim.api.core.v01.Id;
import playground.boescpa.converters.vissim.ConvEvents2Anm;

import java.util.HashMap;

/**
 * WHAT IS IT FOR?
 * WHAT DOES IT?
 *
 * @author boescpa
 */
public class DefaultAnmConverter implements ConvEvents2Anm.AnmConverter {
	@Override
	public HashMap<Id, Long[]> convertRoutes(HashMap<Id, Id[]> keyAmNetwork, String path2AnmFile) {
		return null;
	}

	@Override
	public void writeAnmRoutes(HashMap<Id, Integer> demandPerAnmTrip, String path2AnmFile, String path2NewAnmFile) {

	}
}
