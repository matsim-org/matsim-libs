/* *********************************************************************** *
 * project: org.matsim.*
 * Scenarion2DImpl.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.gregor.sim2_v2.scenario;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.config.Config;

/**
 * @author laemmel
 * 
 */
public class Scenario2DImpl extends ScenarioImpl {

	/**
	 * @param config
	 */
	public Scenario2DImpl(Config config) {
		super(config);
	}

	public Scenario2DImpl() {
		throw new RuntimeException("Do not try to call this constructor!!");
	}

}
