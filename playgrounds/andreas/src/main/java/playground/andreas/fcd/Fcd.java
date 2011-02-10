/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.andreas.fcd;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkTransform;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.utils.gis.matsim2esri.network.FeatureGenerator;
import org.matsim.utils.gis.matsim2esri.network.FeatureGeneratorBuilder;
import org.matsim.utils.gis.matsim2esri.network.LineStringBasedFeatureGenerator;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;
import org.matsim.utils.gis.matsim2esri.network.WidthCalculator;

public class Fcd {
	
	private static final Logger log = Logger.getLogger(Fcd.class);
	
	private TreeMap<Id,FcdNetworkPoint> networkMap;
	private LinkedList<FcdEvent> fcdEventsList;
	
	public Fcd(String netInFile, String fcdEventsInFile) {
		try {
			log.info("Reading fcd network file...");
			this.networkMap = ReadFcdNetwork.readFcdNetwork(netInFile);
			log.info("...done. Network map contains " + this.networkMap.size() + " entries");
			
			log.info("Reading fcd events file...");
			this.fcdEventsList = ReadFcdEvents.readFcdEvents(fcdEventsInFile);
			log.info("...done. Events list contains " + this.fcdEventsList.size() + " entries");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		String netInFile = "D:\\Berlin\\DLR_FCD\\20110207_Analysis\\berlin_2010_anonymized.ext";
		String fcdEventsInFile = "D:\\Berlin\\DLR_FCD\\20110207_Analysis\\fcd-20101028_10min.ano";
		String netOutFile = "D:\\Berlin\\DLR_FCD\\20110207_Analysis\\netFromEvents.xml";
		
		Fcd fcd = new Fcd(netInFile, fcdEventsInFile);
		fcd.writeNetworkFromEvents(netOutFile);
	}

	private void writeNetworkFromEvents(String netOutFile) {
		log.info("Creating network from fcd events...");
		CoordinateTransformation cT = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.DHDN_GK4);
		NetworkImpl net = NetworkImpl.createNetwork();
		
		FcdEvent lastEvent = null;
		for (Iterator<FcdEvent> iterator = this.fcdEventsList.iterator(); iterator.hasNext();) {
			FcdEvent currentEvent = iterator.next();
			
			if(lastEvent == null){
				lastEvent = currentEvent;
				continue;
			}
			
			if(lastEvent.getVehId().toString().equalsIgnoreCase(currentEvent.getVehId().toString())){
				// same track, create link
				if(net.getNodes().get(currentEvent.getLinkId()) == null){
					net.createAndAddNode(currentEvent.getLinkId(), cT.transform(this.networkMap.get(currentEvent.getLinkId()).getCoord()));
				}
				if(net.getNodes().get(lastEvent.getLinkId()) == null){
					net.createAndAddNode(lastEvent.getLinkId(), cT.transform(this.networkMap.get(lastEvent.getLinkId()).getCoord()));
				}
				
				Id newLinkId = new IdImpl(lastEvent.getLinkId().toString() + "-" + currentEvent.getLinkId().toString());
				if(net.getLinks().get(newLinkId) == null){
					net.createAndAddLink(newLinkId, net.getNodes().get(lastEvent.getLinkId()), net.getNodes().get(currentEvent.getLinkId()), 999.9, 9.9, 9999.9, 9.9);
				}
			}			
			
			lastEvent = currentEvent;
		}
		log.info("...done.");
		
		log.info("Dumping matsim network to " + netOutFile + "...");
		NetworkWriter writer = new NetworkWriter(net);
		writer.write(netOutFile);
		log.info("...done");
		
		log.info("Writing network shape file...");
		NetworkTransform nT = new NetworkTransform(TransformationFactory.getCoordinateTransformation(TransformationFactory.DHDN_GK4, TransformationFactory.WGS84));
		nT.run(net);	
		
		//write shape file
		final WidthCalculator wc = new WidthCalculator() {
			@Override
			public double getWidth(Link link) {
				return 1.0;
			}
		};
		
		FeatureGeneratorBuilder builder = new FeatureGeneratorBuilder() {
			@Override
			public FeatureGenerator createFeatureGenerator() {
				FeatureGenerator fg = new LineStringBasedFeatureGenerator(wc, MGC.getCRS(TransformationFactory.WGS84));
				return fg;
			}
		};
		
		Links2ESRIShape l2ES = new Links2ESRIShape(net, netOutFile + ".shp", builder);
		l2ES.write();
		log.info("...done");
	}

}
