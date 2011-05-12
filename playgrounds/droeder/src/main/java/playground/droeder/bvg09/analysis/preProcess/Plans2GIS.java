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
package playground.droeder.bvg09.analysis.preProcess;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.droeder.DaPaths;
import playground.droeder.gis.DaShapeWriter;

/**
 * @author droeder
 *
 */
public class Plans2GIS {
	
	public static void main(String[] args){
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new NetworkReaderMatsimV1(sc).parse(DaPaths.VSP + "BVG09_Auswertung/input/network.final.xml.gz");
		new MatsimPopulationReader(sc).parse(DaPaths.VSP + "BVG09_Auswertung/testPopulation1.xml");
		
		Map<String, SortedMap<Integer, Coord>> lineStrings = new HashMap<String, SortedMap<Integer, Coord>>();
		
		for (Person p : sc.getPopulation().getPersons().values()){
			SortedMap<Integer, Coord> temp = new TreeMap<Integer, Coord>();
			int i = 0;
			for(PlanElement pe : p.getSelectedPlan().getPlanElements()){
				if(pe instanceof Activity){
					temp.put(i, ((Activity) pe).getCoord());
					i++;
				}
			}
			
			if(i<1){
				System.out.println("not enough points for Agent " + p.getId());
			}else{
				lineStrings.put(p.getId().toString(), temp);
			}
		}
		
		DaShapeWriter.writeDefaultLineString2Shape(DaPaths.VSP + "BVG09_Auswertung/trips.shp", "BerlinTrips", lineStrings, null);
	}

}
