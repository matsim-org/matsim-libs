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

import java.io.File;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.ConfigUtils;
import org.openstreetmap.osmosis.core.filter.common.IdTrackerType;
import org.openstreetmap.osmosis.core.xml.common.CompressionMethod;

import playground.mzilske.osm.JOSMTolerantFastXMLReader;
import playground.mzilske.osm.NetworkSink;

/**
 * @author droeder
 *
 */
public class Osm2TransitlineNetworkReader {
	
	private static final Logger log = Logger
			.getLogger(Osm2TransitlineNetworkReader.class);
	
	String inFile;
	String fromCoordSystem;
	String toCoordSystem;
	
	public Osm2TransitlineNetworkReader(String inFile, String fromCoordSystem, String toCoordSystem){
		this.inFile = inFile;
		this.fromCoordSystem = fromCoordSystem;
		this.toCoordSystem = toCoordSystem;
	}
	
	public void convertOsm2Matsim(){
		convertOsm2Matsim(null);
	}
	
	private ScenarioImpl scenario;
	private Map<Id, NetworkImpl> line2Network;
	
	public void convertOsm2Matsim(String[] transitFilter){
		
		log.info("Start...");		
		scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().scenario().setUseTransit(true);
		scenario.getConfig().scenario().setUseVehicles(true);
		JOSMTolerantFastXMLReader reader = new JOSMTolerantFastXMLReader(new File(inFile), false, CompressionMethod.None);		

		CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(this.fromCoordSystem, this.toCoordSystem);
		NetworkSink networkGenerator = new NetworkSink(scenario.getNetwork(), coordinateTransformation);
		
		// Anmerkung trunk, primary und secondary sollten in Bln als ein Typ behandelt werden
		
		// Autobahn
		networkGenerator.setHighwayDefaults(1, "motorway",      3,  100.0/3.6, 1.2, 2000, true); // 70
		networkGenerator.setHighwayDefaults(1, "motorway_link", 2,  60.0/3.6, 1.2, 1500, true); // 60
		// Pseudoautobahn
		networkGenerator.setHighwayDefaults(2, "trunk",         2,  50.0/3.6, 0.75, 1000, false); // 45
		networkGenerator.setHighwayDefaults(2, "trunk_link",    1,  50.0/3.6, 0.5, 1000, false); // 40
		// Durchgangsstrassen
		networkGenerator.setHighwayDefaults(3, "primary",       2,  50.0/3.6, 0.75, 1000, false); // 35
		networkGenerator.setHighwayDefaults(3, "primary_link",  1,  50.0/3.6, 0.5, 1000, false); // 30
		
		// Hauptstrassen
		networkGenerator.setHighwayDefaults(4, "secondary",     1,  50.0/3.6, 0.5, 1000, false); // 30
		// Weitere Hauptstrassen
		networkGenerator.setHighwayDefaults(5, "tertiary",      1,  30.0/3.6, 0.8,  600, false); // 25 
		// bis hier ca wip
		
		// Nebenstrassen
		networkGenerator.setHighwayDefaults(6, "minor",         1,  30.0/3.6, 0.8,  600, false); // nix
		// Alles Moegliche, vor allem Nebenstrassen auf dem Land, meist keine 30er Zone 
		networkGenerator.setHighwayDefaults(6, "unclassified",  1,  30.0/3.6, 0.8,  600, false);
		// Nebenstrassen, meist 30er Zone
		networkGenerator.setHighwayDefaults(6, "residential",   1,  30.0/3.6, 0.6,  600, false);
		// Spielstrassen
		networkGenerator.setHighwayDefaults(6, "living_street", 1,  15.0/3.6, 1.0,  300, false);
		
		log.info("Reading " + this.inFile);
		DrTransitNetworkSink transitNetworkSink = new DrTransitNetworkSink(scenario.getNetwork(), IdTrackerType.BitSet);
		transitNetworkSink.setTransitModes(transitFilter);
		reader.setSink(networkGenerator);
		networkGenerator.setSink(transitNetworkSink);
		reader.run();
		line2Network = transitNetworkSink.getLine2Network();
	}
	
	public Map<Id, NetworkImpl> getLine2Net(){
		log.info("produced " + this.line2Network.size() + " networks...");
		return this.line2Network;
	}
	
	public Scenario getScenario(){
		return this.scenario;
	}
}
