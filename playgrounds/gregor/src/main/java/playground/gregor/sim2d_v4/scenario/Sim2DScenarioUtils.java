/* *********************************************************************** *
 * project: org.matsim.*
 * Sim2DScenarioUtils.java
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

import playground.gregor.sim2d_v4.io.Sim2DEnvironmentReader03;

public abstract class Sim2DScenarioUtils {
	
	public static  Sim2DScenario loadSim2DScenario(Sim2DConfig conf) {
		Sim2DScenario scenario = new Sim2DScenario(conf);
		for (String envPath : conf.getSim2DEnvironmentPaths()){
			Sim2DEnvironment env = new Sim2DEnvironment();
			new Sim2DEnvironmentReader03(env, false).readFile(envPath);
			scenario.addSim2DEnvironment(env);
			Sim2DSectionPreprocessor.preprocessSections(env);
			Sim2DEnvironmentNetworkBuilder.buildAndSetEnvironmentNetwork(env);
//			String netPath = conf.getNetworkPath(envPath);
//			if (netPath != null) { //not yet clear if this can be null, maybe it even must be null [gl dec 12]
//				Config c = ConfigUtils.createConfig();
//				Scenario sc = ScenarioUtils.createScenario(c);
//				new MatsimNetworkReader(sc).readFile(netPath);
//				Network net = sc.getNetwork();
//				env.setNetwork(net);
//			}
		}
		return scenario;
	}
	
	public static Sim2DScenario createSim2dScenario(Sim2DConfig conf) {
		return new Sim2DScenario(conf);
	}
	

}
