/* *********************************************************************** *
 * project: org.matsim.*
 * DgRoederGershensonControllerListenerFactory
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
package playground.dgrether.signalsystems.roedergershenson;

import org.apache.log4j.Logger;
import org.matsim.signalsystems.initialization.SignalsControllerListener;
import org.matsim.signalsystems.initialization.SignalsControllerListenerFactory;


/**
 * @author dgrether
 *
 */
public class DgRoederGershensonSignalsControllerListenerFactory implements
		SignalsControllerListenerFactory {
	
	private static final Logger log = Logger
			.getLogger(DgRoederGershensonSignalsControllerListenerFactory.class);
	
	private SignalsControllerListenerFactory delegate;

	public DgRoederGershensonSignalsControllerListenerFactory(
			SignalsControllerListenerFactory delegate) {
		this.delegate = delegate;
	}

	@Override
	public SignalsControllerListener createSignalsControllerListener() {
		log.info("Using DgRoederGershenson signal model if configured for specific signals in xml data...");
		return new DgRoederGershensonControllerListener(this.delegate.createSignalsControllerListener());
	}

}
