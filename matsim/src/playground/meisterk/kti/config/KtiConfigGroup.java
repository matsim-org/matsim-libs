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

package playground.meisterk.kti.config;

import org.apache.log4j.Logger;
import org.matsim.core.config.Module;
import org.matsim.core.config.groups.PlanomatConfigGroup;

public class KtiConfigGroup extends Module {

	public static final String GROUP_NAME = "kti";

	private final static Logger logger = Logger.getLogger(KtiConfigGroup.class);

	/**
	 * TODO extract interface/abstract class from his config group and PlanomatConfigGroup
	 * 
	 * @author meisterk
	 *
	 */
	public enum KtiConfigParameter {
		
		CONST_BIKE("constBike", "0.0", ""),
		/**
		 * the path to the file with the travel-time matrix (VISUM-format)
		 */
		PT_TRAVEL_TIME_MATRIX_FILENAME("pt_traveltime_matrix_filename", "", ""),
		/**
		 * the path to the file containing the list of all pt-stops and their coordinates.
		 */
		PT_HALTESTELLEN_FILENAME("pt_haltestellen_filename", "", ""),
		/**
		 * the path to the world file containing a layer 'municipality'
		 */
		WORLD_INPUT_FILENAME("worldInputFilename", "", ""),
		/**
		 * boolean variable indicating whether the kti router should be used or not
		 */
		USE_PLANS_CALC_ROUTE_KTI("usePlansCalcRouteKti", "false", ""),
		/**
		 * distance cost for mode "car" in CHF/km
		 */
		DISTANCE_COST_CAR("distanceCostCar", "0.0", ""),
		/**
		 * distance cost for mode "pt" in CHF/km, without travel card
		 */
		DISTANCE_COST_PT("distanceCostPtNoTravelCard", "0.0", ""),
		/**
		 * distance cost for mode "pt" in CHF/km, with travel card "unknown"
		 */
		DISTANCE_COST_PT_UNKNOWN("distanceCostPtUnknownTravelCard", "0.0", "");
		
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
	
	public KtiConfigGroup() {
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
			logger.warn("Unknown parameter name in module " + PlanomatConfigGroup.GROUP_NAME + ": \"" + param_name + "\". It is ignored.");
		}

	}

	public double getConstBike() {
		return Double.parseDouble(KtiConfigParameter.CONST_BIKE.getActualValue());
	}



	public String getPtHaltestellenFilename() {
		return KtiConfigParameter.PT_HALTESTELLEN_FILENAME.getActualValue();
	}

	public void setPtHaltestellenFilename(String ptHaltestellenFilename) {
		KtiConfigParameter.PT_HALTESTELLEN_FILENAME.setActualValue(ptHaltestellenFilename);
	}

	public String getPtTraveltimeMatrixFilename() {
		return KtiConfigParameter.PT_TRAVEL_TIME_MATRIX_FILENAME.getActualValue();
	}

	public void setPtTraveltimeMatrixFilename(String ptTraveltimeMatrixFilename) {
		KtiConfigParameter.PT_TRAVEL_TIME_MATRIX_FILENAME.setActualValue(ptTraveltimeMatrixFilename);
	}

	public String getWorldInputFilename() {
		return KtiConfigParameter.WORLD_INPUT_FILENAME.getActualValue();
	}

	public void setWorldInputFilename(String worldInputFilename) {
		KtiConfigParameter.WORLD_INPUT_FILENAME.setActualValue(worldInputFilename);
	}

	public boolean isUsePlansCalcRouteKti() {
		return Boolean.parseBoolean(KtiConfigParameter.USE_PLANS_CALC_ROUTE_KTI.getActualValue());
	}
	
	public void setUsePlansCalcRouteKti(boolean usePlansCalcRouteKti) {
		KtiConfigParameter.USE_PLANS_CALC_ROUTE_KTI.setActualValue(Boolean.toString(usePlansCalcRouteKti));
	}
	
	public double getDistanceCostCar() {
		return Double.parseDouble(KtiConfigParameter.DISTANCE_COST_CAR.getActualValue());
	}
	
	public double getDistanceCostPtNoTravelCard() {
		return Double.parseDouble(KtiConfigParameter.DISTANCE_COST_PT.getActualValue());
	}
	
	public double getDistanceCostPtUnknownTravelCard() {
		return Double.parseDouble(KtiConfigParameter.DISTANCE_COST_PT_UNKNOWN.getActualValue());
	}
	
	
}
