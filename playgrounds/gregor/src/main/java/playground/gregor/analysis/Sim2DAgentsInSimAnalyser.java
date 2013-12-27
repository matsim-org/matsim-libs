/* *********************************************************************** *
 * project: org.matsim.*
 * Sim2DAgentsInSimAnalyser.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.events.handler.GenericEventHandler;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderXMLv1;

import playground.gregor.sim2d_v4.events.Sim2DAgentConstructEvent;
import playground.gregor.sim2d_v4.events.Sim2DAgentConstructEventHandler;
import playground.gregor.sim2d_v4.events.Sim2DAgentDestructEvent;
import playground.gregor.sim2d_v4.events.Sim2DAgentDestructEventHandler;

public class Sim2DAgentsInSimAnalyser implements Sim2DAgentConstructEventHandler, Sim2DAgentDestructEventHandler, GenericEventHandler{

	private int agentsInSim = 0;
	private double lastUpdate = 0;
	private final BufferedWriter bf;
	
	private final int reps = 0;
	
	public Sim2DAgentsInSimAnalyser(BufferedWriter bf) {
		this.bf = bf;
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void handleEvent(GenericEvent event) {
		if (event.getEventType().equals("Sim2DAgentDestructEvent")) {
			Sim2DAgentDestructEvent ev = new Sim2DAgentDestructEvent(event.getTime(), null);
			handleEvent(ev);
		} else if (event.getEventType().equals("Sim2DAgentConstructEvent")) {
			Sim2DAgentConstructEvent ev = new Sim2DAgentConstructEvent(event.getTime(), null);
			handleEvent(ev);
		}
		 
	}

	@Override
	public void handleEvent(Sim2DAgentDestructEvent event) {
		this.agentsInSim--;
		if (this.lastUpdate + 60 <= event.getTime()) {
			this.lastUpdate = event.getTime();
			report();
		}
		
	}

	private void report() {
		try {
			this.bf.append(this.lastUpdate + " " + this.agentsInSim + "\n");
			System.out.println(this.lastUpdate + " " + this.agentsInSim + "\n");
		
//			if (this.reps++ == 205) {
//				this.bf.flush();
//			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	@Override
	public void handleEvent(Sim2DAgentConstructEvent event) {
		this.agentsInSim++;
		if (this.lastUpdate + 60 <= event.getTime()) {
			this.lastUpdate = event.getTime();
			report();
		}
	}

	public static void main(String [] args) {
		EventsManagerImpl ev = new EventsManagerImpl();
		BufferedWriter bf = null;
		try {
			bf = new BufferedWriter(new FileWriter(new File("/Users/laemmel/devel/gct_TRB/small_single/agents_in_sim2.txt")));
		} catch (IOException e) {
			e.printStackTrace();
		}
		Sim2DAgentsInSimAnalyser rep = new Sim2DAgentsInSimAnalyser(bf);
		ev.addHandler(rep);
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(ev);
		reader.parse("/Users/laemmel/devel/gct/output/ITERS/it.0/0.events.xml.gz");
		
		try {
			bf.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


}
