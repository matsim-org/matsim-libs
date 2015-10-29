/* *********************************************************************** *
 * project: org.matsim.*
 * cleanGautengNetwork.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.jjoubert.roadpricing.network;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.run.NetworkCleaner;
import org.matsim.utils.gis.matsim2esri.network.CapacityBasedWidthCalculator;
import org.matsim.utils.gis.matsim2esri.network.FeatureGeneratorBuilderImpl;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;
import org.matsim.utils.gis.matsim2esri.network.PolygonFeatureGenerator;

/**
 * Class to clean the Gauteng network, particularly for the SANRAL project.
 * Part of the network cleaning is also updating the network capacities where
 * required.
 * 
 * TODO Rewrite this slightly so that it calls Phase-specific methods, 
 * especially since I update/remove specific link IDs.
 * @author jwjoubert
 */
public class cleanGautengNetwork {
	private Logger log;
	private MutableScenario sc;	
	private String inputNetwork;
	private String outputNetwork;
	private String outputShapefile;
	private String laneDefinitionFolder;
	
	private String tempFile1 = "/Users/johanwjoubert/Desktop/Temp/tempMap1.xml.gz";
	private String tempFile2 = "/Users/johanwjoubert/Desktop/Temp/tempNetwork.xml.gz";

	/**
	 * Class to instantiate a Gauteng network cleaner for the SANRAL project.
	 * @param args
	 */
	public static void main(String[] args) {
		cleanGautengNetwork cn = new cleanGautengNetwork();
		cn.log.info("Number of arguments: " + args.length);
		if(args.length == 4){
			cn.inputNetwork = args[0];
			cn.laneDefinitionFolder = args[1];
			cn.outputNetwork = args[2];
			cn.outputShapefile = args[3];

			File f = new File(cn.inputNetwork);
			if(!f.exists() || !f.canRead()){
				throw new RuntimeException("Network file " + args[0] + "does not exist.");
			}
		} else{
			throw new RuntimeException("Need an input and output network file path specified.");
		}
		
		cn.log.info("Gauteng network cleaner created.");
		
		/*
		 * You may comment some of the following lines out should you wish, but
		 * make sure that all the necessary files you require ARE in place, and
		 * that you use the RIGHT files. 
		 */
//		cn.removeIdentifiedLinks(cn.inputNetwork, cn.tempFile1);
		cn.cleanNetwork(cn.inputNetwork, cn.outputNetwork);
//		cn.updateLaneDefinitions(cn.inputNetwork, cn.outputNetwork);		
		
		cn.writeNetworkToShapefile(cn.outputNetwork);
		
		
		boolean deleted1 = new File(cn.tempFile1).delete();
		boolean deleted2 = new File(cn.tempFile2).delete(); 
		if(!deleted1 || !deleted2){
			cn.log.warn("Could not delete one or both of the temporary files:");
			cn.log.warn("   " + cn.tempFile1);
			cn.log.warn("   " + cn.tempFile2);
		} else{
			cn.log.info("Cleaned temporary files.");
		}
		
		cn.log.info("----------------------------");
		cn.log.info("         Completed");
		cn.log.info("============================");
	}

	/**
	 * Class to clean various bits and pieces of the Gauteng network, 
	 * specifically for the SANRAL network. 
	 */
	public cleanGautengNetwork() {
		log = Logger.getLogger(cleanGautengNetwork.class);
	}
	
