/* *********************************************************************** *
 * project: org.matsim.*
 * MeisterkConfigGroup.java
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

package playground.meisterk.org.matsim.config.groups;

import java.util.EnumSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Module;

public class MeisterkConfigGroup extends Module {

	/**
	 * default serial version uid
	 */
	private static final long serialVersionUID = 1L;
	
	public static final String GROUP_NAME = "meisterk";

	private final static Logger logger = Logger.getLogger(MeisterkConfigGroup.class);

	public MeisterkConfigGroup() {
		super(MeisterkConfigGroup.GROUP_NAME);
		for (MeisterkConfigParameter param : MeisterkConfigParameter.values()) {
			param.setActualValue(param.getDefaultValue());
			super.addParam(param.parameterName, param.defaultValue);
		}
	}
	
	public enum MeisterkConfigParameter {
		
		CHAIN_BASED_MODES("chainBasedModes", "car,bike", ""),
		INPUT_SECOND_NETWORK_FILE("inputSecondNetworkFile", "", "");
		
		private final String parameterName;
		private final String defaultValue;
		private String actualValue;
		
		private MeisterkConfigParameter(String parameterName,
				String defaultValue, String actualValue) {
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
	
	@Override
	public void addParam(String param_name, String value) {

		boolean validParameterName = false;

		for (MeisterkConfigParameter param : MeisterkConfigParameter.values()) {

			if (param.getParameterName().equals(param_name)) {
				param.setActualValue(value);
				super.addParam(param_name, value);
				validParameterName = true;
				continue;
			}

		}

		if (!validParameterName) {
			logger.warn("Unknown parameter name in module " + MeisterkConfigGroup.GROUP_NAME + ": \"" + param_name + "\". It is ignored.");
		}

	}
	
	private EnumSet<TransportMode> cachedChainBasedModes = null;

	public EnumSet<TransportMode> getChainBasedModes() {

		if (this.cachedChainBasedModes == null) {
			this.cachedChainBasedModes = EnumSet.noneOf(TransportMode.class);
			
			if (MeisterkConfigParameter.CHAIN_BASED_MODES.getActualValue().length() > 0) {
				String[] chainBasedModesStringArray = MeisterkConfigParameter.CHAIN_BASED_MODES.getActualValue().split(",");
				for (int ii=0; ii < chainBasedModesStringArray.length; ii++) {
					this.cachedChainBasedModes.add(TransportMode.valueOf(chainBasedModesStringArray[ii]));
				}
			}
		}
		
		return cachedChainBasedModes;
	}
	
	public void setChainBasedModes(String chainBasedModes) {
		MeisterkConfigParameter.CHAIN_BASED_MODES.setActualValue(chainBasedModes);
	}
	
}
