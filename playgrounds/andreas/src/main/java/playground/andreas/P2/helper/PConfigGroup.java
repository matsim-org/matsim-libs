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

package playground.andreas.P2.helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Module;
import org.matsim.core.utils.misc.StringUtils;

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
	
	private static final String MIN_X = "minX";
	private static final String MIN_Y = "minY";
	private static final String MAX_X = "maxX";
	private static final String MAX_Y = "maxY";
	private static final String NUMBER_OF_COOPERATIVES = "numberOfCooperatives";
	private static final String COST_PER_KILOMETER = "costPerKilometer";
	private static final String EARNINGS_PER_KILOMETER_AND_PASSENGER = "earningsPerKilometerAndPassenger";
	private static final String COST_PER_VEHICLE = "costPerVehicle";
	private static final String USEFRANCHISE = "useFranchise";
	private static final String WRITESTATS = "writeStats";
	
	private static final String PMODULE = "Module_";
	private static final String PMODULE_PROBABILITY = "ModuleProbability_";
	private static final String PMODULE_PARAMETER = "ModuleParameter_";
	
	// Defaults
	private double minX = Double.MIN_VALUE;	
	private double minY = Double.MIN_VALUE;	
	private double maxX = Double.MAX_VALUE;
	private double maxY = Double.MAX_VALUE;
	private int numberOfCooperatives = 1;
	private double costPerKilometer = 0.30;
	private double earningsPerKilometerAndPassenger = 0.50;
	private double costPerVehicle = 1000.0;
	private boolean useFranchise = false;
	private boolean writeStats = false;

	// Strategies
	private final LinkedHashMap<Id, PStrategySettings> strategies = new LinkedHashMap<Id, PStrategySettings>();
	
	
	public PConfigGroup(){
		super(GROUP_NAME);
		log.info("Started...");
		log.warn("SerialVersionUID has to be checked. Current one is " + PConfigGroup.serialVersionUID);
	}
	
	// Setter
	
	@Override
	public void addParam(final String key, final String value) {
		if (MIN_X.equals(key)) {
			this.minX = Double.parseDouble(value);
		} else if (MIN_Y.equals(key)) {
			this.minY = Double.parseDouble(value);
		} else if (MAX_X.equals(key)) {
			this.maxX = Double.parseDouble(value);
		} else if (MAX_Y.equals(key)) {
			this.maxY = Double.parseDouble(value);
		} else if (NUMBER_OF_COOPERATIVES.equals(key)) {
			this.numberOfCooperatives = Integer.parseInt(value);
		} else if (COST_PER_KILOMETER.equals(key)){
			this.costPerKilometer = Double.parseDouble(value);
		} else if (EARNINGS_PER_KILOMETER_AND_PASSENGER.equals(key)){
			this.earningsPerKilometerAndPassenger = Double.parseDouble(value);
		} else if (COST_PER_VEHICLE.equals(key)){
			this.costPerVehicle = Double.parseDouble(value);
		} else if (USEFRANCHISE.equals(key)){
			this.useFranchise = Boolean.parseBoolean(value);
		} else if (WRITESTATS.equals(key)){
			this.writeStats = Boolean.parseBoolean(value);
		} else if (key != null && key.startsWith(PMODULE)) {
			PStrategySettings settings = getStrategySettings(new IdImpl(key.substring(PMODULE.length())), true);
			settings.setModuleName(value);
		} else if (key != null && key.startsWith(PMODULE_PROBABILITY)) {
			PStrategySettings settings = getStrategySettings(new IdImpl(key.substring(PMODULE_PROBABILITY.length())), true);
			settings.setProbability(Double.parseDouble(value));
		} else if (key != null && key.startsWith(PMODULE_PARAMETER)) {
			PStrategySettings settings = getStrategySettings(new IdImpl(key.substring(PMODULE_PARAMETER.length())), true);
			settings.setParameters(value);
		}
	}
	
	// Getter
	
	@Override
	public TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		
		map.put(MIN_X, Double.toString(this.minX));
		map.put(MIN_Y, Double.toString(this.minY));
		map.put(MAX_X, Double.toString(this.maxX));
		map.put(MAX_Y, Double.toString(this.maxY));
		map.put(NUMBER_OF_COOPERATIVES, Integer.toString(this.numberOfCooperatives));
		map.put(COST_PER_KILOMETER, Double.toString(this.costPerKilometer));
		map.put(EARNINGS_PER_KILOMETER_AND_PASSENGER, Double.toString(this.earningsPerKilometerAndPassenger));
		map.put(COST_PER_VEHICLE, Double.toString(this.costPerVehicle));
		map.put(USEFRANCHISE, Boolean.toString(this.useFranchise));
		map.put(WRITESTATS, Boolean.toString(this.writeStats));
		
		for (Entry<Id, PStrategySettings>  entry : this.strategies.entrySet()) {
			map.put(PMODULE + entry.getKey().toString(), entry.getValue().getModuleName());
			map.put(PMODULE_PROBABILITY + entry.getKey().toString(), Double.toString(entry.getValue().getProbability()));
			map.put(PMODULE_PARAMETER + entry.getKey().toString(), entry.getValue().getParametersAsString());
		}
		
		return map;
	}
	
	@Override
	public final Map<String, String> getComments() {
		Map<String,String> map = super.getComments();
		
		map.put(MIN_X, "min x coordinate for service area");
		map.put(MIN_Y, "min y coordinate for service area");
		map.put(MAX_X, "max x coordinate for service area");
		map.put(MAX_Y, "max y coordinate for service area");
		map.put(NUMBER_OF_COOPERATIVES, "number of cooperatives operating");
		map.put(COST_PER_KILOMETER, "cost per vehicle and kilometer travelled");
		map.put(EARNINGS_PER_KILOMETER_AND_PASSENGER, "earnings per passenger kilometer");
		map.put(COST_PER_VEHICLE, "cost to purchase or sell a vehicle");
		map.put(USEFRANCHISE, "Will use a franchise system if set to true");
		map.put(WRITESTATS, "will write statistics if set to true");
		
		for (Entry<Id, PStrategySettings>  entry : this.strategies.entrySet()) {
			map.put(PMODULE + entry.getKey().toString(), "name of strategy");
			map.put(PMODULE_PROBABILITY + entry.getKey().toString(), "probability that a strategy is applied to a given a plan. despite its name, this really is a ``weight''");
			map.put(PMODULE_PARAMETER + entry.getKey().toString(), "parameters of the strategy");
		}

		return map;
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

	public int getNumberOfCooperatives() {
		return this.numberOfCooperatives;
	}
	
	public double getCostPerKilometer() {
		return this.costPerKilometer;
	}

	public double getEarningsPerKilometerAndPassenger() {
		return this.earningsPerKilometerAndPassenger;
	}
		
	public double getCostPerVehicle() {
		return this.costPerVehicle;
	}
	
	public boolean getUseFranchise() {
		return this.useFranchise;
	}
	
	public boolean getWriteStats() {
		return this.writeStats;
	}
	
	public Collection<PStrategySettings> getStrategySettings() {
		return this.strategies.values();
	}
	
	private PStrategySettings getStrategySettings(final Id index, final boolean createIfMissing) {
		PStrategySettings settings = this.strategies.get(index);
		if (settings == null && createIfMissing) {
			settings = new PStrategySettings(index);
			this.strategies.put(index, settings);
		}
		return settings;
	}
	
	public static class PStrategySettings{
		private Id id;
		private double probability = -1.0;
		private String moduleName = null;
		private String[] parameters = null;

		public PStrategySettings(final Id id) {
			this.id = id;
		}

		public void setProbability(final double probability) {
			this.probability = probability;
		}

		public double getProbability() {
			return this.probability;
		}

		public void setModuleName(final String moduleName) {
			this.moduleName = moduleName;
		}

		public String getModuleName() {
			return this.moduleName;
		}		

		public Id getId() {
			return this.id;
		}

		public void setId(final Id id) {
			this.id = id;
		}
		
		public ArrayList<String> getParametersAsArrayList(){
			ArrayList<String> list = new ArrayList<String>();
			for (int i = 0; i < this.parameters.length; i++) {
				list.add(this.parameters[i]);
			}
			return list;
		}
		
		public String getParametersAsString() {
			StringBuffer strBuffer = new StringBuffer();
			
			if (this.parameters.length > 0) {
		        strBuffer.append(this.parameters[0]);
		        for (int i = 1; i < this.parameters.length; i++) {
		            strBuffer.append(",");
		            strBuffer.append(this.parameters[i]);
		        }
		    }
			return strBuffer.toString();
		}

		public void setParameters(String parameter) {
			if (parameter != null) {
				String[] parts = StringUtils.explode(parameter, ',');
				this.parameters = new String[parts.length];
				for (int i = 0, n = parts.length; i < n; i++) {
					this.parameters[i] = parts[i].trim().intern();
				}
			}			
		}

	}
}