	/**
	 * Method to correct lane definitions on the SANRAL network.
	 */
	private void updateLaneDefinitions(String networkToRead, String networkToWrite){
		this.log.info("Updating lane definitions...");
		sc = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		NetworkReaderMatsimV1 nwr = new NetworkReaderMatsimV1(sc);
		nwr.parse(networkToRead);

		File folder = new File(this.laneDefinitionFolder);
		if(folder.isDirectory()){
			for(int i = 1; i <= 6; i++){
				File f = new File(this.laneDefinitionFolder + "/Lanes" + i +".txt");
				if(f.exists() && f.canRead()){
					/*
					 * Read all the link Ids and set their number of lanes.
					 */
					String line = null;
					try {
						BufferedReader br = IOUtils.getBufferedReader(f.getAbsolutePath());
						try{
							while((line = br.readLine()) != null){
								if(!line.startsWith("%")){
									Integer id = Integer.parseInt(line);
									Link l = this.sc.getNetwork().getLinks().get(Id.create(id, Link.class));
									if(l != null){
										double oldLanes = l.getNumberOfLanes();
										double oldCap = l.getCapacity();
										l.setNumberOfLanes(i);
										l.setCapacity(i*(oldCap / oldLanes));
									} else{
										this.log.warn("Could not find link " + line);
									}
								}else{
									// Skip the line, it is a comment.						
								}
							}
						}finally{
							br.close();
						}
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					} catch (NumberFormatException e){
						this.log.error("The line " + line + " is not a parsable integer.");
						e.printStackTrace();
					}
				}
			}
		} else{
			this.log.warn(this.laneDefinitionFolder + " is not a folder!");
		}
		this.log.info("Done.");
		
		NetworkWriter nww = new NetworkWriter(sc.getNetwork());
		nww.write(networkToWrite);
	}
	
	
	/**
	 * Reading in the uncleaned network.
	 */
	private void readNetwork(String networkToRead){
		this.log.info("Reading cleaned network from " + networkToRead);
		sc = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		new NetworkReaderMatsimV1(sc).parse(networkToRead);
		this.log.info("Network read successfully.");
	}
	
	/**
	 * Clean the network using the org.matsim.run.NetworkCleaner class.
	 */
	private void cleanNetwork(String fileToClean, String fileToWrite){
		NetworkCleaner nc = new NetworkCleaner();
		nc.run(fileToClean, fileToWrite);
		this.log.info("Network cleaned.");
	}

	/**
	 * Write the cleaned network (links only) as a shapefile. 
	 */
	private void writeNetworkToShapefile(String fileToWrite) {
		this.log.info("Reading cleaned up network from " + fileToWrite);
		readNetwork(fileToWrite);
		
		this.log.info("Writing network links to " + outputShapefile);
		this.sc.getConfig().global().setCoordinateSystem("WGS84_UTM35S");
		FeatureGeneratorBuilderImpl builder = new FeatureGeneratorBuilderImpl(sc.getNetwork(), "WGS84_UTM35S");
		builder.setWidthCoefficient(-0.01);
		builder.setFeatureGeneratorPrototype(PolygonFeatureGenerator.class);
		builder.setWidthCalculatorPrototype(CapacityBasedWidthCalculator.class);
		
		new Links2ESRIShape(sc.getNetwork(), outputShapefile, builder).write();
		this.log.info("Network links written.");
	}
	
