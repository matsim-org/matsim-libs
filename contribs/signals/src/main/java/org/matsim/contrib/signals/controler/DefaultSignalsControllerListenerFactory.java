/* *********************************************************************** *
 * project: org.matsim.*
 * DefaultSignalsControllerListenerFactory
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
package org.matsim.contrib.signals.controler;

import org.apache.log4j.Logger;


/**
 * Factory implementation for the MATSim default, data driven signal model
 * @author dgrether
 */
public class DefaultSignalsControllerListenerFactory implements SignalsControllerListenerFactory {
	
	private static final Logger log = Logger.getLogger(DefaultSignalsControllerListenerFactory.class);
	
	@Override
	public SignalsControllerListener createSignalsControllerListener() {
		log.info("using MATSim default signal model...");
		return new DefaultSignalsControllerListener();
	}

}
