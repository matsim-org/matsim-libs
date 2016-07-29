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
package playground.wrashid.nan.extended;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;

//this code calculates circulation flow or outflow, 
//depending on what event handler you choose in OutFlowInfoCollectorWithPT
//e.g. if you choose LinkLeaveEventHandler, then you are collecting circulation flow
public class MainFlowAnalysisWithPt {


	public static void main(String[] args) {
		
		String networkFile="C:/Users/zheng/Desktop/RashidJAR/output/config_10pct_test518/output_network.xml.gz";
		String eventsFile="C:/Users/zheng/Desktop/RashidJAR/output/config_10pct_test518/ITERS/it.10/10.events.txt.gz";
		
//		String networkFile="C:/Users/zheng/Desktop/MATSIM RUNS/DATA/Artemis_Congested2/output_network.xml";
//		String eventsFile="C:/Users/zheng/Desktop/MATSIM RUNS/DATA/Artemis_Congested3/50.events.txt.gz";
		//Coord center=null; // center=null means use all links
		boolean isOldEventFile=false;
		int binSizeInSeconds=60;
		
//		String networkFile="C:/Users/Nan/Desktop/For matsim/matsim-0.1.1/examples/equil/network.xml";
//		String eventsFile="C:/Users/Nan/Desktop/For matsim/matsim-0.1.1/output/equil/ITERS/it.5/5.events.txt.gz";
//		Coord center=new CoordImpl(0,0);
		//boolean isOldEventFile=false;
		//Coord center=new CoordImpl(682440.875,247376.687);
		//Coord center=new CoordImpl(683139.3125,247387.9531);// high density center
		//double radiusInMeters=2500;
		//Coord center=new CoordImpl(683400.75,247500.0687); //(4-21)
		Coord center= new Coord(683139.3125, 247387.9531);// high density center
		double radiusInMeters=2000;
		
		
		Map<Id<Link>, ? extends Link> links = NetworkReadExample.getNetworkLinks(networkFile,center,radiusInMeters);// input/set center and radius
		
		
		OutFlowInfoCollectorWithPt flowAnalyzer=new OutFlowInfoCollectorWithPt((Map<Id<Link>, Link>) links,isOldEventFile,binSizeInSeconds); 
		// in order for FlowInfoCollector functioning, we call links from NetworkReadExample
		//InFlowInfoCollectorWithPt inflowAnalyzer=new InFlowInfoCollectorWithPt(links,isOldEventFile,binSizeInSeconds);		
		
		flowAnalyzer.reset(0);
		//inflowAnalyzer.reset(0);
				
		EventsManagerImpl events = new EventsManagerImpl();  //create new object of events-manager class
		//EventsManagerImpl eventsInflow=new EventsManagerImpl();
		
		events.addHandler(flowAnalyzer); // add handler
		//eventsInflow.addHandler(inflowAnalyzer);
		
		MatsimEventsReader reader = new MatsimEventsReader(events);
		//EventsReaderTXTv1 readerInflow= new EventsReaderTXTv1(eventsInflow);
		
		reader.readFile(eventsFile); //where we find events data
		//readerInflow.readFile(eventsFile);
		
		flowAnalyzer.printLinkFlows();
		//inflowAnalyzer.printLinkInFlow();
		//flowAnalyzer.printLinkWeights();
		
		
	}
	
}
