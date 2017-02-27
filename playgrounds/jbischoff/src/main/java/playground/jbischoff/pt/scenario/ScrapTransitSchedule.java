/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
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

/**
 * 
 */
package playground.jbischoff.pt.scenario;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class ScrapTransitSchedule {
public static void main(String[] args) {
	
	String path = "C:/Users/Joschka/Documents/shared-svn/studies/jbischoff/multimodal/berlin/input/25pct/";
	Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	new TransitScheduleReader(scenario).readFile(path+"transitSchedule.xml.gz");
	System.out.println(scenario.getTransitSchedule().getTransitLines().size() + " lines ");
	System.out.println(scenario.getTransitSchedule().getFacilities().size() + " stops");
	Set<TransitLine> removedLines = new HashSet<>(); 
	
	
	for (TransitLine tl : scenario.getTransitSchedule().getTransitLines().values()){
		String ls = tl.getId().toString();
		if (ls.contains("-T-")||ls.contains("-B-")||ls.startsWith("BEH")||ls.startsWith("BMO")||ls.startsWith("BOS")||ls.startsWith("BTG")||ls.startsWith("BTS")||
				ls.startsWith("HER")||ls.startsWith("HVG")||ls.startsWith("BEH")||ls.startsWith("OVG")||ls.startsWith("RVS")||ls.startsWith("V")||ls.startsWith("WE")){
			removedLines.add(tl);
		}
		
	}
	for (TransitLine tl : removedLines){
		scenario.getTransitSchedule().removeTransitLine(tl);
	}	
	Set<TransitStopFacility> usedStops = new HashSet<>();
	for (TransitLine tl : scenario.getTransitSchedule().getTransitLines().values()){
		for (TransitRoute tr : tl.getRoutes().values()){
			for (TransitRouteStop stop : tr.getStops()){
				usedStops.add(stop.getStopFacility());
			}
		}
	}
	Set<TransitStopFacility> unUsedStops = new HashSet<>();

	for (TransitStopFacility f : scenario.getTransitSchedule().getFacilities().values()){
		if (!usedStops.contains(f)){
			unUsedStops.add(f);
			
		}
	}
	for (TransitStopFacility f : unUsedStops){
		scenario.getTransitSchedule().removeStopFacility(f);
	}
	for (TransitLine tl : scenario.getTransitSchedule().getTransitLines().values()){
		System.out.println((tl.getId() + " " + tl.getName()));
	}
	System.out.println(scenario.getTransitSchedule().getTransitLines().size() + " lines ");
	System.out.println(scenario.getTransitSchedule().getFacilities().size() + " stops");
	new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile(path+"scrappedSchedule.xml");
	}
	
}
