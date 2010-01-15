/* *********************************************************************** *
 * project: org.matsim.*
 * PopulationConvergenceConfigGroup.java
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

package playground.meisterk.phd.config;

import org.apache.log4j.Logger;
import org.matsim.core.config.Module;

/**
 * Holds config parameters for population convergence analysis.
 * 
 * @author meisterk
 *
 */
@SuppressWarnings("serial")
public class PopulationConvergenceConfigGroup extends Module {

	public static final String GROUP_NAME = "populationConvergence";

	private final static Logger logger = Logger.getLogger(PopulationConvergenceConfigGroup.class);

	public enum PopulationConvergenceConfigParameter {
		
		/**
		 * The probability that the selected plan is the best of all plans of the agent.
		 * <h3>Possible values</h3>
		 * 0.0 >= alpha_selected >= 1.0
		 * <h3>Default value</h3>
		 * "0.95"
		 */
		ALPHA_SELECTED("alpha_selected", "0.95", "");

		private final String parameterName;
		private final String defaultValue;
		private String actualValue;

		private PopulationConvergenceConfigParameter(String parameterName,
				String defaultValue, String actualValue) {
			this.parameterName = parameterName;
			this.defaultValue = defaultValue;
			this.actualValue = actualValue;
		}

		/**
		 * @return the default string value of this parameter
		 */
		public String getDefaultValue() {
			return defaultValue;
		}

		/**
		 * @return the actual string value of this parameter
		 */
		public String getActualValue() {
			return actualValue;
		}

		/**
		 * @return the identifier of this parameter
		 */
		public String getParameterName() {
			return parameterName;
		}

		/**
		 * Sets the actual value of this parameter.
		 * 
		 * @param actualValue the new value of this parameter
		 */
		public void setActualValue(String actualValue) {
			this.actualValue = actualValue;
		}

		
	}

	public PopulationConvergenceConfigGroup() {
		super(PopulationConvergenceConfigGroup.GROUP_NAME);
		
		for (PopulationConvergenceConfigParameter param : PopulationConvergenceConfigParameter.values()) {
			param.setActualValue(param.getDefaultValue());
			super.addParam(param.getParameterName(), param.getDefaultValue());
		}
	}

	@Override
	public void addParam(final String param_name, final String value) {

		boolean validParameterName = false;

		for (PopulationConvergenceConfigParameter param : PopulationConvergenceConfigParameter.values()) {

			if (param.getParameterName().equals(param_name)) {
				param.setActualValue(value);
				super.addParam(param_name, value);
				validParameterName = true;
				continue;
			}

		}

		if (!validParameterName) {
			logger.warn("Unknown parameter name in module " + PopulationConvergenceConfigGroup.GROUP_NAME + ": \"" + param_name + "\". It is ignored.");
		}

	}

	public double getAlphaSelected() {
		return Double.parseDouble(PopulationConvergenceConfigParameter.ALPHA_SELECTED.getActualValue());
	}

}
