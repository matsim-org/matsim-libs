package playground.wrashid.nan;

/* *********************************************************************** *
 * project: org.matsim.*
 * MainDensityAnalysis.java
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
import java.util.HashMap;
import java.util.Map;

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderTXTv1;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordImpl;

public class MainFlowAnalysis {

	public static void main(String[] args) {
		
		String networkFile="D:/study/DATA/1pct zh with freight-cross-border-transit-mohit/network.xml";
		String eventsFile="D:/study/DATA/1pct zh with freight-cross-border-transit-mohit/50.events.txt.gz";
		Coord center=null; // center=null means use all links
		boolean isOldEventFile=false;
		int binSizeInSeconds=3600;
		
//		String networkFile="C:/Users/Nan/Desktop/For matsim/matsim-0.1.1/examples/equil/network.xml";
//		String eventsFile="C:/Users/Nan/Desktop/For matsim/matsim-0.1.1/output/equil/ITERS/it.5/5.events.txt.gz";
//		Coord center=new CoordImpl(0,0);
		//boolean isOldEventFile=false;
		
		double radiusInMeters=50000;
		
		
		Map<Id, Link> links = NetworkReadExample.getNetworkLinks(networkFile,center,radiusInMeters);// input/set center and radius
		
		
		OutFlowInfoCollector flowAnalyzer=new OutFlowInfoCollector(links,isOldEventFile,binSizeInSeconds); 
		// in order for FlowInfoCollector functioning, we call links from NetworkReadExample
		//InFlowInfoCollector inflowAnalyzer=new InFlowInfoCollector(NetworkReadExample.getFilteredEquilNetLinks());		
		
		flowAnalyzer.reset(0);
		//inflowAnalyzer.reset(0);
				
		EventsManagerImpl events = new EventsManagerImpl();  //create new object of events-manager class
		//EventsManagerImpl eventsInflow=new EventsManagerImpl();
		
		events.addHandler(flowAnalyzer); // add handler
		//eventsInflow.addHandler(inflowAnalyzer);
		
		EventsReaderTXTv1 reader = new EventsReaderTXTv1(events);
		//EventsReaderTXTv1 readerInflow= new EventsReaderTXTv1(eventsInflow);
		
		reader.readFile(eventsFile); //where we find events data
		//readerInflow.readFile(eventsFile);
		
		flowAnalyzer.printLinkFlows();
		//inflowAnalyzer.printLinkInFlow();
		
		
	}
	
}
