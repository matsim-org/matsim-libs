/* *********************************************************************** *
 * project: org.matsim.*
 * CarsOnLaneHandler.java
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

package playground.jbischoff.BAsignals;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.events.LaneEnterEvent;
import org.matsim.core.events.handler.LaneEnterEventHandler;
import org.matsim.signalsystems.model.SignalSystem;

public class CarsOnLaneHandler implements LaneEnterEventHandler {
	private AdaptiveControllHead adaptiveControllHead;
	private Map<Id,Double> timeStamp;
	private static final Logger log = Logger.getLogger(CarsOnLaneHandler.class);
	private Map<Double,List<Id>> gapsAtSecond;	
	private Map<Id,SignalSystem> signalSystemMap; 
	private Set<Id> carsPassed;

//	Scenario scenario;
	
public CarsOnLaneHandler(){
this.timeStamp = new HashMap<Id,Double>();
this.gapsAtSecond = new HashMap<Double,List<Id>>();
this.signalSystemMap = new HashMap<Id,SignalSystem>();
this.carsPassed = new HashSet<Id>();
}
public void setAdaptiveControllHead(AdaptiveControllHead ach){
	this.adaptiveControllHead=ach;
}
public  void addSystem(SignalSystem system){
	this.signalSystemMap.put(system.getId(),system);
}
	@Override
	public void handleEvent(LaneEnterEvent event) {
		if (this.laneIsAdaptive(event.getLaneId())&(!event.getLaneId().toString().endsWith(".ol"))){
			//log.info(event.getTime()+" time in l");
			this.carsPassed.add(event.getPersonId());
			if (!timeStamp.containsKey(event.getLaneId()))
			{
			timeStamp.put(event.getLaneId(),event.getTime());	
			}
			double timeGap = calcTimeGap(event);
			if ((timeGap!=0)&&(timeGap<JBBaParams.DETECTORACCURACY)){
				//log.info(event.getTime()+": Time Gap on Lane " +event.getLaneId()+ " , sg: "+this.adaptiveControllHead.getSignalGroupforLaneId(event.getLaneId()) +" is "+timeGap);
				Id sysid = this.adaptiveControllHead.getSignalSystemforLaneId(event.getLaneId());
				JbSignalController jbs =  (JbSignalController) this.signalSystemMap.get(sysid).getSignalController();
				jbs.addGapAtSecond(event.getTime()+1.0, this.adaptiveControllHead.getSignalGroupforLaneId(event.getLaneId()));
			}
			timeStamp.put(event.getLaneId(),event.getTime());	
			
		} 
		
	}

	private double calcTimeGap(LaneEnterEvent event){
		double timeGap = event.getTime() - this.timeStamp.get(event.getLaneId()); 
		return timeGap;
}
	
public List<Id> getGapListatSecond(double second){
	if (!this.gapsAtSecond.containsKey(second)){
	this.gapsAtSecond.put(second,new LinkedList<Id>());
	}
	return this.gapsAtSecond.get(second);
	
} 
	
private boolean laneIsAdaptive(Id laneid)
	{
	try {
	Id ssid = this.adaptiveControllHead.getSignalSystemforLaneId(laneid);
	if (ssid!=null) {
	return true;
	}
	else return false;
	} catch (NullPointerException e){
		return false;
	}
	}
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}
	public AdaptiveControllHead getAdaptiveControllHead() {
		return adaptiveControllHead;
	}

public long getPassedAgents(){
	return this.carsPassed.size();
}


}

