/* *********************************************************************** *
 * project: org.matsim.*
 * MultiModalNetworkCreator.java
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

package org.matsim.ptproject.qsim.multimodalsimengine.tools;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.internal.NetworkRunnable;
import org.matsim.core.config.groups.MultiModalConfigGroup;

/*
 * Simple tool that converts a car only network into a multi modal network.
 * The multiModalConfigGroup specifies: 
 * 	- the available modes which will be added to the network
 *  - the cutoff speed for non motorized modes (highways should not be available for pedestrians) 
 */
public class MultiModalNetworkCreator implements NetworkRunnable {

	private static final Logger log = Logger.getLogger(MultiModalNetworkCreator.class);
	
	private MultiModalConfigGroup multiModalConfigGroup;
	
	public MultiModalNetworkCreator(MultiModalConfigGroup multiModalConfigGroup) {
		this.multiModalConfigGroup = multiModalConfigGroup;
	}
		
	@Override
	public void run(Network network) {
		
		if (!multiModalConfigGroup.isCreateMultiModalNetwork()) {
			log.warn("Creation of multi modal network is not enabled in the config group - network is not adapted!");
			return;
		}
		
		double cutoffSpeed = multiModalConfigGroup.getCutoffValueForNonCarModes();
		String simulatedModes = multiModalConfigGroup.getSimulatedModes();
		
		Set<String> modes = new HashSet<String>();
		if (simulatedModes.contains("walk")) modes.add(TransportMode.walk);
		if (simulatedModes.contains("bike")) modes.add(TransportMode.bike);
		if (simulatedModes.contains("pt")) modes.add(TransportMode.pt);
		
		for (Link link : network.getLinks().values()) {
			if (Math.round(link.getFreespeed()) <= cutoffSpeed) {
				Set<String> allowedModes = new HashSet<String>(link.getAllowedModes());
				allowedModes.addAll(modes);
				link.setAllowedModes(allowedModes);
			}
		}
	}
}