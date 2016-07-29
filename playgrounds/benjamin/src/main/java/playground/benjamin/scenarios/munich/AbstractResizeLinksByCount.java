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
package playground.benjamin.scenarios.munich;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import org.matsim.counts.Volume;

import playground.benjamin.utils.Links2ShapeWriter;


/**
 * @author droeder
 *
 */
public abstract class AbstractResizeLinksByCount {
	private static final Logger log = Logger.getLogger(AbstractResizeLinksByCount.class);
	
	//givens
	private final String netFile;
	private String outFile;
	private Map<String, String> shortNameMap;
	
	//interns
	private Network oldNet;
	private Network newNet;

	private Map<Id<Link>, Link> modifiedLinks2shp = null;
	private Map<Id, SortedMap<String, String>> modAttributes = null;
	private Map<Id<Link>, Link> unmodifiedLinks2shp = null;
	private Map<Id, SortedMap<String, String>> unmodAttributes = null;
	private Double scaleFactor = null;
	private boolean countsMatched = false;
	private Counts<Link> origCounts;
	private Counts<Link> rescaledCounts = null;
		
	
	/**
	 * use this contructor if the counts loc_Ids are NOT matched to the linkIds. The shortNameMap 
	 * consists of  toNodeIds mapped to counts cs_Ids! 
	 * @param networkFile
	 * @param counts
	 * @param shortNameMap
	 * @param scaleFactor
	 */
	public AbstractResizeLinksByCount(String networkFile, Counts counts, Map<String, String> shortNameMap, Double scaleFactor){
		this.netFile = networkFile;
		this.origCounts = counts;
		this.scaleFactor = scaleFactor;
		this.shortNameMap = shortNameMap;
		this.prepareNetwork();
	}
	
	/**
	 * use this constructor if counts loc_Ids and linkIds are matched!
	 * @param networkFile
	 * @param counts
	 * @param scaleFactor
	 */
	public AbstractResizeLinksByCount(String networkFile, Counts counts, Double scaleFactor){
		this.netFile = networkFile;
		this.origCounts = counts;
		this.scaleFactor = scaleFactor;
		this.prepareNetwork();
		this.countsMatched = true;
	}
	
	
	/**
	 * Resizes the given network and write it to the outFile. 
	 * If necessary the given counts are preprocessed. They are written to outFile_counts.xml
	 * In Addition shapefiles with the refactored and with the remaining links are written.
	 */
	public void run (String outFile){
		this.outFile = outFile;
		if(!this.countsMatched){
			this.origCounts = this.preProcessCounts(this.origCounts);
			this.writePreprocessedCounts(outFile);
		}

		this.resize();
		this.writeNewNetwork();

		writeModifiedLinks2Shape();
		writeUnmodifiedLinks2Shape();
	}
	
	private Counts<Link> preProcessCounts(Counts<Link> oldCounts) {
		Counts<Link> tempCounts = new Counts();
		Node node;
		Count<Link> oldCount;
		Count<Link> newCount;
		Id outLink = null;
		tempCounts.setDescription("none");
		tempCounts.setName("counts merged");
		tempCounts.setYear(2009);
		
		for(Entry<String, String> e : this.shortNameMap.entrySet()){
			if(this.newNet.getNodes().containsKey(Id.create(e.getKey(), Node.class)) && this.origCounts.getCounts().containsKey(Id.create(e.getValue(), Link.class))){
				node =  this.oldNet.getNodes().get(Id.create(e.getKey(), Node.class));
				oldCount = this.origCounts.getCounts().get(Id.create(e.getValue(), Link.class));
				
				//nodes with countingStations on it contain only one outlink
				for(Link l : node.getOutLinks().values()){
					outLink = l.getId();
					break;
				}
				if(!(outLink == null)){
					newCount = tempCounts.createAndAddCount(outLink, oldCount.getCsId());
					newCount.setCoord(oldCount.getCoord());
					for(Entry<Integer, Volume> ee : oldCount.getVolumes().entrySet()){
						newCount.createVolume(ee.getKey().intValue(), ee.getValue().getValue());
					}
				}
			}
		}
		return tempCounts;
	}
	
	public Counts getOriginalCounts(){
		return this.origCounts;
	}
	
	public Counts getRescaledCounts(){
		if(this.rescaledCounts == null){
			this.rescaleCounts();
		}
		return this.rescaledCounts;
	}
	
	public Count getRescaledCount(Id locId){
		if(this.rescaledCounts == null){
			this.rescaleCounts();
		}
		return this.rescaledCounts.getCount(locId);
	}
	
