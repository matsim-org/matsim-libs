/* *********************************************************************** *
 * project: org.matsim.*
 * GetActsAtLink.java
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

package playground.wrashid.lib.tools.plan;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.TwoKeyHashMapWithDouble;

import playground.wrashid.lib.tools.kml.BasicPointVisualizer;
import playground.wrashid.lib.tools.kml.Color;

public class GetActStatisticsAtLinks {

	public static void main(String[] args) {
		String basePath="H:/data/cvs/ivt/studies/switzerland/";
		//String plansFile = basePath + "plans/teleatlas-ivtcheu-zrhCutC/census2000v2_zrhCutC_25pct/plans.xml.gz";
		String plansFile = "H:/data/experiments/ARTEMIS/input/plans_census2000v2_zrhCutC_1pct.xml";
		String networkFile = basePath + "networks/teleatlas-ivtcheu-zrhCutC/network.xml.gz";
		String facilititiesPath = basePath + "facilities/facilities.zrhCutC.xml.gz";
		Scenario scenario = GeneralLib.readScenario(plansFile, networkFile, facilititiesPath);
		
		TwoKeyHashMapWithDouble<Id<Link>,String> numberOfActivitiesAtLinks=new TwoKeyHashMapWithDouble<Id<Link>,String>();
		//IntegerValueHashMap<Id> numberOfAcitivitesAtLink=new IntegerValueHashMap<Id>();
		
		for (Id<Person> personId:scenario.getPopulation().getPersons().keySet()){
			Person p=scenario.getPopulation().getPersons().get(personId);
			
			List<PlanElement> planElements = p.getSelectedPlan().getPlanElements();
			
			for (int i=0;i<planElements.size();i++){
				PlanElement pe=planElements.get(i);
				if (pe instanceof Activity){
					
					String arrLegMode="";
					String depLegMode="";
					if (i>1){
						arrLegMode=((Leg) planElements.get(i-1)).getMode();
					}
					
					if (i<planElements.size()-1){
						depLegMode=((Leg) planElements.get(i+1)).getMode();
					}
					
					Id<Link> linkId=((Activity) pe).getLinkId();
					numberOfActivitiesAtLinks.incrementBy(linkId,arrLegMode  + "-" +  ((Activity) pe).getType() + "-" + depLegMode  , 1.0);
				}
			}
		}
		
		//numberOfAcitivitesAtLink.printToConsole();
		
		Id<Link> linkOfInterestId=Id.create("17560001856956FT", Link.class);
		
		BasicPointVisualizer basicPointVisualizer=new BasicPointVisualizer();
		basicPointVisualizer.addPointCoordinate(scenario.getNetwork().getLinks().get(linkOfInterestId).getFromNode().getCoord(), "from-node", Color.BLACK);
		basicPointVisualizer.addPointCoordinate(scenario.getNetwork().getLinks().get(linkOfInterestId).getToNode().getCoord(), "to-node", Color.BLACK);
		
		basicPointVisualizer.write("H:/data/experiments/ARTEMIS/zh/dumb charging/output/analysis/visualizeLink-17560001856956FT.kml");
		
		System.out.println("number Of activities at link:");
		numberOfActivitiesAtLinks.get(linkOfInterestId).printToConsole();
		
		System.out.println("average activities per link:"+numberOfActivitiesAtLinks.getAverage());
	}
	
}
