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
package playground.gregor.sim2d_v2.scenario;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.gregor.sim2d_v2.simulation.floor.StaticEnvironmentDistancesField;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;

/**
 * @author laemmel
 * 
 */
public class Scenario2DImpl extends ScenarioImpl {

	private Map<MultiPolygon, List<Link>> mps;
	private StaticEnvironmentDistancesField sff;
	private HashMap<Id, LineString> lsmp;

	/**
	 * @param config
	 */
	public Scenario2DImpl(Config config) {
		super(config);
	}

	public Scenario2DImpl() {
		super(ConfigUtils.createConfig());
		throw new RuntimeException("Do not try to call this constructor!!");
	}

	/**
	 * @param sff
	 */
	public void setStaticForceField(StaticEnvironmentDistancesField sff) {
		//FIXME rename to setStaticEnvironmentDistancesField
		this.sff = sff;

	}

	/**
	 * 
	 * @return
	 */
	public StaticEnvironmentDistancesField getStaticForceField() {
		//FIXME rename to getStaticEnvironmentDistancesField
		return this.sff;
	}

	/**
	 * @param lsmp
	 */
	public void setLineStringMap(HashMap<Id, LineString> lsmp) {
		this.lsmp = lsmp;

	}

	/**
	 * @return
	 */
	public Map<Id, LineString> getLineStringMap() {
		return this.lsmp;
	}

	/**
	 * @param mps
	 */
	/* package */void setFloorLinkMapping(Map<MultiPolygon, List<Link>> mps) {
		this.mps = mps;
	}

	/**
	 * @return
	 */
	public Map<MultiPolygon, List<Link>> getFloorLinkMapping() {
		return this.mps;
	}


}
