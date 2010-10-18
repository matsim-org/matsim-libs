/* *********************************************************************** *
 * project: org.matsim.*
 * SubtourModeChoice.java
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

package org.matsim.core.replanning.modules;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanomatConfigGroup.TripStructureAnalysisLayerOption;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.misc.StringUtils;
import org.matsim.population.algorithms.ChooseRandomLegModeForSubtour;
import org.matsim.population.algorithms.PermissibleModesCalculator;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * Changes the transportation mode of all legs of one randomly chosen subtour in a plan to a randomly chosen
 * different mode given a list of possible modes.
 *
 * A subtour is a consecutive subset of a plan which starts and ends at the same link.
 * 
 * Certain modes are considered only if the choice would not require some resource to appear
 * out of thin air. For example, you can only drive your car back from work if you have previously parked it
 * there. These are called chain-based modes.
 * 
 * The assumption is that each chain-based mode requires one resource (car, bike, ...) and that this
 * resource is initially positioned at home. Home is the location of the first activity in the plan.
 * 
 * If the plan initially violates this constraint, this module may (!) repair it. 
 * 
 * @author michaz
 * 
 */
public class SubtourModeChoice extends AbstractMultithreadedModule {
	
	private final static String CONFIG_MODULE = "subtourModeChoice";
	private final static String CONFIG_PARAM_MODES = "modes";
	private final static String CONFIG_PARAM_CHAINBASEDMODES = "chainBasedModes";
	
	private final static String[] DEFAULT_CHAIN_BASED_MODES = new String[] { TransportMode.car, TransportMode.bike };
	private final static String[] DEFAULT_AVAILABLE_MODES = new String[] { TransportMode.car, TransportMode.pt, TransportMode.bike, TransportMode.walk };

	private static class AllowTheseModesForEveryone implements PermissibleModesCalculator {

		private List<String> availableModes;

		public AllowTheseModesForEveryone(String[] availableModes) {
			this.availableModes = Arrays.asList(availableModes);
		}

		@Override
		public Collection<String> getPermissibleModes(Plan plan) {
			return availableModes; 
		}

	}
	
	private PermissibleModesCalculator permissibleModesCalculator;
	private String[] chainBasedModes;
	
	public SubtourModeChoice(final Config config) {
		super(config.global().getNumberOfThreads());
		String modes = config.findParam(CONFIG_MODULE, CONFIG_PARAM_MODES);
		String chainBasedModes = config.findParam(CONFIG_MODULE, CONFIG_PARAM_CHAINBASEDMODES);
		String[] availableModes = explodeModesWithDefaults(modes, DEFAULT_AVAILABLE_MODES);
		this.chainBasedModes = explodeModesWithDefaults(chainBasedModes, DEFAULT_CHAIN_BASED_MODES);
		this.permissibleModesCalculator = new AllowTheseModesForEveryone(availableModes);
	}

	private String[] explodeModesWithDefaults(String modes, String[] defaults) {
		String[] availableModes;
		if (modes != null) {
			String[] parts = StringUtils.explode(modes, ',');
			availableModes = new String[parts.length];
			for (int i = 0, n = parts.length; i < n; i++) {
				availableModes[i] = parts[i].trim().intern();
			}
		} else {
			availableModes = defaults;
		}
		return availableModes;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		ChooseRandomLegModeForSubtour chooseRandomLegMode = new ChooseRandomLegModeForSubtour(this.permissibleModesCalculator, this.chainBasedModes, MatsimRandom.getLocalInstance());
		chooseRandomLegMode.setTripStructureAnalysisLayer(TripStructureAnalysisLayerOption.link);
		return chooseRandomLegMode;
	}

	/**
	 * Decides if a person may use a certain mode of transport. Can be used for car ownership.
	 * 
	 */
	public void setPermissibleModesCalculator(PermissibleModesCalculator permissibleModesCalculator) {
		this.permissibleModesCalculator = permissibleModesCalculator;
	}

}
