package org.matsim.config.groups;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.BasicLeg;
import org.matsim.config.Module;
import org.matsim.network.NetworkLayer;
import org.matsim.planomat.costestimators.CetinCompatibleLegTravelTimeEstimator;
import org.matsim.planomat.costestimators.CharyparEtAlCompatibleLegTravelTimeEstimator;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.router.util.TravelCost;
import org.matsim.router.util.TravelTime;

public class PlanomatConfigGroup extends Module {

	private static final long serialVersionUID = 1L;

	public static final String GROUP_NAME = "planomat";

	public static final String OPTIMIZATION_TOOLBOX_JGAP = "jgap";

	public static final String CETIN_COMPATIBLE = "org.matsim.planomat.costestimators.CetinCompatibleLegTravelTimeEstimator";
	public static final String CHARYPAR_ET_AL_COMPATIBLE = "org.matsim.planomat.costestimators.CharyparEtAlCompatibleLegTravelTimeEstimator";
	public static final String MY_RECENT_EVENTS = "org.matsim.planomat.costestimators.MyRecentEventsBasedEstimator";

	public enum PlanomatConfigParameter {

		OPTIMIZATION_TOOLBOX("optimizationToolbox", PlanomatConfigGroup.OPTIMIZATION_TOOLBOX_JGAP, ""),
		POPSIZE("populationSize", "10", ""),
		JGAP_MAX_GENERATIONS("jgapMaxGenerations", "100", ""),
		POSSIBLE_MODES("possibleModes", "", ""),
		LEVEL_OF_TIME_RESOLUTION("levelOfTimeResolution", "7", ""),
		LEG_TRAVEL_TIME_ESTIMATOR_NAME("legTravelTimeEstimator", PlanomatConfigGroup.CETIN_COMPATIBLE, ""),
		DO_LOGGING("doLogging", "false", "");

		private final String parameterName;
		private final String defaultValue;
		private String actualValue;

		private PlanomatConfigParameter(String parameterName,
				String defaultValue, String actualValue) {
			this.parameterName = parameterName;
			this.defaultValue = defaultValue;
			this.actualValue = actualValue;
		}

		public String getDefaultValue() {
			return defaultValue;
		}

		public String getActualValue() {
			return actualValue;
		}

		public String getParameterName() {
			return parameterName;
		}

		public void setActualValue(String actualValue) {
			this.actualValue = actualValue;
		}

	}

	private final static Logger logger = Logger.getLogger(PlanomatConfigGroup.class);

	public PlanomatConfigGroup() {
		super(PlanomatConfigGroup.GROUP_NAME);

		for (PlanomatConfigParameter param : PlanomatConfigParameter.values()) {
			param.setActualValue(param.getDefaultValue());
			super.addParam(param.parameterName, param.defaultValue);
		}

	}

	@Override
	public void addParam(final String param_name, final String value) {

		boolean validParameterName = false;

		for (PlanomatConfigParameter param : PlanomatConfigParameter.values()) {

			if (param.parameterName.equals(param_name)) {
				param.setActualValue(value);
				super.addParam(param_name, value);
				validParameterName = true;
				continue;
			}

		}

		if (!validParameterName) {
			logger.warn("Unknown parameter name in module " + PlanomatConfigGroup.GROUP_NAME + ": \"" + param_name + "\". It is ignored.");
		}

//		TODO add remaining config parameters to test

//		} else if (LINK_TRAVEL_TIME_ESTIMATOR.equals(param_name)) {
//		this.setLinkTravelTimeEstimatorName(value);
//		} else if (OPTIMIZATION_TOOLBOX.equals(param_name)) {
//		if (OPTIMIZATION_TOOLBOX_JGAP.equals(value)) {
//		this.setOptimizationToolbox(value);
//		} else {
//		log.error("Unknown optimization toolbox identifier. Aborting...");
//		}
//		} else if (POPSIZE.equals(param_name)) {
//		int popSize = 0;
//		try {
//		popSize = Integer.parseInt(value);
//		} catch (NumberFormatException e) {
//		// TODO Auto-generated catch block
//		e.printStackTrace();
//		}
//		if (popSize <= 0) {
//		Gbl.errorMsg("JGAP Population size must be a non-null positive integer.");
//		}
//		this.setPopSize(popSize);
//		} 

	}

