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
package playground.droeder.eMobility.v2.handler;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.GenericEvent;
import org.matsim.core.api.experimental.events.handler.GenericEventHandler;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.droeder.eMobility.v2.fleet.EmobFleet;

/**
 * @author droeder
 *
 */
public class EmobAnalysis implements GenericEventHandler{
	
	private HashMap<String, StateStore> veh2State;
	private Network net;

	public EmobAnalysis(Network net){
		this.veh2State = new HashMap<String, StateStore> ();
		this.net = net;
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	public void handleEvent(GenericEvent e) {
		if(e.getAttributes().get("type").equals(EmobFleet.SOCCHANGEEVENT)){
			String veh = e.getAttributes().get("veh");
			if(!this.veh2State.containsKey(veh)){
				this.veh2State.put(veh, new StateStore());
//				this.veh2distState.put(veh, new StateStore());
			}
			this.veh2State.get(veh).addValue(e.getTime(), Double.parseDouble(e.getAttributes().get("SoC")), net.getLinks().get(e.getAttributes().get("link")));
//			this.veh2distState.get(veh).addValue(x, y)
		}
		
	}
	
	//TODO create chart here
	public void dump2csv(String outFile){
		BufferedWriter w = IOUtils.getBufferedWriter(outFile);
		
			try {
				for(Entry<String, StateStore> e: this.veh2State.entrySet()){
					w.write(e.getKey() + ";");
					w.newLine();
					w.write("xTime;");
					for(Double x : e.getValue().getXtimeValues()){
						w.write(x + ";");
					}
					w.newLine();
					w.write("xDist;");
					for(Double x : e.getValue().getXdistanceValues()){
						w.write(x + ";");
					}
					w.newLine();
					w.write("y;");
					for(Double y: e.getValue().getYvalues()){
						w.write(y +";");
					}
					w.newLine();
					w.newLine();
				}
				w.flush();
				w.close();
				
			} catch (IOException e1) {
				e1.printStackTrace();
			}
	}

	private class StateStore{
		private ArrayList<Double> xTime;
		private ArrayList<Double> xDist;
		private ArrayList<Double> y;
		private Coord lastCoord = null;
		
		public StateStore(){
			this.xTime = new ArrayList<Double>();
			this.xDist = new ArrayList<Double>();
			this.y = new ArrayList<Double>();
		}
		
		public void addValue(Double time, Double y, Link l){
			this.xTime.add(time);
			this.y.add(y);
			
			//happens if an Agent starts. Here we use the toNode, because Activities are located at the end of a link 
			if(this.lastCoord == null){
				this.lastCoord = l.getToNode().getCoord();
			}
			Double dist = CoordUtils.calcDistance(this.lastCoord, l.getToNode().getCoord());
			if(xDist.size() > 0){
				dist+=xDist.get(xDist.size()-1);
			}
			xDist.add(dist);

			//prepare for the next socChangeEvent
			this.lastCoord = l.getToNode().getCoord();
		}
		
		public Double[] getXtimeValues(){
			return (Double[]) this.xTime.toArray();
		}
		
		public Double[] getXdistanceValues(){
			return (Double[]) this.xDist.toArray();
		}
		
		public Double[] getYvalues(){
			return (Double[]) this.y.toArray();
		}
		
		
	}
	
}
