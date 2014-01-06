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
	
	
//	private double offsetX;
//	private double offsetY;
	
	//EXPERIMENTAL [GL Oct'13]
	public final static boolean EXPERIMENTAL_VD_APPROACH = false;
	
	private double timeStepSize = 0.1;
	private int eventsInterval = 0;
	
	private final  List<String> sim2DEnvironmentsPaths = new ArrayList<String>();
	private final Map<String,String> sim2DEnvNetworkMapping = new HashMap<String, String>();
	
	
	/*package*/ Sim2DConfig() {}

	
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
//		throw new RuntimeException("not longer supported!");
		this.sim2DEnvNetworkMapping.put(sim2DEnv, network);
	}
	
	public String getNetworkPath(String sim2DEnvPath) {
//		throw new RuntimeException("not longer supported!");
		return this.sim2DEnvNetworkMapping.get(sim2DEnvPath);
	}
	

}
