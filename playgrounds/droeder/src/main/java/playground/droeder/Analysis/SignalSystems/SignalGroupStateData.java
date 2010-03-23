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
package playground.droeder.Analysis.SignalSystems;

import java.util.HashMap;
import java.util.Map;

import org.matsim.core.events.SignalGroupStateChangedEvent;
import org.matsim.signalsystems.control.SignalGroupState;

/**
 * @author droeder
 *
 */
public class SignalGroupStateData {
	Map<Double, SignalGroupState> data = new HashMap<Double, SignalGroupState>();
	SignalGroupState oldState = null;

	public void processStateChange(SignalGroupStateChangedEvent e) {
		if (oldState == null){
			oldState = e.getNewState();
		}
		if(!(oldState ==  e.getNewState())){
			data.put(e.getTime(), oldState);
			oldState = e.getNewState();
		}
	}

		  
	public Map<Double, SignalGroupState> getStateTimeMap() {
	    return data;
	}

}
