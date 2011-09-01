/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.andreas.P.init;

import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.Module;

/**
 * Config group to configure p
 * 
 * @author aneumann
 *
 */
@Deprecated
public class PConfigGroup extends Module{
	
	/**
	 * TODO [AN] This one has to be checked
	 */
	private static final long serialVersionUID = 4840713748058034511L;

	private static final Logger log = Logger.getLogger(PConfigGroup.class);
	
	// Tags
	
	public static final String GROUP_NAME = "p";
	
	private static final String GRID_DISTANCE = "gridDistance";
	private static final String MIN_X = "minX";
	private static final String MIN_Y = "minY";
	private static final String MAX_X = "maxX";
	private static final String MAX_Y = "maxY";
	private static final String NUMBER_OF_AGENTS = "numberOfAgents";
	private static final String NUMBER_OF_PLANS = "numberOfPlans";
	
	// from config
	private String network = "network";
	private String outputDir = "outputDir";

	// Defaults
	
	private double gridDistance = -1.0;
	private double minX = -1.0;	
	private double minY = -1.0;	
	private double maxX = -1.0;
	private double maxY = -1.0;
	private int numberOfAgents = 1;
	private int numberOfPlans = 4;

	private String currentOutputBase;
	private String nextOutputBase;
	
	public PConfigGroup() {
		super(GROUP_NAME);
		log.info("Started...");
		log.warn("SerialVersionUID has to be checked. Current one is " + PConfigGroup.serialVersionUID);
	}
	
	public PConfigGroup(Config config) {
		this();
		addParam(config);
	}
	
	// Setter
	
	private void addParam(Config config){
		this.gridDistance = Double.parseDouble(config.getParam(GROUP_NAME, GRID_DISTANCE));
		this.minX = Double.parseDouble(config.getParam(GROUP_NAME, MIN_X));
		this.minY = Double.parseDouble(config.getParam(GROUP_NAME, MIN_Y));
		this.maxX = Double.parseDouble(config.getParam(GROUP_NAME, MAX_X));
		this.maxY = Double.parseDouble(config.getParam(GROUP_NAME, MAX_Y));
		this.numberOfAgents = Integer.parseInt(config.getParam(GROUP_NAME, NUMBER_OF_AGENTS));
		this.numberOfPlans = Integer.parseInt(config.getParam(GROUP_NAME, NUMBER_OF_PLANS));
		
		// from config
		this.network = config.getParam("network", "inputNetworkFile");
		this.outputDir = config.getParam("controler", "outputDirectory");
	}
	
	@Override
	public void addParam(final String key, final String value) {
		
		if (GRID_DISTANCE.equals(key)) {
			this.gridDistance = Double.parseDouble(value);
		} else if (MIN_X.equals(key)) {
			this.minX = Double.parseDouble(value);
		} else if (MIN_Y.equals(key)) {
			this.minY = Double.parseDouble(value);
		} else if (MAX_X.equals(key)) {
			this.maxX = Double.parseDouble(value);
		} else if (MAX_Y.equals(key)) {
			this.maxY = Double.parseDouble(value);
		} else if (NUMBER_OF_AGENTS.equals(key)) {
			this.numberOfAgents = Integer.parseInt(value);
		} else if (NUMBER_OF_PLANS.equals(key)) {
			this.numberOfPlans = Integer.parseInt(value);
		}
		
		
	}
	
	// Getter
	
	public double getGridDistance() {
		return this.gridDistance;
	}	

	public String getNetwork() {
		return this.network;
	}

	public double getMinX() {
		return this.minX;
	}

	public double getMinY() {
		return this.minY;
	}

	public double getMaxX() {
		return this.maxX;
	}

	public double getMaxY() {
		return this.maxY;
	}

	public int getNumberOfAgents() {
		return this.numberOfAgents;
	}
	
	public int getNumberOfPlans() {
		return this.numberOfPlans;
	}

	public String getOutputDir() {
		return this.outputDir;
	}

	public String getCurrentOutputBase() {
		return this.currentOutputBase;
	}

	public String getNextOutputBase() {
		return this.nextOutputBase;
	}

	

	@Override
	public TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		
		map.put(GRID_DISTANCE, Double.toString(this.gridDistance));
		map.put(MIN_X, Double.toString(this.minX));
		map.put(MIN_Y, Double.toString(this.minY));
		map.put(MAX_X, Double.toString(this.maxX));
		map.put(MAX_Y, Double.toString(this.maxY));
		map.put(NUMBER_OF_AGENTS, Double.toString(this.numberOfAgents));
		map.put(NUMBER_OF_PLANS, Double.toString(this.numberOfPlans));
		
		return map;
	}
	
	@Override
	public final Map<String, String> getComments() {
		Map<String,String> map = super.getComments();
		
		map.put(GRID_DISTANCE, "Distance between two stops in cartesian coordinate system");
		map.put(MIN_X, "min x coordinate for served area");
		map.put(MIN_Y, "min y coordinate for served area");
		map.put(MAX_X, "max x coordinate for served area");
		map.put(MAX_Y, "max y coordinate for served area");
		map.put(NUMBER_OF_AGENTS, "number of drivers");
		map.put(NUMBER_OF_PLANS, "number of plans a driver remembers");

		return map;
	}

	public void setCurrentOutPutBase(String curentOutputBase) {
		this.currentOutputBase = curentOutputBase;		
	}

	public void setNextOutPutBase(String nextOutputBase) {
		this.nextOutputBase = nextOutputBase;		
	}
	
}
