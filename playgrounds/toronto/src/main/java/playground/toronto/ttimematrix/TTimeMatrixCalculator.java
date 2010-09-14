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
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.collections.Tuple;

public class TTimeMatrixCalculator implements AgentDepartureEventHandler, AgentArrivalEventHandler {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////
	
	private static final Logger log = Logger.getLogger(TTimeMatrixCalculator.class);
	private final Map<Id,Id> l2zMapping;
	private final Map<Id,Set<Id>> z2nMapping;
	private final Set<Integer> hours;
	private final SpanningTree st;
	private final NetworkImpl network;

	private int hour = 0;
	private Map<Id,Map<Id,Tuple<Double,Integer>>> ttimeMatrix = new HashMap<Id, Map<Id,Tuple<Double,Integer>>>();
	private final Map<String,AgentDepartureEvent> departures = new HashMap<String,AgentDepartureEvent>();
	
	private final Map<String,String> matrix = new TreeMap<String, String>();
	
	//////////////////////////////////////////////////////////////////////
	// constructor
	//////////////////////////////////////////////////////////////////////
	
	public TTimeMatrixCalculator(final Map<Id,Id> l2zMapping, final int[] hours, final SpanningTree st, final NetworkImpl network) {
		log.info("init...");
		this.l2zMapping = l2zMapping;
		this.st = st;
		this.network = network;
		this.z2nMapping = createZoneToNodeMapping();
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
	
	private final Map<Id,Set<Id>> createZoneToNodeMapping() {
		log.info("  init ZoneToNodeMapping...");
		Map<Id,Set<Id>> map = new HashMap<Id,Set<Id>>();
		for (Id linkId : l2zMapping.keySet()) {
			Id nodeId = network.getLinks().get(linkId).getToNode().getId();
			Id centroidNodeId = l2zMapping.get(linkId);
			Set<Id> nodeIds = map.get(centroidNodeId);
			if (nodeIds == null) { nodeIds = new HashSet<Id>(); map.put(centroidNodeId,nodeIds); }
			nodeIds.add(nodeId);
		}
		log.info("    z2nMapping contains "+map.size()+" zones.");
		log.info("  done.");
		return map;
	}
	
	/**
	 * Produces a Map that has "fromZone,toZone" as key, and leaves the values open
	 */
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

	/**
	 * Initializes a place where link2link ttimes can be added into zones
	 * (so I assume that this class will actually run the spanning tree algo
	 * for every link.  well, actually no, see later.)
	 */
	private final void initTTimeMatrix() {
		log.info("  init hourly ttimeMatrix...");

		// produces one entry for every zone.  Since it is a set, repeated entries are not inserted.
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
		// so if you say ttimeMatrix.get(fromZoneId), you get tmap<ToZoneId,Tuple>.
		// if you say tmap.get(toZoneId), you get tuple<Double,Integer>
		// Seems that "Double" ends up being the ttimeSum, and "Integer" ends up being the cnt
		log.info("  done.");
	}
	
	//////////////////////////////////////////////////////////////////////

	/**
	 * Adds a link travel time entry to the ttime matrix
	 * (at this point, does not depend on departure time) 
	 * 
	 * @param fzone
	 * @param tzone
	 * @param ttime
	 */
	private final void addTTime(Id fzone, Id tzone, double ttime) {
		Map<Id,Tuple<Double,Integer>> tmap = ttimeMatrix.get(fzone);
		Tuple<Double,Integer> tuple = tmap.get(tzone);
		tmap.put(tzone,new Tuple<Double, Integer>(tuple.getFirst()+ttime,tuple.getSecond()+1));
	}
	
	//////////////////////////////////////////////////////////////////////

	/**
	 * For a given "hour" (given externally): 
	 *    Goes through all fromZones
	 *    Picks representative startNode (the centroid) for that zone
	 *    Runs the spanning tree algo.  
	 *    For every toZone:
	 *        IF there is nothing in "tuple" then it takes the result from the spanning tree algo
	 *        ELSE it takes the entry from "tuple" (I assume those are results of real trip times)
	 *        It appends the ttime from the fZone to the toZone to a comma separated string
	 *        
	 * The method is triggered whenever an eventfile has finished a certain hour (meaning the probe particles seem
	 * to be averages over everybody ARRIVING during a certain hour, while the spanning tree seems to start from 
	 * a DEPARTURE time)
	 */
	private final void storeMatrix() {
		log.info("  gather hourly ttimes to the matrix...");
		System.out.println("0%       10%       20%       30%       40%       50%       60%       70%       80%       90%       100%");
		System.out.println("+---------+---------+---------+---------+---------+---------+---------+---------+---------+---------+");
		int nofZones = ttimeMatrix.size();
		int cnt = 0;
		for (Id fzone : ttimeMatrix.keySet()) {
			st.setOrigin(network.getNodes().get(fzone));
			st.setDepartureTime(hour*3600);
			st.run(network);
			Map<Id,Tuple<Double,Integer>> tmap = ttimeMatrix.get(fzone);
			for (Id tzone : tmap.keySet()) {
				Tuple<Double,Integer> tuple = tmap.get(tzone);
				String key = fzone.toString()+","+tzone.toString();
				String values = matrix.get(key);
				if (tuple.getSecond() == 0) {
					if (fzone.equals(tzone)) {
						double ttime = calcAverageIntraZonalTravelTime(st);
						values = values +  "," + Math.round(ttime);
					}
					else {
						double ttime = st.getTree().get(tzone).getTime()-hour*3600;
						values = values +  "," + Math.round(ttime);
					}
				}
				else {
					values = values + "," + Math.round(tuple.getFirst()/tuple.getSecond());
				}
				matrix.put(key,values);
			}
			cnt++;
			if ((cnt % ((int)(nofZones/100.0))) == 0) { System.out.print("."); }
		}
		System.out.print("\n");
		log.info("  done.");
	}
	
	private final double calcAverageIntraZonalTravelTime(SpanningTree stLocal) {
		Id originId = stLocal.getOrigin().getId();
		Set<Id> nodeIds = z2nMapping.get(originId);
		if (nodeIds == null) {
			throw new RuntimeException("It is expected that there is at least one node given for each centroid.");
		}
		double ttime = 0.0;
		for (Id nodeId : nodeIds) {
			ttime += stLocal.getTree().get(nodeId).getTime()-hour*3600;
		}
		ttime = ttime / nodeIds.size();
		return ttime;
	}
	
	//////////////////////////////////////////////////////////////////////
	// event handlers
	//////////////////////////////////////////////////////////////////////
	
	public void handleEvent(AgentDepartureEvent event) {
		departures.put(event.getPersonId().toString(),event);
	}

	public void handleEvent(AgentArrivalEvent event) {
		AgentDepartureEvent dEvent = departures.remove(event.getPersonId().toString());
		if (dEvent == null) throw new RuntimeException("Missing AgentDepartureEvent for AgentArrivalEvent: " + event.toString());
		else if (event.getTime() < hour*3600) {
			throw new RuntimeException("At hour "+hour+": AgentArrivalEvent too early! ("+event.toString()+")");
		}
		
		if (event.getTime() >= (hour+1)*3600) {
			if (hours.contains(hour)) {
				log.info("finishing hour " + hour + " ...");
				storeMatrix();
				initTTimeMatrix();
				log.info("done.");
			}
			hour++;
		}
		
		if (hours.contains(hour)) {
			double ttime = event.getTime() - dEvent.getTime();
			Id fzone = l2zMapping.get(new IdImpl(dEvent.getLinkId().toString()));
			Id tzone = l2zMapping.get(new IdImpl(event.getLinkId().toString()));
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
