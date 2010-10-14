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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsReaderMatsimV1;
import org.xml.sax.SAXException;

import playground.andreas.osmBB.osm2counts.Osm2Counts;

/**
 * @author droeder
 *
 */
public class ResizeLinksByCount2 extends AbstractResizeLinksByCount{
	
	private static final Logger log = Logger.getLogger(ResizeLinksByCount2.class);
	
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
		
		ResizeLinksByCount2 r = new ResizeLinksByCount2(networkFile, counts, shortNameMap);
		r.run("d:/VSP/output/osm_bb/counts_network_resized.xml");
	}
	
	public ResizeLinksByCount2(String networkFile, Counts counts, Map<String, String> shortNameMap){
		super(networkFile, counts, shortNameMap);
	}
	

	protected void resize() {
		this.listCountsOnOrigId();
	}

	private void listCountsOnOrigId(){
		Map<String, List<String>> origId2Cs = new HashMap<String, List<String>>();
		String origId = null;
		List<String> counters = null;
		Node node = null;
		
		for(Entry<String, String> e:  shortNameMap.entrySet()){
			node = this.net.getNodes().get(new IdImpl(e.getValue()));
		}
		
//		for(Count c : counts.getCounts().values()){
//			log.info(c.getLocId());
//			origId = ((LinkImpl)net.getLinks().get(c.getLocId())).getOrigId();
//			
//			if (!origId2Cs.containsKey(origId)){
//				counters = new ArrayList<String>();
//			}else{
//				counters = origId2Cs.get(origId);
//				log.warn("There is more than one countingStation on origId " + origId);
//			}
//			
//			counters.add(c.getCsId());
//			origId2Cs.put(origId, counters);
//		}
	}
	
}
