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

package playground.mrieser.svi.converters;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;

/**
 * @author mrieser / senozon
 */
public class DynusTNetworkReader {

	private final static Logger log = Logger.getLogger(DynusTNetworkReader.class);
	
	private final Network network;
	
	public DynusTNetworkReader(final Network network) {
		this.network = network;
	}
	
	public void readFiles(final String xyFilename, final String networkFilename) {
		readXy(xyFilename);
		readNetwork(networkFilename);
	}
	
	private void readXy(final String xyFilename) {
		NetworkFactory nf = this.network.getFactory();
		BufferedReader reader = IOUtils.getBufferedReader(xyFilename);
		try {
			String line;
			while ((line = reader.readLine()) != null) {
				String[] parts = line.trim().split("\\s+");
				if (parts.length == 3) {
					Node n = nf.createNode(Id.create(parts[0], Node.class), new Coord(Double.parseDouble(parts[1]), Double.parseDouble(parts[2])));
					this.network.addNode(n);
				} else {
					log.warn("Cannot parse line " + line);
					log.warn("Got parts: " + Arrays.toString(parts));
				}
			}
		} catch (IOException e) {
			log.error("Error reading file " + xyFilename, e);
		} finally {
			try {
				reader.close();
			}
			catch (IOException e2) {
				log.error("Could not close file " + xyFilename, e2);
			}
		}
	}
	
	private void readNetwork(final String networkFilename) {
		NetworkFactory nf = this.network.getFactory();
		BufferedReader reader = IOUtils.getBufferedReader(networkFilename);
		try {
			String header = reader.readLine();
			String[] headerParts = header.trim().split("\\s+");
			int nOfNodes = Integer.parseInt(headerParts[1]);
			int nOfLinks = Integer.parseInt(headerParts[2]);
			for (int i = 0; i < nOfNodes; i++) {
				reader.readLine();
			}
			for (int i = 0; i < nOfLinks; i++) {
				String line = reader.readLine();
				String[] parts = line.trim().split("\\s+");
				Id<Link> linkId = Id.create(i, Link.class);
				Node fromNode = this.network.getNodes().get(Id.create(parts[0], Node.class));
				Node toNode = this.network.getNodes().get(Id.create(parts[1], Node.class));
				if (fromNode == null || toNode == null) {
					System.out.println("breakpoint");
				}
				Link link = nf.createLink(linkId, fromNode, toNode);
				this.network.addLink(link);
				link.setLength(Double.parseDouble(parts[4]) / 3.2808399); // convert feet to meter
				link.setNumberOfLanes(Double.parseDouble(parts[5]));
				link.setFreespeed(Double.parseDouble(parts[8]) / 3.6 / 0.621371192); // convert miles per hour to meter per second
				link.setCapacity(Double.parseDouble(parts[9]));
			}
		} catch (IOException e) {
			log.error("Error reading file " + networkFilename, e);
		} finally {
			try {
				reader.close();
			}
			catch (IOException e2) {
				log.error("Could not close file " + networkFilename, e2);
			}
		}
	}
	
	public static void main(String[] args) {
		
		String xyDatFilename = "/Volumes/Data/projects/sviDosierungsanlagen/scenarios/kreuzlingen/output29/xy.dat";
		String networkDatFilename = "/Volumes/Data/projects/sviDosierungsanlagen/scenarios/kreuzlingen/output29/network.dat";
		String networkXmlFilename = "/Volumes/Data/projects/sviDosierungsanlagen/scenarios/kreuzlingen/network.DynusT.xml.gz";
		
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new DynusTNetworkReader(sc.getNetwork()).readFiles(xyDatFilename, networkDatFilename);
		new NetworkWriter(sc.getNetwork()).write(networkXmlFilename);
	}
	
}
