/* *********************************************************************** *
 * project: org.matsim.*
 * SaturnNetworkBuilder.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.gis.matsim2esri.network.CapacityBasedWidthCalculator;
import org.matsim.utils.gis.matsim2esri.network.FeatureGeneratorBuilderImpl;
import org.matsim.utils.gis.matsim2esri.network.LanesBasedWidthCalculator;
import org.matsim.utils.gis.matsim2esri.network.LineStringBasedFeatureGenerator;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;
import org.matsim.utils.gis.matsim2esri.network.PolygonFeatureGenerator;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class SaturnNetworkBuilder {
	private final static Logger log = Logger.getLogger(SaturnNetworkBuilder.class);
	private Scenario sc;

	/**
	 * Implements the {@link SaturnNetworkBuilder} and passes two files as input: 
	 * the tab-delimited list of node Ids, each with a coordinate pair; and the
	 * tab-delimited list of links, each with a starting node Id, a destination an
	 * ending node Id, and a length. An output folder path is passed from which
	 * the output network file and shapefile names are derived.
	 * @param args String array with the following arguments, and in the following 
	 * order:
	 * <ol>
	 * 	<li> <b>nodeFile</b> absolute path of the tab-delimited file containing
	 * 		node Ids and coordinate pairs;
	 * 	<li> <b>linkFile</b> absolute path of the tab-delimited file containing
	 * 		start and end node Ids, and link lengths.
	 * 	<li> <b>outputPath</b> absolute path of the folder where output will be
	 * 		written to.
	 * </ol>
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException {
		SaturnNetworkBuilder snb = null;
		if(args.length != 3){
			throw new RuntimeException("Incorrect number of arguments.");
		} else{
			snb = new SaturnNetworkBuilder();
		}
		if(!(new File(args[0])).exists() || !(new File(args[0])).canRead()){
			throw new FileNotFoundException("Cannot read node file from " + args[0]);
		}
		if(!(new File(args[1])).exists() || !(new File(args[1])).canRead()){
			throw new FileNotFoundException("Cannot read link file from " + args[0]);
		}
		if(!(new File(args[2])).isDirectory()){
			throw new RuntimeException("The output path is not a folder: " + args[2]);
		}
		
		snb.parseNodes(args[0]);
		snb.parseLinks(args[1]);
		snb.writeNetwork(args[2], "WGS84_UTM35S");
		
		double density = ((double) snb.sc.getNetwork().getLinks().size()) / Math.pow((double)snb.sc.getNetwork().getNodes().size(), 2) * 100;
		log.info(String.format("  Nodes: %d", snb.sc.getNetwork().getNodes().size()));
		log.info(String.format("  Links: %d", snb.sc.getNetwork().getLinks().size()));
		log.info(String.format("Density: %2.6f%%", density));
		

		log.info("-----------------------------------------");
		log.info("               Complete");
		log.info("=========================================");
	}
	
	
	public SaturnNetworkBuilder() {
		sc = new ScenarioImpl();
	}


	/**
	 * Parse nodes from the tab-delimited text file. The first column must contain 
	 * a unique Id for each node; the second the longitude; and the third the 
	 * latitude.
	 * @param filename the absolute path of the tab-delimited file containing the
	 * 		node attributes.
	 */
	public void parseNodes(String filename) {
		int counter = 0;
		int multiplier = 1;
		log.info("Parsing nodes from " + filename);
		NetworkFactory nf = sc.getNetwork().getFactory();
		
		try {
			BufferedReader br = IOUtils.getBufferedReader(filename);
			try{
				String line = null;
				while((line = br.readLine()) != null){
					String [] entries = line.split("\t");
					Node n = nf.createNode(new IdImpl(entries[0]), new CoordImpl(entries[1], entries[2]));
					sc.getNetwork().addNode(n);
					
					/* Report progress */
					if(++counter == multiplier){
						log.info("   nodes parsed: " + counter);
						multiplier *= 2;
					}
				}				
			} finally{
				br.close();
			}
		} catch (FileNotFoundException e) {
			log.warn("   nodes parsed: " + counter + " (Exception)");
			e.printStackTrace();
		} catch (IOException e) {
			log.warn("   nodes parsed: " + counter + " (Exception)");
			e.printStackTrace();
		}
		log.info("   nodes parsed: " + counter + " (Done)");
	}

	
	/**
	 * Parse the link from the tab-delimited file. The first column of the file
	 * should contain the origin node Id; the second column the destination node
	 * Id; and the third the link's length. Number of lanes and capacity may also
	 * be included in the file, but as of now (Jan 2011) not used.
	 * @param filename the absolute path of the text file containing link attributes.
	 */
	public void parseLinks(String filename) {
		int counter = 0;
		int multiplier = 1;
		log.info("Parsing links from " + filename);
		NetworkFactory nf = sc.getNetwork().getFactory();
		
		try {
			BufferedReader br = IOUtils.getBufferedReader(filename);
			try{
				String line = null;
				while((line = br.readLine()) != null){
					String [] entries = line.split("\t");
					if(entries.length != 4){
						log.warn("entries length: " + entries.length);
					}
					Node fromNode = sc.getNetwork().getNodes().get(new IdImpl(entries[0]));
					Node toNode = sc.getNetwork().getNodes().get(new IdImpl(entries[1]));
					if(fromNode == null || toNode == null){
						log.warn("fromNode: " + fromNode);
						log.warn("  toNode: " + toNode);
					}
					Link l = nf.createLink(new IdImpl(counter), fromNode, toNode);
					l.setLength(Double.parseDouble(entries[2]));
					sc.getNetwork().addLink(l);
					
					/* Report progress */
					if(++counter == multiplier){
						log.info("   links parsed: " + counter);
						multiplier *= 2;
					}
				}				
			} finally{
				br.close();
			}
		} catch (FileNotFoundException e) {
			log.warn("   links parsed: " + counter + " (Exception)");
			e.printStackTrace();
		} catch (IOException e) {
			log.warn("   links parsed: " + counter + " (Exception)");
			e.printStackTrace();
		}
		log.info("   links parsed: " + counter + " (Done)");	
	}


	/**
	 * Writes the parsed network to three files:
	 * <ol>
	 * 	<li> MATSim {@link Network} <code>xml.gz</code>;
	 * 	<li> ESRI shapefile as <i>lines</i>;
	 * 	<li> ESRI shapefile as <i>polygons</i>. 
	 * </ol>
	 * @param folder absolute path where output will be written to;
	 * @param CRS the {@link MGC} text indicating the coordinate reference system, e.g. "<code>WGS84_UTM35S</code>" 
	 */
	private void writeNetwork(String folder, String CRS) {
		sc.getConfig().global().setCoordinateSystem(CRS);

		String networkFile = folder + "network.xml.gz";
		String lineFile = folder + "network_l.shp";
		String polygonFile = folder + "network_p.shp";
		
		log.info("Writing network to " + networkFile);
		final Network network = sc.getNetwork();
		FeatureGeneratorBuilderImpl builder = new FeatureGeneratorBuilderImpl(network, CRS);
		builder.setFeatureGeneratorPrototype(LineStringBasedFeatureGenerator.class);
		builder.setWidthCoefficient(0.5);
		builder.setWidthCalculatorPrototype(LanesBasedWidthCalculator.class);
		new Links2ESRIShape(network,lineFile, builder).write();

		CoordinateReferenceSystem crs = MGC.getCRS(CRS);
		builder.setWidthCoefficient(-0.003); /* for Commonwealth countries */
		builder.setFeatureGeneratorPrototype(PolygonFeatureGenerator.class);
		builder.setWidthCalculatorPrototype(CapacityBasedWidthCalculator.class);
		builder.setCoordinateReferenceSystem(crs);
		new Links2ESRIShape(network,polygonFile, builder).write();		
	}

}

