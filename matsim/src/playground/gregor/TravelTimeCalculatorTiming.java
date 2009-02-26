/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeCalculatorTiming.java
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

package playground.gregor;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.events.Events;
import org.matsim.events.MatsimEventsReader;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.network.LinkImpl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkFactory;
import org.matsim.network.NetworkLayer;
import org.matsim.trafficmonitoring.OptimisticTravelTimeAggregator;
import org.matsim.trafficmonitoring.TravelTimeCalculator;
import org.matsim.trafficmonitoring.TravelTimeAggregatorFactory;
import org.matsim.trafficmonitoring.TravelTimeDataArray;
import org.matsim.utils.io.IOUtils;

public class TravelTimeCalculatorTiming {
	
	
	private final String netfile;
	private final String eventsfile;
	private final NetworkLayer network;
	TravelTimeCalculator ttcalc;




	public TravelTimeCalculatorTiming(final String network, final String events, final Class<? extends LinkImpl> clazz) {
		this.netfile = network;
		this.eventsfile = events;
		Gbl.createConfig(null);
		Gbl.createWorld();
				// create a network
		NetworkFactory nf = new NetworkFactory();
		nf.setLinkPrototype(clazz);
		this.network = new NetworkLayer(nf);
		new MatsimNetworkReader(this.network).readFile(this.netfile);
		Gbl.getWorld().setNetworkLayer(this.network);
		Gbl.getWorld().complete();
	}
	
	public void readEvents(final int binSize, final BufferedWriter writer) {
		
		long start,stop;
		System.out.println("Reading events ...");
		
		Events events = new Events();
		MatsimEventsReader eventsReader = new MatsimEventsReader(events);

		// setup traveltime calculator
		TravelTimeAggregatorFactory factory = new TravelTimeAggregatorFactory();
		factory.setTravelTimeAggregatorPrototype(OptimisticTravelTimeAggregator.class);
		factory.setTravelTimeDataPrototype(TravelTimeDataArray.class);
		
		this.ttcalc = new TravelTimeCalculator(this.network, binSize * 60, 30*3600, factory);
		events.addHandler(this.ttcalc);
		// read events
		start = System.currentTimeMillis();
		eventsReader.readFile(this.eventsfile);
		stop =  System.currentTimeMillis();
//		this.ttcalc.counter.printCounter();
		System.gc();
		System.out.println("elapsed time: " + (stop - start));
		try {
			long totalMem = Runtime.getRuntime().totalMemory();
			long freeMem = Runtime.getRuntime().freeMemory();
			long usedMem = totalMem - freeMem;
			writer.write((stop - start) + "," + usedMem + ",");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Gbl.printMemoryUsage();
		events.printEventsCount();
		
		
	}
	
//	private void reReadEvents() {
//		long start,stop;
//		System.out.println("Reading events  again ...");
//		Events events = new Events();
//		MatsimEventsReader eventsReader = new MatsimEventsReader(events);
//		ttcalc.resetTravelTimes();
//		events.addHandler(ttcalc);
//		// read events
//		start = System.currentTimeMillis();
//		eventsReader.readFile(this.eventsfile);
//		stop =  System.currentTimeMillis();
//		System.out.println("elapsed time: " + (stop-start));
//		Gbl.printMemoryUsage();
//		events.printEventsCount();
//		
//		
//	}
	
	public void readTT(final BufferedWriter writer) {
		long start,stop;
		long elapsed = 0;
		double oatt = 0;
		System.out.println("Reading tt for all links ...");
		start = System.currentTimeMillis();	
		for (final Link link : this.network.getLinks().values()) {
			for (int i = 0; i < 20; i++) {
				for (double time = 3*3600; time <= 15*3600; time += 1*60) {
	
					final double ttime = this.ttcalc.getLinkTravelTime(link, time);
	
					oatt += ttime;
				}
			}
		}
		stop = System.currentTimeMillis();
		elapsed += (stop - start);		
		try {
			writer.write((int) elapsed + "\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
			System.out.println("elapsed time: " + elapsed + " oatt:" + oatt);		
//			try {
//				Thread.sleep(20000);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
		}
	
	
	public static void main(final String [] args) {
		
		BufferedWriter writer = null;
		try {
			 writer = IOUtils.getBufferedWriter("arrayTT.csv", false);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			writer.write("binSize,storageTime,Mem,readTime\n");
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		String network = "../inputs/networks/padang_net_evac_v20080618.xml";
		String events = "../outputs/output/ITERS/it.201/201.events.txt.gz";
		TravelTimeCalculatorTiming timer = new TravelTimeCalculatorTiming(network, events, LinkImpl.class);
		
		for (int i = 15; i > 0; i--) {
			try {
				writer.write(i + ",");
			} catch (IOException e) {
				e.printStackTrace();
			}
			timer.readEvents(15,writer);
			timer.readTT(writer);
		}
		try {
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		timer.reReadEvents();
//		timer.readTT();
	}



}
