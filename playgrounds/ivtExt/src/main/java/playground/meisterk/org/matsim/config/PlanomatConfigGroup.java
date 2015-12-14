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

package playground.meisterk.org.matsim.config;

import org.apache.log4j.Logger;
import org.matsim.core.config.ConfigGroup;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * Provides access to planomat config parameters.
 *
 * @author meisterk
 *
 */
public class PlanomatConfigGroup extends ConfigGroup {

	public static final String GROUP_NAME = "planomat";

	/**
	 * Enumeration of different specifications of traffic flow simulations how trips in activity plans are interpreted.
	 *
	 * @author meisterk
	 *
	 */
	public static enum SimLegInterpretation {
		/**
		 * Use this value if events should be interpreted the way they are simulated in traffic flow simulations based on the specification in:<br>
		 * Cetin, N. (2005) Large-scale parallel graph-based simulations, Ph.D. Thesis, ETH Zurich, Zurich.<br>
		 * <ul>
		 * <li> The link of the origin activity is not simulated, and thus not included in this leg travel time estimation.
		 * <li> The link of the destination activity is simulated, and thus has to be included in this leg travel time estimation.
		 * </ul>
		 */
		CetinCompatible,
		/**
		 * Use this value if events should be interpreted the way they are simulated in traffic flow simulations based on the specification in:<br>
		 * Charypar, D., K. W. Axhausen and K. Nagel (2007)
		 * Event-driven queue-based traffic flow microsimulation,
		 * paper presented at the 86th Annual Meeting of the Transportation Research Board, Washington, D.C., January 2007.<br>
		 * <ul>
		 * <li> The link of the origin activity is simulated, and thus has to be included in this leg travel time estimation.
		 * <li> The link of the destination activity is not simulated, and thus not included in this leg travel time estimation.
		 * </ul>
		 */
		CharyparEtAlCompatible;
	}

	public static enum RoutingCapability {
		fixedRoute,
		/**
		 * @deprecated The related algorithm should not be used because it has too a long computation time to be useful.
		 */
		linearInterpolation;
	}

	public static enum TripStructureAnalysisLayerOption {facility,link}

	/**
	 * Holds all planomat parameter names, and their default values.
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
		POPSIZE("populationSize", Integer.toString(Integer.MIN_VALUE)),
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
		JGAP_MAX_GENERATIONS("jgapMaxGenerations", "100"),
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
		POSSIBLE_MODES("possibleModes", ""),
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
		LEVEL_OF_TIME_RESOLUTION("levelOfTimeResolution", "7"),
		/**
		 * Defines how events should be interpreted.
		 * <h3>Possible values</h3>
		 * <ul>
		 * <li><code>CetinCompatible</code> for use with java mobsim</li>
		 * <li><code>CharyparEtAlCompatibleLegTravelTimeEstimator</code> for use with deqsim implementations</li>
		 * </ul>
		 * <h3>Default value</h3>
		 * <code>CetinCompatible</code>
		 * <h3>Notes</h3>
		 * Different implementations of traffic flow simulations use different interpretations of trips.
		 * Planomat has to use the same interpretation as the used traffic flow simulation.
		 */
		SIM_LEG_INTERPRETATION("simLegInterpretation", PlanomatConfigGroup.SimLegInterpretation.CetinCompatible.toString()),
		/**
		 * TODO comment that
		 */
		ROUTING_CAPABILITY("routingCapability", PlanomatConfigGroup.RoutingCapability.fixedRoute.toString()),
		/**
		 * Enables logging in order to get some insight to the planomat functionality.
		 * <h3>Possible values</h3>
		 * "false", "true"
		 * <h3>Default value</h3>
		 * "false"
		 */
		DO_LOGGING("doLogging", "false"),
		/**
		 * This parameter can be used to specify the layer on which basis the trip structure of a plan is analysed.
		 * <h3>Possible values</h3>
		 * "link", "facility"
		 * <h3>Default value</h3>
		 * "facility"
		 * <h3>To do</h3>
		 * TODO This parameter does not really belong to the planomat config group, but can be used by whatever algorithm. Might be moved to {@link GlobalConfigGroup}.
		 */
		TRIP_STRUCTURE_ANALYSIS_LAYER("tripStructureAnalysisLayer", PlanomatConfigGroup.TripStructureAnalysisLayerOption.facility.toString());


		private final String parameterName;
		private final String defaultValue;

		private PlanomatConfigParameter(final String parameterName, final String defaultValue) {
			this.parameterName = parameterName;
			this.defaultValue = defaultValue;
		}

		/**
		 * @return the default string value of this parameter
		 */
		public String getDefaultValue() {
			return this.defaultValue;
		}