	// getters/setters

	public LegTravelTimeEstimator getLegTravelTimeEstimator(TravelTime travelTime, TravelCost travelCost, DepartureDelayAverageCalculator tDepDelayCalc, NetworkLayer network) {

		LegTravelTimeEstimator legTravelTimeEstimator = null;

		if (PlanomatConfigParameter.LEG_TRAVEL_TIME_ESTIMATOR_NAME.getActualValue().equalsIgnoreCase(PlanomatConfigGroup.CETIN_COMPATIBLE)) {
			legTravelTimeEstimator = new CetinCompatibleLegTravelTimeEstimator(travelTime, travelCost, tDepDelayCalc, network);
		} else if (PlanomatConfigParameter.LEG_TRAVEL_TIME_ESTIMATOR_NAME.getActualValue().equalsIgnoreCase(PlanomatConfigGroup.CHARYPAR_ET_AL_COMPATIBLE)) {
			legTravelTimeEstimator = new CharyparEtAlCompatibleLegTravelTimeEstimator(travelTime, travelCost, tDepDelayCalc, network);
		} else {
			throw new RuntimeException("legTravelTimeEstimator value: \"" + PlanomatConfigParameter.LEG_TRAVEL_TIME_ESTIMATOR_NAME.getActualValue() + "\" is not allowed.");
		}

		return legTravelTimeEstimator;
	}

	public int getJgapMaxGenerations() {
		return Integer.parseInt(PlanomatConfigParameter.JGAP_MAX_GENERATIONS.getActualValue());
	}

	public BasicLeg.Mode[] getPossibleModes() {

		BasicLeg.Mode[] possibleModes = null; 

		if (!PlanomatConfigParameter.POSSIBLE_MODES.getActualValue().equals("")) {
			String[] possibleModesStringArray = PlanomatConfigParameter.POSSIBLE_MODES.getActualValue().split(",");
			possibleModes = new BasicLeg.Mode[possibleModesStringArray.length];
			for (int ii=0; ii < possibleModesStringArray.length; ii++) {
				possibleModes[ii] = BasicLeg.Mode.valueOf(possibleModesStringArray[ii]);
			}
		}
		return possibleModes;

	}

	public void setPossibleModes(String possibleModes) {
		PlanomatConfigParameter.POSSIBLE_MODES.setActualValue(possibleModes);
	}

	public String getLegTravelTimeEstimatorName() {
		return PlanomatConfigParameter.LEG_TRAVEL_TIME_ESTIMATOR_NAME.getActualValue();
	}

	public int getLevelOfTimeResolution() {
		return Integer.parseInt(PlanomatConfigParameter.LEVEL_OF_TIME_RESOLUTION.getActualValue());
	}

	public boolean isDoLogging() {
		return Boolean.parseBoolean(PlanomatConfigParameter.DO_LOGGING.getActualValue());
	}

	public String getOptimizationToolbox() {
		return PlanomatConfigParameter.OPTIMIZATION_TOOLBOX.getActualValue();
	}

	public int getPopSize() {
		return Integer.parseInt(PlanomatConfigParameter.POPSIZE.getActualValue());
	}

	public void setPopSize(int i) {
		PlanomatConfigParameter.POPSIZE.setActualValue(Integer.toString(i));
	}

	public void setJgapMaxGenerations(int i) {
		PlanomatConfigParameter.JGAP_MAX_GENERATIONS.setActualValue(Integer.toString(i));
	}

}
