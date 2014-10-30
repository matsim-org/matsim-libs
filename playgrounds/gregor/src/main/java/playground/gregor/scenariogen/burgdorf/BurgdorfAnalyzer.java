/* *********************************************************************** *
 * project: org.matsim.*
 * BurgdorfAnalyzer.java
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

package playground.gregor.scenariogen.burgdorf;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;

public class BurgdorfAnalyzer implements PersonDepartureEventHandler, LinkLeaveEventHandler {
	
	private final TreeMap<Double,Integer> bahnhof = new TreeMap<Double, Integer>();
	private final TreeMap<Double,Integer> festplatz = new TreeMap<Double, Integer>();
	private final TreeMap<Double,Integer> onRoad = new TreeMap<Double, Integer>();

	private final Id<Link> bex = Id.create("198", Link.class);
	private final Id<Link> fen = Id.create("200", Link.class);
	
	private int dep = 0;
	private int bLeft = 0;
	private int fEnter = 0;

	private double time = -1;
	
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (this.time < event.getTime() ) {
			update(event.getTime());
		}		
	
		if (event.getLinkId().equals(this.bex)) {
			this.bLeft++;
		} else if (event.getLinkId().equals(this.fen)) {
			this.fEnter++;
		}

	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (this.time < event.getTime() ) {
			update(event.getTime());
		}	
		this.dep++;
		
	}
	
	
	private void update(double d) {
		this.bahnhof.put(this.time, this.dep-this.bLeft);
		this.festplatz.put(this.time, this.fEnter);
		this.onRoad.put(this.time, this.bLeft - this.fEnter);
		this.time = d;
	}

	public static void main(String [] args) {
		String file = "/Users/laemmel/devel/burgdorf/matsim-scenario/output/ITERS/it.0/0.events.xml.gz";
		BurgdorfAnalyzer a = new BurgdorfAnalyzer();
		EventsManager manager = EventsUtils.createEventsManager();
		manager.addHandler(a);
		
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(manager);
		reader.parse(file);
		
		TreeMap<Double, Integer> b = a.bahnhof;
		TreeMap<Double, Integer> f = a.festplatz;
		TreeMap<Double, Integer> r = a.onRoad;
		
		for (double d = 5*3600 + 60*30; d < 9*3600; d++) {
			
			Integer bb = b.get(d);
			Integer ff = f.get(d);
			Integer rr = r.get(d);
			if (bb == null) {
				bb = b.floorEntry(d).getValue();
				ff = f.floorEntry(d).getValue();
				rr = r.floorEntry(d).getValue();
			}
			
			System.out.println(d + "  b:" + bb + " ff:" + ff + " rr:" + rr);
			String outFile = "/Users/laemmel/svn/shared-svn/documents/sli/2011/gregor/000movieMaker/stats/" + ((int)d);
			try {
				BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
				writer.append("BB="+bb+"\n");
				writer.append("FF="+ff+"\n");
				writer.append("RR="+rr+"\n");
				writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}

}
