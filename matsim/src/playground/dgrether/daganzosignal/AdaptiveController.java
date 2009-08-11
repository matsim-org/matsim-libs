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
import org.matsim.signalsystems.basic.BasicSignalGroupDefinition;
import org.matsim.signalsystems.config.BasicAdaptiveSignalSystemControlInfo;
import org.matsim.signalsystems.control.AdaptiveSignalSystemControlerImpl;


/**
 * @author dgrether
 *
 */
public class AdaptiveController extends
		AdaptiveSignalSystemControlerImpl {
	
	private static final Logger log = Logger.getLogger(AdaptiveController.class);

	/**
	 * @param controlInfo
	 */
	public AdaptiveController(BasicAdaptiveSignalSystemControlInfo controlInfo) {
		super(controlInfo);
	}

	/**
	 * @see org.matsim.signalsystems.control.SignalSystemController#givenSignalGroupIsGreen(org.matsim.signalsystems.basic.BasicSignalGroupDefinition)
	 */
	public boolean givenSignalGroupIsGreen(double time, BasicSignalGroupDefinition signalGroup) {
		log.info("isGreen?");
		
		return true;
	}

}
