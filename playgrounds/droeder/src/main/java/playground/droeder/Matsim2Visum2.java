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
package playground.droeder;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import playground.droeder.gis.DaShapeWriter;

/**
 * @author droeder
 *
 */
public class Matsim2Visum2 {
	private static final Logger log = Logger.getLogger(Matsim2Visum2.class);
	
	public static void main(String[] args){
		Matsim2Visum2.network2Shape4Visum("D:/VSP/svn/runs/run1553/23jul-ba16ext/output_network.xml.gz", "C:/Users/Daniel/Desktop/test.shp");
	}
	final String PATH =  "D:/VSP/svn/runs/run1553/23jul-ba16ext/";
	
	
	public static void network2Shape4Visum(String networkfile, String outFile){
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(sc).parse(networkfile);
		
		Map<Id, SortedMap<String, Object>> attribs = new HashMap<Id, SortedMap<String, Object>>();
		
		int cnt = 0;
		int msg = 1;
		int size = sc.getNetwork().getLinks().size();
		String[] lanes;
		for(Link l: sc.getNetwork().getLinks().values()){
			SortedMap<String, Object> temp = new TreeMap<String, Object>();
			temp.put("Kapazität", String.valueOf((int)l.getCapacity()).replace(".", ","));
			lanes = String.valueOf(l.getNumberOfLanes()).split(".");
			if(lanes.length < 1){
				temp.put("Fahrstreifen", "1");
			}else{
				temp.put("lanes", lanes[0]);
			}
			temp.put("v0 IV", String.valueOf(l.getFreespeed()*3.6).replace(".", ","));
			temp.put("Länge", String.valueOf(l.getLength()/1000).replace(".", ","));
			attribs.put(l.getId(), temp);
			cnt++;
			if(l.getAllowedModes().contains(TransportMode.car)){
				temp.put("VsysSet", "P");
			}
			if(cnt%msg == 0){
				msg*=2;
				log.info("processed links: " + cnt + " of " + size);
			}
		}
		
		DaShapeWriter.writeLinks2Shape(outFile, sc.getNetwork().getLinks(), attribs);
	}

}
