/* *********************************************************************** *
 * project: org.matsim.*
 * MyControlerListener.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

/**
 * 
 */

package playground.ikaddoura.injectExample;

import org.apache.log4j.Logger;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

import com.google.inject.Inject;


/**
 * @author ikaddoura
 *
 */

public class IKControlerListener implements IterationEndsListener {
	
	private static final Logger log = Logger.getLogger(IKControlerListener.class);

	@Inject
	private IKEventHandler handler; // needs to be bound to the simulation
	
	@Inject
	private IKWriter writer; // needs to be bound to the simulation
	
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		log.info("ITERATION ENDS... Number of link enter events: " + handler.getCounter());
		writer.write(handler.getCounter());
	}
	
}
