/* *********************************************************************** *
 * project: org.matsim.*
 * ChangeTourMode.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.yu.test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanomatConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.utils.misc.StringUtils;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.meisterk.org.matsim.population.algorithms.PlanAnalyzeTourModeChoiceSet;

/**
 * uses {@code PlanAnalyzeTourModeChoiceSet} to realize "subtour mode choice"
 * 
 * @author yu
 * 
 */
public class SubTourModeChoice extends AbstractMultithreadedModule {
	private Config config;
	private EnumSet<TransportMode> availableModes = EnumSet.of(
			TransportMode.car, TransportMode.pt), chainBasedModes = EnumSet
			.of(TransportMode.car);
	private ActivityFacilities facilities;
	private Network network;
	private Random random = MatsimRandom.getLocalInstance();

	public SubTourModeChoice(final Config config,
			ActivityFacilities facilities, Network network) {
		super(config.global().getNumberOfThreads());
		this.config = config;
		this.facilities = facilities;
		this.network = network;

		String modes = config.vspExperimental().getModesForSubTourModeChoice();
		if (modes != null) {
			String[] parts = StringUtils.explode(modes, ',');
			this.availableModes = EnumSet.noneOf(TransportMode.class);
			for (int i = 0, n = parts.length; i < n; i++) {
				this.availableModes.add(TransportMode.valueOf(parts[i].trim()));
			}
		}

		String chainModes = config.vspExperimental().getChainBasedModes();
		if (chainModes != null) {
			String[] parts = StringUtils.explode(chainModes, ',');
			this.chainBasedModes = EnumSet.noneOf(TransportMode.class);
			for (int i = 0, n = parts.length; i < n; i++) {
				this.chainBasedModes
						.add(TransportMode.valueOf(parts[i].trim()));
			}
		}
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return new ChooseRandomTourModeSet(chainBasedModes, this.config
				.planomat().getTripStructureAnalysisLayer(), facilities,
				network);
	}

	// ---------------------------------------------------------------
	public class ChooseRandomTourModeSet implements PlanAlgorithm {
		private PlanAnalyzeTourModeChoiceSet patmcs;

		public ChooseRandomTourModeSet(
				final EnumSet<TransportMode> chainBasedModes,
				final PlanomatConfigGroup.TripStructureAnalysisLayerOption tripStructureAnalysisLayer,
				final ActivityFacilities facilities, final Network network) {
			patmcs = new PlanAnalyzeTourModeChoiceSet(chainBasedModes,
					tripStructureAnalysisLayer, facilities, network);
			patmcs.setModeSet(availableModes);
		}

		@Override
		public void run(Plan plan) {
			this.patmcs.run(plan);
			ArrayList<TransportMode[]> choiceSet = this.patmcs.getChoiceSet();

			List<PlanElement> pes = plan.getPlanElements();
			int legsSize = (pes.size() - 1) / 2;
			TransportMode[] currentModes = new TransportMode[legsSize];
			for (int i = 0; i < legsSize; i++)
				currentModes[i] = ((Leg) pes.get(2 * i + 1)).getMode();

			TransportMode[] randomModes = currentModes;
			do {
				randomModes = choiceSet.get(random.nextInt(choiceSet.size()));
			} while (currentModes.equals(randomModes));

			for (int i = 0; i < legsSize; i++)
				((Leg) pes.get(2 * i + 1)).setMode(randomModes[i]);
		}
	}
}
