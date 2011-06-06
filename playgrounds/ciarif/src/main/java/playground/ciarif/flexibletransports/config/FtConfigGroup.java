package playground.ciarif.flexibletransports.config;

import org.apache.log4j.Logger;
import org.matsim.core.config.Module;
import org.matsim.core.config.groups.PlanomatConfigGroup;

import playground.meisterk.kti.config.KtiConfigGroup.KtiConfigParameter;


public class FtConfigGroup extends Module {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final String GROUP_NAME = "ft";

	private final static Logger logger = Logger.getLogger(FtConfigGroup.class);

	
	public enum FtConfigParameter {
		
		/**
		 * constant to be added to the score of a bike leg.
		 * represents fixed costs of walk access/egress to the bike
		 * value is calculated as marginalUtilityOfTravelTimeWalk divided by assumed sum of access and egress time
		 * TODO should be replaced by actual access/egress walk legs, 
		 * which take time in the activity plan and thus generates exact additional opportunity costs 
		 */
		CONST_BIKE("constBike", "0.0", ""),
		/**
		 * the path to the file with the travel-time matrix (VISUM-format)
		 */
		CS_STATIONS_FILENAME ("cs_stations_filename", "", ""),
		/**
		 * the path to the file with the car sharing stations and their coordinates.
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
		USE_PLANS_CALC_ROUTE_FT("usePlansCalcRouteFt", "false", ""),
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
		
		INTRAZONAL_PT_SPEED("intrazonalPtSpeed", "1.0", ""),
		
		TRAVELING_RIDE ("travelingRide", "0.0", ""), 
		
		CONST_RIDE ("constRide", "0.0", ""), 
		
		MARG_UTIL_DIST_RIDE("marginalUtilityOfDistanceRide", "0.0", ""),
		
		DISTANCE_COST_RIDE("distanceCostRide", "0.0", ""),
		
		CONST_PT ("constPt", "0.0", ""),
		
		INVALIDATE_SCORES("invalidateScores", "false", "");
		
		private final String parameterName;
		private final String defaultValue;
		private String actualValue;

		private FtConfigParameter(String parameterName, String defaultValue,
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
	
	public FtConfigGroup() {
		super(GROUP_NAME);
		for (FtConfigParameter param : FtConfigParameter.values()) {
			param.setActualValue(param.getDefaultValue());
			super.addParam(param.getParameterName(), param.getDefaultValue());
		}
	}

	
	
	@Override
	public void addParam(String param_name, String value) {
		boolean validParameterName = false;

		for (FtConfigParameter param : FtConfigParameter.values()) {

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
		return Double.parseDouble(FtConfigParameter.CONST_BIKE.getActualValue());
	}


	public String getCarSharingStationsFilename() {
		return FtConfigParameter.CS_STATIONS_FILENAME.getActualValue();
	}
	
	public void setCarSharingStationsFilename(String ptHaltestellenFilename) {
		FtConfigParameter.PT_HALTESTELLEN_FILENAME.setActualValue(ptHaltestellenFilename);
	}
	
	public String getPtHaltestellenFilename() {
		return FtConfigParameter.PT_HALTESTELLEN_FILENAME.getActualValue();
	}

	public void setPtHaltestellenFilename(String ptHaltestellenFilename) {
		FtConfigParameter.PT_HALTESTELLEN_FILENAME.setActualValue(ptHaltestellenFilename);
	}

	public String getPtTraveltimeMatrixFilename() {
		return FtConfigParameter.PT_TRAVEL_TIME_MATRIX_FILENAME.getActualValue();
	}

	public void setPtTraveltimeMatrixFilename(String ptTraveltimeMatrixFilename) {
		FtConfigParameter.PT_TRAVEL_TIME_MATRIX_FILENAME.setActualValue(ptTraveltimeMatrixFilename);
	}

	public String getWorldInputFilename() {
		return FtConfigParameter.WORLD_INPUT_FILENAME.getActualValue();
	}

	public void setWorldInputFilename(String worldInputFilename) {
		FtConfigParameter.WORLD_INPUT_FILENAME.setActualValue(worldInputFilename);
	}

	public boolean isUsePlansCalcRouteFt() {
		return Boolean.parseBoolean(FtConfigParameter.USE_PLANS_CALC_ROUTE_FT.getActualValue());
	}
	
	public void setUsePlansCalcRouteFt(boolean usePlansCalcRouteKti) {
		FtConfigParameter.USE_PLANS_CALC_ROUTE_FT.setActualValue(Boolean.toString(usePlansCalcRouteKti));
	}
	
	public void setDistanceCostCar(double newValue) {
		FtConfigParameter.DISTANCE_COST_CAR.setActualValue(Double.toString(newValue));
	}
	
	public double getDistanceCostCar() {
		return Double.parseDouble(FtConfigParameter.DISTANCE_COST_CAR.getActualValue());
	}
	
	public double getDistanceCostPtNoTravelCard() {
		return Double.parseDouble(FtConfigParameter.DISTANCE_COST_PT.getActualValue());
	}
	
	public void setDistanceCostPtNoTravelCard(double newValue) {
		FtConfigParameter.DISTANCE_COST_PT.setActualValue(Double.toString(newValue));
	}
	
	public double getDistanceCostPtUnknownTravelCard() {
		return Double.parseDouble(FtConfigParameter.DISTANCE_COST_PT_UNKNOWN.getActualValue());
	}
	
	public void setDistanceCostPtUnknownTravelCard(double newValue) {
		FtConfigParameter.DISTANCE_COST_PT_UNKNOWN.setActualValue(Double.toString(newValue));
	}
	
	public double getTravelingBike() {
		return Double.parseDouble(FtConfigParameter.TRAVELING_BIKE.getActualValue());
	}

	public void setTravelingBike(double newValue) {
		FtConfigParameter.TRAVELING_BIKE.setActualValue(Double.toString(newValue));
	}

	public double getConstCar() {
		return Double.parseDouble(FtConfigParameter.CONST_CAR.getActualValue());
	}

	public void setConstBike(double newValue) {
		FtConfigParameter.CONST_BIKE.setActualValue(Double.toString(newValue));
	}

	public void setConstCar(double newValue) {
		FtConfigParameter.CONST_CAR.setActualValue(Double.toString(newValue));
	}

	public double getTravelingRide() {
		return Double.parseDouble(FtConfigParameter.TRAVELING_RIDE.getActualValue());
	}

	public void setTravelingRide(double newValue) {
		FtConfigParameter.TRAVELING_RIDE.setActualValue(Double.toString(newValue));
	}
	public void setConstRide(double newValue) {
		FtConfigParameter.CONST_RIDE.setActualValue(Double.toString(newValue));
	}

	public double getConstRide() {
		return Double.parseDouble(FtConfigParameter.CONST_RIDE.getActualValue());
	}
	
	public void setMarginalUtilityOfDistanceRide(double newValue) {
		FtConfigParameter.MARG_UTIL_DIST_RIDE.setActualValue(Double.toString(newValue));
	}
	
	public double getMarginalUtilityOfDistanceRide() {
		return Double.parseDouble(FtConfigParameter.MARG_UTIL_DIST_RIDE.getActualValue());
	}

	public void setDistanceCostRide(double newValue) {
		FtConfigParameter.DISTANCE_COST_RIDE.setActualValue(Double.toString(newValue));
	}
	public double getDistanceCostRide() {
		return Double.parseDouble(FtConfigParameter.DISTANCE_COST_RIDE.getActualValue());
	}

	public void setConstPt(double newValue) {
		FtConfigParameter.CONST_PT.setActualValue(Double.toString(newValue));
	}

	public double getConstPt() {
		// TODO Auto-generated method stub
		return Double.parseDouble(FtConfigParameter.CONST_PT.getActualValue());
	}



	public double getIntrazonalPtSpeed() {
		// TODO Auto-generated method stub
		return Double.parseDouble(FtConfigParameter.INTRAZONAL_PT_SPEED.getActualValue());
	}

	public boolean isInvalidateScores() {
		return Boolean.parseBoolean(FtConfigParameter.INVALIDATE_SCORES.getActualValue());
	}


}
