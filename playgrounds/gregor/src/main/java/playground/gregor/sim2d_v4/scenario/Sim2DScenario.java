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

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

public class Sim2DScenario {
	
	
	private final List<Sim2DEnvironment> envs = new ArrayList<Sim2DEnvironment>();
	private final Sim2DConfig config;
	
	/*package*/ Sim2DScenario(Sim2DConfig conf) {
		this.config = conf;
	}
	
	public Sim2DConfig getSim2DConfig() {
		return this.config;
	}
	
	public List<Sim2DEnvironment> getSim2DEnvironments() {
		return this.envs;
	}
	
	public void connect(Scenario sc) {
		sc.addScenarioElement(this);
		Network scNet = sc.getNetwork();
		for (Sim2DEnvironment env : this.envs) {
			Network envNet = env.getEnvironmentNetwork();
			connect(envNet,scNet);
		}
	}


	private void connect(Network envNet, Network scNet) {
		for (Node n : envNet.getNodes().values()) {
			scNet.addNode(n);
		}
		for (Link l : envNet.getLinks().values()) {
			scNet.addLink(l); //TODO check if capperiod, effectivecellsize, lanewidth is the same for both networks
		}
	}
	

}