	/**
	 * This method removes critical links that were included in the network
	 * because OpenStreetMap only clips a rectangle. I've manually identified 
	 * these links from QGis. Once removed, the network cleaner can be used to
	 * remove the disconnected components. 
	 */
	private void removeIdentifiedLinks(String fileToRead, String fileToWrite){
		this.log.info("Removing identified links.");
		readNetwork(fileToRead);
		
		Network nw = sc.getNetwork();
		nw.removeLink(Id.create("182490", Link.class));
		nw.removeLink(Id.create("182491", Link.class));
		nw.removeLink(Id.create("209887", Link.class));
		nw.removeLink(Id.create("209888", Link.class));
		nw.removeLink(Id.create("107997", Link.class));
		nw.removeLink(Id.create("107998", Link.class));
		nw.removeLink(Id.create("40734", Link.class));
		nw.removeLink(Id.create("76728", Link.class));
		nw.removeLink(Id.create("76729", Link.class));
		nw.removeLink(Id.create("207738", Link.class));
		nw.removeLink(Id.create("207739", Link.class));
		nw.removeLink(Id.create("58645", Link.class));
		nw.removeLink(Id.create("58646", Link.class));
		nw.removeLink(Id.create("41712", Link.class));
		nw.removeLink(Id.create("41713", Link.class));
		nw.removeLink(Id.create("7577", Link.class));
		nw.removeLink(Id.create("7578", Link.class));
		nw.removeLink(Id.create("117665", Link.class));
		nw.removeLink(Id.create("117666", Link.class));
		nw.removeLink(Id.create("183173", Link.class));
		nw.removeLink(Id.create("183107", Link.class));		
		nw.removeLink(Id.create("117400", Link.class));
		nw.removeLink(Id.create("117401", Link.class));
		nw.removeLink(Id.create("117402", Link.class));
		nw.removeLink(Id.create("101505", Link.class));
		nw.removeLink(Id.create("101506", Link.class));
		nw.removeLink(Id.create("101499", Link.class));
		nw.removeLink(Id.create("101507", Link.class));
		nw.removeLink(Id.create("101545", Link.class));
		nw.removeLink(Id.create("101546", Link.class));
		nw.removeLink(Id.create("35684", Link.class));
		nw.removeLink(Id.create("35685", Link.class));
		nw.removeLink(Id.create("73104", Link.class));
		nw.removeLink(Id.create("73105", Link.class));
		nw.removeLink(Id.create("118847", Link.class));
		nw.removeLink(Id.create("190409", Link.class));
		nw.removeLink(Id.create("156269", Link.class));
		nw.removeLink(Id.create("156270", Link.class));
		nw.removeLink(Id.create("138055", Link.class));
		nw.removeLink(Id.create("138056", Link.class));
		nw.removeLink(Id.create("143665", Link.class));
		nw.removeLink(Id.create("143666", Link.class));
		nw.removeLink(Id.create("143667", Link.class));
		nw.removeLink(Id.create("143668", Link.class));
		nw.removeLink(Id.create("143669", Link.class));
		nw.removeLink(Id.create("143670", Link.class));
		nw.removeLink(Id.create("118831", Link.class));
		nw.removeLink(Id.create("118832", Link.class));
		nw.removeLink(Id.create("184800", Link.class));
		nw.removeLink(Id.create("184801", Link.class));
		nw.removeLink(Id.create("96293", Link.class));
		nw.removeLink(Id.create("96294", Link.class));
		nw.removeLink(Id.create("9864", Link.class));
		nw.removeLink(Id.create("9865", Link.class));
		nw.removeLink(Id.create("29391", Link.class));
		nw.removeLink(Id.create("29392", Link.class));
		nw.removeLink(Id.create("220156", Link.class));
		nw.removeLink(Id.create("220157", Link.class));
		nw.removeLink(Id.create("174206", Link.class));
		nw.removeLink(Id.create("188247", Link.class));
		nw.removeLink(Id.create("159992", Link.class));
		nw.removeLink(Id.create("159991", Link.class));
		nw.removeLink(Id.create("29193", Link.class));
		nw.removeLink(Id.create("29192", Link.class));
		nw.removeLink(Id.create("60660", Link.class));
		nw.removeLink(Id.create("60659", Link.class));
		nw.removeLink(Id.create("102491", Link.class));
		nw.removeLink(Id.create("103331", Link.class));
		nw.removeLink(Id.create("39870", Link.class));
		nw.removeLink(Id.create("39869", Link.class));
		nw.removeLink(Id.create("140894", Link.class));
		nw.removeLink(Id.create("140893", Link.class));
		nw.removeLink(Id.create("138155", Link.class));
		nw.removeLink(Id.create("90858", Link.class));
		nw.removeLink(Id.create("98626", Link.class));
		nw.removeLink(Id.create("98625", Link.class));
		nw.removeLink(Id.create("78685", Link.class));
		nw.removeLink(Id.create("78684", Link.class));
		nw.removeLink(Id.create("167466", Link.class));
		nw.removeLink(Id.create("167212", Link.class));
		nw.removeLink(Id.create("50024", Link.class));
		nw.removeLink(Id.create("50025", Link.class));
		nw.removeLink(Id.create("500025", Link.class));
		nw.removeLink(Id.create("127100", Link.class));
		nw.removeLink(Id.create("38090", Link.class));
		nw.removeLink(Id.create("207714", Link.class));
		nw.removeLink(Id.create("207715", Link.class));
		nw.removeLink(Id.create("83343", Link.class));
		nw.removeLink(Id.create("83344", Link.class));
//		nw.removeLink(Id.create(""));
		
		this.log.info("Identified links removed.");
		this.log.info("Writing network to " + fileToWrite);
		NetworkWriter nww = new NetworkWriter(nw);
		nww.write(fileToWrite);
		this.log.info("Network written.");
	}
}

