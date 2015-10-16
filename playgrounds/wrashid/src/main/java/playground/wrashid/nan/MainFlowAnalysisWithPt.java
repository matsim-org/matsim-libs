/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.wrashid.nan;

import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;

public class MainFlowAnalysisWithPt {

	public static void main(String[] args) {
		String networkFile="C:/experiments/berlin/output_network.xml.gz";
		String eventsFile="C:/experiments/berlin/ITERS/it.50/50.events.xml.gz";
		Coord center= new Coord((double) 4594503, (double) 5820304); // center=null means use all links
		boolean isOldEventFile=false;
		int binSizeInSeconds=60;
		
//		String networkFile="C:/Users/Nan/Desktop/For matsim/matsim-0.1.1/examples/equil/network.xml";
//		String eventsFile="C:/Users/Nan/Desktop/For matsim/matsim-0.1.1/output/equil/ITERS/it.5/5.events.txt.gz";
//		Coord center=new CoordImpl(0,0);
		//boolean isOldEventFile=false;
		
		double radiusInMeters=1000;
		
		
		Map<Id<Link>, ? extends Link> links = NetworkReadExample.getNetworkLinks(networkFile,center,radiusInMeters);// input/set center and radius
		
		
		OutFlowInfoCollectorWithPt flowAnalyzer=new OutFlowInfoCollectorWithPt(links,isOldEventFile,binSizeInSeconds); 
		// in order for FlowInfoCollector functioning, we call links from NetworkReadExample
		//InFlowInfoCollector inflowAnalyzer=new InFlowInfoCollector(NetworkReadExample.getFilteredEquilNetLinks());		
		
		flowAnalyzer.reset(0);
		//inflowAnalyzer.reset(0);
				
		EventsManager events = EventsUtils.createEventsManager();  //create new object of events-manager class
		//EventsManagerImpl eventsInflow=new EventsManagerImpl();
		
		events.addHandler(flowAnalyzer); // add handler
		//eventsInflow.addHandler(inflowAnalyzer);
		
		//EventsReaderTXTv1 reader = new EventsReaderTXTv1(events);
		//reader.readFile(eventsFile); //where we find events data
		
		
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		reader.parse(eventsFile);

		
		flowAnalyzer.printLinkFlows();
		//inflowAnalyzer.printLinkInFlow();
		
		
	}
	
}
