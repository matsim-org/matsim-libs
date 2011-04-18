/* *********************************************************************** *
 * project: org.matsim.*
 * Sim2DConfigGroup.java
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
package playground.gregor.sim2d_v2.config;

import java.util.TreeMap;
import java.util.Map.Entry;

import org.matsim.core.config.Module;

/**
 * @author laemmel
 * 
 */
public class Sim2DConfigGroup extends Module {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final String GROUP_NAME = "sim2d";

	public static final String STATIC_ENV_FIELD_FILE = "staticEnvFieldFile";
	public static final String FLOOR_SHAPE_FILE = "floorShapeFile";
	public static final String LS_SHAPE_FILE = "lsShapeFile";
	public static final String TIME_STEP_SIZE = "timeStepSize";

	public static final String EVENTS_INTERVAL = "eventsInterval";



	private int eventsInterval = 1;

	private double timeStepSize = 1./25;

	private String staticEnvFieldFile = null;

	private String floorShapeFile;

	private String lsShapeFile;

	public Sim2DConfigGroup(Module sim2d) {
		super(GROUP_NAME);
		for (Entry<String, String> e : sim2d.getParams().entrySet()) {
			addParam(e.getKey(), e.getValue());
		}

	}

	/**
	 * 
	 */
	public Sim2DConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public void addParam(final String key, final String value) {
		if (STATIC_ENV_FIELD_FILE.equals(key)) {
			setStaticEnvFieldFile(value);
		} else if (FLOOR_SHAPE_FILE.equals(key)) {
			setFloorShapeFile(value);
		} else if (LS_SHAPE_FILE.equals(key)) {
			setLSShapeFile(value);
		} else if (TIME_STEP_SIZE.equals(key)) {
			setTimeStepSize(value);
		} else if (EVENTS_INTERVAL.equals(key)) {
			setEventsInterval(value);
		}else {
			throw new IllegalArgumentException(key);
		}
	}



	@Override
	public String getValue(final String key) {
		if (STATIC_ENV_FIELD_FILE.equals(key)) {
			return getStaticEnvFieldFile();
		} else if (FLOOR_SHAPE_FILE.equals(key)) {
			return getFloorShapeFile();
		} else if (LS_SHAPE_FILE.equals(key)) {
			return getLSShapeFile();
		}else if (TIME_STEP_SIZE.equals(key)) {
			return Double.toString(getTimeStepSize());
		}else if (EVENTS_INTERVAL.equals(key)) {
			return Integer.toString(getEventsInterval());
		}
		throw new IllegalArgumentException(key);
	}



	@Override
	public final TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		map.put(LS_SHAPE_FILE, getValue(LS_SHAPE_FILE));
		map.put(STATIC_ENV_FIELD_FILE, getValue(STATIC_ENV_FIELD_FILE));
		map.put(FLOOR_SHAPE_FILE, getValue(FLOOR_SHAPE_FILE));
		map.put(TIME_STEP_SIZE,getValue(TIME_STEP_SIZE));
		map.put(EVENTS_INTERVAL,getValue(EVENTS_INTERVAL));
		return map;
	}

	/**
	 * @return
	 */
	public String getLSShapeFile() {
		return this.lsShapeFile;
	}

	/**
	 * @return
	 */
	public String getFloorShapeFile() {
		return this.floorShapeFile;
	}

	/**
	 * @return
	 */
	public String getStaticEnvFieldFile() {
		return this.staticEnvFieldFile;
	}

	/**
	 * @param value
	 */
	private void setLSShapeFile(String value) {
		this.lsShapeFile = value;

	}

	/**
	 * @param value
	 */
	private void setFloorShapeFile(String value) {
		this.floorShapeFile = value;

	}

	/**
	 * @param value
	 */
	private void setStaticEnvFieldFile(String value) {
		this.staticEnvFieldFile = value;

	}

	/**
	 * Iteration interval for XYZAzimuth events output
	 * @param value
	 */
	public void setEventsInterval(String value) {
		this.eventsInterval = Integer.parseInt(value);
	}

	/**
	 * 
	 * @return events interval
	 */
	public int getEventsInterval() {
		return this.eventsInterval;
	}


	/**
	 * 
	 * @param value
	 */
	private void setTimeStepSize(String value) {
		this.timeStepSize = Double.parseDouble(value);

	}


	/**
	 * The time step size for the 2D Simulation (fraction of the simulation time step size)
	 * @return time step size
	 */
	public double getTimeStepSize() {
		return this.timeStepSize;
	}
}
