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
package playground.droeder.gershensonSignals;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.LaneEnterEvent;
import org.matsim.core.events.LaneLeaveEvent;
import org.matsim.core.events.handler.LaneEnterEventHandler;
import org.matsim.core.events.handler.LaneLeaveEventHandler;
import org.matsim.signalsystems.config.AdaptiveSignalSystemControlInfo;
import org.matsim.signalsystems.control.AdaptiveSignalSystemControlerImpl;
import org.matsim.signalsystems.systems.SignalGroupDefinition;

/**
 * @author droeder
 *
 */
public class GershensonAdaptiveTrafficLightController extends
			AdaptiveSignalSystemControlerImpl implements LaneEnterEventHandler, LaneLeaveEventHandler{
	
	private final Id id1 = new IdImpl("1");
	private final Id id2 = new IdImpl("2");
	private final Id id11 = new IdImpl("11");
	private final Id id13 = new IdImpl("13");
	private double vehOnLink11 = 0;	
	private double counterLink11 = vehOnLink11;
	private double vehOnLink13 = 0;
	private double counterLink13 = vehOnLink13;
	private double  greenTime = 0;
	private double allGreenTime = 0;
	private static int threshold = 50;
	

 
	public GershensonAdaptiveTrafficLightController(AdaptiveSignalSystemControlInfo controlInfo) {
		super(controlInfo);
	}
	
	public void reset(int iteration) {
		iteration = 0;
		if (counterLink11 > threshold){
			counterLink11 = 0;
		}
		if (counterLink13 > threshold){
			counterLink13 = 0;
		}
	}
	
	
	public void handleEvent(LaneEnterEvent e) {
		
		if (e.getLinkId().equals(id11) && e.getLaneId().equals(id2)) {
			this.vehOnLink11++;
		}	
		else if (e.getLinkId().equals(id13) && e.getLaneId().equals(id2)) {
			this.vehOnLink13++;
		}	
	}

	
	public void handleEvent(LaneLeaveEvent e) {
		if (e.getLinkId().equals(id11) && e.getLaneId().equals(id2)) {
			this.vehOnLink11--;
		}
		if (e.getLinkId().equals(id13) && e.getLaneId().equals(id2)) {
			this.vehOnLink13--;
		}		
	}


	public boolean givenSignalGroupIsGreen(double time, SignalGroupDefinition signalGroup) {
		greenTime = time - allGreenTime;
		 		
		counterLink11 = vehOnLink11 * greenTime;
		counterLink13 = vehOnLink13 * greenTime;
		
		if (signalGroup.getLinkRefId().equals(id11)){
			
			if (vehOnLink11 > 0 && counterLink13 < threshold) {
				return true;
			}
			else {
				allGreenTime = allGreenTime + greenTime;
				return false;
			}
			
		}
		if (signalGroup.getLinkRefId().equals(id13)){
			if (vehOnLink13 > 0 && counterLink11 < threshold){
				return true;
			}
			else {
				allGreenTime = allGreenTime + greenTime;
				return false;
			}
			
		}
		return true;
	}

}
