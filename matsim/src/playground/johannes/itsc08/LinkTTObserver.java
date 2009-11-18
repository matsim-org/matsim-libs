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

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.events.BasicLinkEnterEvent;
import org.matsim.api.basic.v01.events.BasicLinkLeaveEvent;
import org.matsim.api.basic.v01.events.handler.BasicLinkEnterEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicLinkLeaveEventHandler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.io.IOUtils;

import playground.johannes.eut.EstimReactiveLinkTT;

public class LinkTTObserver implements BasicLinkLeaveEventHandler, BasicLinkEnterEventHandler, IterationStartsListener, IterationEndsListener {

	private EstimReactiveLinkTT linktt;
	
	private Map<Id, BasicLinkEnterEvent> events; // personId
	
	private BufferedWriter realTTWriter;
	
	private BufferedWriter estimTTWriter;
	
	private LinkImpl link;
	
	public LinkTTObserver(NetworkLayer network, EstimReactiveLinkTT linktt) {
		link = network.getLink("2");
		this.linktt = linktt;
	}
	
	public void handleEvent(BasicLinkLeaveEvent event) {
		if(event.getLinkId().toString().equals("2")) {
			BasicLinkEnterEvent enter = events.remove(event.getPersonId());
			double realTT = event.getTime() - enter.getTime();
			try {
				realTTWriter.write(String.valueOf(enter.getTime()));
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
		events = new HashMap<Id, BasicLinkEnterEvent>();
	}

	public void handleEvent(BasicLinkEnterEvent event) {
		events.put(event.getPersonId(), event);
		
		double estimTT = linktt.getLinkTravelTime(link, event.getTime());
		try {
			estimTTWriter.write(String.valueOf(event.getTime()));
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
