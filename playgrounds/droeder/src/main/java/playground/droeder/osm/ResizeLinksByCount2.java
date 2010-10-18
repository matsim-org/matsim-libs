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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsReaderMatsimV1;
import org.matsim.counts.CountsWriter;
import org.matsim.counts.Volume;
import org.xml.sax.SAXException;

import playground.andreas.osmBB.osm2counts.Osm2Counts;

/**
 * @author droeder
 *
 */
public class ResizeLinksByCount2 extends AbstractResizeLinksByCount{
	
	private static final Logger log = Logger.getLogger(ResizeLinksByCount2.class);
	private boolean countsMatched = false;
	private Counts newCounts;
	private boolean multipleCountsOnOrigLink = false;
	private Map<Id, List<Id>> origin2counts;
	
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
		r.run("d:/VSP/output/osm_bb/network_resized");
	}


	
	/**
	 * use this contructor if the counts loc_Ids are NOT matched to the linkIds. The shortNameMap 
	 * consists of  toNodeIds mapped to counts cs_Ids!  
	 * @param networkFile
	 * @param counts
	 * @param shortNameMap
	 */
	public ResizeLinksByCount2(String networkFile, Counts counts, Map<String, String> shortNameMap){
		super(networkFile, counts, shortNameMap);
	}
	
	/**
	 * use this constructor if counts loc_Ids and linkIds are matched!
	 * @param networkFile
	 * @param counts
	 */
	public ResizeLinksByCount2(String networkFile, Counts counts){
		super(networkFile, counts, null);
		this.countsMatched = true;
	}
		
	public void run(String outFile){
		if(!countsMatched){
			this.preProcessCounts();
			this.writePreprocessedCounts(outFile);
		}else{
			this.newCounts = super.oldCounts;
		}
		this.checkNumberOfCountsOnOrigLink();
		super.run(outFile + ".xml");
		
	}
	
	private void preProcessCounts() {
		Node node;
		Count oldCount;
		Count newCount;
		Id outLink = null;
		this.newCounts = new Counts();
		this.newCounts.setDescription("none");
		this.newCounts.setName("counts merged");
		this.newCounts.setYear(2009);
		
		for(Entry<String, String> e : this.shortNameMap.entrySet()){
			if(this.net.getNodes().containsKey(new IdImpl(e.getKey())) && this.oldCounts.getCounts().containsKey(new IdImpl(e.getValue()))){
				node =  this.net.getNodes().get(new IdImpl(e.getKey()));
				oldCount = this.oldCounts.getCounts().get(new IdImpl(e.getValue()));
				
				//nodes with countingStations on it contain only one outlink
				for(Link l : node.getOutLinks().values()){
					outLink = l.getId();
					break;
				}
				if(!(outLink == null)){
					newCount = this.newCounts.createCount(outLink, oldCount.getCsId());
					newCount.setCoord(oldCount.getCoord());
					for(Entry<Integer, Volume> ee : oldCount.getVolumes().entrySet()){
						newCount.createVolume(ee.getKey(), ee.getValue().getValue());
					}
				}
			}
		}
	}

	private void checkNumberOfCountsOnOrigLink() {
		this.origin2counts = new HashMap<Id, List<Id>>();
		Id origId;
		for(Id id : newCounts.getCounts().keySet()){
			origId = new IdImpl(((LinkImpl) this.net.getLinks().get(id)).getOrigId());
			if(this.origin2counts.containsKey(origId)){
				this.origin2counts.get(origId).add(id);
				this.multipleCountsOnOrigLink = true;
			}else{
				this.origin2counts.put(origId, new LinkedList<Id>());
				this.origin2counts.get(origId).add(id);
			}
		}
	}

	protected void resize() {
		for(Entry<Id, List<Id>> e : origin2counts.entrySet()){
			System.out.print(e.getKey() +" ");
			for(Id id: e.getValue()){
				System.out.print(id + " ");
			}
			System.out.println();
		}
		
		System.out.println(this.newCounts.getCounts().size() + " " + multipleCountsOnOrigLink);
	}
	
	private void writePreprocessedCounts(String outFile) {
		log.info("writing counts to " + outFile + "_counts.xml...");
		new CountsWriter(newCounts).write(outFile + "_counts.xml");
		log.info("done...");
	}

	
}
