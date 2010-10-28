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
package playground.droeder.osm;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsReaderMatsimV1;
import org.xml.sax.SAXException;

import playground.andreas.osmBB.osm2counts.Osm2Counts;


/**
 * @author droeder
 *
 */
public class ResizeLinksByCount extends AbstractResizeLinksByCount{
	private static final Logger log = Logger.getLogger(ResizeLinksByCount.class);
	
	private Map<String, Double> origId2MaxCount = new HashMap<String, Double>();
		
	public static void main(String[] args){
		String countsFile = "d:/VSP/output/osm_bb/Di-Do_counts.xml";
		String filteredOsmFile = "d:/VSP/output/osm_bb/counts.osm";
		String networkFile = "d:/VSP/output/osm_bb/counts_network.xml";
		
		Osm2Counts osm2Counts = new Osm2Counts(filteredOsmFile);
		osm2Counts.prepareOsm();
		HashMap<String, String> shortNameMap = osm2Counts.getShortNameMap();
		Counts counts = new Counts();
		CountsReaderMatsimV1 countsReader = new CountsReaderMatsimV1(counts);
		try {
			countsReader.parse(countsFile);
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ResizeLinksByCount r = new ResizeLinksByCount(networkFile, counts, shortNameMap);
		r.run("d:/VSP/output/osm_bb/counts_network_resized");
	}
	
	public ResizeLinksByCount(String networkFile, Counts counts, Map<String, String> shortNameMap){
		super(networkFile, counts, shortNameMap);
	}
	
	protected void resize() {
		String origId = null;
		Double maxCount = null;
		Double capPerLane = null;
		Integer nrOfNewLanes = null;

		
		for(Link l : newNet.getLinks().values()){
			
			checkAndRegisterOrigId((LinkImpl) l);
			
			origId = ((LinkImpl) l).getOrigId();
			if(this.origId2MaxCount.containsKey(origId)){
				maxCount = origId2MaxCount.get(origId);
				capPerLane = l.getCapacity() / l.getNumberOfLanes();
				
				// if maxCount < cap set cap to maxCount and keep nrOfLanes
				if(maxCount < l.getCapacity()){
					log.warn("link " + l.getId() + " oldCap= " + l.getCapacity() + " oldNrOfLanes= " + 
							l.getNumberOfLanes() + ": maxCount < oldCap. Set Capacity to maxcount="+ 
							maxCount + " and keep numberOfLanes...");
					l.setCapacity(maxCount);
				}
				// else set nrOfNewLanes to int(maxCount/capPerLane) and cap to maxCount
				else{
					nrOfNewLanes = (int) (maxCount/capPerLane);
					log.info("link " + l.getId() + " oldCap= " + l.getCapacity() + " oldNrOfLanes= " + 
							l.getNumberOfLanes() + " : set nr of lanes to " + 
							nrOfNewLanes + " and capacity to " + maxCount);
					l.setNumberOfLanes(nrOfNewLanes);
					l.setCapacity(maxCount);
				}		
				this.addLink2shp(l.getId());
			}
		}
	}
	
	
	/*
	 * checks and registers the origId and maxcount of a link if there is a countingstation
	 */
	private void checkAndRegisterOrigId(LinkImpl l) {
		Node node = null;
		Count count = null;
		Double maxCount = null;
		
		node = l.getFromNode();
		if( (!this.origId2MaxCount.containsKey( ((LinkImpl) l).getOrigId()))  && this.shortNameMap.containsKey(node.getId().toString())){
			count = oldCounts.getCount(new IdImpl(shortNameMap.get(node.getId().toString())));
			if(!(count == null)){
				maxCount = count.getMaxVolume().getValue();
				this.origId2MaxCount.put(((LinkImpl) l).getOrigId(), maxCount);
				log.info("count " + count.getCsId() + " registered to originLink " + l.getOrigId());
			}else{
				log.warn("No count found for Node " + node.getId() + " but there should be one!!!");
			}
		}
		
	}
}
