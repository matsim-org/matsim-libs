/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.mrieser.core.mobsim.features.fastQueueNetworkFeature;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;

import org.apache.log4j.Logger;

/*package*/ class SingleCPUOperator implements Operator {

	private final static Logger log = Logger.getLogger(SingleCPUOperator.class);

	private final LinkedList<QueueLink> activeLinks = new LinkedList<QueueLink>();
	private final LinkedList<QueueNode> activeNodes = new LinkedList<QueueNode>();
	private final ArrayList<QueueLink> linksToActivate = new ArrayList<QueueLink>(100);
	private final ArrayList<QueueNode> nodesToActivate = new ArrayList<QueueNode>(100);

	public SingleCPUOperator() {
	}

	@Override
	public void activateLink(final QueueLink link) {
		this.linksToActivate.add(link);
	}

	@Override
	public void activateNode(final QueueNode node) {
		this.nodesToActivate.add(node);
	}

	@Override
	public void beforeMobSim() {
	}

	@Override
	public void afterMobSim() {
	}

	@Override
	public void doSimStep(double time) {
		this.activeNodes.addAll(this.nodesToActivate);
		this.nodesToActivate.clear();
		if (time % 3600 == 0 && log.isDebugEnabled()) {
			log.debug("# active nodes = " + this.activeNodes.size());
		}
		ListIterator<QueueNode> simNodes = this.activeNodes.listIterator();
		while (simNodes.hasNext()) {
			QueueNode node = simNodes.next();
			node.moveNode(time);
			if (!node.isActive()) {
				simNodes.remove();
			}
		}

		this.activeLinks.addAll(this.linksToActivate);
		this.linksToActivate.clear();
		if (time % 3600 == 0 && log.isDebugEnabled()) {
			log.debug("# active links = " + this.activeLinks.size());
		}
		ListIterator<QueueLink> simLinks = this.activeLinks.listIterator();
		while (simLinks.hasNext()) {
			QueueLink link = simLinks.next();
			link.doSimStep(time);
			if (!link.isActive()) {
				simLinks.remove();
			}
		}
	}
}
