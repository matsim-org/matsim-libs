/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.ikaddoura.flow;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

import javax.xml.stream.XMLStreamException;

import org.apache.log4j.Logger;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

/**
 * @author ikaddoura
 * 
 * This class analyzes traffic flow items and writes them into a csv and a shapefile.
 *
 */
public class TrafficFlowDataAnalysis {
	private final Logger log = Logger.getLogger(TrafficFlowDataAnalysis.class);

	private final String crs = TransformationFactory.WGS84;
	
	private final String inputDirectory = "../../../shared-svn/studies/ihab/flow/berlin/";
	private final String outputDirectory = "../../../shared-svn/studies/ihab/flow/berlin/shp/";
	private final String networkFile = "../../../shared-svn/studies/ihab/berlin/network.xml";
	
// ##################################################################
	
	public static void main(String[] args) throws XMLStreamException, IOException, ParseException {
		
		TrafficFlowDataAnalysis incidentAnalysis = new TrafficFlowDataAnalysis();
		incidentAnalysis.run();	
	}

	public void run() throws XMLStreamException, IOException, ParseException {
		
		OutputDirectoryLogging.catchLogEntries();
		try {
			OutputDirectoryLogging.initLoggingWithOutputDirectory(this.outputDirectory);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		collectTrafficItems();
		
		OutputDirectoryLogging.closeOutputDirLogging();
	}

	private void collectTrafficItems() throws XMLStreamException, IOException {
		
		log.info("Collecting traffic items from all xml files in directory " + this.inputDirectory + "...");
	
		File[] fileList = new File(inputDirectory).listFiles();
		
		if (fileList.length == 0) {
			throw new RuntimeException("No file in " + this.inputDirectory + ". Aborting...");
		}
						
		for (File f : fileList) {
			 
			if (f.getName().endsWith(".xml.gz")) {
				
				log.info("###############################");

				log.info("File: " + f.getName());
			
				TrafficItemXMLReader reader = new TrafficItemXMLReader();
				reader.setValidating(false);
				reader.readFile(f.toString());
				
				log.info("Traffic flow items: ");
				for (TrafficItem item : reader.getTrafficItems()) {
					log.info(item.toString());
				}
				log.info("###############################");
				
				log.info("Writing traffic flow items to shape file...");
				TrafficFlowItem2ShapeWriter shpWriter = new TrafficFlowItem2ShapeWriter(reader.getTrafficItems());
				String outputFile = f.getName().replaceAll(".xml.gz", ".shp");
				shpWriter.writeTrafficItemsToShapeFile(outputDirectory + outputFile, crs);
				
				log.info("Writing traffic flow items to shape file... Done.");

				log.info("###############################");
				
				TrafficFlowItem2Network flow2Network = new TrafficFlowItem2Network(reader.getTrafficItems(), networkFile);
				flow2Network.run();
				
			}
		}
		
		log.info("Collecting traffic items from all xml files in directory " + this.inputDirectory + "... Done.");	
	}
}
