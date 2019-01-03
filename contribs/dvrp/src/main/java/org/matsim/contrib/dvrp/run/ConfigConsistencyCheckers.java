/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.dvrp.run;

import java.util.HashSet;
import java.util.function.Consumer;

import org.matsim.core.config.ConfigGroup;

/**
 * @author Michal Maciejewski (michalm)
 */
public class ConfigConsistencyCheckers {
	public static <C extends ConfigGroup & Modal> void checkSingleOrMultiModeConsistency(C cfg,
			MultiModal<C> multiModeCfg, Consumer<C> consistencyChecker) {
		if (cfg != null) {
			if (multiModeCfg != null) {
				throw new RuntimeException("Either single or multi-mode " + cfg.getName() + " must be defined");
			}
			consistencyChecker.accept(cfg);
		} else {
			if (multiModeCfg == null) {
				throw new RuntimeException("Either single or multi-mode " + cfg.getName() + " must be defined");
			}
			multiModeCfg.getModalElements().stream().forEach(consistencyChecker);
			if (!areModesUnique(multiModeCfg)) {
				throw new RuntimeException("Modes in multi-mode config are not unique");
			}
		}
	}

	public static boolean areModesUnique(MultiModal<?> multiModal) {
		return multiModal.modes().allMatch(new HashSet<>()::add);
	}
}
