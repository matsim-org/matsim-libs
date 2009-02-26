/* *********************************************************************** *
 * project: org.matsim.*
 * LinkTTObserver.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.johannes.itsc08;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.matsim.controler.events.IterationEndsEvent;
import org.matsim.controler.events.IterationStartsEvent;
import org.matsim.controler.listener.IterationEndsListener;
import org.matsim.controler.listener.IterationStartsListener;
import org.matsim.events.LinkEnterEvent;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.events.handler.LinkEnterEventHandler;
import org.matsim.events.handler.LinkLeaveEventHandler;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.io.IOUtils;

import playground.johannes.eut.EstimReactiveLinkTT;

public class LinkTTObserver implements LinkLeaveEventHandler, LinkEnterEventHandler, IterationStartsListener, IterationEndsListener {

	private EstimReactiveLinkTT linktt;
	
	private Map<Person, LinkEnterEvent> events;
	
	private BufferedWriter realTTWriter;
	
	private BufferedWriter estimTTWriter;
	
	private Link link;
	
	public LinkTTObserver(NetworkLayer network, EstimReactiveLinkTT linktt) {
		link = network.getLink("2");
		this.linktt = linktt;
	}
	
	public void handleEvent(LinkLeaveEvent event) {
		if(event.linkId.equals("2")) {
			LinkEnterEvent enter = events.remove(event.agent);
			double realTT = event.time - enter.time;
			try {
				realTTWriter.write(String.valueOf(enter.time));
				realTTWriter.write("\t");
				realTTWriter.write(String.valueOf(realTT));
				realTTWriter.newLine();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
	}

	public void reset(int iteration) {
		events = new HashMap<Person, LinkEnterEvent>();
		
	}

	public void handleEvent(LinkEnterEvent event) {
		events.put(event.agent, event);
		
		double estimTT = linktt.getLinkTravelTime(link, event.time);
		try {
			estimTTWriter.write(String.valueOf(event.time));
			estimTTWriter.write("\t");
			estimTTWriter.write(String.valueOf(estimTT));
			estimTTWriter.newLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	public void notifyIterationStarts(IterationStartsEvent event) {
		try {
			realTTWriter = IOUtils.getBufferedWriter(Controler.getIterationFilename("realTTs.txt"));
			realTTWriter.write("time\ttraveltime");
			realTTWriter.newLine();
			
			estimTTWriter = IOUtils.getBufferedWriter(Controler.getIterationFilename("estimTTs.txt"));
			estimTTWriter.write("time\ttraveltime");
			estimTTWriter.newLine();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	public void notifyIterationEnds(IterationEndsEvent event) {
		try {
			realTTWriter.close();
			estimTTWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
