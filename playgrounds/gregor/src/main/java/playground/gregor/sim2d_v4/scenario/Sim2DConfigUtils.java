/* *********************************************************************** *
 * project: org.matsim.*
 * Sim2DConfigUtils.java
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

import playground.gregor.sim2d_v4.io.Sim2DConfigReader01;

public abstract class Sim2DConfigUtils {

	
	public static Sim2DConfig createConfig() {
		Sim2DConfig config = new Sim2DConfig();
		return config;
	}
	
	public static Sim2DConfig loadConfig(String path) {
		Sim2DConfig config = new Sim2DConfig();
		new Sim2DConfigReader01(config, false).readFile(path);
		return config;
	}
}
