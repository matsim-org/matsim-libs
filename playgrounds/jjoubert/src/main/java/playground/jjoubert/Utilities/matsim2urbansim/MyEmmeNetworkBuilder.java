/* *********************************************************************** *
 * project: org.matsim.*
 * MyEmmeNetworkBuilder.java
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

package playground.jjoubert.Utilities.matsim2urbansim;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.ConfigUtils;

/**
 * Reads node and link DBF files (that have been converted to TAB-delimited 
 * text files) generated from Emme transport models.
 * <p/>
 * Keyword(s): emme/2
 * 
 * @author jjoubert *
 */
public class MyEmmeNetworkBuilder {
	private static final Logger log = Logger.getLogger(MyEmmeNetworkBuilder.class);
	private String nodeFile;
	private int nodeIdField;
	private int nodeXField;
	private int nodeYField;
	private String linkFile;
	private int linkIdField;
	private int linkFromNodeField;
	private int linkToNodeField;
	private int linkLengthField;
	private int linkLanesField;
	private int linkCapacityField;
	private int linkSpeedField;
	private String networkFile;
	private Boolean overwrite;
	private ScenarioImpl scenario;
	

	/**
	 * Converting Emme network into a MATSim network. The Emme network is
	 * assumed to have been given as unprojected shapefiles for both nodes 
	 * and links. <br> 
	 * <b>Note:</b> Convert both the node and links DBF files into CSV format.<br><br>
	 * 
	 * The <i>Nodes</i> file must contain the following fields:
	 * <ol>
	 * 		<li> Node Id;
	 * 		<li> Longitude (x) value;
	 * 		<li> Latitude (y) value;
	 * </ol>
	 * The <i>Links</i> file must contain the following fields:
	 * <ol>
	 * 		<li> Link Id;
	 * 		<li> the node Id of the origin node;
	 * 		<li> the node Id of the destination node;
	 * 		<li> length of the node (in any unit, a factor is sued to convert 
	 * 			 the value to m);
	 * 		<li> number of lanes (preferably);
	 * 		<li> capacity (in vehicles per hour)
	 * @param args
	 */
	public static void main(String[] args) {
		MyEmmeNetworkBuilder menb = null;
		if(args.length >= 3){
			menb = new MyEmmeNetworkBuilder();
			// Nodes
			menb.nodeFile = args[0];
			menb.nodeIdField = Integer.parseInt(args[1]);
			menb.nodeXField = Integer.parseInt(args[2]);
			menb.nodeYField = Integer.parseInt(args[3]);
			// Links
			menb.linkFile = args[4];
			menb.linkIdField = Integer.parseInt(args[5]);
			menb.linkFromNodeField = Integer.parseInt(args[6]);
			menb.linkToNodeField = Integer.parseInt(args[7]);
			menb.linkLengthField = Integer.parseInt(args[8]);
			menb.linkLanesField = Integer.parseInt(args[9]);
			menb.linkCapacityField = Integer.parseInt(args[10]);
			menb.linkSpeedField = Integer.parseInt(args[11]);
			menb.networkFile = args[12];
			if(args.length >= 14){
				menb.overwrite = Boolean.parseBoolean(args[12]);
				if(args.length > 14){
					throw new IllegalArgumentException("Too many arguments passed");					
				}
			} 
		} else{
			throw new IllegalArgumentException("Too few arguments passed");
		}
		log.info("Generating a network from Emme data");
		
		File output = new File(menb.networkFile);
		if(output.exists() && menb.overwrite == false){
			throw new RuntimeException("The output file " + output.getAbsolutePath() + " exists and may not be overwritten.");
		}
		
		menb.buildNodes();
		menb.buildLinks();
		
		NetworkWriter nw = new NetworkWriter(menb.scenario.getNetwork());
		nw.writeFileV1(menb.networkFile);
		
		
		log.info("----------------------------------------");
		log.info("           Process complete");
		log.info("========================================");
	}

	public MyEmmeNetworkBuilder() {
		scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		nodeFile = null;
		linkFile = null;
		networkFile = null;
		overwrite = false;
	}
	
	/**
	 * Reads all the nodes from the DBF-converted CSV file.
	 */
	public void buildNodes(){
		log.info("Building nodes.");
		NetworkFactoryImpl f = scenario.getNetwork().getFactory();
		try {
			BufferedReader br = IOUtils.getBufferedReader(nodeFile);
			try{
				@SuppressWarnings("unused")
				String header = br.readLine();
				String line = null;
				while((line = br.readLine()) != null){
					String[] values = line.split("\t");
					Integer nodeId = Integer.parseInt(values[nodeIdField]);
					Double nodeX = Double.parseDouble(values[nodeXField]);
					Double nodeY = Double.parseDouble(values[nodeYField]);					
					scenario.getNetwork().addNode(f.createNode(new IdImpl(nodeId), new CoordImpl(nodeX, nodeY)));
				}
			} finally{
				br.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("Nodes completed (" + scenario.getNetwork().getNodes().size() + " nodes)");
	}
	
	/**
	 * 
	 */
	public void buildLinks(){
		log.info("Building links.");
		NetworkFactoryImpl f = scenario.getNetwork().getFactory();
		try {
			BufferedReader br = IOUtils.getBufferedReader(linkFile);
			try{
				@SuppressWarnings("unused")
				String header = br.readLine();
				String line = null;
				while((line = br.readLine()) != null){
					String[] values = line.split("\t");
					Integer id = Integer.parseInt(values[linkIdField]);
					Integer from = Integer.parseInt(values[linkFromNodeField]);
					Integer to = Integer.parseInt(values[linkToNodeField]);
					Double len = Double.parseDouble(values[linkLengthField]);
					Double lanes = Double.parseDouble(values[linkLanesField]);
					Double cap = Double.parseDouble(values[linkCapacityField]);
					Double speed = Double.parseDouble(values[linkSpeedField]);

					Link l = f.createLink(
							new IdImpl(id), 
							scenario.getNetwork().getNodes().get(new IdImpl(from)), 
							scenario.getNetwork().getNodes().get(new IdImpl(to)), 
							scenario.getNetwork(), 
							len*1000, 					// Nelson Mandela data is in km
							(speed*1000.0/3600.0), 		// Nelson Mandela data in in km/h
							cap, 
							lanes);
					scenario.getNetwork().addLink(l);
				}
			} finally{
				br.close();
			}
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}		
		log.info("Links completed (" + scenario.getNetwork().getLinks().size() + " links)");
	}
}

