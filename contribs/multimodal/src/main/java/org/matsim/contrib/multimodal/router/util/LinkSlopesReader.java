/* *********************************************************************** *
 * project: org.matsim.*
 * LinkSlopesReader.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.contrib.multimodal.router.util;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import java.util.HashMap;
import java.util.Map;

public class LinkSlopesReader {

	private static final Logger log = Logger.getLogger(LinkSlopesReader.class);
	
	private static final String ATTRIBUTE_NAME = "slope";
	
	/**
	 * If height information for a network is available, the links' slopes
	 * is returned in a Map<LinkId, Slope in %>.
	 *  
	 * It is assumed that the heightInformation ObjectAttributes contain a field
	 * "slope" for each link Id in the network.
	 * 
	 * @return Map<LinkId, Slope in %> or null, if no slopes file has been set in the config group.
	 */
	public Map<Id<Link>, Double> getLinkSlopes(MultiModalConfigGroup configGroup, Network network) {
		
		ObjectAttributes slopeInformation = this.getSlopeInformation(configGroup);
		
		if (slopeInformation == null) return null;
		
		Map<Id<Link>, Double> linkSlopes = this.getLinkSlopes(network, slopeInformation);
		
		int found = linkSlopes.size();
		int total = network.getLinks().size();
		log.info("Found slope information for " + found + " of " + total + " links.");
		
		return linkSlopes;
	}
	
	/**
	 * Reads height information from the file specified in the config group
	 * and returns them as ObjectAttributes.
	 */
	private ObjectAttributes getSlopeInformation(MultiModalConfigGroup configGroup) {
		
		String slopeInformationFile = configGroup.getSlopeInformationFile();
		if (slopeInformationFile != null) {
			ObjectAttributes objectAttributes = new ObjectAttributes();
			log.info("Loading slope information from " + slopeInformationFile);
			new ObjectAttributesXmlReader(objectAttributes).readFile(slopeInformationFile);
			return objectAttributes;
		} else {
			log.warn("No slope information file specified in the multi modal config group!");
			return null;
		}
	}
	
	private Map<Id<Link>, Double> getLinkSlopes(Network network, ObjectAttributes slopeInformation) {
		
		Map<Id<Link>, Double> linkSlopes = new HashMap<>();
		
		for (Id<Link> linkId : network.getLinks().keySet()) {
			Object slope = slopeInformation.getAttribute(linkId.toString(), ATTRIBUTE_NAME);
			if (slope != null) linkSlopes.put(linkId, Double.valueOf(slope.toString()));
		}
		return linkSlopes;
	}
}
