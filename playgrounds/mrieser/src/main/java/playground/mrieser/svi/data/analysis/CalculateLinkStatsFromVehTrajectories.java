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

package playground.mrieser.svi.data.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Time;

import playground.mrieser.svi.data.vehtrajectories.VehicleTrajectory;
import playground.mrieser.svi.data.vehtrajectories.VehicleTrajectoryHandler;

/**
 * Calculates the number of vehicles per link and per hour. Outputs the data
 * in a textual table.
 * 
 * @author mrieser
 */
public class CalculateLinkStatsFromVehTrajectories implements VehicleTrajectoryHandler {

	private final static Logger log = Logger.getLogger(CalculateLinkStatsFromVehTrajectories.class);
	
	private final Network network;
	private final HashMap<Link, int[]> linkVolumes = new LinkedHashMap<Link, int[]>();
	private final HashMap<Link, double[]> sumLinkTravelTimes = new LinkedHashMap<Link, double[]>();
	private final int binSize = 3600;
	private final int maxBinIndex = 86400 / this.binSize + 1;
	
	public CalculateLinkStatsFromVehTrajectories(final Network network) {
		this.network = network;
	}
	
	@Override
	public void handleVehicleTrajectory(VehicleTrajectory trajectory) {
		int[] nodes = trajectory.getTravelledNodes();
		double[] times = trajectory.getTravelledNodeTimes();
		
		double time = trajectory.getStartTime();
		
		Node prevNode = null;
		for (int i = 0; i < nodes.length; i++) {
			Node node = this.network.getNodes().get(Id.create(nodes[i], Node.class));
			if (prevNode != null) {
				Link link = NetworkUtils.getConnectingLink(prevNode, node);
				if (link == null) {
					log.error("No link found from " + prevNode.getId() + " to " + node.getId() + " for trajectory " + trajectory.getVehNr());
					break;
				}
				double linkTime = times[i];
				int bin = getTimeBin(time);
				getLinkVolumes(link)[bin]++;
				getLinkTravelTimes(link)[bin] += linkTime;
				time += linkTime;
			}
			prevNode = node;
		}
	}
	
	private int[] getLinkVolumes(final Link link) {
		int[] volumes = this.linkVolumes.get(link);
		if (volumes == null) {
			volumes = new int[this.maxBinIndex + 1];
			this.linkVolumes.put(link, volumes);
		}
		return volumes;
	}
	
	private double[] getLinkTravelTimes(final Link link) {
		double[] traveltimes = this.sumLinkTravelTimes.get(link);
		if (traveltimes == null) {
			traveltimes = new double[this.maxBinIndex + 1];
			this.sumLinkTravelTimes.put(link, traveltimes);
		}
		return traveltimes;
	}
	
	private int getTimeBin(final double secondsSinceMidnight) {
		return (int) Math.min(secondsSinceMidnight / this.binSize, this.maxBinIndex);
	}
	
	public void writeLinkVolumesToFile(final String filename) {
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(filename);
			
			writer.write("LinkID\tFromNode\tToNode");
			for (int i = 0; i <= this.maxBinIndex; i++) {
				writer.write("\t");
				writer.write(Time.writeTime(i * this.binSize, Time.TIMEFORMAT_HHMM, '_'));
			}
			writer.write(IOUtils.NATIVE_NEWLINE);
			
			for (Map.Entry<Link, int[]> e : linkVolumes.entrySet()) {
				Link link = e.getKey();
				int[] volumes = e.getValue();

				writer.write(link.getId().toString());
				writer.write("\t");
				writer.write(link.getFromNode().getId().toString());
				writer.write("\t");
				writer.write(link.getToNode().getId().toString());
				
				for (int i = 0; i <= this.maxBinIndex; i++) {
					writer.write("\t");
					writer.write(Integer.toString(volumes[i]));
				}
				writer.write(IOUtils.NATIVE_NEWLINE);
			}
			
			writer.close();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public void writeLinkTravelTimesToFile(final String filename) {
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(filename);
			
			writer.write("LinkID\tFromNode\tToNode");
			for (int i = 0; i <= this.maxBinIndex; i++) {
				writer.write("\t");
				writer.write(Time.writeTime(i * this.binSize, Time.TIMEFORMAT_HHMM, '_'));
			}
			writer.write(IOUtils.NATIVE_NEWLINE);
			
			for (Map.Entry<Link, int[]> e : linkVolumes.entrySet()) {
				Link link = e.getKey();
				int[] volumes = e.getValue();
				
				writer.write(link.getId().toString());
				writer.write("\t");
				writer.write(link.getFromNode().getId().toString());
				writer.write("\t");
				writer.write(link.getToNode().getId().toString());
				
				double[] sumTravelTimes = this.sumLinkTravelTimes.get(link);
				
				for (int i = 0; i <= this.maxBinIndex; i++) {
					writer.write("\t");
					if (volumes[i] > 0) {
						writer.write(Double.toString(sumTravelTimes[i] / volumes[i]));
					} else {
						writer.write(Double.toString(link.getLength() / link.getFreespeed()));
					}
				}
				writer.write(IOUtils.NATIVE_NEWLINE);
			}
			
			writer.close();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	public void writeLinkTravelSpeedsToFile(final String filename) {
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(filename);
			
			writer.write("LinkID\tFromNode\tToNode");
			for (int i = 0; i <= this.maxBinIndex; i++) {
				writer.write("\t");
				writer.write(Time.writeTime(i * this.binSize, Time.TIMEFORMAT_HHMM, '_'));
			}
			writer.write(IOUtils.NATIVE_NEWLINE);
			
			for (Map.Entry<Link, int[]> e : linkVolumes.entrySet()) {
				Link link = e.getKey();
				int[] volumes = e.getValue();
				
				writer.write(link.getId().toString());
				writer.write("\t");
				writer.write(link.getFromNode().getId().toString());
				writer.write("\t");
				writer.write(link.getToNode().getId().toString());
				
				double[] sumTravelTimes = this.sumLinkTravelTimes.get(link);
				
				for (int i = 0; i <= this.maxBinIndex; i++) {
					writer.write("\t");
					if (volumes[i] > 0) {
						writer.write(Double.toString(link.getLength() / (sumTravelTimes[i] / volumes[i]) * 3.6));
					} else {
						writer.write(Double.toString(link.getFreespeed() * 3.6));
					}
				}
				writer.write(IOUtils.NATIVE_NEWLINE);
			}
			
			writer.close();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
