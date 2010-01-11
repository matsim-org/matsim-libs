/* *********************************************************************** *
 * project: org.matsim.*
 * AdaptiveController
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.dgrether.daganzosignal;

import org.apache.log4j.Logger;
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
 * @author dgrether
 *
 */
public class AdaptiveController extends
		AdaptiveSignalSystemControlerImpl implements LaneEnterEventHandler, LaneLeaveEventHandler{
	
	private static final Logger log = Logger.getLogger(AdaptiveController.class);

	private final Id id1 = new IdImpl("1");
	private final Id id4 = new IdImpl("4");
	private final Id id5 = new IdImpl("5");
	private int vehOnLink5Lane1 = 0;
	
	public AdaptiveController(AdaptiveSignalSystemControlInfo controlInfo) {
		super(controlInfo);
	}

	public boolean givenSignalGroupIsGreen(double time, SignalGroupDefinition signalGroup) {
		if (signalGroup.getLinkRefId().equals(id5)){
			if (vehOnLink5Lane1 > 0) {
				return true;
			}
			return false;
		}
		if (signalGroup.getLinkRefId().equals(id4)){
			if (vehOnLink5Lane1 > 0){
				return false;
			}
			return true;
		}
		return true;
	}

	public void handleEvent(LaneEnterEvent e) {
		if (e.getLinkId().equals(id5) && e.getLaneId().equals(id1)) {
			this.vehOnLink5Lane1++;
		}
	}
	public void handleEvent(LaneLeaveEvent e) {
		if (e.getLinkId().equals(id5) && e.getLaneId().equals(id1)) {
			this.vehOnLink5Lane1--;
		}
	}

	public void reset(int iteration) {
	}

}
