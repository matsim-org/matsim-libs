/* *********************************************************************** *
 * project: org.matsim.*
 * MultiModalSimEngine.java
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

package org.matsim.ptproject.qsim.multimodalsimengine;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.matsim.core.mobsim.framework.Steppable;
import org.matsim.ptproject.qsim.interfaces.MobsimEngine;
import org.matsim.ptproject.qsim.interfaces.NetsimLink;
import org.matsim.ptproject.qsim.interfaces.NetsimNode;
import org.matsim.ptproject.qsim.interfaces.Mobsim;
import org.matsim.ptproject.qsim.multimodalsimengine.router.costcalculator.MultiModalTravelTime;
import org.matsim.ptproject.qsim.multimodalsimengine.router.costcalculator.MultiModalTravelTimeCost;

public class MultiModalSimEngine implements MobsimEngine, NetworkElementActivator, Steppable {

	/*package*/ Mobsim qSim;
	/*package*/ MultiModalTravelTime multiModalTravelTime;
	/*package*/ List<MultiModalQLinkExtension> allLinks = null;
	/*package*/ List<MultiModalQLinkExtension> activeLinks;
	/*package*/ List<MultiModalQNodeExtension> activeNodes;
	/*package*/ Queue<MultiModalQLinkExtension> linksToActivate;
	/*package*/ Queue<MultiModalQNodeExtension> nodesToActivate;
//	/*package*/ List<MultiModalQLinkExtension> linksToActivate;
//	/*package*/ List<MultiModalQNodeExtension> nodesToActivate;

	/*package*/ MultiModalSimEngine(Mobsim qSim) {
		this.qSim = qSim;
		
		activeLinks = new ArrayList<MultiModalQLinkExtension>();
		activeNodes = new ArrayList<MultiModalQNodeExtension>();
		linksToActivate = new ConcurrentLinkedQueue<MultiModalQLinkExtension>();	// thread-safe Queue!
		nodesToActivate = new ConcurrentLinkedQueue<MultiModalQNodeExtension>();	// thread-safe Queue!
//		linksToActivate = new ArrayList<MultiModalQLinkExtension>();
//		nodesToActivate = new ArrayList<MultiModalQNodeExtension>();
		
		multiModalTravelTime = new MultiModalTravelTimeCost(qSim.getScenario().getConfig().plansCalcRoute(), 
				qSim.getScenario().getConfig().multiModal());
	}
	
	@Override
	public Mobsim getMobsim() {
		return qSim;
	}

	@Override
	public void onPrepareSim() {
		allLinks = new ArrayList<MultiModalQLinkExtension>();
		for (NetsimLink qLink : this.qSim.getNetsimNetwork().getNetsimLinks().values()) {
			allLinks.add(this.getMultiModalQLinkExtension(qLink));
		}
	}

	@Override
	public void doSimStep(double time) {
		moveNodes(time);
		moveLinks(time);
	}

	/*package*/ void moveNodes(final double time) {
		reactivateNodes();
		
		ListIterator<MultiModalQNodeExtension> simNodes = this.activeNodes.listIterator();
		MultiModalQNodeExtension node;
		boolean isActive;

		while (simNodes.hasNext()) {
			node = simNodes.next();
			isActive = node.moveNode(time);
			if (!isActive) {
				simNodes.remove();
			}
		}
	}

	/*package*/ void moveLinks(final double time) {
		reactivateLinks();
		
		ListIterator<MultiModalQLinkExtension> simLinks = this.activeLinks.listIterator();
		MultiModalQLinkExtension link;
		boolean isActive;

		while (simLinks.hasNext()) {
			link = simLinks.next();
			isActive = link.moveLink(time);
			if (!isActive) {
				simLinks.remove();
			}
		}
	}
	
	@Override
	public void afterSim() {
		/* Reset vehicles on ALL links. We cannot iterate only over the active links
		 * (this.simLinksArray), because there may be links that have vehicles only
		 * in the buffer (such links are *not* active, as the buffer gets emptied
		 * when handling the nodes.
		 */
		for (MultiModalQLinkExtension link : this.allLinks) {
			link.clearVehicles();
		}
	}
	
	@Override
	public void activateLink(MultiModalQLinkExtension link) {
		linksToActivate.add(link);
	}

	@Override
	public void activateNode(MultiModalQNodeExtension node) {
		nodesToActivate.add(node);
	}

	@Override
	public int getNumberOfSimulatedLinks() {
		return activeLinks.size();
	}

	@Override
	public int getNumberOfSimulatedNodes() {
		return activeNodes.size();
	}

	/*package*/ void reactivateLinks() {
		if (!linksToActivate.isEmpty()) {
			activeLinks.addAll(linksToActivate);
			linksToActivate.clear();
		}
	}
	
	/*package*/ void reactivateNodes() {
		if (!nodesToActivate.isEmpty()) {
			activeNodes.addAll(nodesToActivate);
			nodesToActivate.clear();
		}
	}

	/*package*/ MultiModalTravelTime getMultiModalTravelTime() {
		return this.multiModalTravelTime;
	}
	
	/*package*/ MultiModalQNodeExtension getMultiModalQNodeExtension(NetsimNode qNode) {
		return (MultiModalQNodeExtension) qNode.getCustomAttributes().get(MultiModalQNodeExtension.class.getName());
	}

	/*package*/ MultiModalQLinkExtension getMultiModalQLinkExtension(NetsimLink qLink) {
		return (MultiModalQLinkExtension) qLink.getCustomAttributes().get(MultiModalQLinkExtension.class.getName());
	}
}
