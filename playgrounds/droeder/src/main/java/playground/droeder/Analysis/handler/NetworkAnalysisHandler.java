/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
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
package playground.droeder.Analysis.handler;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.PersonEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.utils.io.IOUtils;

import playground.droeder.gis.DaShapeWriter;

/**
 * @author droeder
 *
 */
public class NetworkAnalysisHandler implements LinkEnterEventHandler, LinkLeaveEventHandler, AgentArrivalEventHandler{
	
	private LinkStats linkStats;
	private NetStats netStat;
	private static final Logger log = Logger
		.getLogger(NetworkAnalysisHandler.class);
	
	public NetworkAnalysisHandler(Network net, int timeSliceSize, double usedScaleFactorSim){
		this.linkStats = new LinkStats(timeSliceSize, usedScaleFactorSim);
		this.netStat = new NetStats(net);
	}
	
	public void dumpCsv(String outDir){
		Map<Id, SortedMap<String, Double>> agCnt = this.linkStats.getNumAgentsByLinkAndSlice();

		// writeLinkStats
		BufferedWriter timeWriter = IOUtils.getBufferedWriter(outDir + "avLinkTravelTimes.csv");
		BufferedWriter congestionWriter = IOUtils.getBufferedWriter(outDir + "linkCongestion.csv");
//		int nrOfSlice = this.linkStats.getLastTimeSlice();
		try {
			boolean first = true;
			// write values
//			int cnt = 0;
//			int msg = 1;
			for(Entry<Id, SortedMap<String, Double>> e: this.linkStats.getAvTTimeByLinkAndSlice().entrySet()){
				// create header
				if(first){
					timeWriter.append("linkId\\slice;");
					congestionWriter.append("linkId\\slice;");
					for(String s: e.getValue().keySet()){
						timeWriter.append(s + ";");
						congestionWriter.append(s + ";");
					}
					timeWriter.newLine();
					congestionWriter.newLine();
					first = false;
				}
				timeWriter.append(e.getKey() + ";");
				congestionWriter.append(e.getKey() + ";");
				
				for(Double d: e.getValue().values()){
					timeWriter.append(String.valueOf(d) + ";");
				}
				timeWriter.newLine();
				
				for(Double d: this.linkStats.getNumAgentsByLinkAndSlice().get(e.getKey()).values()){
					congestionWriter.append(String.valueOf(d) + ";");
				}
				congestionWriter.newLine();
//				cnt++;
//				if(cnt%msg == 0){
//					System.out.println(cnt);
//					msg*=2;
//				}
			}
			timeWriter.flush();
			timeWriter.close();
			log.info(outDir + "avLinkTravelTimes.csv written...");
			congestionWriter.flush();
			congestionWriter.close();
			log.info(outDir + "linkCongestion.csv written...");
			
			BufferedWriter writer = IOUtils.getBufferedWriter(outDir + "vehMilesTrav.csv");
			writer.append(String.valueOf(this.netStat.getVhmt()));
			writer.flush();
			writer.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	public Map<Id, SortedMap<String, Double>> getNumAgentsByLinkAndSlice(){
		return this.linkStats.getNumAgentsByLinkAndSlice();
	}
	
	public Map<Id, SortedMap<String, Double>> getAvLinkTTByLinkAndSlice(){
		return this.linkStats.getAvTTimeByLinkAndSlice();
	}
	
	public void dumpShp(String outDir, Network net){
		Map<Id, SortedMap<String, Object>> linkCongestion = new HashMap<Id, SortedMap<String,Object>>();
		Map<Id, SortedMap<String, Double>> nrAg = linkStats.getNumAgentsByLinkAndSlice();
		
		for(Id id:  net.getLinks().keySet()){
			SortedMap<String, Object> temp = new TreeMap<String, Object>();
			Double abs = 0.;
			for(Integer slice: this.linkStats.getUsedSlices()){
				if(nrAg.containsKey(id)){
					if(nrAg.get(id).containsKey(String.valueOf(slice))){
						temp.put(String.valueOf(slice), nrAg.get(id).get(String.valueOf(slice)));
						abs += nrAg.get(id).get(String.valueOf(slice));
					}else{
						temp.put(String.valueOf(slice), 0.);
					}
				}else{
					temp.put(String.valueOf(slice), 0.);
				}
			}
			temp.put("absolut", abs);
			linkCongestion.put(id, temp);
		}
		
		DaShapeWriter.writeLinks2Shape(outDir + "linkCongestions.shp", net.getLinks(), linkCongestion);

	}

	/* (non-Javadoc)
	 * @see org.matsim.core.events.handler.EventHandler#reset(int)
	 */
	@Override
	public void reset(int iteration) {
		
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler#handleEvent(org.matsim.core.api.experimental.events.LinkLeaveEvent)
	 */
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		this.linkStats.processEvent(event);
		this.netStat.processEvent(event);
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler#handleEvent(org.matsim.core.api.experimental.events.LinkEnterEvent)
	 */
	@Override
	public void handleEvent(LinkEnterEvent event) {
		this.linkStats.processEvent(event);
		this.netStat.processEvent(event);
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler#handleEvent(org.matsim.core.api.experimental.events.AgentDepartureEvent)
	 */
	@Override
	public void handleEvent(AgentArrivalEvent event) {
		this.linkStats.processEvent(event);
	}
}

class NetStats{
	
	private Network net;
	private double vhmt;
	private Set<Id> observer;
	
	public NetStats(Network net){
		this.net = net;
		this.vhmt = 0.;
		this.observer = new HashSet<Id>();
	}
	
	public void processEvent(LinkEvent e){
		if(e instanceof LinkEnterEvent){
			this.observer.add(e.getPersonId());
		}else if(e instanceof LinkLeaveEvent){
			if(this.observer.contains(e.getPersonId())){
				this.vhmt += this.net.getLinks().get(e.getLinkId()).getLength();
				observer.remove(e.getPersonId());
			}
		}
	}
	
	public double getVhmt(){
		return this.vhmt;
	}
	
	
}

class LinkStats{
	Map<Id, Double> observedAgents; 
	Map<Id, SortedMap<String, Double>> absLinkTT;
	Map<Id, SortedMap<String, Double>> avLinkTT;
	Map<Id, SortedMap<String, Double>> agentsPassedLink;
	Set<Integer> slices;
	private int timeSliceSize;
	private int lastSlice;
	private double scaleFactor;
	private boolean absoluteAvTT = false;
	private boolean absoluteTT = false;
	private boolean absoluteAgentCnt = false; 
	
	public LinkStats(int timeSliceSize, double scaleFactor){
		this.observedAgents = new HashMap<Id, Double>();
		this.absLinkTT = new HashMap<Id, SortedMap<String, Double>>();
		this.avLinkTT =  new HashMap<Id, SortedMap<String,Double>>();
		this.agentsPassedLink = new HashMap<Id, SortedMap<String, Double>>();
		this.timeSliceSize = timeSliceSize;
		this.scaleFactor = scaleFactor;
		this.slices = new HashSet<Integer>();
		this.lastSlice = -1;
	}
	
	public void processEvent(PersonEvent e){
		this.absoluteFalse();
		if(e instanceof LinkEnterEvent){
			//observe if the agent passes the hole Link
			this.observedAgents.put(e.getPersonId(), e.getTime());
		}else if(e instanceof LinkLeaveEvent){
			// register if the agent passes the hole link and remove from observer
			if(this.observedAgents.containsKey(e.getPersonId())){
				this.addTime((LinkEvent) e);
			}
		}else if(e instanceof AgentArrivalEvent){
			//agent stays at this link for activity, so we can't use him for TTCalculation of the Link
			if(this.observedAgents.containsKey(e.getPersonId())){
				this.observedAgents.remove(e.getPersonId());
			}
		}
	}
	
	private void absoluteFalse(){
		this.absoluteAvTT = false;
		this.absoluteTT = false;
		this.absoluteAgentCnt = false;
	}
	
	private void addTime(LinkEvent e){
		// calc the actual timeSlice
		int timeSlice = (int)(e.getTime() / this.timeSliceSize);
		// add timeSlice 2 all maps
		if (this.lastSlice < timeSlice){
			this.addTimeSlice(timeSlice);
			this.lastSlice = timeSlice;
		}
		// check, if the link is already registered
		if(!this.absLinkTT.containsKey(e.getLinkId())){
			this.absLinkTT.put(e.getLinkId(), new TreeMap<String, Double>());
			this.agentsPassedLink.put(e.getLinkId(), new TreeMap<String, Double>());
			// add timeslice to the new link
			this.addTimeSlice(timeSlice, e.getLinkId());
		}
		//calc values
		double tt = absLinkTT.get(e.getLinkId()).get(String.valueOf(timeSlice)) + 
				((e.getTime() - this.observedAgents.get(e.getPersonId()))/ this.scaleFactor);
		double cnt = this.agentsPassedLink.get(e.getLinkId()).get(String.valueOf(timeSlice)) + (1/this.scaleFactor);
		// add values to the map
		this.absLinkTT.get(e.getLinkId()).put(String.valueOf(timeSlice), tt);
		this.agentsPassedLink.get(e.getLinkId()).put(String.valueOf(timeSlice), cnt);
		// remove the agent from the observerMap, because he passed the link
		this.observedAgents.remove(e.getPersonId());
	}
	
	private void addTimeSlice(int timeSlice){
		for(Entry<Id, SortedMap<String, Double>> e: this.absLinkTT.entrySet()){
			this.addTimeSlice(timeSlice, e.getValue());
			this.addTimeSlice(timeSlice, this.agentsPassedLink.get(e.getKey()));
		}
	}
	
	private void addTimeSlice(int timeSlice, Id id) {
		this.addTimeSlice(timeSlice, this.absLinkTT.get(id));
		this.addTimeSlice(timeSlice, this.agentsPassedLink.get(id));
	}
	
	private void addTimeSlice(int timeSlice, Map<String, Double> map){
		if(!this.slices.contains(timeSlice)) this.slices.add(timeSlice);
		String slice;
		for(int i = 0; i <(timeSlice+1);i++){
			slice = String.valueOf(i);
			// add the actual slice and all before, if there value is 0
			if(!map.containsKey(slice)){
				map.put(slice, 0.);
			}
		}
	}

	public Map<Id, SortedMap<String, Double>> getAvTTimeByLinkAndSlice(){
		if(this.absoluteAvTT){
			return this.avLinkTT;
		}else{
			SortedMap<String, Double> v;
			double time, cnt;
			for(Entry<Id, SortedMap<String, Double>> e: this.absLinkTT.entrySet()){
				v = new TreeMap<String, Double>();
				time = 0;
				cnt = 0;
				for(Entry<String, Double> ee: e.getValue().entrySet()){
					time += ee.getValue();
					cnt += this.agentsPassedLink.get(e.getKey()).get(ee.getKey());
					v.put(ee.getKey(), ee.getValue()/this.agentsPassedLink.get(e.getKey()).get(ee.getKey()));
				}
				v.put("absolute", time/cnt);
				this.avLinkTT.put(e.getKey(), v);
			}
			this.absoluteAvTT = true;
			return this.avLinkTT;
		}
	}
	
	public Map<Id, SortedMap<String, Double>> getNumAgentsByLinkAndSlice(){
		if(!this.absoluteAgentCnt){
			this.absoluteAgentCnt = true;
			this.addAbsolut(this.agentsPassedLink);
		}
		return this.agentsPassedLink;
	}
	
	public Map<Id, SortedMap<String, Double>> getAbsTTimeByLinkAndSlice(){
		if(!this.absoluteTT){
			this.addAbsolut(this.absLinkTT);
			this.absoluteTT = true;
		}
		return this.absLinkTT;
	}
	
	private void addAbsolut(Map<Id, SortedMap<String, Double>> map){
		double abs;
		for(Entry<Id, SortedMap<String, Double>> e: map.entrySet()){
			abs = 0;
			for(Double d: e.getValue().values()){
				abs += d;
			}
			e.getValue().put("absolute", abs);
		}
	}
	
	public Set<Integer> getUsedSlices(){
		return this.slices;
	}
	
//	public int getLastTimeSlice(){
//		return this.lastSlice;
//	}
}
