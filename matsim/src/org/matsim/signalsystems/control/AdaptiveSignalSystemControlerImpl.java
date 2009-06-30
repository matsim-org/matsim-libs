/* *********************************************************************** *
 * project: org.matsim.*
 * AdaptiveSignalSystemControler
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
package org.matsim.signalsystems.control;

import org.matsim.signalsystems.config.BasicAdaptiveSignalSystemControlInfo;


/**
 * Abstract shell for Adaptive Signal System Controlers. Must be extended when 
 * implementing custom adaptive controlers.
 * @author dgrether
 *
 */
public abstract class AdaptiveSignalSystemControlerImpl implements AdaptiveSignalSystemControler {

	private BasicAdaptiveSignalSystemControlInfo controlInfo;

	public AdaptiveSignalSystemControlerImpl(BasicAdaptiveSignalSystemControlInfo controlInfo) {
		this.controlInfo = controlInfo;
	}
	
	public BasicAdaptiveSignalSystemControlInfo getControlInfo(){
		return this.controlInfo;
	}
}
