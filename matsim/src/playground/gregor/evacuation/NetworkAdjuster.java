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
import org.matsim.interfaces.core.v01.Link;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkWriter;
import org.matsim.utils.misc.Time;

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
	
	
	public NetworkLayer performChanges(double effectiveCellSize, double effectiveLaneWidth){
		
		this.network.setEffectiveCellSize(effectiveCellSize);
		this.network.setEffectiveLaneWidth(effectiveLaneWidth);
		
		ConcurrentLinkedQueue<Link> links = new ConcurrentLinkedQueue<Link>();
		for (Link link : this.network.getLinks().values()) {
			links.add(link);
		}
		
		while (links.peek() != null) {
			Link link = links.poll();
			double freespeed = link.getFreespeed(Time.UNDEFINED_TIME);
			double capacity = link.getCapacity(Time.UNDEFINED_TIME);
			String origid = link.getOrigId();
			String type = link.getType();
			
			double min_width = link.getCapacity(Time.UNDEFINED_TIME) / this.flowCap;
			double storage = min_width * link.getLength() * this.storageCap;
			double laneCap = this.network.getEffectiveLaneWidth() * link.getLength() * this.storageCap;
			
			double lanes = Math.max(storage/laneCap, 1.0);

			this.network.removeLink(link);
			this.network.createLink(link.getId(), link.getFromNode(), link.getToNode(), link.getLength(), freespeed, capacity, lanes, origid, type);
			// uhmm.... why not just using link.setLanes(), instead of deleting the link and adding it with again with slightly changed values?
		}

		return this.network;
		
	}

	
	public static void main(String [] args) {
		
		
		
		final double storageCap = 5.4;
		final double flowCap = 1.33;
		final double effCS = 0.26;
		final double effLW = 0.71;
		
		String configFile = "./configs/timeVariantEvac.xml";

		Config config = Gbl.createConfig(new String[] {configFile});
		System.out.println("  reading the network...");
		NetworkLayer network = null;
//		NetworkLayerBuilder.setNetworkLayerType(NetworkLayerBuilder.NETWORK_DEFAULT);
		network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
		System.out.println("  done.");
		
		NetworkAdjuster na = new NetworkAdjuster(network);
		na.setStorageCap(storageCap);
		na.setFlowCap(flowCap);
		network = na.performChanges(effCS,effLW);
		
		NetworkWriter writer = new NetworkWriter(network,"padang_net2.xml");
		writer.write();
		
	}
	
	
}
