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

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.counts.Counts;

import playground.droeder.gis.DaShapeWriter;


/**
 * @author droeder
 *
 */
public abstract class AbstractResizeLinksByCount {
	private static final Logger log = Logger.getLogger(ResizeLinksByCount.class);
	
	//givens
	private String netFile;
	private String outFile;
	protected Counts oldCounts;
	protected Map<String, String> shortNameMap;
	
	//interns
	protected Network newNet;
	private Network oldNet;

	private Map<Id, Link> modifiedLinks2shp = null;
	private Map<Id, SortedMap<String, String>> modAttributes = null;
	private Map<Id, Link> unmodifiedLinks2shp = null;
	private Map<Id, SortedMap<String, String>> unmodAttributes = null;
		
	public AbstractResizeLinksByCount(String networkFile, Counts counts, Map<String, String> shortNameMap){
		this.netFile = networkFile;
		this.oldCounts = counts;
		this.shortNameMap = shortNameMap;
		this.prepareNetwork(netFile);
	}
	
	public void run (String outFile){
		this.outFile = outFile;
		this.resize();
		this.writeNewNetwork();

		writeModifiedLinks2Shape();
		writeUnmodifiedLinks2Shape();
	}

	private void prepareNetwork(String netFile2) {
		log.info("Start reading network!");
		Scenario oldScenario = new ScenarioImpl();
		Scenario newScenario = new ScenarioImpl();
		log.info("Reading " + this.netFile);
		new MatsimNetworkReader(oldScenario).readFile(this.netFile);
		new MatsimNetworkReader(newScenario).readFile(this.netFile);
		this.newNet= newScenario.getNetwork();
		this.oldNet = oldScenario.getNetwork();
	}

	protected abstract void resize();

	

	private void writeNewNetwork() {
		log.info("Writing resized network to " + this.outFile + "!");
		new NetworkWriter(this.newNet).write(this.outFile + ".xml");
	}
	
	
	protected void addLink2shp(Id link){
		if(modifiedLinks2shp == null){
			this.modifiedLinks2shp = new HashMap<Id, Link>();
			this.modAttributes = new HashMap<Id, SortedMap<String,String>>();
		}
		this.modifiedLinks2shp .put(link, this.newNet.getLinks().get(link));
		
		SortedMap<String, String> attrib = new TreeMap<String, String>();
		attrib.put("oldCap", String.valueOf(this.oldNet.getLinks().get(link).getCapacity()));
		attrib.put("newCap", String.valueOf(this.newNet.getLinks().get(link).getCapacity()));
		attrib.put("origId", ((LinkImpl)this.oldNet.getLinks().get(link)).getOrigId());
		
		this.modAttributes.put(link, attrib);
	}
	
	private void writeModifiedLinks2Shape(){
		log.info("Writing modified links to *.shp...");
		DaShapeWriter.writeLinks2Shape(this.outFile + "_modifiedLinks.shp", modifiedLinks2shp, modAttributes);
		log.info("done...");
	}
	
	private void writeUnmodifiedLinks2Shape(){
		//preprocess
		if(!(modifiedLinks2shp==null)){
			this.unmodifiedLinks2shp = new HashMap<Id, Link>();
			this.unmodAttributes = new HashMap<Id, SortedMap<String,String>>();
			
			for(Link l : this.newNet.getLinks().values()){
				if(!modifiedLinks2shp.containsKey(l.getId())){
					this.unmodifiedLinks2shp .put(l.getId(), l);
					
					SortedMap<String, String> attrib = new TreeMap<String, String>();
					attrib.put("Cap", String.valueOf(l.getCapacity()));
					
					this.unmodAttributes.put(l.getId(), attrib);
				}
			}
		}else{
			unmodifiedLinks2shp =(Map<Id, Link>) this.oldNet.getLinks();
		}
		log.info("Writing unmodified links to *.shp...");
		DaShapeWriter.writeLinks2Shape(this.outFile + "_unmodifiedLinks.shp", unmodifiedLinks2shp, unmodAttributes);
		log.info("done...");
	}
}
