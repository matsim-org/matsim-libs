/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.mrieser.svi.converters;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author mrieser / senozon
 */
public class DynusTNetworkWriter {

	private final static Logger log = Logger.getLogger(DynusTNetworkWriter.class);
	
	private final Network network;
	
	public DynusTNetworkWriter(final Network network) {
		this.network = network;
	}
	
	public void writeToDirectory(final String directoryName) {
		Map<Id, Integer> nodesIndex = new HashMap<Id, Integer>();
		int i = 0;
		for (Node node : this.network.getNodes().values()) {
			i++;
			nodesIndex.put(node.getId(), i);
		}
		
		writeXy(directoryName + "/xy.dat", nodesIndex);
		writeNetwork(directoryName + "/network.dat", nodesIndex);
	}
	
	private void writeXy(final String filename, Map<Id, Integer> nodesIndex) {
		BufferedWriter writer = IOUtils.getBufferedWriter(filename);
		try {
			for (Node node : this.network.getNodes().values()) {
				writer.write(Integer.toString(nodesIndex.get(node.getId())));
				writer.write('\t');
				writer.write(Double.toString(node.getCoord().getX()));
				writer.write('\t');
				writer.write(Double.toString(node.getCoord().getY()));
				writer.write("\r\n");
			}
		} catch (IOException e) {
			log.error("Error writing file " + filename, e);
		} finally {
			try {
				writer.close();
			}
			catch (IOException e2) {
				log.error("Could not close file " + filename, e2);
			}
		}
	}
	
	private void writeNetwork(final String filename, final Map<Id, Integer> nodesIndex) {
		BufferedWriter writer = IOUtils.getBufferedWriter(filename);
		try {
			// header
			// nzones,noofnodes,noofarcs,kay,SuperZoneSwitch
			
			writer.write(Integer.toString(1)); // TODO nOfZones
			writer.write('\t');
			writer.write(Integer.toString(nodesIndex.size()));
			writer.write('\t');
			writer.write(Integer.toString(this.network.getLinks().size()));
			writer.write('\t');
			writer.write('1');
			writer.write('\t');
			writer.write('0');
			writer.write("\r\n");
			
			// nodes
			//  m_dynust_network_node_nde(i)%IntoOutNodeNum, m_dynust_network_node_nde(i)%izone !%%%
			for (Node node : this.network.getNodes().values()) {
				writer.write(Integer.toString(nodesIndex.get(node.getId())));
				writer.write('\t');
				writer.write('1'); // TODO zone index
				writer.write("\r\n");
			}
			
			// arcs = links
			// iu,id,MTbay,MTbayR,i3,m_dynust_network_arc_de(i)%nlanes,m_dynust_network_arc_de(i)%FlowModelNum,m_dynust_network_arc_de(i)%Vfadjust,m_dynust_network_arc_de(i)%SpeedLimit,mfrtp,sattp,m_dynust_network_arc_nde(i)%link_iden, m_dynust_network_arc_de(i)%LGrade
			for (Link link : this.network.getLinks().values()) {
				writer.write(Integer.toString(nodesIndex.get(link.getFromNode().getId())));
				writer.write('\t');
				writer.write(Integer.toString(nodesIndex.get(link.getToNode().getId())));
				writer.write('\t');
				writer.write('0'); // MTbay
				writer.write('\t');
				writer.write('0'); // MTbayR
				writer.write('\t');
				writer.write(Double.toString(link.getLength())); // i3, link length?
				writer.write('\t');
				writer.write(Integer.toString((int) link.getNumberOfLanes())); // nOfLanes
				writer.write('\t');
				writer.write('1'); // FlowModelNum; TODO currently all is highway
				writer.write('\t');
				writer.write('5'); // Vfadjust; speed adjustment in mph
				writer.write('\t');
				writer.write(Double.toString(link.getFreespeed())); // SpeedLimit, miles per hour
				writer.write('\t');
				writer.write(Double.toString(link.getCapacity())); // mfrtp; maximum service flow rate?
				writer.write('\t');
				writer.write(Double.toString(link.getCapacity())); // sattp; saturation flow rate
				writer.write('\t');
				writer.write('1'); // m_dynust_network_arc_nde(i)%link_iden; link type identification, TODO currently all is highway
				writer.write('\t');
				writer.write('0'); // m_dynust_network_arc_de(i)%LGrade
				writer.write("\r\n");
			}
		} catch (IOException e) {
			log.error("Error writing file " + filename, e);
		} finally {
			try {
				writer.close();
			}
			catch (IOException e2) {
				log.error("Could not close file " + filename, e2);
			}
		}
	}
}
