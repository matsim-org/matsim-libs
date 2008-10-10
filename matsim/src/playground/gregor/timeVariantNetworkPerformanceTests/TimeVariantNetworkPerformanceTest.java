/* *********************************************************************** *
 * project: org.matsim.*
 * TimeVartiantNetworkPerformanceTest.java
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

package playground.gregor.timeVariantNetworkPerformanceTests;

import org.matsim.basic.v01.IdImpl;
import org.matsim.gbl.MatsimRandom;
import org.matsim.network.Link;
import org.matsim.network.NetworkChangeEvent;
import org.matsim.network.NetworkFactory;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.network.TimeVariantLinkImpl;
import org.matsim.network.NetworkChangeEvent.ChangeType;
import org.matsim.network.NetworkChangeEvent.ChangeValue;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.utils.misc.Time;

public class TimeVariantNetworkPerformanceTest {
	
	Link link;
	Link timeVarinatLink;
	private NetworkLayer tvNetwork;
	
	
	
	private void init() {
		NetworkLayer network = new NetworkLayer();
		NetworkFactory nf = new NetworkFactory();
		nf.setLinkPrototype(TimeVariantLinkImpl.class);
		this.tvNetwork = new NetworkLayer(nf);
		Node n1 = network.createNode(new IdImpl(0), new CoordImpl(0,0));
		Node n2 = network.createNode(new IdImpl(10), new CoordImpl(0,0));
		this.link = network.createLink(new IdImpl(0), n1, n2, 1000, 100, 10, 1);
		
		Node tvn1 = this.tvNetwork.createNode(new IdImpl(0), new CoordImpl(0,0));
		Node tvn2 = this.tvNetwork.createNode(new IdImpl(10), new CoordImpl(0,0));
		this.timeVarinatLink = this.tvNetwork.createLink(new IdImpl(0), tvn1, tvn2, 1000, 100, 10, 1);
	}

	private void runBaseTest(final int run) {
		String type = "treemap";
		init();
		long start;// = System.currentTimeMillis();
		long stop;// = System.currentTimeMillis();
//		double time = 0;
//		start = System.currentTimeMillis();
//		for (int i = 0; i < 1000000000; i++) {
//			time += this.link.getFreespeed(Time.UNDEFINED_TIME);
//		}
//		stop = System.currentTimeMillis();
//		System.out.println("LinkImpl," + (stop-start) + ",freespeed," + time);
		
		
		double tvtime = 0;
		double time = 0;
		double alltime = 0;
		for (int j = 0; j < 1; j++) {
			start = System.currentTimeMillis();
			for (int i = 0; i < 10000000; i++) {
				alltime += this.timeVarinatLink.getFreespeedTravelTime(Time.UNDEFINED_TIME);
			}
			stop = System.currentTimeMillis();
			tvtime += stop -start;
		
			start = System.currentTimeMillis();
			for (int i = 0; i < 10000000; i++) {
				alltime += this.link.getFreespeedTravelTime(Time.UNDEFINED_TIME);
			}
			stop = System.currentTimeMillis();
			time += stop - start;
		}
		System.out.println("LinkImpl," + time + ",direct," + alltime + ",NaN," + run);
		System.out.println("TimeVariantLinkImpl," + tvtime + ","+ type + "," + alltime + ",0," + run);
		
		for (int nEvents = 0; nEvents <= 100; nEvents++) {
			tvtime = 0;

			
			for (int j = 0; j < 100; j++) {
				double qTime = MatsimRandom.random.nextDouble() * 3600 * 24;
				start = System.currentTimeMillis();
				for (int i = 0; i < 100000; i++) {
					alltime += this.timeVarinatLink.getFreespeedTravelTime(qTime);
				}
				stop = System.currentTimeMillis();
				tvtime += stop -start;
			
//				start = System.currentTimeMillis();
//				for (int i = 0; i < 100000000; i++) {
//					alltime += this.link.getFreespeed(Time.UNDEFINED_TIME);
//				}
//				stop = System.currentTimeMillis();
//				time += stop - start;
			}		
			System.out.println("TimeVariantLinkImpl," + tvtime + ","+ type + "," + alltime + "," + nEvents + "," + run);
			double t = MatsimRandom.random.nextDouble() * 3600 * 24;
			NetworkChangeEvent e = new NetworkChangeEvent(t);
			e.addLink(this.timeVarinatLink);
			e.setFreespeedChange(new ChangeValue(ChangeType.ABSOLUTE,50));
			this.tvNetwork.addNetworkChangeEvent(e);
			
		}
		time = 0;
		alltime = 0;
		for (int j = 0; j < 1; j++) {
			start = System.currentTimeMillis();
			for (int i = 0; i < 10000000; i++) {
				alltime += this.link.getFreespeedTravelTime(Time.UNDEFINED_TIME);
			}
			stop = System.currentTimeMillis();
			time += stop - start;
		}
		System.out.println("LinkImpl," + time + ",direct," + alltime + ",NaN," + run);
		
		
	}

	public static void main(final String [] args) {
		
		TimeVariantNetworkPerformanceTest test = new TimeVariantNetworkPerformanceTest();
		System.out.println("linke type,time,type,num_events,run");
		for (int i = 0 ; i < 10; i++) {
			test.runBaseTest(i);
		}
	}



}
