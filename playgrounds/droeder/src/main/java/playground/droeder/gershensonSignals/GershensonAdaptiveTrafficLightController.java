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


	public GershensonAdaptiveTrafficLightController(
			AdaptiveSignalSystemControlInfo controlInfo) {
		super(controlInfo);
		// TODO Auto-generated constructor stub
	}
	
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void handleEvent(LaneEnterEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(LaneLeaveEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean givenSignalGroupIsGreen(double time,
			SignalGroupDefinition signalGroup) {
		// TODO Auto-generated method stub
		return false;
	}

}
