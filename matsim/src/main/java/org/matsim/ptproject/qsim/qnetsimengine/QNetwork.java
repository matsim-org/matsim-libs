/* *********************************************************************** *
 * project: org.matsim.*
 * QueueNetwork.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2009 by the members listed in the COPYING,  *
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

package org.matsim.ptproject.qsim.qnetsimengine;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.interfaces.NetsimNetworkFactory;
import org.matsim.ptproject.qsim.interfaces.NetsimNetwork;
import org.matsim.ptproject.qsim.interfaces.NetsimNode;
import org.matsim.ptproject.qsim.interfaces.NetsimEngine;
import org.matsim.vis.snapshots.writers.VisLink;
import org.matsim.vis.snapshots.writers.VisNetwork;
import org.matsim.vis.snapshots.writers.VisNode;

/**
 *
 * @author david
 * @author mrieser
 * @author dgrether
 */
@Deprecated // please try to use the interfaces outside the package.  This will also make your code portable.  kai, oct'10
public final class QNetwork implements VisNetwork, NetsimNetwork {

	private final Map<Id, QLinkInternalI> links;

	private final Map<Id, QNode> nodes;

	private final Network networkLayer;

	private final NetsimNetworkFactory<QNode, QLinkInternalI> queueNetworkFactory;

//	private QSim qSim;
	private QSimEngineImpl qSimEngine;

	/**
	 * This is deliberately package-private.  Please use the factory
	 */
	QNetwork(final QSim qs) {
		this(qs, new DefaultQNetworkFactory());
	}

	/**
	 * This is deliberately package-private.  Please use the factory
	 */
	QNetwork(final QSim qs, final NetsimNetworkFactory<QNode, QLinkInternalI> factory ) {
//		this.qSim = qs;
		this.networkLayer = qs.getScenario().getNetwork();
		this.queueNetworkFactory = factory;
		this.links = new LinkedHashMap<Id, QLinkInternalI>((int)(networkLayer.getLinks().size()*1.1), 0.95f);
		this.nodes = new LinkedHashMap<Id, QNode>((int)(networkLayer.getLinks().size()*1.1), 0.95f);
	}


	@Override
	public void initialize(NetsimEngine simEngine) {
		this.qSimEngine = (QSimEngineImpl) simEngine;
		this.qSimEngine.setQNetwork(this);
		for (Node n : networkLayer.getNodes().values()) {
			this.nodes.put(n.getId(), this.queueNetworkFactory.createNetsimNode(n, simEngine));
		}
		for (Link l : networkLayer.getLinks().values()) {
			this.links.put(l.getId(), this.queueNetworkFactory.createNetsimLink(l, simEngine, this.nodes.get(l.getToNode().getId())));
		}
		for (QNode n : this.nodes.values()) {
			n.init();
		}
	}

	@Override
	public Network getNetwork() {
		return this.networkLayer;
	}

	@Override
	public Map<Id, QLinkInternalI> getNetsimLinks() {
		return Collections.unmodifiableMap(this.links);
	}

	@Override
	public Map<Id, ? extends VisLink> getVisLinks() {
		return Collections.unmodifiableMap(this.links);
	}

	@Override
	public Map<Id, QNode> getNetsimNodes() {
		return Collections.unmodifiableMap(this.nodes);
	}

	@Override
	public Map<Id, ? extends VisNode> getVisNodes() {
		return Collections.unmodifiableMap(this.nodes);
	}

	@Override
	public QLinkInternalI getNetsimLink(final Id id) {
		return this.links.get(id);
	}

	@Override
	public NetsimNode getNetsimNode(final Id id) {
		return this.nodes.get(id);
	}



}
