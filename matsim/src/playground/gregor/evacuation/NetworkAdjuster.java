/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkAdjuster.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.gregor.evacuation;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkLayerBuilder;
import org.matsim.network.NetworkWriter;
import org.matsim.utils.identifiers.IdI;

public class NetworkAdjuster {
	
	private NetworkLayer network;
	private double storageCap;
	private double flowCap;

	public NetworkAdjuster(NetworkLayer network){
		this.network = network;
	}

	public void setStorageCap(double storageCap) {
		this.storageCap = storageCap;
	}
	
	public void setFlowCap(double flowCap) {
		this.flowCap = flowCap;
	}
	
	
	public NetworkLayer performChanges(){
		
		ConcurrentLinkedQueue<Link> links = new ConcurrentLinkedQueue<Link>();
		for (Link link : this.network.getLinks().values()) {
			links.add(link);
		}
		
		while (links.peek() != null) {
			Link link = links.poll();
			String id = link.getId().toString();
			String from = link.getFromNode().getId().toString();
			String to = link.getToNode().getId().toString();
			String length = Double.toString(link.getLength());
			String freespeed =  Double.toString(link.getFreespeed(org.matsim.utils.misc.Time.UNDEFINED_TIME));
			String capacity = Double.toString(link.getCapacity());
			String origid = link.getOrigId();
			String type = link.getType();
			
			double min_width = link.getCapacity() / this.flowCap;
			double storage = min_width * link.getLength() * this.storageCap;
			double laneCap = this.network.getEffectiveLaneWidth() * link.getLength() * this.storageCap;
			
			double lanes = Math.max(storage/laneCap ,1.0 );
			
			String permlanes = Double.toString(lanes);
			
			this.network.removeLink(link);
			this.network.createLink(id, from, to, length, freespeed, capacity, permlanes, origid, type);
			
		}
		
		return this.network;
		
	}

	
	public static void main(String [] args) {
		
		
		
		final double storageCap = 5.4;
		final double flowCap = 1.33;
		
		String configFile = "./configs/evacuationConf.xml";

		Config config = Gbl.createConfig(new String[] {configFile});
		System.out.println("  reading the network...");
		NetworkLayer network = null;
		NetworkLayerBuilder.setNetworkLayerType(NetworkLayerBuilder.NETWORK_DEFAULT);
		network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
		System.out.println("  done.");
		
		NetworkAdjuster na = new NetworkAdjuster(network);
		na.setStorageCap(storageCap);
		na.setFlowCap(flowCap);
		network = na.performChanges();
		
		NetworkWriter writer = new NetworkWriter(network,"padang_net2.xml");
		writer.write();
		
	}
	
	
}
