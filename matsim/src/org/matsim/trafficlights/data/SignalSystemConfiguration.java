/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriter.java
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
package org.matsim.trafficlights.data;

import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.utils.identifiers.IdI;


/**
 * @author dgrether
 *
 */
public class SignalSystemConfiguration {

	private static final Logger log = Logger.getLogger(SignalSystemConfiguration.class);

	private IdI id;

	private SignalSystemControlInfo signalSystemControler;

	public SignalSystemConfiguration(IdI id) {
		this.id = id;
	}

	public IdI getId() {
		return this.id;
	}

	public SignalSystemControlInfo getSignalSystemControler() {
		return this.signalSystemControler;
	}

	public void setSignalSystemControler(SignalSystemControlInfo signalControl) {
		this.signalSystemControler = signalControl;
	}

	public Set<SignalGroupDefinition> getSignalGroupDefinitions() {
		return this.signalSystemControler.getSignalGroupDefinitions();
	}




}
