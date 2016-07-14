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
 * Writes a MATSim network into multiple text files suitable to insert the data
 * into an Excel file readable by DynusT/NEXTA for creating the network.
 *
 * @author mrieser / senozon
 */
public class DynusTNetworkWriterXls {

	private final static Logger log = Logger.getLogger(DynusTNetworkWriterXls.class);

	private final Network network;

	public DynusTNetworkWriterXls(final Network network) {
		this.network = network;
	}

	public void writeToDirectory(final String directoryName) {
		Map<Id, Integer> nodesIndex = new HashMap<Id, Integer>();
		int i = 0;
		for (Node node : this.network.getNodes().values()) {
			i++;
			nodesIndex.put(node.getId(), i);
		}

		writeNodes(directoryName + "/NODE_4xls.txt", nodesIndex);
		writeLinks(directoryName + "/LINK_4xls.txt", nodesIndex);
	}

	private void writeNodes(final String filename, final Map<Id, Integer> nodesIndex) {
		BufferedWriter writer = IOUtils.getBufferedWriter(filename);
		try {
			writer.write("ID\tLongitude\tLatitude\tTAZ\tCTRL_TYPE\r\n");
			for (Node node : this.network.getNodes().values()) {
				writer.write(Integer.toString(nodesIndex.get(node.getId())));
				writer.write('\t');
				writer.write(Double.toString(node.getCoord().getX()));
				writer.write('\t');
				writer.write(Double.toString(node.getCoord().getY()));
				writer.write("\t0\t1");
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

	private void writeLinks(final String filename, final Map<Id, Integer> nodesIndex) {
		BufferedWriter writer = IOUtils.getBufferedWriter(filename);
		try {
			// header
			writer.write("ID\tLength\tDir\tTYPE\tLANES\tTAZ\tFrom_ID\tTo_ID\tGRADE\tNAME\tLEFTTURNBAY\tLIMIT\tADJSPEED\tSATURATION_FLOW_RATE\tMAX_SERVICE_RATE\tRIGHTTURNBAY\r\n");

			// arcs = links
			// iu,id,MTbay,MTbayR,i3,m_dynust_network_arc_de(i)%nlanes,m_dynust_network_arc_de(i)%FlowModelNum,m_dynust_network_arc_de(i)%Vfadjust,m_dynust_network_arc_de(i)%SpeedLimit,mfrtp,sattp,m_dynust_network_arc_nde(i)%link_iden, m_dynust_network_arc_de(i)%LGrade
			for (Link link : this.network.getLinks().values()) {
				String linkId = link.getId().toString();
				if (linkId.equals("0")) {
					linkId = "999999";
				}
				writer.write(linkId);
				writer.write('\t');
				writer.write(Double.toString(link.getLength() * 3.2808399)); // i3, link length, convert from meter to feet
				writer.write('\t');
				writer.write('1'); // unidirectional
				writer.write('\t');
				writer.write('5'); // arterial
				writer.write('\t');
				writer.write(Integer.toString((int) link.getNumberOfLanes())); // nOfLanes
				writer.write('\t');
				writer.write('0'); // TAZ
				writer.write('\t');
				writer.write(Integer.toString(nodesIndex.get(link.getFromNode().getId()))); // From_ID
				writer.write('\t');
				writer.write(Integer.toString(nodesIndex.get(link.getToNode().getId()))); // To_ID
				writer.write('\t');
				// Grade
				writer.write('\t');
				// Name
				writer.write('\t');
				// LeftTurnBay
				writer.write('\t');
				writer.write(Double.toString(link.getFreespeed() * 3.6 * 0.621371192)); // SpeedLimit, convert from meter per second to kilometer per hour to miles per hour
				writer.write('\t');
				writer.write('5'); // ADJSPEED; speed adjustment in mph
				writer.write('\t');
				// SATURATION_FLOW_RATE
				writer.write('\t');
				// MAX_SERVICE_RATE
				writer.write('\t');
				// RIGHTTURNBAY
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
