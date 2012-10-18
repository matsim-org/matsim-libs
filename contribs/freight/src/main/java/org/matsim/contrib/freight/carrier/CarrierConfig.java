/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.contrib.freight.carrier;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.core.config.Module;
import org.matsim.core.config.groups.PlansConfigGroup;

public class CarrierConfig {
	
	public static class VehiclesConfigGroup extends Module {

		public static final String GROUP_NAME = "vehicles";

		private static final String INPUT_FILE = "vehicleFile";
		
		private String inputFile = null;
		

		public VehiclesConfigGroup() {
			super(GROUP_NAME);
		}

		@Override
		public String getValue(final String key) {
			if (INPUT_FILE.equals(key)) {
				return getInputFile();
			}
			else {
				throw new IllegalArgumentException(key);
			}
		}

		@Override
		public void addParam(final String key, final String value) {
			if (INPUT_FILE.equals(key)) {
				setInputFile(value.replace('\\', '/'));
			} else {
				throw new IllegalArgumentException(key);
			}
		}

		@Override
		public Map<String, String> getComments() {
			Map<String, String> comments = super.getComments();
			return comments;
		}

		@Override
		public final TreeMap<String, String> getParams() {
			TreeMap<String, String> map = new TreeMap<String, String>();
			addParameterToMap(map, INPUT_FILE);
			return map;
		}

		/* direct access */

		public String getInputFile() {
			return this.inputFile;
		}

		public void setInputFile(final String inputFile) {
			this.inputFile = inputFile;
		}

	}
	
	private final TreeMap<String, Module> modules = new TreeMap<String, Module>();
	
	private PlansConfigGroup plans = null;
	
	private VehiclesConfigGroup vehicles = null;

	public boolean isWithinDayReScheduling() {
		return withinDayReScheduling;
	}

	public void setWithinDayReScheduling(boolean withinDayReScheduling) {
		this.withinDayReScheduling = withinDayReScheduling;
	}

	private boolean withinDayReScheduling = false;
	
	public void addCoreModules(){
		plans = new PlansConfigGroup();
		this.modules.put(PlansConfigGroup.GROUP_NAME, this.plans);
		
		vehicles = new VehiclesConfigGroup();
		this.modules.put(VehiclesConfigGroup.GROUP_NAME, this.vehicles);
	}

	public PlansConfigGroup plans() {
		return plans;
	}

	public VehiclesConfigGroup vehicles() {
		return vehicles;
	}
		
	
}
