/* *********************************************************************** *
 * project: org.matsim.*
 * Sim2DConfig.java
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Sim2DConfig {
	
	//work in progress ...
	
	private double timeStepSize;
	private int eventsInterval;
	
	private final  List<String> sim2DEnvironmentsPaths = new ArrayList<String>();
	private final Map<String,String> sim2DEnvNetworkMapping = new HashMap<String, String>();
	private final Map<String,List<String>> sim2DEnvAccessorNodesMapping = new HashMap<String, List<String>>();
	private final Map<String,String> sim2DAccessorNodeQSimAccessorNodeMapping = new HashMap<String, String>();
	 
	public void setEventsInterval(int eventsInterval) {
		this.eventsInterval = eventsInterval;
	}
	
	public int getEventsInterval() {
		return this.eventsInterval;
	}
	
	public void setTimeStepSize(double timeStepSize) {
		this.timeStepSize = timeStepSize;
	}
	
	public double getTimeStepSize() {
		return this.timeStepSize;
	}
	
	public void addSim2DEnvironmentPath(String path) {
		this.sim2DEnvironmentsPaths.add(path);
	}
	
	public List<String> getSim2DEnvironmentPaths() {
		return this.sim2DEnvironmentsPaths;
	}
	
	public void addSim2DEnvNetworkMapping(String sim2DEnv, String network) {
		this.sim2DEnvNetworkMapping.put(sim2DEnv, network);
	}
	
	public String getNetworkPath(String sim2DEnvPath) {
		return this.sim2DEnvNetworkMapping.get(sim2DEnvPath);
	}
	
	public void addSim2DEnvAccessorNode(String sim2DEnvPath, String accessorNode) {
		List<String> l = this.sim2DEnvAccessorNodesMapping.get(sim2DEnvPath);
		if (l == null) {
			l = new ArrayList<String>();
			this.sim2DEnvAccessorNodesMapping.put(sim2DEnvPath, l);
		}
		l.add(accessorNode);
	}
	
	public List<String> getSim2DEnvAccessorNodes(String sim2DEnvPath) {
		return this.sim2DEnvAccessorNodesMapping.get(sim2DEnvPath);
	}
	
	public void addSim2DAccessorNodeQSimAccessorNodeMapping(String sim2DNode, String qsimNode) {
		this.sim2DAccessorNodeQSimAccessorNodeMapping.put(sim2DNode, qsimNode);
	}
	
	public String getQSimNode(String sim2DNode) {
		return this.sim2DAccessorNodeQSimAccessorNodeMapping.get(sim2DNode);
	}

}
