/* *********************************************************************** *
 * project: org.matsim.*
 * Sim2DScenario.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.gregor.sim2d_v4.scenario;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

public class Sim2DScenario {
	
	private static final Logger log = Logger.getLogger(Sim2DScenario.class);
	
	/** name to use to add this class as a scenario element */
	public static final String ELEMENT_NAME = "sim2DScenario";
	
	private final Map<Id,Sim2DEnvironment> envs = new HashMap<Id,Sim2DEnvironment>();
	
	private final Map<Link,Sim2DEnvironment> linkEnvironmentMapping = new HashMap<Link, Sim2DEnvironment>();
	
	private final Sim2DConfig config;
	
	private boolean connected = false;

	private Scenario sc;
	
	public Sim2DScenario(Sim2DConfig conf) {
		this.config = conf;
	}
	
	public Sim2DConfig getSim2DConfig() {
		return this.config;
	}
	
	public Collection<Sim2DEnvironment> getSim2DEnvironments() {
		return this.envs.values();
	}
	
	
	public void connect(Scenario sc) {
		if (this.connected) {
			log.warn("2D Sceanrio already connected!");
			return;
		}
		log.info("Connecting 2D scenario.");
		sc.addScenarioElement( ELEMENT_NAME , this);
		this.sc = sc;
		Network scNet = sc.getNetwork();
		for (Sim2DEnvironment env : this.envs.values()) {
			
			Network envNet = env.getEnvironmentNetwork();
			connect(envNet,scNet);
			for (Section sec : env.getSections().values()){
				for (Id refId : sec.getRelatedLinkIds()) {
					Link l = scNet.getLinks().get(refId);
					env.addLinkSectionMapping(l, sec);
					this.linkEnvironmentMapping.put(l, env);
				}
			}
		}
		this.connected = true;
	}
	
	public Sim2DEnvironment getSim2DEnvironment(Link l) {
		return this.linkEnvironmentMapping.get(l);
	}


	private void connect(Network envNet, Network scNet) {
		for (Node n : envNet.getNodes().values()) {
			Node nn = scNet.getNodes().get(n.getId());
			if (nn == null) {
				n.getInLinks().clear();
				n.getOutLinks().clear();
				scNet.addNode(n);
			}
		}
		for (Link l : envNet.getLinks().values()) {
			if (scNet.getLinks().get(l.getId()) != null) { 
				//don't create links that already exist
				continue;
			}
			Node nFrom = scNet.getNodes().get(l.getFromNode().getId());
			Node nTo = scNet.getNodes().get(l.getToNode().getId());
			if (l.getFromNode() != nFrom) {
				l.setFromNode(nFrom);
//				nFrom.addOutLink(l);
			}
			if (l.getToNode() != nTo) {
				l.setToNode(nTo);
//				nTo.addInLink(l);
			}
			scNet.addLink(l); //TODO check if capperiod, effectivecellsize, lanewidth is the same for both networks
		}
	}

	public void addSim2DEnvironment(Sim2DEnvironment env) {
		this.envs.put(env.getId(), env);
		
	}

	public Sim2DEnvironment getSim2DEnvironment(Id id) {
		return this.envs.get(id);
	}
	
	public Scenario getMATSimScenario() {
		return this.sc;
	}
	

}