	public final Network getOrigNetwork(){
		return this.oldNet;
	}
	
	public Link getOriginalLink(Id id){
		return (Link) this.oldNet.getLinks().get(id);
	}
	
	public Network getNewNetwork(){
		return this.newNet;
	}
	
	public void setNewLinkData(Id link, Double capacity, double nrOfLanes){
		this.newNet.getLinks().get(link).setCapacity(capacity);
		this.newNet.getLinks().get(link).setNumberOfLanes(nrOfLanes);
	}
	
	private void rescaleCounts() {
		this.rescaledCounts = new Counts();
		this.rescaledCounts.setDescription("none");
		this.rescaledCounts.setName("rescaled counts");
		this.rescaledCounts.setYear(2009);
		
		Count temp ;
		for(Count<Link> c : this.origCounts.getCounts().values()){
			temp = rescaledCounts.createAndAddCount(c.getLocId(), c.getCsId());
			temp.setCoord(c.getCoord());
			for(Entry<Integer, Volume> ee : c.getVolumes().entrySet()){
				temp.createVolume(ee.getKey().intValue(), ee.getValue().getValue() * this.scaleFactor);
			}
		}
	}

	private void prepareNetwork() {
		log.info("Start reading network!");
		Scenario oldScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Scenario newScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		log.info("Reading " + this.netFile);
		new MatsimNetworkReader(oldScenario.getNetwork()).readFile(this.netFile);
		new MatsimNetworkReader(newScenario.getNetwork()).readFile(this.netFile);
		this.newNet = newScenario.getNetwork();
		this.oldNet = oldScenario.getNetwork();
	}
	
	protected abstract void resize();

	private void writeNewNetwork() {
		log.info("Writing resized network to " + this.outFile + "!");
		new NetworkWriter(this.newNet).write(this.outFile);
	}
	
	
	protected void addLink2shp(Id link){
		if(this.modifiedLinks2shp == null){
			this.modifiedLinks2shp = new HashMap<Id<Link>, Link>();
			this.modAttributes = new HashMap<Id, SortedMap<String,String>>();
		}
		this.modifiedLinks2shp .put(link, this.newNet.getLinks().get(link));
		
		SortedMap<String, String> attrib = new TreeMap<String, String>();
		attrib.put("oldCap", String.valueOf(this.oldNet.getLinks().get(link).getCapacity()));
		attrib.put("newCap", String.valueOf(this.newNet.getLinks().get(link).getCapacity()));
		attrib.put("diffCap", String.valueOf(this.newNet.getLinks().get(link).getCapacity() - this.oldNet.getLinks().get(link).getCapacity()));		
		attrib.put("origId", NetworkUtils.getOrigId( ((Link)this.oldNet.getLinks().get(link)) ));
		
		this.modAttributes.put(link, attrib);
	}
	
	private void writeModifiedLinks2Shape(){
		log.info("Writing modified links to *.shp...");
		Links2ShapeWriter.writeLinks2Shape(this.outFile + "_modifiedLinks.shp", this.modifiedLinks2shp, this.modAttributes);
		log.info("done...");
	}
	
	@SuppressWarnings("unchecked")
	private void writeUnmodifiedLinks2Shape(){
		//preprocess
		if(!(this.modifiedLinks2shp==null)){
			this.unmodifiedLinks2shp = new HashMap<Id<Link>, Link>();
			this.unmodAttributes = new HashMap<Id, SortedMap<String,String>>();
			
			for(Link l : this.newNet.getLinks().values()){
				if(!this.modifiedLinks2shp.containsKey(l.getId())){
					this.unmodifiedLinks2shp .put(l.getId(), l);
					
					SortedMap<String, String> attrib = new TreeMap<String, String>();
					attrib.put("Cap", String.valueOf(l.getCapacity()));
					
					this.unmodAttributes.put(l.getId(), attrib);
				}
			}
		}else{
			this.unmodifiedLinks2shp = (Map<Id<Link>, Link>) this.oldNet.getLinks();
		}
		log.info("Writing unmodified links to *.shp...");
		Links2ShapeWriter.writeLinks2Shape(this.outFile + "_unmodifiedLinks.shp", this.unmodifiedLinks2shp, this.unmodAttributes);
		log.info("done...");
	}
	
	private void writePreprocessedCounts(String outFileName) {
		log.info("writing counts to " + outFileName + "_counts.xml...");
		new CountsWriter(this.origCounts).write(outFileName + "_counts.xml");
		log.info("wrting counts finished...");
	}
	
}
