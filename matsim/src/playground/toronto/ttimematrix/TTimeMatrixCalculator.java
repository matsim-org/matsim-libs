/* *********************************************************************** *
 * project: org.matsim.*
 * TTimeMatrixCalculator.java
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

package playground.toronto.ttimematrix;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.handler.AgentArrivalEventHandler;
import org.matsim.events.handler.AgentDepartureEventHandler;
import org.matsim.utils.collections.Tuple;

public class TTimeMatrixCalculator implements AgentDepartureEventHandler, AgentArrivalEventHandler {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////
	
	private static final Logger log = Logger.getLogger(TTimeMatrixCalculator.class);
	private final Map<Id,Id> l2zMapping;
	private final Set<Integer> hours;

	private int hour = 0;
	private Map<Id,Map<Id,Tuple<Double,Integer>>> ttimeMatrix = new HashMap<Id, Map<Id,Tuple<Double,Integer>>>();
	private final Map<String,AgentDepartureEvent> departures = new HashMap<String,AgentDepartureEvent>();
	
	private final Map<String,String> matrix = new TreeMap<String, String>();
	
	//////////////////////////////////////////////////////////////////////
	// constructor
	//////////////////////////////////////////////////////////////////////
	
	public TTimeMatrixCalculator(final Map<Id,Id> l2zMapping, final int[] hours) {
		log.info("init...");
		this.l2zMapping = l2zMapping;
		Set<Integer> hrs = new TreeSet<Integer>();
		for (int i=0; i<hours.length; i++) { hrs.add(hours[i]); }
		this.hours = hrs;
		initMatrix();
		initTTimeMatrix();
		log.info("done.");
	}
	
	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////
	
	private final void initMatrix() {
		log.info("  init matrix...");
		Set<Id> zids = new HashSet<Id>();
		for (Id zid : l2zMapping.values()) { zids.add(zid); }
		
		matrix.clear();
		for (Id fzone : zids) {
			for (Id tzone : zids) {
				String key = fzone.toString()+","+tzone.toString();
				matrix.put(key,"");
			}
		}
		log.info("  done.");
	}
	
	//////////////////////////////////////////////////////////////////////

	private final void initTTimeMatrix() {
		log.info("  init hourly ttimeMatrix...");
		Set<Id> zids = new HashSet<Id>();
		for (Id zid : l2zMapping.values()) { zids.add(zid); }
		
		ttimeMatrix.clear();
		for (Id fzone : zids) {
			Map<Id,Tuple<Double,Integer>> tmap = new HashMap<Id, Tuple<Double,Integer>>();
			ttimeMatrix.put(fzone,tmap);
			for (Id tzone : zids) {
				Tuple<Double,Integer> tuple = new Tuple<Double, Integer>(0.0,0);
				tmap.put(tzone,tuple);
			}
		}
		log.info("  done.");
	}
	
	//////////////////////////////////////////////////////////////////////

	private final void addTTime(Id fzone, Id tzone, double ttime) {
		Map<Id,Tuple<Double,Integer>> tmap = ttimeMatrix.get(fzone);
		Tuple<Double,Integer> tuple = tmap.get(tzone);
		tmap.put(tzone,new Tuple<Double, Integer>(tuple.getFirst()+ttime,tuple.getSecond()+1));
	}
	
	//////////////////////////////////////////////////////////////////////

	private final void storeMatrix() {
		log.info("  store hourly ttimes to the matrix...");
		for (Id fzone : ttimeMatrix.keySet()) {
			Map<Id,Tuple<Double,Integer>> tmap = ttimeMatrix.get(fzone);
			for (Id tzone : tmap.keySet()) {
				Tuple<Double,Integer> tuple = tmap.get(tzone);
				String key = fzone.toString()+","+tzone.toString();
				String values = matrix.get(key);
				if (tuple.getSecond() == 0) {
					// TODO blamermi: instead of -1 here should be the freespeed
					// travel time from fzone to tzone
					values = values + ",-1";
				}
				else {
					values = values + "," + Math.round(tuple.getFirst()/tuple.getSecond());
				}
				matrix.put(key,values);
			}
		}
		log.info("  done.");
	}
	
	//////////////////////////////////////////////////////////////////////
	// event handlers
	//////////////////////////////////////////////////////////////////////
	
	public void handleEvent(AgentDepartureEvent event) {
		departures.put(event.agentId,event);
	}

	public void handleEvent(AgentArrivalEvent event) {
		AgentDepartureEvent dEvent = departures.remove(event.agentId);
		if (dEvent == null) throw new RuntimeException("Missing AgentDepartureEvent for AgentArrivalEvent: " + event.toString());
		else if (event.time < hour*3600) {
			throw new RuntimeException("At hour "+hour+": AgentArrivalEvent too early! ("+event.toString()+")");
		}
		
		if (event.time >= (hour+1)*3600) {
			if (hours.contains(hour)) {
				log.info("finishing hour " + hour + " ...");
				storeMatrix();
				initTTimeMatrix();
				log.info("done.");
			}
			hour++;
		}
		
		if (hours.contains(hour)) {
			double ttime = event.time - dEvent.time;
			Id fzone = l2zMapping.get(new IdImpl(dEvent.linkId));
			Id tzone = l2zMapping.get(new IdImpl(event.linkId));
			addTTime(fzone,tzone,ttime);
		}
	}

	//////////////////////////////////////////////////////////////////////
	// reset
	//////////////////////////////////////////////////////////////////////
	
	public void reset(int iteration) {
		hour = 0;
		departures.clear();
		initMatrix();
		initTTimeMatrix();
	}

	//////////////////////////////////////////////////////////////////////
	// write
	//////////////////////////////////////////////////////////////////////
	
	public final void writeMatrix(final String outfile) {
		try {
			FileWriter fw = new FileWriter(outfile);
			BufferedWriter out = new BufferedWriter(fw);
			// header
			out.write("fzone,tzone");
			for (Integer h : hours) { out.write(",hour"+h); }
			out.write("\n");

			for (String key : matrix.keySet()) {
				String values = matrix.get(key);
				out.write(key+values+"\n");
			}
			out.close();
			fw.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}
