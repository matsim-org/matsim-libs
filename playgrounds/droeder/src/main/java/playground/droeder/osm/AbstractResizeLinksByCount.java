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
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
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
	protected Network net;
	private Network oldNet;
	private Map<String, SortedMap<Integer, Coord>> lineStrings = null;
	private Map<String, SortedMap<String, String>> attributes = null;
		
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

		//null-pointer
//		if(!(lineStrings == null)){
//			writeResizedLinks2Shape();
//		}
	}

	private void prepareNetwork(String netFile2) {
		log.info("Start reading network!");
		Scenario oldScenario = new ScenarioImpl();
		Scenario newScenario = new ScenarioImpl();
		log.info("Reading " + this.netFile);
		new MatsimNetworkReader(oldScenario).readFile(this.netFile);
		new MatsimNetworkReader(newScenario).readFile(this.netFile);
		this.net= newScenario.getNetwork();
		this.oldNet = oldScenario.getNetwork();
	}

	protected abstract void resize();

	

	private void writeNewNetwork() {
		log.info("Writing resized network to " + this.outFile + "!");
		new NetworkWriter(this.net).write(this.outFile);
	}
	
	protected void addLineString(List<Id> links){
		if(links == null) return;
		
		if(lineStrings == null){
			this.lineStrings = new HashMap<String, SortedMap<Integer,Coord>>();
			this.attributes = new HashMap<String, SortedMap<String,String>>();
		}
		
		Integer count = 0;
		String origId = null;
		LinkImpl l = null;
		SortedMap<Integer, Coord> lineString = new TreeMap<Integer, Coord>();
		SortedMap<String, String> attributes = new TreeMap<String, String>();
		
		for(Id link : links){
			l = (LinkImpl) this.net.getLinks().get(link);
			origId = l.getOrigId();
			lineString.put(count, l.getFromNode().getCoord());
			attributes.put(l.getId().toString() + "_new", String.valueOf(l.getCapacity()));
			attributes.put(l.getId() + "_old", String.valueOf(this.oldNet.getLinks().get(link).getCapacity()));
			
			count++;
			if(count == links.size()){
				lineString.put(count, l.getToNode().getCoord());
			}
			
		}
		if(lineString.size() > 1){
			this.lineStrings.put(origId, lineString);
			this.attributes.put(origId, attributes);
		}
		
	}
	
	private void writeResizedLinks2Shape(){
		log.info("Writing shape with resized links...");
		DaShapeWriter.writeDefaultLineString2Shape(this.outFile + "reszized.shp", "resizedLinks", lineStrings, attributes);
		log.info("done...");
		
	}
}
