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

/**
 * Provides access to planomat config parameters.
 * 
 * @author meisterk
 *
 */
public class PlanomatConfigGroup extends Module {

	private static final long serialVersionUID = 1L;

	public static final String GROUP_NAME = "planomat";

	public static final String OPTIMIZATION_TOOLBOX_JGAP = "jgap";

	public static final String CETIN_COMPATIBLE = "org.matsim.planomat.costestimators.CetinCompatibleLegTravelTimeEstimator";
	public static final String CHARYPAR_ET_AL_COMPATIBLE = "org.matsim.planomat.costestimators.CharyparEtAlCompatibleLegTravelTimeEstimator";

	/**
	 * Holds all planomat parameter names, their default and their actual values.
	 * 
	 * @author meisterk
	 */
	public enum PlanomatConfigParameter {

//		OPTIMIZATION_TOOLBOX("optimizationToolbox", PlanomatConfigGroup.OPTIMIZATION_TOOLBOX_JGAP, ""),
		/**
		 * The population size of the GA.
		 * <h3>Possible values</h3>
		 * Any positive integer > 0.
		 * <h3>Default value</h3>
		 * "10"
		 */
		POPSIZE("populationSize", "10", ""),
		/**
		 * Number of generations the GA will evolve.
		 * <h3>Possible values</h3>
		 * Any positive integer.
		 * <h3>Default value</h3>
		 * "100"
		 * <h3>Notes</h3>
		 * The more generations are evolved, the more likely the GA finds the optimal solution.
		 * The more generations are evolved, the longer is the required computing time.
		 * This stop criterion is the simplest possible one.
		 * It does not respect different sizes of GA problems, e.g. activity plans with different numbers of activities.
		 * It will be replaced soon by a more sophisticated calculation of the number of required generations, prospectively
		 * the approach of Greenhalgh, D. und S. Marshall (2000) Convergence criteria for genetic algorithms, SIAM Journal
		 * on Computing, 30 (1) 269–282.
		 */
		JGAP_MAX_GENERATIONS("jgapMaxGenerations", "100", ""),
		/**
		 * Defines the choice set for leg modes.
		 * <h3>Examples of values</h3>
		 * <ul>
		 * <li>"car"</li>
		 * <li>"car,pt"</li>
		 * <li>"car,pt,walk"</li>
		 * </ul>
		 * <h3>Default value</h3>
		 * ""
		 * <h3>Notes</h3>
		 * Planomat will produce no other leg modes than those listed in the value of this parameter. 
		 * When set to its default value, leg modes remain untouched, and planomat will only perform time optimization.
		 */
		POSSIBLE_MODES("possibleModes", "", ""),
		/**
		 * Exponent for the number of time bins for the discrete encoding of activity durations.
		 * <h3>Possible values</h3>
		 * Any positive integer.
		 * <h3>Default value</h3>
		 * "7"
		 * <h3>Notes</h3>
		 * The maximum possible duration of an activity is 24 hours. This duration is separated into time bins. 
		 * The number of time bins is 2^levelOfTimeResolution. So a value of 6 gives 2^6 = 64 time bins.
		 * The higher this parameter is chosen, the shorter is a time bin and the more precise the optimal solution can be approached.
		 * It is suggested to leave this parameter at its default value. A change in it becomes useful only if the above mentioned
		 * more sophisticated calculation of the number of required generations is implemented.
		 */
		LEVEL_OF_TIME_RESOLUTION("levelOfTimeResolution", "7", ""),
		/**
		 * Defines how events should be interpreted.
		 * <h3>Possible values</h3>
		 * The parameter value is the class name of an implementation of the interface {@link LegTravelTimeEstimator}. The following values are possible:
		 * <ul>
		 * <li>{@link org.matsim.planomat.costestimators.CetinCompatibleLegTravelTimeEstimator} for use with java mobsim</li>
		 * <li>{@link org.matsim.planomat.costestimators.CharyparEtAlCompatibleLegTravelTimeEstimator} for use with deqsim implementations</li>
		 * </ul>
		 * <h3>Default value</h3>
		 * {@link org.matsim.planomat.costestimators.CetinCompatibleLegTravelTimeEstimator}
		 * <h3>Notes</h3>
		 * Different implementations of traffic flow simulations use different interpretations of trips.  
		 * Planomat has to use the same interpretation as the used traffic flow simulation.
		 */
		LEG_TRAVEL_TIME_ESTIMATOR_NAME("legTravelTimeEstimator", PlanomatConfigGroup.CETIN_COMPATIBLE, ""),
		/**
		 * Enables logging in order to get some insight to the planomat functionality.
		 * <h3>Possible values</h3>
		 * "false", "true"
		 * <h3>Default value</h3>
		 * "false"
		 */
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

	}

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

//	public String getOptimizationToolbox() {
//		return PlanomatConfigParameter.OPTIMIZATION_TOOLBOX.getActualValue();
//	}

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
