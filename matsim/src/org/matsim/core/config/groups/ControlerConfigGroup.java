/* *********************************************************************** *
 * project: org.matsim.*
 * ControlerConfigGroup.java
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

package org.matsim.core.config.groups;

import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.core.config.Module;

public class ControlerConfigGroup extends Module {

	public enum RoutingAlgorithmType {Dijkstra, AStarLandmarks};
	
	private static final long serialVersionUID = 1L;

	private static final Logger log = Logger.getLogger(ControlerConfigGroup.class);
	
	public static final String GROUP_NAME = "controler";

	private static final String OUTPUT_DIRECTORY = "outputDirectory";
	private static final String FIRST_ITERATION = "firstIteration";
	private static final String LAST_ITERATION = "lastIteration";
	private static final String ROUTINGALGORITHM_TYPE = "routingAlgorithmType";
	private static final String RUNID = "runId";
	private static final String LINKTOLINK_ROUTING_ENABLED = "enableLinkToLinkRouting";
	
	private String outputDirectory = "./output";
	private int firstIteration = 0;
	private int lastIteration = 1000;
	private RoutingAlgorithmType routingAlgorithmType = RoutingAlgorithmType.Dijkstra;
	
	private boolean linkToLinkRoutingEnabled = false;
	
	private String runId = null;
	
	public ControlerConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public String getValue(final String key) {
		if (OUTPUT_DIRECTORY.equals(key)) {
			return getOutputDirectory();
		} else if (FIRST_ITERATION.equals(key)) {
			return Integer.toString(getFirstIteration());
		} else if (LAST_ITERATION.equals(key)) {
			return Integer.toString(getLastIteration());
		} else if (ROUTINGALGORITHM_TYPE.equals(key)){
			return this.getRoutingAlgorithmType().toString();
		} else if (RUNID.equals(key)){
			return this.getRunId();
		} else if (LINKTOLINK_ROUTING_ENABLED.equals(key)){
			return Boolean.toString(this.linkToLinkRoutingEnabled);
		}
		else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	public void addParam(final String key, final String value) {
		if (OUTPUT_DIRECTORY.equals(key)) {
			setOutputDirectory(value.replace('\\', '/'));
		} else if (FIRST_ITERATION.equals(key)) {
			setFirstIteration(Integer.parseInt(value));
		} else if (LAST_ITERATION.equals(key)) {
			setLastIteration(Integer.parseInt(value));
		} else if (ROUTINGALGORITHM_TYPE.equals(key)){
			if (RoutingAlgorithmType.Dijkstra.toString().equalsIgnoreCase(value)){
				setRoutingAlgorithmType(RoutingAlgorithmType.Dijkstra);
			}
			else if (RoutingAlgorithmType.AStarLandmarks.toString().equalsIgnoreCase(value)){
				setRoutingAlgorithmType(RoutingAlgorithmType.AStarLandmarks);
			}
			else {
				throw new IllegalArgumentException(value + " is not a valid parameter value for key: "+ key + " of config group " + this.GROUP_NAME);
			}
		} else if (RUNID.equals(key)){
			this.setRunId(value.trim());
		} else if (LINKTOLINK_ROUTING_ENABLED.equalsIgnoreCase(key)){
			if (value != null)
				this.linkToLinkRoutingEnabled = Boolean.getBoolean(value.trim());
		}
		else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	protected final TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		map.put(OUTPUT_DIRECTORY, getValue(OUTPUT_DIRECTORY));
		map.put(FIRST_ITERATION, getValue(FIRST_ITERATION));
		map.put(LAST_ITERATION, getValue(LAST_ITERATION));
		map.put(ROUTINGALGORITHM_TYPE, getValue(ROUTINGALGORITHM_TYPE));
		map.put(RUNID, getValue(RUNID));
		map.put(LINKTOLINK_ROUTING_ENABLED, Boolean.toString(this.isLinkToLinkRoutingEnabled()));
		return map;
	}
	
	@Override
	protected final Map<String, String> getComments() {
		Map<String,String> map = super.getComments();
		map.put(ROUTINGALGORITHM_TYPE, "The type of routing (least cost path) algorithm used, may have the values: " + RoutingAlgorithmType.Dijkstra + " or " + RoutingAlgorithmType.AStarLandmarks);
		map.put(RUNID, "An identifier for the current run which is used as prefix for output files and mentioned in output xml files etc.");
		return map;
	}

	/* direct access */

	public void setOutputDirectory(final String outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	public String getOutputDirectory() {
		return this.outputDirectory;
	}

	public void setFirstIteration(final int firstIteration) {
		this.firstIteration = firstIteration;
	}

	public int getFirstIteration() {
		return this.firstIteration;
	}

	public void setLastIteration(final int lastIteration) {
		this.lastIteration = lastIteration;
	}

	public int getLastIteration() {
		return this.lastIteration;
	}
	
	public RoutingAlgorithmType getRoutingAlgorithmType(){
		return this.routingAlgorithmType;
	}
	
	public void setRoutingAlgorithmType(RoutingAlgorithmType type){
		this.routingAlgorithmType = type;
	}
	
	public String getRunId(){
		return this.runId;
	}
	
	public void setRunId(String runid){
		this.runId = runid;
	}

	public boolean isLinkToLinkRoutingEnabled() {
		return this.linkToLinkRoutingEnabled;
	}
	
	public void setLinkToLinkRoutingEnabled(boolean enabled){
		this.linkToLinkRoutingEnabled = enabled;
	}
	
}
