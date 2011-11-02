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
package playground.droeder.Analysis;

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
	private boolean linkStat;
	private static final Logger log = Logger
		.getLogger(NetworkAnalysisHandler.class);
	
	public NetworkAnalysisHandler(boolean linkStats, int timeSliceSize, double usedScaleFactorSim){
		this.linkStat = linkStats;
		if(this.linkStat){
			this.linkStats = new LinkStats(timeSliceSize, usedScaleFactorSim);
		}
	}
	
	public void dumpCsv(String outDir){
		Map<Id, Map<Integer, Double>> agCnt = this.linkStats.getNumAgentsByLinkAndSlice();

		// writeLinkStats
		BufferedWriter timeWriter = IOUtils.getBufferedWriter(outDir + "avLinkTravelTimes.csv");
		BufferedWriter congestionWriter = IOUtils.getBufferedWriter(outDir + "linkCongestion.csv");
		int nrOfSlice = this.linkStats.getLastTimeSlice();
		try {
			//write header
			timeWriter.append("linkId\\slice;");
			congestionWriter.append("linkId\\slice;");
			for(int i = 0; i < nrOfSlice ; i++){
				timeWriter.append(i + ";");
				congestionWriter.append(i + ";");
			}
			timeWriter.append("total;");
			timeWriter.newLine();
			congestionWriter.append("total;");
			congestionWriter.newLine();
			// write values
			for(Entry<Id, Map<Integer, Double>> e: this.linkStats.getAvTTimeByLinkAndSlice().entrySet()){
				timeWriter.append(e.getKey() + ";");
				congestionWriter.append(e.getKey() + ";");
				double tt = 0, cnt = 0;
				for(int i = 0; i <nrOfSlice; i++){
					if(e.getValue().containsKey(i)){
						timeWriter.append((e.getValue().get(i)/agCnt.get(e.getKey()).get(i)) + ";");
						tt+= (e.getValue().get(i)/agCnt.get(e.getKey()).get(i));
						congestionWriter.append(agCnt.get(e.getKey()).get(i)+ ";");
						cnt+= agCnt.get(e.getKey()).get(i);
					}else{
						timeWriter.append(";");
						congestionWriter.append(";");
					}
				}
				timeWriter.append(tt +";");
				timeWriter.newLine();
				congestionWriter.append(cnt + ";");
				congestionWriter.newLine();
			}
			timeWriter.flush();
			timeWriter.close();
			log.info(outDir + "avLinkTravelTimes.csv written...");
			congestionWriter.flush();
			congestionWriter.close();
			log.info(outDir + "linkCongestion.csv written...");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	public Map<Id, Map<Integer, Double>> getNumAgentsByLinkAndSlice(){
		return this.linkStats.getNumAgentsByLinkAndSlice();
	}
	
	public void dumpShp(String outDir, Network net){
		Map<Id, SortedMap<String, Object>> linkCongestion = new HashMap<Id, SortedMap<String,Object>>();
		Map<Id, Map<Integer, Double>> nrAg = linkStats.getNumAgentsByLinkAndSlice();
		
		for(Id id:  net.getLinks().keySet()){
			SortedMap<String, Object> temp = new TreeMap<String, Object>();
			Double abs = 0.;
			for(int i = 0; i < linkStats.getLastTimeSlice(); i++){
				if(nrAg.containsKey(id)){
					if(nrAg.get(id).containsKey(i)){
						temp.put(String.valueOf(i), nrAg.get(id).get(i));
						abs += nrAg.get(id).get(i);
					}else{
						temp.put(String.valueOf(i), 0.);
					}
				}else{
					temp.put(String.valueOf(i), 0.);
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
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler#handleEvent(org.matsim.core.api.experimental.events.LinkLeaveEvent)
	 */
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if(linkStat){
			this.linkStats.processEvent(event);
		}		
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler#handleEvent(org.matsim.core.api.experimental.events.LinkEnterEvent)
	 */
	@Override
	public void handleEvent(LinkEnterEvent event) {
		if(linkStat){
			this.linkStats.processEvent(event);
		}
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler#handleEvent(org.matsim.core.api.experimental.events.AgentDepartureEvent)
	 */
	@Override
	public void handleEvent(AgentArrivalEvent event) {
		if(linkStat){
			this.linkStats.processEvent(event);
		}
	}

}

class LinkStats{
	Map<Id, Double> observedAgents; 
	Map<Id, Map<Integer, Double>> absLinkTT;
	HashMap<Id, Map<Integer, Double>> agentsPassedLink;
	private int timeSliceSize;
	private int lastSlice;
	private double scaleFactor;
	
	public LinkStats(int timeSliceSize, double scaleFactor){
		this.observedAgents = new HashMap<Id, Double>();
		this.absLinkTT = new HashMap<Id, Map<Integer, Double>>();
		this.agentsPassedLink = new HashMap<Id, Map<Integer, Double>>();
		this.timeSliceSize = timeSliceSize;
		this.scaleFactor = scaleFactor;
	}
	
	public void processEvent(PersonEvent e){
		if(e instanceof LinkEnterEvent){
			//observe if the agent passes the hole Link
			this.observedAgents.put(e.getPersonId(), e.getTime());
		}else if(e instanceof LinkLeaveEvent){
			// register if the agent passes the hole link and remove from observer
			if(this.observedAgents.containsKey(e.getPersonId())){
				this.addTime((LinkEvent) e);
			}
		}else if(e instanceof AgentArrivalEvent){
			//agent stays at this link for activity
			if(this.observedAgents.containsKey(e.getPersonId())){
				this.observedAgents.remove(e.getPersonId());
			}
		}
	}
	
	private void addTime(LinkEvent e){
		int timeSlice = (int)(e.getTime() / this.timeSliceSize);
		this.lastSlice = timeSlice;
		if(!this.absLinkTT.containsKey(e.getLinkId())){
			this.absLinkTT.put(e.getLinkId(), new HashMap<Integer, Double>());
			this.agentsPassedLink.put(e.getLinkId(), new HashMap<Integer, Double>());
		}
		if(!this.absLinkTT.get(e.getLinkId()).containsKey(timeSlice)){
			this.absLinkTT.get(e.getLinkId()).put(timeSlice, 0.);
			this.agentsPassedLink.get(e.getLinkId()).put(timeSlice, 0.);
		}
		double tt = absLinkTT.get(e.getLinkId()).get(timeSlice) + ((e.getTime() - this.observedAgents.get(e.getPersonId()))/ this.scaleFactor);
		double cnt = this.agentsPassedLink.get(e.getLinkId()).get(timeSlice) + (1/this.scaleFactor);
		this.absLinkTT.get(e.getLinkId()).put(timeSlice, tt);
		this.agentsPassedLink.get(e.getLinkId()).put(timeSlice, cnt);
		this.observedAgents.remove(e.getPersonId());
	}
	
	public Map<Id, Map<Integer, Double>> getAbsTTimeByLinkAndSlice(){
		return this.absLinkTT;
	}
	
	public Map<Id, Map<Integer, Double>> getAvTTimeByLinkAndSlice(){
		Map<Id, Map<Integer, Double>> temp = new HashMap<Id, Map<Integer, Double>>();
		Map<Integer, Double> v;
		for(Entry<Id, Map<Integer, Double>> e: this.absLinkTT.entrySet()){
			v = new HashMap<Integer, Double>();
			for(Entry<Integer, Double> ee: e.getValue().entrySet()){
				v.put(ee.getKey(), ee.getValue()/this.agentsPassedLink.get(e.getKey()).get(ee.getKey()));
			}
			temp.put(e.getKey(), v);
		}
		return temp;
	}
	
	
	public HashMap<Id, Map<Integer, Double>> getNumAgentsByLinkAndSlice(){
		return this.agentsPassedLink;
	}
	
	public int getLastTimeSlice(){
		return this.lastSlice;
	}
}
