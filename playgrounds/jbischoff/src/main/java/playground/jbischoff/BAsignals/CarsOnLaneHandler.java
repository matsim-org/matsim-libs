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
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.events.LaneEnterEvent;
import org.matsim.core.events.handler.LaneEnterEventHandler;
import org.matsim.signalsystems.model.SignalSystem;

public class CarsOnLaneHandler implements LaneEnterEventHandler {
	private AdaptiveControllHead adaptiveControllHead;
	private Map<Id,Double> timeStamp;
	private static final Logger log = Logger.getLogger(CarsOnLaneHandler.class);

//	Scenario scenario;
	
public CarsOnLaneHandler(AdaptiveControllHead ach){
this.adaptiveControllHead=ach;
this.timeStamp = new HashMap<Id,Double>();
}
	@Override
	public void handleEvent(LaneEnterEvent event) {
		if (this.laneIsAdaptive(event.getLaneId())){
			if (!timeStamp.containsKey(event.getLaneId()))
			{
			timeStamp.put(event.getLaneId(),event.getTime());	
			}
			double timeGap = calcTimeGap(event);
			if ((timeGap!=0)&&(timeGap<20)){
				log.info(event.getTime()+": Time Gap on Lane" +event.getLaneId()+ " is "+timeGap);
			}
			timeStamp.put(event.getLaneId(),event.getTime());	
			
			
		} 
		
	}

	private double calcTimeGap(LaneEnterEvent event){
		double timeGap = event.getTime() - this.timeStamp.get(event.getLaneId()); 
		return timeGap;
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

}

