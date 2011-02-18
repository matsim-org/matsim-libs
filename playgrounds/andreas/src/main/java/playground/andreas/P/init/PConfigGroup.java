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
public class PConfigGroup extends Module{
	
	/**
	 * TODO [AN] This one has to be checked
	 */
	private static final long serialVersionUID = 4840713748058034511L;

	private static final Logger log = Logger.getLogger(PConfigGroup.class);
	
	// Tags
	
	public static final String GROUP_NAME = "p";
	
	private static final String GRID_DISTANCE = "gridDistance";	
	
	// Defaults
	
	private double gridDistance = -1.0;	
	
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
	}
	
	@Override
	public void addParam(final String key, final String value) {
		
		if (GRID_DISTANCE.equals(key)) {
			this.gridDistance = Double.parseDouble(value);
		} //else if (OFFSET_PT.equals(key)) {
		//	this.offsetPt = Double.parseDouble(value);
		//} else if (OFFSET_RIDE.equals(key)) {
		
		
	}
	
	// Getter
	
	public double getGridDistance() {
		return this.gridDistance;
	}	

	@Override
	public TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		
		map.put(GRID_DISTANCE, Double.toString(this.gridDistance));
		
		return map;
	}
	
	@Override
	public final Map<String, String> getComments() {
		Map<String,String> map = super.getComments();
		
		String offsetDefaultMessage = "[unit_of_money/leg] money needed in order to start a trip (leg)";
		
		map.put(GRID_DISTANCE, "Distance between two stops in cartesian coordinate system");

		return map;
	}
	
}
