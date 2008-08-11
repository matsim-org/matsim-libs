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

import org.matsim.events.Events;
import org.matsim.events.MatsimEventsReader;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.LinkImpl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkFactory;
import org.matsim.network.NetworkLayer;
import org.matsim.trafficmonitoring.OptimisticTravelTimeAggregator;
import org.matsim.trafficmonitoring.TravelTimeCalculator;
import org.matsim.trafficmonitoring.TravelTimeCalculatorFactory;
import org.matsim.trafficmonitoring.TravelTimeRoleArray;

public class TravelTimeCalculatorTiming {
	
	
	private final String netfile;
	private final String eventsfile;
	private final NetworkLayer network;
	TravelTimeCalculator ttcalc;




	public TravelTimeCalculatorTiming(String network, String events, Class<? extends LinkImpl> clazz) {
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
	
	public void readEvents() {
		
		long start,stop;
		System.out.println("Reading events ...");
		
		Events events = new Events();
		MatsimEventsReader eventsReader = new MatsimEventsReader(events);

		// setup traveltime calculator
		TravelTimeCalculatorFactory factory = new TravelTimeCalculatorFactory();
		factory.setTravelTimeAggregatorPrototype(OptimisticTravelTimeAggregator.class);
		factory.setTravelTimeRolePrototype(TravelTimeRoleArray.class);
		
		this.ttcalc = new TravelTimeCalculator(network, 15 * 60, 30*3600, factory);
		events.addHandler(ttcalc);
		// read events
		start = System.currentTimeMillis();
		eventsReader.readFile(this.eventsfile);
		stop =  System.currentTimeMillis();
		System.out.println("elapsed time: " + (stop - start));
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
	
	public void readTT() {
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
		System.out.println("elapsed time: " + elapsed + " oatt:" + oatt);		
	}
	
	
	public static void main(String [] args) {
		
		String network = "./networks/padang_net_evac_v20080618.xml";
		String events = "./output/ITERS/it.50/50.events.txt.gz";
		TravelTimeCalculatorTiming timer = new TravelTimeCalculatorTiming(network, events, LinkImpl.class);
		timer.readEvents();
		timer.readTT();
//		timer.reReadEvents();
//		timer.readTT();
	}



}
