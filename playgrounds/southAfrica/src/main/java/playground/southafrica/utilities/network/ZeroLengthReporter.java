/* *********************************************************************** *
 * project: org.matsim.*
 * ZeroLengthReporter.java
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

/**
 * 
 */
package playground.southafrica.utilities.network;

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.southafrica.utilities.Header;

/**
 * Class to identify zero-length links, and report their OpenStreetMap
 * identities so they can be fixed.
 * 
 * @author jwjoubert
 */
public class ZeroLengthReporter {
	final private static Logger LOG = Logger.getLogger(ZeroLengthReporter.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(ZeroLengthReporter.class.toString(), args);
		
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		String networkFile = args[0];
		String outputFile = args[1];
		new MatsimNetworkReader(sc.getNetwork()).parse(networkFile);
		
		reportZeroLengthLinks(sc.getNetwork(), outputFile);
		
		Header.printFooter();
	}
	
	private ZeroLengthReporter(){
	}
	
	public static void reportZeroLengthLinks(Network network, String output){
		LOG.info("Reporting zero-length links...");
		int count = 0;
		BufferedWriter bw = IOUtils.getBufferedWriter(output);
		try{
			bw.write("linkId,originId,fromNodeId,toNodeId");
			bw.newLine();
			for(Link link : network.getLinks().values()){
				if(link.getLength() <= 0.0){
					/* Write to standard out. */
					LOG.info("Link " + link.getId().toString() + 
							" is " + String.format("%.2f", link.getLength()) + "m (OSM " + 
					((LinkImpl)link).getOrigId() + "); from " + 
							link.getFromNode().getId().toString() + " to " + 
					link.getToNode().getId().toString());
					
					/* Write to file. */
					bw.write(String.format("%s,%s,%s,%s,%.1f\n", 
							link.getId().toString(),
							((LinkImpl)link).getOrigId(),
							link.getFromNode().getId().toString(),
							link.getToNode().getId().toString(),
							link.getLength()));
					count++;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + output);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + output);
			}
		}
		
		if(count == 0){
			LOG.warn("Great! No more zero-length links.");
		} else{
			LOG.warn("There are a total of " + count + " zero-length links.");
		}
		
		LOG.info("Done reporting zero-length links.");
	}

}
