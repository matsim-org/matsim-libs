/* *********************************************************************** *
 * project: org.matsim.*
 * SignalGroupDef
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
package playground.dgrether.signalsNew.model;

import org.matsim.signalsystems.control.SignalGroupState;
import org.matsim.signalsystems.control.SignalSystemController;

import playground.dgrether.signalsNew.data.v20.SignalGroupData;


/**
 * @author dgrether
 *
 */
public interface SignalGroup {

	public SignalGroupData getSignalGroupData();
	
	public void setSignalSystemController(
	    SignalSystemController signalSystemController);

	public SignalSystemController getSignalController();
	
	
	public void addSignalizedItem(SignalizedItem item);
	
	public void updateState(SignalGroupState state);
	
	public SignalGroupState getState();
	
	
}