		/**
		 * @return the identifier of this parameter
		 */
		public String getParameterName() {
			return this.parameterName;
		}

	}

	private final static Logger logger = Logger.getLogger(PlanomatConfigGroup.class);


	public PlanomatConfigGroup() {
		super(PlanomatConfigGroup.GROUP_NAME);

		for (PlanomatConfigParameter param : PlanomatConfigParameter.values()) {
			super.addParam(param.getParameterName(), param.getDefaultValue());
		}

	}

	@Override
	public void addParam(final String param_name, final String value) {
		// emulate previous behavior of reader (ignore null values at reading). td Apr'15
		if ( "null".equalsIgnoreCase( value ) ) return;

		boolean validParameterName = false;

		for (PlanomatConfigParameter param : PlanomatConfigParameter.values()) {

			if (param.getParameterName().equals(param_name)) {
				super.addParam(param_name, value);
				validParameterName = true;
				continue;
			}

		}

		if (!validParameterName) {
			logger.warn("Unknown parameter name in module " + PlanomatConfigGroup.GROUP_NAME + ": \"" + param_name + "\". It is ignored.");
		}

	}

	public int getJgapMaxGenerations() {
		return Integer.parseInt(this.getParams().get(PlanomatConfigParameter.JGAP_MAX_GENERATIONS.getParameterName()));
	}

	/**
	 * set representation of the possible modes
	 */
	private TreeSet<String> cachedPossibleModes = null;

	public Set<String> getPossibleModes() {

		// the lazy initialization of this.cachedPossibleModes has to be synchronized because several planomat may share access to the only instance of PlanomatConfigGroup
		synchronized (this) {
			if (this.cachedPossibleModes == null) {

				this.cachedPossibleModes = new TreeSet<String>();

				if (!super.getParams().get(PlanomatConfigParameter.POSSIBLE_MODES.getParameterName()).equals(PlanomatConfigParameter.POSSIBLE_MODES.getDefaultValue())) {
					String[] possibleModesStringArray = super.getParams().get(PlanomatConfigParameter.POSSIBLE_MODES.getParameterName()).split(",");
					for (int ii = 0; ii < possibleModesStringArray.length; ii++) {
						this.cachedPossibleModes.add(possibleModesStringArray[ii].intern());
					}
				}

			}
		}
		return Collections.unmodifiableSet(this.cachedPossibleModes);

	}

	public void setPossibleModes(final String possibleModes) {
		super.addParam(PlanomatConfigParameter.POSSIBLE_MODES.getParameterName(), possibleModes);
	}

	public SimLegInterpretation getSimLegInterpretation() {
		return SimLegInterpretation.valueOf(super.getParams().get(PlanomatConfigParameter.SIM_LEG_INTERPRETATION.getParameterName()));
	}

	public RoutingCapability getRoutingCapability() {
		return RoutingCapability.valueOf(super.getParams().get(PlanomatConfigParameter.ROUTING_CAPABILITY.getParameterName()));
	}

	public void setRoutingCapability(final PlanomatConfigGroup.RoutingCapability routingCapability) {
		super.addParam(PlanomatConfigParameter.ROUTING_CAPABILITY.getParameterName(), routingCapability.toString());
	}

	public int getLevelOfTimeResolution() {
		return Integer.parseInt(super.getParams().get(PlanomatConfigParameter.LEVEL_OF_TIME_RESOLUTION.getParameterName()));
	}

	public boolean isDoLogging() {
		return Boolean.parseBoolean(super.getParams().get(PlanomatConfigParameter.DO_LOGGING.getParameterName()));
	}

	public void setDoLogging(final boolean doLogging) {
		super.addParam(PlanomatConfigParameter.DO_LOGGING.getParameterName(), Boolean.toString(doLogging));
	}

	public int getPopSize() {
		return Integer.parseInt(super.getParams().get(PlanomatConfigParameter.POPSIZE.getParameterName()));
	}

	public void setPopSize(final int i) {
		super.addParam(PlanomatConfigParameter.POPSIZE.getParameterName(), Integer.toString(i));
	}

	public void setJgapMaxGenerations(final int i) {
		super.addParam(PlanomatConfigParameter.JGAP_MAX_GENERATIONS.getParameterName(), Integer.toString(i));
	}

	public PlanomatConfigGroup.TripStructureAnalysisLayerOption getTripStructureAnalysisLayer() {
		return TripStructureAnalysisLayerOption.valueOf(super.getParams().get(PlanomatConfigParameter.TRIP_STRUCTURE_ANALYSIS_LAYER.getParameterName()));
	}

	public void setTripStructureAnalysisLayer(final PlanomatConfigGroup.TripStructureAnalysisLayerOption newValue) {
		super.addParam(PlanomatConfigParameter.TRIP_STRUCTURE_ANALYSIS_LAYER.getParameterName(), newValue.toString());
	}

	public void setSimLegInterpretation(final PlanomatConfigGroup.SimLegInterpretation newValue) {
		super.addParam(PlanomatConfigParameter.SIM_LEG_INTERPRETATION.getParameterName(), newValue.toString());
	}

}
