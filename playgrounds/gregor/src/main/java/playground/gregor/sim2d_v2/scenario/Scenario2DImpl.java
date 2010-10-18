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
import java.util.Queue;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.config.Config;

import playground.gregor.sim2d.events.XYZAzimuthEvent;
import playground.gregor.sim2d.simulation.SegmentedStaticForceField;
import playground.gregor.sim2d_v2.simulation.floor.StaticForceField;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;

/**
 * @author laemmel
 * 
 */
public class Scenario2DImpl extends ScenarioImpl {

	private Map<MultiPolygon, List<Link>> mps;
	private SegmentedStaticForceField ssff;
	private StaticForceField sff;
	private HashMap<Id, LineString> lsmp;
	private Queue<Event> phantomPopulation = null;

	/**
	 * @param config
	 */
	public Scenario2DImpl(Config config) {
		super(config);
	}

	public Scenario2DImpl() {
		throw new RuntimeException("Do not try to call this constructor!!");
	}

	/**
	 * @param ssff
	 */
	public void setSegmentedStaticForceField(SegmentedStaticForceField ssff) {
		this.ssff = ssff;

	}

	/**
	 * @return
	 */
	public SegmentedStaticForceField getSegmentedStaticForceField() {
		return this.ssff;
	}

	/**
	 * @param sff
	 */
	public void setStaticForceField(StaticForceField sff) {
		this.sff = sff;

	}

	/**
	 * 
	 * @return
	 */
	public StaticForceField getStaticForceField() {
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

	/**
	 * @param phantomPopulation2
	 */
	public void setPhantomPopulation(Queue<Event> phantomPopulation2) {
		this.phantomPopulation = phantomPopulation2;

	}

	/**
	 * @return the phantomPopulation
	 */
	public Queue<Event> getPhantomPopulation() {
		return this.phantomPopulation;
	}

}
