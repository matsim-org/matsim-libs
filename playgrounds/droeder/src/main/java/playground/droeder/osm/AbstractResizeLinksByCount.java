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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.counts.Counts;


/**
 * @author droeder
 *
 */
public abstract class AbstractResizeLinksByCount {
	private static final Logger log = Logger.getLogger(ResizeLinksByCount.class);
	
	//givens
	private String netFile;
	private String outFile;
	protected Counts counts;
	protected Map<String, String> shortNameMap;
	
	//interns
	protected Network net;
		
	public AbstractResizeLinksByCount(String networkFile, Counts counts, Map<String, String> shortNameMap){
		this.netFile = networkFile;
		this.counts = counts;
		this.shortNameMap = shortNameMap;
	}
	
	public void run (String outFile){
		this.outFile = outFile;
		this.prepareNetwork(netFile);
		this.resize();
		this.writeNewNetwork();
	}

	private void prepareNetwork(String netFile2) {
		log.info("Start reading network!");
		Scenario oldScenario = new ScenarioImpl();
		this.net= oldScenario.getNetwork();
		log.info("Reading " + this.netFile);
		new MatsimNetworkReader(oldScenario).readFile(this.netFile);
	}

	protected abstract void resize();

	

	private void writeNewNetwork() {
		log.info("Writing resized network to " + this.outFile + "!");
		new NetworkWriter(this.net).write(this.outFile);
	}
}
