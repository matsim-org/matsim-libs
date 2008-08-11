/* *********************************************************************** *
 * project: org.matsim.*
 * ScenarioGenerator.java
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

package playground.gregor.evacuation.scenarioGenerator;



import org.apache.log4j.Logger;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkWriter;
import org.matsim.network.algorithms.NetworkSegmentDoubleLinks;




public class ScenarioGenerator {
	
	private static final Logger log = Logger.getLogger(ScenarioGenerator.class);
	private static final String VERSION = "v20080716";
	

	public static void main(String [] args) {
		
		
		final double warningTimeOffset = 0;
		
		final String nodes = "./padang/scenario_"+ VERSION + "/input/nodes.shp";
		final String links = "./padang/scenario_" + VERSION + "/input/links.shp";
		final String plans = "./padang/scenario_" + VERSION + "/input/padang_plans_v20080616.xml.gz";
		final String evacarea = "./padang/scenario_"+ VERSION + "/input/evacarea.shp";
		final String safearea = "./padang/scenario_"+ VERSION + "/input/safearea.shp";
		final String barriers = "./padang/scenario_"+ VERSION + "/input/bariers.shp";
		final String changeevents = "./padang/scenario_"+ VERSION + "/input/change.shp";
		
		
		final String netfile = "./padang/scenario_"+ VERSION + "/network.xml";
		final String evacnetfile = "./padang/scenario_"+ VERSION + "/network_evac.xml";
		final String evacareafile = "./padang/scenario_"+ VERSION + "/evacuationarea.xml.gz";
		
		log.info("generating MATSim network...");
		try {
			NetworkGenerator.main(new String [] {nodes,links,netfile});
		} catch (Exception e) {
			throw new RuntimeException(e);
		} 
		log.info("done.");
		
		log.info("segmenting `double links`...");
			segmentDblLinks(netfile);
		log.info("done.");
	
		
		log.info("generating Evacuation area...");
		EvacuationAreaFileGenerator.main2(new String [] {netfile,evacarea,safearea,evacareafile});
		log.info("done.");
		
		log.info("generating Evacuation net...");
		EvacuationNetGenerator.main(new String[] {netfile, evacnetfile, evacareafile});
		log.info("done.");
		
		
		log.info("adapting ChangeEvents ...");
		
		log.info("done.");
	}

	
	private static void segmentDblLinks(String netfile) {

		final NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netfile);
		new NetworkSegmentDoubleLinks().run(network);
		new NetworkWriter(network,netfile).write();

	}
}
