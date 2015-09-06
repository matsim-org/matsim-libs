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

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;

public class MainDensityAnalysisWithPt {

	public static void main(String[] args) {
		String networkFile="C:/experiments/berlin/output_network.xml.gz";
		String eventsFile="C:/experiments/berlin/ITERS/it.50/50.events.xml.gz";
		Coord center= new Coord((double) 4594503, (double) 5820304); // center=null means use all links
		boolean isOldEventFile=false;
		int binSizeInSeconds=300;
		
//		String networkFile="C:/Users/Nan/Desktop/For matsim/matsim-0.1.1/examples/equil/network.xml";
//		String eventsFile="C:/Users/Nan/Desktop/For matsim/matsim-0.1.1/output/equil/ITERS/it.5/5.events.txt.gz";
//		Coord center=new CoordImpl(0,0);
		//boolean isOldEventFile=false;
		
		double radiusInMeters=1000;
		
		
		
		
		Map<Id<Link>, ? extends Link> links = NetworkReadExample.getNetworkLinks(networkFile,center,radiusInMeters);// input/set center and radius
		InFlowInfoCollectorWithPt inflowHandler=new InFlowInfoCollectorWithPt(links,isOldEventFile,binSizeInSeconds); 
		OutFlowInfoCollectorWithPt outflowHandler=new OutFlowInfoCollectorWithPt(links,isOldEventFile,binSizeInSeconds);// "links" makes run faster
		
		inflowHandler.reset(0);
		outflowHandler.reset(0);
				
		EventsManager events = EventsUtils.createEventsManager();  //create new object of events-manager class
		
		events.addHandler(inflowHandler); // add handler
		events.addHandler(outflowHandler);
		
		//EventsReaderTXTv1 reader = new EventsReaderTXTv1(events);
		//reader.readFile(eventsFile); //where we find events data
		
		
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		reader.parse(eventsFile);
		
		HashMap<Id<Link>, int[]> linkInFlow = inflowHandler.getLinkInFlow();	//get the matrix
		HashMap<Id<Link>, int[]> linkOutFlow = outflowHandler.getLinkOutFlow();	
		
		HashMap<Id<Link>, int[]> deltaFlow = deltaFlow(linkInFlow, linkOutFlow);
		HashMap<Id<Link>, double[]> density = calculateDensity(deltaFlow,links);
		
		printDensity(density,links);
		
	}
	
	public static HashMap<Id<Link>, int[]> deltaFlow(HashMap<Id<Link>, int[]> linkInFlow,HashMap<Id<Link>, int[]> linkOutFlow){
		
		HashMap<Id<Link>, int[]> result=new HashMap<>();
		for (Id<Link> linkId:linkInFlow.keySet())	{
			int[] inflowBins = linkInFlow.get(linkId);
			int[] outflowBins = linkOutFlow.get(linkId);
			int[] deltaflowBins = new int[inflowBins.length];
			result.put(linkId, deltaflowBins);// put them into result arrays
			for (int i=0;i<inflowBins.length;i++){
				
				if (deltaflowBins==null || inflowBins==null || outflowBins==null){
					System.out.println();
				}
				
				deltaflowBins[i]=inflowBins[i]-outflowBins[i];
				
				
				
			}
			if (linkId.equals(Id.create(126323, Link.class))){
				System.out.println();
			}
		}	
		
		return result;
	}
	
	public static HashMap<Id<Link>, double[]> calculateDensity(HashMap<Id<Link>, int[]> deltaFlow, Map<Id<Link>, ? extends Link> links){
			//send actual link info.)
		HashMap<Id<Link>, double[]> density=new HashMap<>();
		
		for (Id<Link> linkId:deltaFlow.keySet()){
			density.put(linkId,null);
		}
		
		
		for (Id<Link> linkId:density.keySet()){
			
			if (linkId.equals(Id.create(126216, Link.class))){
				System.out.println();
			}
			
			int[] deltaflowBins = deltaFlow.get(linkId);//give labels to deltaflowBins
			double[] densityBins = new double[deltaflowBins.length];
			Link link = links.get(linkId);
			densityBins[0]=deltaflowBins[0];
			for (int i=1;i<deltaflowBins.length;i++){
				densityBins[i]=(densityBins[i-1]+deltaflowBins[i]);
			}
			
			for (int i=1;i<deltaflowBins.length;i++){
				densityBins[i]=densityBins[i]/(link.getLength()*link.getNumberOfLanes())*1000;
			}
			
			density.put(linkId,densityBins);
			deltaFlow.remove(linkId);
		}
		
		return density;
	}
	
	public static void printDensity(HashMap<Id<Link>, double[]> density, Map<Id<Link>, ? extends Link> links) { // print
		for (Id<Link> linkId : density.keySet()) {
			double[] bins = density.get(linkId);

			Link link = links.get(linkId);

			System.out.print(linkId + " - " + link.getCoord() + ": ");

			for (int i = 0; i < bins.length; i++) {
				System.out.print(bins[i] + "\t");
			}

			System.out.println();
		}
	}
	
}

