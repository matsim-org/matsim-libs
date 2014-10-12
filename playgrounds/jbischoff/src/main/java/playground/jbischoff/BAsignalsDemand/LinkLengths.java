/* *********************************************************************** *
 * project: org.matsim.*
 * LinkLengths.java
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

package playground.jbischoff.BAsignalsDemand;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.lanes.data.v20.LaneData20;
import org.matsim.lanes.data.v20.LaneDefinitions20;
import org.matsim.lanes.data.v20.LaneDefinitions20Impl;
import org.matsim.lanes.data.v20.LaneDefinitionsReader20;
import org.matsim.lanes.data.v20.LaneDefinitionsWriter20;
import org.matsim.lanes.data.v20.LanesToLinkAssignment20;
import org.xml.sax.SAXException;

import playground.jbischoff.BAsignals.JbBaPaths;

public class LinkLengths {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws ParserConfigurationException 
	 * @throws SAXException 
	 * @throws JAXBException 
	 */
	public static void main(String[] args) throws IOException, JAXBException, SAXException, ParserConfigurationException {
		String config = JbBaPaths.BASIMH+"scenario-lsa/cottbusConfig.xml";
		FileReader fr;
		BufferedReader br;
		String filename="/Users/JB/Desktop/BA-Arbeit/sim/faultylinks.csv";		
		List<Id<Link>> fl = new ArrayList<>();
		fr = new FileReader(new File (filename));
		br = new BufferedReader(fr);
		ScenarioLoaderImpl loader = ScenarioLoaderImpl.createScenarioLoaderImplAndResetRandomSeed(config);
		Scenario scenario = loader.loadScenario();
		
		String line = null;
		while ((line = br.readLine()) != null) {
			
	         String[] result = line.split(";");
	         Id<Link> current = Id.create(result[1], Link.class);
	         fl.add(current);
	         
	
			}
		br.close();
		
		//System.out.println(scenario.getNetwork().getLinks().get(Id.create(6667)).getLength());
		for (Id<Link> lid : fl){
			System.out.println(lid + " length is "+scenario.getNetwork().getLinks().get(lid).getLength());
			
		}
		LaneDefinitions20 ldf = new LaneDefinitions20Impl();
		LaneDefinitionsReader20 ldr = new LaneDefinitionsReader20(ldf, "http://www.matsim.org/files/dtd/laneDefinitions_v2.0.xsd");
		ldr.readFile("/Users/JB/Desktop/BA-Arbeit/sim/scenario/scenario-lsa/lanes_cottbus_v20_jbol.xml");
		for (LanesToLinkAssignment20 ltla : ldf.getLanesToLinkAssignments().values()){
			for (LaneData20 la : ltla.getLanes().values()){
				if (la.getId().toString().endsWith(".ol")){
				String[] a = la.getId().toString().split(".ol");
				
				int lid = Integer.parseInt(a[0]);
				try{
				System.out.println(lid+" l: "+scenario.getNetwork().getLinks().get(Id.create(lid, Link.class)).getLength());
				la.setStartsAtMeterFromLinkEnd(scenario.getNetwork().getLinks().get(Id.create(lid, Link.class)).getLength());
				
				} catch (Exception e){
					System.err.println("Error while getting length for Link "+lid);
				}
				}
				
				//			}}
			}
		
	
		
		}
	
		LaneDefinitionsWriter20 ldw = new LaneDefinitionsWriter20(ldf);
		ldw.write("/Users/JB/Desktop/BA-Arbeit/sim/scenario/scenario-lsa/lanes_cottbus_v20_jbol_c.xml");
	}
}
