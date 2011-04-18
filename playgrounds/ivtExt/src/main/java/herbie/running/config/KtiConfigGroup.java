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
		
		/**
		 * constant to be added to the score of a bike leg.
		 * represents fixed costs of walk access/egress to the bike
		 * value is calculated as marginalUtilityOfTravelTimeWalk divided by assumed sum of accces and egress time
		 * TODO should be replaced by actual access/egress walk legs, 
		 * which take time in the activity plan and thus generates exact additional opportunity costs 
		 */
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
		 * distance cost for mode "car"
		 * unit: CHF/km
		 */
		DISTANCE_COST_CAR("distanceCostCar", "0.0", ""),
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
		 * marginal utility of travel time for mode "bike"
		 * unit: 1/h
		 */
		TRAVELING_BIKE("travelingBike", "0.0", ""), 
		/**
		 * constant to be added to the score of a car leg.
		 * represents fixed costs of walk access/egress to the car
		 * value is calculated as marginalUtilityOfTravelTimeWalk divided by assumed sum of accces and egress time
		 * TODO should be replaced by actual access/egress walk legs, 
		 * which take time in the activity plan and thus generate additional opportunity costs 
		 */
		CONST_CAR("constCar", "0.0", ""),
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
	
	public void setDistanceCostCar(double newValue) {
		KtiConfigParameter.DISTANCE_COST_CAR.setActualValue(Double.toString(newValue));
	}
	
	public double getDistanceCostCar() {
		return Double.parseDouble(KtiConfigParameter.DISTANCE_COST_CAR.getActualValue());
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
	
	public double getTravelingBike() {
		return Double.parseDouble(KtiConfigParameter.TRAVELING_BIKE.getActualValue());
	}

	public void setTravelingBike(double newValue) {
		KtiConfigParameter.TRAVELING_BIKE.setActualValue(Double.toString(newValue));
	}

	public double getConstCar() {
		return Double.parseDouble(KtiConfigParameter.CONST_CAR.getActualValue());
	}

	public void setConstBike(double newValue) {
		KtiConfigParameter.CONST_BIKE.setActualValue(Double.toString(newValue));
	}

	public void setConstCar(double newValue) {
		KtiConfigParameter.CONST_CAR.setActualValue(Double.toString(newValue));
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
	
}
