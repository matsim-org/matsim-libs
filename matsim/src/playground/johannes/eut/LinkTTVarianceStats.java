/* *********************************************************************** *
 * project: org.matsim.*
 * LinkTTVarianceStats.java
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

/**
 * 
 */
package playground.johannes.eut;

import java.io.BufferedWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.matsim.controler.Controler;
import org.matsim.controler.events.IterationEndsEvent;
import org.matsim.controler.events.ShutdownEvent;
import org.matsim.controler.listener.IterationEndsListener;
import org.matsim.controler.listener.ShutdownListener;
import org.matsim.interfaces.networks.basicNet.BasicLinkI;
import org.matsim.interfaces.networks.basicNet.BasicNetI;
import org.matsim.router.util.TravelTimeI;
import org.matsim.utils.io.IOUtils;

/**
 * @author illenberger
 *
 */
public class LinkTTVarianceStats implements IterationEndsListener, ShutdownListener {
	
	private Map<BasicLinkI, List<Double>> linkSamples;
	
	private TravelTimeI travelTimes;
	
	private int startTime;
	
	private int endTime;
	
	private int binsize;
	
	public LinkTTVarianceStats(TravelTimeI travelTimes, int start, int end, int binsize) {
		this.travelTimes = travelTimes;
		this.startTime = start;
		this.endTime = end;
		this.binsize = binsize;
		linkSamples = new HashMap<BasicLinkI, List<Double>>();
	}
	
	public void notifyIterationEnds(IterationEndsEvent event) {
		BasicNetI network = event.getControler().getNetwork();
		LinkTTStats linkStats = new LinkTTStats(network, travelTimes, startTime, endTime, binsize);
	
		for(BasicLinkI link : network.getLinks().values()) {
			List<Double> samples = linkSamples.get(link);
			if(samples == null) {
				samples = new LinkedList<Double>();
				linkSamples.put(link, samples);
			}
			samples.add(linkStats.getLinkAttributes(link).avrTT);
		}
	}

	public void notifyShutdown(ShutdownEvent event) {
		Map<BasicLinkI, Double> variances = new HashMap<BasicLinkI, Double>();
		double varianceSum = 0;
		for(BasicLinkI link : linkSamples.keySet()) {
			List<Double> samples = linkSamples.get(link);
			
			double avr = 0;
			for(Double sample : samples) {
				avr += sample;
			}
			avr = avr/(double)samples.size();
			
			double squaresum = 0;
			for(Double sample : samples) {
				squaresum += Math.pow(sample - avr, 2);
			}
			
			double variance = Math.sqrt((1.0 / (double)(samples.size() - 1)) * squaresum);
			varianceSum += variance;
			variances.put(link, variance);
		}
		
		double varianceAvr = varianceSum / (double)linkSamples.size();
		
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(Controler.getOutputFilename("linkvariances.txt"));
			writer.write("link\tvariance");
			writer.newLine();
			
			for(BasicLinkI link : variances.keySet()) {
				writer.write(link.getId().toString());
				writer.write("\t");
				writer.write(variances.get(link).toString());
				writer.newLine();
			}
			writer.write("avr\t");
			writer.write(String.valueOf(varianceAvr));
			writer.newLine();
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
