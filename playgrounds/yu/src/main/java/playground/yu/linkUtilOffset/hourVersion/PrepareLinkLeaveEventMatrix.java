/* *********************************************************************** *
 * project: org.matsim.*
 * PrepareLinkLeaveEventMatrix.java
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

/**
 * 
 */
package playground.yu.linkUtilOffset.hourVersion;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;

/**
 * @author yu
 * 
 */
public class PrepareLinkLeaveEventMatrix implements LinkLeaveEventHandler {
	/**
	 * @param Map
	 *            {@code Id} agentId,{@code LinkLeaveEventChain}
	 */
	private Map<Id, LinkLeaveEventChain> chains = new HashMap<Id, LinkLeaveEventChain>();
	/**
	 * @param Map
	 *            {@code Id} linkId, {@code Set}{@code Integer} timeBins
	 */
	private Map<Id, Set<Integer>> linksTimes = new HashMap<Id, Set<Integer>>();

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		LinkLeaveEventSection section = new LinkLeaveEventSection(event);

		Id agentId = event.getPersonId();
		LinkLeaveEventChain chain = this.chains.get(agentId);
		if (chain == null) {
			chain = new LinkLeaveEventChain();
			this.chains.put(agentId, chain);
		}
		chain.addSection(section);

		trimmingLinksTimes(section);
	}

	@Override
	public void reset(int iteration) {

	}

	public void trimmingLinksTimes(LinkLeaveEventSection section) {
		Id linkId = section.getLinkId();
		Set<Integer> timeBins = this.linksTimes.get(linkId);
		if (timeBins == null) {
			timeBins = new HashSet<Integer>();
			this.linksTimes.put(linkId, timeBins);
		}
		timeBins.add(section.getTime());
	}
	// TODO output number of Matrix column and rows
}
