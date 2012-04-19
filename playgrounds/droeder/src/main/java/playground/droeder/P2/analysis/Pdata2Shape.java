/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
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
package playground.droeder.P2.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;

import playground.andreas.P2.plan.PPlan;
import playground.droeder.gis.DaShapeWriter;

/**
 * @author droeder
 *
 */
public class Pdata2Shape {
	
	public static void pPlans2Shape(List<PPlan> plans, String outFile){
		Map<String, List<Coord>> lineStrings = new HashMap<String, List<Coord>>();
		
		List<Coord> temp;
		int i = 0;
		for(PPlan p: plans){
			temp = new ArrayList<Coord>();
			for(TransitRouteStop s: p.getLine().getRoutes().values().iterator().next().getStops()){
				temp.add(s.getStopFacility().getCoord());
			}
			lineStrings.put(String.valueOf(i) + " " + p.getId().toString(), temp);
			i++;
		}
		
		DaShapeWriter.writeDefaultLineStrings2Shape(outFile, "cooperatives", lineStrings);
	}

}
