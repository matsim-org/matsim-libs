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

	public static final String ENABLE_CIRCULAR_AGENT_INTERACTION_MODULE = "enableCircularAgentInteractionModule";
	public static final String ENABLE_COLLISION_PREDICTION_AGENT_INTERACTION_MODULE = "enableCollisionPredictionAgentInteractionModule";
	public static final String ENABLE_COLLISION_PREDICTION_PHANTOM_AGENT_INTERACTION_MODULE = "enableCollisionPredictionPhantomAgentInteractionModule";
	public static final String ENABLE_COLLISION_PREDICTION_ENVIRONMENT_FORCE_MODULE = "enbableCollisionPredictionEnvironmentForceModule";
	public static final String ENABLE_DRIVING_FORCE_MODULE = "enableDrivingForceModule";
	public static final String ENABLE_ENVIRONMENT_FORCE_MODULE = "enableEnvironmentForceModule";
	public static final String ENABLE_PATH_FORCE_MODULE = "enablePathForthModule";

	public static final String PHANTOM_POPULATION_EVENTS_FILE = "phantomPopulationEventsFile";

	private int eventsInterval = 1;

	private double timeStepSize = 1./25;

	private String staticEnvFieldFile = null;

	private String floorShapeFile;

	private String lsShapeFile;

	private String phantomPopulationEventsFile;

	private boolean enableCircularAgentInteractionModule = false;
	private boolean enableCollisionPredictionAgentInteractionModule = true;


	private boolean enableCollisionPredictionPhantomAgentInteractionModule = false;


	private boolean enableCollisionPredictionEnvironmentForceModule = true;
	private boolean enableDrivingForceModule = true;


	private boolean enableEnvironmentForceModule = false;


	private boolean enablePathForceModule = true;



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
		} else if (PHANTOM_POPULATION_EVENTS_FILE.equals(key)){
			setPhantomPopulationEventsFile(value);
		} else if (ENABLE_CIRCULAR_AGENT_INTERACTION_MODULE.equals(key)){
			setEnableCircularAgentInterActionModule(value);
		} else if (ENABLE_COLLISION_PREDICTION_AGENT_INTERACTION_MODULE.equals(key)){
			setEnableCollisionPredictionAgentInteractionModule(value);
		} else if (ENABLE_COLLISION_PREDICTION_ENVIRONMENT_FORCE_MODULE.equals(key)){
			setEnableCollisionPredictionEnvironmentForceModule(value);
		} else if (ENABLE_COLLISION_PREDICTION_PHANTOM_AGENT_INTERACTION_MODULE.equals(key)) {
			setEnableCollisionPredictionPhantomAgentIneractionModule(value);
		} else if (ENABLE_DRIVING_FORCE_MODULE.equals(key)) {
			setEnableDrivingForceModule(value);
		} else if (ENABLE_ENVIRONMENT_FORCE_MODULE.equals(key)){
			setEnableEnvironmentForceModule(value);
		} else if (ENABLE_PATH_FORCE_MODULE.equals(key)){
			setEnablePathForceModule(value);
		}
		else {
			throw new IllegalArgumentException(key);
		}
	}


	public void setPhantomPopulationEventsFile(String value) {
		this.phantomPopulationEventsFile = value;
	}

	public boolean isEnableCollisionPredictionAgentInteractionModule() {
		return this.enableCollisionPredictionAgentInteractionModule;
	}
	public boolean isEnableCollisionPredictionPhantomAgentInteractionModule() {
		return this.enableCollisionPredictionPhantomAgentInteractionModule;
	}
	public boolean isEnableDrivingForceModule() {
		return this.enableDrivingForceModule;
	}

	public boolean isEnableCollisionPredictionEnvironmentForceModule() {
		return this.enableCollisionPredictionEnvironmentForceModule;
	}
	public boolean isEnableEnvironmentForceModule() {
		return this.enableEnvironmentForceModule;
	}
	public boolean isEnablePathForceModule() {
		return this.enablePathForceModule;
	}
	public boolean isEnableCircularAgentInteractionModule() {
		return this.enableCircularAgentInteractionModule;
	}

	public void setEnablePathForceModule(String value) {
		this.enablePathForceModule = Boolean.parseBoolean(value);

	}

	public void setEnableEnvironmentForceModule(String value) {
		this.enableEnvironmentForceModule = Boolean.parseBoolean(value);

	}

	public void setEnableDrivingForceModule(String value) {
		this.enableDrivingForceModule = Boolean.parseBoolean(value);

	}

	public void setEnableCollisionPredictionPhantomAgentIneractionModule(
			String value) {
		this.enableCollisionPredictionPhantomAgentInteractionModule = Boolean.parseBoolean(value);

	}

	public void setEnableCollisionPredictionEnvironmentForceModule(String value) {
		this.enableCollisionPredictionEnvironmentForceModule = Boolean.parseBoolean(value);

	}

	public void setEnableCollisionPredictionAgentInteractionModule(String value) {
		this.enableCollisionPredictionAgentInteractionModule = Boolean.parseBoolean(value);

	}

	public void setEnableCircularAgentInterActionModule(String value) {
		this.enableCircularAgentInteractionModule = Boolean.parseBoolean(value);

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
		}else if (PHANTOM_POPULATION_EVENTS_FILE.equals(key)){
			return getPhantomPopulationEventsFile();
		} else if (ENABLE_CIRCULAR_AGENT_INTERACTION_MODULE.equals(key)){
			return Boolean.toString(isEnableCircularAgentInteractionModule());
		}else if (ENABLE_COLLISION_PREDICTION_AGENT_INTERACTION_MODULE.equals(key)){
			return Boolean.toString(isEnableCollisionPredictionAgentInteractionModule());
		} else if (ENABLE_COLLISION_PREDICTION_ENVIRONMENT_FORCE_MODULE.equals(key)){
			return Boolean.toString(isEnableCollisionPredictionEnvironmentForceModule());
		}else if (ENABLE_COLLISION_PREDICTION_PHANTOM_AGENT_INTERACTION_MODULE.equals(key)){
			return Boolean.toString(isEnableCollisionPredictionPhantomAgentInteractionModule());
		}else if (ENABLE_DRIVING_FORCE_MODULE.equals(key)) {
			return Boolean.toString(isEnableDrivingForceModule());
		} else if (ENABLE_ENVIRONMENT_FORCE_MODULE.equals(key)) {
			return Boolean.toString(isEnableEnvironmentForceModule());
		} else if (ENABLE_PATH_FORCE_MODULE.equals(key)) {
			return Boolean.toString(isEnablePathForceModule());
		}
		throw new IllegalArgumentException(key);
	}



	public String getPhantomPopulationEventsFile() {
		return this.phantomPopulationEventsFile;
	}

	@Override
	public final TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		map.put(LS_SHAPE_FILE, getValue(LS_SHAPE_FILE));
		map.put(STATIC_ENV_FIELD_FILE, getValue(STATIC_ENV_FIELD_FILE));
		map.put(FLOOR_SHAPE_FILE, getValue(FLOOR_SHAPE_FILE));
		map.put(TIME_STEP_SIZE,getValue(TIME_STEP_SIZE));
		map.put(EVENTS_INTERVAL,getValue(EVENTS_INTERVAL));
		map.put(ENABLE_CIRCULAR_AGENT_INTERACTION_MODULE,getValue(ENABLE_CIRCULAR_AGENT_INTERACTION_MODULE));
		map.put(ENABLE_COLLISION_PREDICTION_AGENT_INTERACTION_MODULE,getValue(ENABLE_COLLISION_PREDICTION_AGENT_INTERACTION_MODULE));
		map.put(ENABLE_COLLISION_PREDICTION_ENVIRONMENT_FORCE_MODULE, getValue(ENABLE_COLLISION_PREDICTION_ENVIRONMENT_FORCE_MODULE));
		map.put(ENABLE_COLLISION_PREDICTION_PHANTOM_AGENT_INTERACTION_MODULE, getValue(ENABLE_COLLISION_PREDICTION_PHANTOM_AGENT_INTERACTION_MODULE));
		map.put(ENABLE_DRIVING_FORCE_MODULE, getValue(ENABLE_DRIVING_FORCE_MODULE));
		map.put(ENABLE_ENVIRONMENT_FORCE_MODULE, getValue(ENABLE_ENVIRONMENT_FORCE_MODULE));
		map.put(ENABLE_PATH_FORCE_MODULE, getValue(ENABLE_PATH_FORCE_MODULE));
		map.put(PHANTOM_POPULATION_EVENTS_FILE, getValue(PHANTOM_POPULATION_EVENTS_FILE));
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
