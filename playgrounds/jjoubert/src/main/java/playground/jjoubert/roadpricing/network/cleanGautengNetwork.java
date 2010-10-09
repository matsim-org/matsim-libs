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

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.network.NetworkWriter;
import org.matsim.run.NetworkCleaner;
import org.matsim.utils.gis.matsim2esri.network.CapacityBasedWidthCalculator;
import org.matsim.utils.gis.matsim2esri.network.FeatureGeneratorBuilderImpl;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;
import org.matsim.utils.gis.matsim2esri.network.PolygonFeatureGenerator;
import org.xml.sax.SAXException;

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
	private ScenarioImpl sc;	
	private String inputNetwork;
	private String outputNetwork;
	private String outputShapefile;
	
	private String tempFile = "/Users/johanwjoubert/Desktop/Temp/tempMap.xml.gz";

	/**
	 * Class to instantiate a Gauteng network cleaner for the SANRAL project.
	 * @param args
	 */
	public static void main(String[] args) {
		if(args.length == 3){
			File f = new File(args[0]);
			if(!f.exists() || !f.canRead()){
				throw new RuntimeException("Network file " + args[0] + "does not exist.");
			}
		} else{
			throw new RuntimeException("Need an input and output network file path specified.");
		}
		
		cleanGautengNetwork cn = new cleanGautengNetwork();
		cn.inputNetwork = args[0];
		cn.outputNetwork = args[1];
		cn.outputShapefile = args[2];
		cn.log.info("Gauteng network cleaner created.");
		
		/*
		 * You may comment some of the following lines out should you wish, but
		 * make sure that all the necessary files you require ARE in place, and
		 * that you use the RIGHT files. 
		 */
		cn.removeIdentifiedLinks(cn.inputNetwork, cn.tempFile);
		cn.cleanNetwork(cn.tempFile, cn.outputNetwork);
		cn.writeNetworkToShapefile(cn.outputNetwork);
		
		
		
		File tempFile = new File(cn.tempFile);
		boolean deleted = tempFile.delete();
		if(!deleted && tempFile.exists()){
			cn.log.warn("Could not delete the file " + cn.tempFile);
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
	 * Method to correct capacities on the SANRAL network. 
	 */
	private void updateCapacities(String fileToRead){
		this.log.info("Updating capacities...");
		sc = new ScenarioImpl();
		NetworkReaderMatsimV1 nwr = new NetworkReaderMatsimV1(sc);
		try {
			nwr.parse(fileToRead);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// N1
		
		
		
		this.log.info("Done.");
	}
	
	/**
	 * Reading in the uncleaned network.
	 */
	private void readNetwork(String networkToRead){
		this.log.info("Reading cleaned network from " + this.inputNetwork);
		sc = new ScenarioImpl();
		
		try {
			new NetworkReaderMatsimV1(sc).parse(networkToRead);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
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
		
		NetworkImpl nw = sc.getNetwork();
		nw.removeLink(new IdImpl("182490"));
		nw.removeLink(new IdImpl("182491"));
		nw.removeLink(new IdImpl("209887"));
		nw.removeLink(new IdImpl("209888"));
		nw.removeLink(new IdImpl("107997"));
		nw.removeLink(new IdImpl("107998"));
		nw.removeLink(new IdImpl("40734"));
		nw.removeLink(new IdImpl("76728"));
		nw.removeLink(new IdImpl("76729"));
		nw.removeLink(new IdImpl("207738"));
		nw.removeLink(new IdImpl("207739"));
		nw.removeLink(new IdImpl("58645"));
		nw.removeLink(new IdImpl("58646"));
		nw.removeLink(new IdImpl("41712"));
		nw.removeLink(new IdImpl("41713"));
		nw.removeLink(new IdImpl("7577"));
		nw.removeLink(new IdImpl("7578"));
		nw.removeLink(new IdImpl("117665"));
		nw.removeLink(new IdImpl("117666"));
		nw.removeLink(new IdImpl("183173"));
		nw.removeLink(new IdImpl("183107"));		
		nw.removeLink(new IdImpl("117400"));
		nw.removeLink(new IdImpl("117401"));
		nw.removeLink(new IdImpl("117402"));
		nw.removeLink(new IdImpl("101505"));
		nw.removeLink(new IdImpl("101506"));
		nw.removeLink(new IdImpl("101499"));
		nw.removeLink(new IdImpl("101507"));
		nw.removeLink(new IdImpl("101545"));
		nw.removeLink(new IdImpl("101546"));
		nw.removeLink(new IdImpl("35684"));
		nw.removeLink(new IdImpl("35685"));
		nw.removeLink(new IdImpl("73104"));
		nw.removeLink(new IdImpl("73105"));
		nw.removeLink(new IdImpl("118847"));
		nw.removeLink(new IdImpl("190409"));
		nw.removeLink(new IdImpl("156269"));
		nw.removeLink(new IdImpl("156270"));
		nw.removeLink(new IdImpl("138055"));
		nw.removeLink(new IdImpl("138056"));
		nw.removeLink(new IdImpl("143665"));
		nw.removeLink(new IdImpl("143666"));
		nw.removeLink(new IdImpl("143667"));
		nw.removeLink(new IdImpl("143668"));
		nw.removeLink(new IdImpl("143669"));
		nw.removeLink(new IdImpl("143670"));
		nw.removeLink(new IdImpl("118831"));
		nw.removeLink(new IdImpl("118832"));
		nw.removeLink(new IdImpl("184800"));
		nw.removeLink(new IdImpl("184801"));
		nw.removeLink(new IdImpl("96293"));
		nw.removeLink(new IdImpl("96294"));
		nw.removeLink(new IdImpl("9864"));
		nw.removeLink(new IdImpl("9865"));
		nw.removeLink(new IdImpl("29391"));
		nw.removeLink(new IdImpl("29392"));
		nw.removeLink(new IdImpl("220156"));
		nw.removeLink(new IdImpl("220157"));
		nw.removeLink(new IdImpl("174206"));
		nw.removeLink(new IdImpl("188247"));
		nw.removeLink(new IdImpl("159992"));
		nw.removeLink(new IdImpl("159991"));
		nw.removeLink(new IdImpl("29193"));
		nw.removeLink(new IdImpl("29192"));
		nw.removeLink(new IdImpl("60660"));
		nw.removeLink(new IdImpl("60659"));
		nw.removeLink(new IdImpl("102491"));
		nw.removeLink(new IdImpl("103331"));
		nw.removeLink(new IdImpl("39870"));
		nw.removeLink(new IdImpl("39869"));
		nw.removeLink(new IdImpl("140894"));
		nw.removeLink(new IdImpl("140893"));
		nw.removeLink(new IdImpl("138155"));
		nw.removeLink(new IdImpl("90858"));
		nw.removeLink(new IdImpl("98626"));
		nw.removeLink(new IdImpl("98625"));
		nw.removeLink(new IdImpl("78685"));
		nw.removeLink(new IdImpl("78684"));
		nw.removeLink(new IdImpl("167466"));
		nw.removeLink(new IdImpl("167212"));
		nw.removeLink(new IdImpl("50024"));
		nw.removeLink(new IdImpl("50025"));
		nw.removeLink(new IdImpl("500025"));
		nw.removeLink(new IdImpl("127100"));
		nw.removeLink(new IdImpl("38090"));
		nw.removeLink(new IdImpl("207714"));
		nw.removeLink(new IdImpl("207715"));
		nw.removeLink(new IdImpl("83343"));
		nw.removeLink(new IdImpl("83344"));
//		nw.removeLink(new IdImpl(""));
		
		this.log.info("Identified links removed.");
		this.log.info("Writing network to " + fileToWrite);
		NetworkWriter nww = new NetworkWriter(nw);
		nww.write(fileToWrite);
		this.log.info("Network written.");
	}
}

