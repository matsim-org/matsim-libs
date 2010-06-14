/* *********************************************************************** *
 * project: org.matsim.*
 * SignalizedItem
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

import org.matsim.api.core.v01.Id;
import org.matsim.signalsystems.control.SignalGroupState;


/**
 * @author dgrether
 *
 */
public interface SignalizedItem {
	
	public void setSignalized(boolean isSignalized);
	
	public void setSignalStateAllTurningMoves(SignalGroupState state);
	
	public void setSignalStateForTurningMove(SignalGroupState state, Id toLinkId);

}
