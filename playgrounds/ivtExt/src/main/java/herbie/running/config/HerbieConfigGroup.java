/* *********************************************************************** *
 * project: org.matsim.*
 * KtiConfigGroup.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package herbie.running.config;

import org.apache.log4j.Logger;
import org.matsim.core.config.Module;
import org.matsim.core.config.groups.PlanomatConfigGroup;

public class HerbieConfigGroup extends Module {

	private static final long serialVersionUID = 1L;

	public static final String GROUP_NAME = "herbie";

	private final static Logger logger = Logger.getLogger(HerbieConfigGroup.class);

	/**
	 * TODO extract interface/abstract class from his config group and PlanomatConfigGroup
	 * 
	 * @author meisterk
	 *
	 */
	public enum KtiConfigParameter {
		/**
		 * distance cost for mode "pt", without travel card
		 * unit: CHF/km
		 */
		DISTANCE_COST_PT("distanceCostPtNoTravelCard", "0.0", ""),
		/**
		 * distance cost for mode "pt", with travel card "unknown"
		 * unit: CHF/km
		 */
		DISTANCE_COST_PT_UNKNOWN("distanceCostPtUnknownTravelCard", "0.0", ""),

		/**
		 * speed of legs with mode "pt" for intrazonal legs. 
		 * The VISUM travel time matrix contains no travel time data for intrazonal trips, 
		 * because there exists no intrazonal traffic in VISUM. When the travel time of
		 * an intrazonal trip is estimated, the speed given here is used.
		 * unit: [m/s] 
		 */
		INTRAZONAL_PT_SPEED("intrazonalPtSpeed", "1.0", ""), 
		/**
		 * indicates whether plan scores are invalidated (set to null) when reading plans
		 * used in simulation of initial datapuls population where plans have scores with an unknown meaning
		 * this generates trouble when using a learning rate different to 1.0
		 */
		MARGINAL_DISTANCE_COST_RATE_BIKE("monetaryDistanceCostRateBike", "0.0", ""),
		
		INVALIDATE_SCORES("invalidateScores", "false", "");
		
		private final String parameterName;
		private final String defaultValue;
		private String actualValue;

		private KtiConfigParameter(String parameterName, String defaultValue,
				String actualValue) {
			this.parameterName = parameterName;
			this.defaultValue = defaultValue;
			this.actualValue = actualValue;
		}

		public String getActualValue() {
			return actualValue;
		}

		public void setActualValue(String actualValue) {
			this.actualValue = actualValue;
		}

		public String getParameterName() {
			return parameterName;
		}

		public String getDefaultValue() {
			return defaultValue;
		}
		
	}
	
	public HerbieConfigGroup() {
		super(GROUP_NAME);
		for (KtiConfigParameter param : KtiConfigParameter.values()) {
			param.setActualValue(param.getDefaultValue());
			super.addParam(param.getParameterName(), param.getDefaultValue());
		}
	}

	
	
	@Override
	public void addParam(String param_name, String value) {
		boolean validParameterName = false;

		for (KtiConfigParameter param : KtiConfigParameter.values()) {

			if (param.getParameterName().equals(param_name)) {
				param.setActualValue(value);
				super.addParam(param_name, value);
				validParameterName = true;
				continue;
			}

		}
		if (!validParameterName) {
			logger.warn("Unknown parameter name in module " + 
					PlanomatConfigGroup.GROUP_NAME + ": \"" + param_name + "\". It is ignored.");
		}
	}	
	public double getDistanceCostPtNoTravelCard() {
		return Double.parseDouble(KtiConfigParameter.DISTANCE_COST_PT.getActualValue());
	}
	
	public void setDistanceCostPtNoTravelCard(double newValue) {
		KtiConfigParameter.DISTANCE_COST_PT.setActualValue(Double.toString(newValue));
	}
	
	public double getDistanceCostPtUnknownTravelCard() {
		return Double.parseDouble(KtiConfigParameter.DISTANCE_COST_PT_UNKNOWN.getActualValue());
	}
	
	public void setDistanceCostPtUnknownTravelCard(double newValue) {
		KtiConfigParameter.DISTANCE_COST_PT_UNKNOWN.setActualValue(Double.toString(newValue));
	}
	
	public double getIntrazonalPtSpeed() {
		return Double.parseDouble(KtiConfigParameter.INTRAZONAL_PT_SPEED.getActualValue());
	}

	public void setIntrazonalPtSpeed(double newValue) {
		KtiConfigParameter.INTRAZONAL_PT_SPEED.setActualValue(Double.toString(newValue));
	}

	public boolean isInvalidateScores() {
		return Boolean.parseBoolean(KtiConfigParameter.INVALIDATE_SCORES.getActualValue());
	}
	
	public double getMarginalDistanceCostRateBike(){
		return Double.parseDouble(KtiConfigParameter.MARGINAL_DISTANCE_COST_RATE_BIKE.getActualValue());
	}
	
	public void setDistanceCostBike(double newValue){
		KtiConfigParameter.MARGINAL_DISTANCE_COST_RATE_BIKE.setActualValue(Double.toString(newValue));
	}
}
