/* *********************************************************************** *
 * project: org.matsim.*
 * DigicoreVehicleWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.southafrica.projects.complexNetworks.pathDependence;

import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.core.utils.io.UncheckedIOException;

import playground.southafrica.projects.complexNetworks.pathDependence.PathDependentNetwork.PathDependentNode;

public class DigicorePathDependentNetworkWriter extends MatsimXmlWriter implements MatsimWriter{
	private final static Logger LOG = Logger.getLogger(DigicorePathDependentNetworkWriter.class);
	private final PathDependentNetwork network;

		
	public DigicorePathDependentNetworkWriter(PathDependentNetwork network){
		super();
		this.network = network;
		
		/* Clean up the network. This is necessary as some nodes may exist in
		 * the network, and they can be reached, but it may be impossible to
		 * actually go to a next node from there. Cleaning up ensures that each
		 * node dependency has at least one next node, even if it is 'sink'. */
		LOG.info("Cleaning up the network... this may take some time.");
		int nodesCleaned = 0;
		for(PathDependentNode node : this.network.getPathDependentNodes().values()){
			for(Map<Id<Node>, Double> map : node.getPathDependence().values()){
				if(map.isEmpty()){
					map.put(Id.create("sink", Node.class), 1.0);
					nodesCleaned++;
				}
			}
		}
		
		LOG.info("Done cleaning up the network (" + nodesCleaned + " fixed)");
	}

	
	@Override
	public void write(final String filename){
		writeV2(filename);
	}
	
	
	public void writeV1(final String filename){
		String dtd = "http://matsim.org/files/dtd/digicorePathDependentNetwork_v1.dtd";
		DigicorePathDependentNetworkWriterHandler handler = new DigicorePathDependentNetworkWriterHandlerImpl_v1();
		
		try {
			openFile(filename);
			writeXmlHead();
			writeDoctype("digicoreNetwork", dtd);
			
			handler.startNetwork(network, writer);
			for(PathDependentNode node : network.getPathDependentNodes().values()){
				handler.startNode(node, writer);
				for(Id<Node> id : node.getPathDependence().keySet()){
					handler.startPreceding(id, writer);
					
					/* Write the following nodes. */
					Map<Id<Node>, Double> map = node.getNextNodes(id);
					handler.startFollowing(map, writer);
					handler.endFollowing(writer);
					
					handler.endPreceding(writer);					
				}
				handler.endNode(node, this.writer);
			}
			handler.endNetwork(this.writer);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} finally{
			try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close writer.");	
			}
		}
	}
	
	
	public void writeV2(final String filename){
		String dtd = "http://matsim.org/files/dtd/digicorePathDependentNetwork_v2.dtd";
		DigicorePathDependentNetworkWriterHandler handler = new DigicorePathDependentNetworkWriterHandlerImpl_v2();
		
		try {
			openFile(filename);
			writeXmlHead();
			writeDoctype("digicoreNetwork", dtd);
			
			handler.startNetwork(network, writer);
			for(PathDependentNode node : network.getPathDependentNodes().values()){
				handler.startNode(node, writer);
				for(Id<Node> id : node.getPathDependence().keySet()){
					handler.startPreceding(id, writer);
					
					/* Write the following nodes. */
					Map<Id<Node>, Double> map = node.getNextNodes(id);
					handler.startFollowing(map, writer);
					handler.endFollowing(writer);
					
					handler.endPreceding(writer);	
					
				}

				/* Write the start hours if available. */
				Map<String, Integer> hourMap = node.getStartTimeMap();
				if(!hourMap.isEmpty()){
					handler.startStartTime(hourMap, writer);
					handler.endStartTime(writer);
				}
				
				/* Write the number of activities if available. */
				Map<String, Integer> activityMap = node.getNumberOfActivityMap();
				if(!activityMap.isEmpty()){
					handler.startActivities(activityMap, writer);
					handler.endActivities(writer);
				}
				
				handler.endNode(node, this.writer);
			}
			handler.endNetwork(this.writer);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} finally{
			try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close writer.");	
			}
		}
	}

}

