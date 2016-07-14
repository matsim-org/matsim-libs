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

package org.matsim.contrib.multimodal.tools;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.core.api.internal.NetworkRunnable;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.misc.Counter;

import java.util.HashSet;
import java.util.Set;

/*
 * Simple tool that converts a car only network into a multi modal network.
 * The multiModalConfigGroup specifies: 
 * 	- the available modes which will be added to the network
 *  - the cutoff speed for non motorized modes (highways should not be available for pedestrians) 
 */
public class MultiModalNetworkCreator implements NetworkRunnable {

	private static final Logger log = Logger.getLogger(MultiModalNetworkCreator.class);
	
	private final MultiModalConfigGroup multiModalConfigGroup;
	
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
		
		Set<String> modes = CollectionUtils.stringToSet(simulatedModes);
		
		Counter counter = new Counter("Converted links to multi-modal links: ");
		for (Link link : network.getLinks().values()) {
			if (Math.round(link.getFreespeed()) <= cutoffSpeed) {
				Set<String> allowedModes = new HashSet<>(link.getAllowedModes());
				allowedModes.addAll(modes);
				link.setAllowedModes(allowedModes);
				counter.incCounter();
			}
		}
		counter.printCounter();
	}
}