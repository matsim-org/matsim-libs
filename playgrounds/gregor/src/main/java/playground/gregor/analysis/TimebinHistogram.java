/* *********************************************************************** *
 * project: org.matsim.*
 * TimebinHistogram.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.gregor.analysis;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.events.EventsReaderTXTv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.utils.misc.IntegerCache;

import playground.gregor.MY_STATIC_STUFF;

public class TimebinHistogram implements LinkEnterEventHandler {

	
	HashMap<String,LinkInfo> linkInfos = new HashMap<String, LinkInfo>();
	
	private int binSize = 5;

	
	public void reset(int iteration) {
		// TODO Auto-generated method stub
	}

	public void handleEvent(LinkEnterEvent event) {
		LinkInfo li = this.linkInfos.get(event.getLinkId().toString());
		if (li == null) {
			li = new LinkInfo();
			this.linkInfos.put(event.getLinkId().toString(), li);
		}
		
		
		Integer slot = getTimeBin(event.getTime());
		if (!li.activated) {
			li.activated = true;
			li.firstSlot = slot;
		}
		li.lastSlot = slot;
		Integer i = li.counts.get(slot);
		if (i == null) {
			li.counts.put(slot, 1);
		} else {
			li.counts.put(slot, i+1);
		}
	}

	
	private Integer getTimeBin(double time) {
		int slice = ((int) time)/this.binSize ;
		return IntegerCache.getInteger(slice);
	}
	
	public void writeToFile(String file) throws IOException {
		int maxSlot = getTimeBin(3*3600 + 30 * 60);
		FileWriter out = new FileWriter(file);
		BufferedWriter bw = new BufferedWriter(out);
		for (LinkInfo li : this.linkInfos.values()) {
			for (int i = li.firstSlot; i <= Math.min(li.lastSlot,maxSlot); i++) {
				Integer c = li.counts.get(i);
				if (c == null) {
					bw.append("0\n");
					
				} else {
					bw.append(c + "\n");
				}
			}
			
		}		
		bw.close();
		
		
	}
	
	
	@Override
	public String toString() {
		StringBuffer buff = new StringBuffer();
		for (LinkInfo li : this.linkInfos.values()) {
			for (Integer i : li.counts.values()) {
				buff.append(i);
				buff.append("\n");
			}
			
		}
		
		return buff.toString();
	}
	
	private static class LinkInfo {
		HashMap<Integer,Integer> counts = new HashMap<Integer, Integer>();
		boolean activated = false;
		int firstSlot;
		int lastSlot;
		
	}
	
	public static void main(String [] args) {
		String eventsFile = MY_STATIC_STUFF.OUTPUTS + "run337/800.events.txt.gz";
		System.out.println("loading:" + eventsFile);
		EventsManager events = (EventsManager) EventsUtils.createEventsManager();
		TimebinHistogram hist = new TimebinHistogram();
		events.addHandler(hist);
		new EventsReaderTXTv1(events).readFile(eventsFile);
		try {
			hist.writeToFile(MY_STATIC_STUFF.OUTPUTS + "run337/800.tbinhist.txt");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


}
