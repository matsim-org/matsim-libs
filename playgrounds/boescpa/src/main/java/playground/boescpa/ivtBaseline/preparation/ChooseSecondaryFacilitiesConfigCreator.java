/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.boescpa.ivtBaseline.preparation;

import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import playground.ivt.kticompatibility.KtiLikeScoringConfigGroup;

import java.util.HashMap;
import java.util.Map;

/**
 * Creates an ivt config with location choice.
 *
 * @author boescpa
 */
public class ChooseSecondaryFacilitiesConfigCreator extends IVTConfigCreator {

	public static void main(String[] args) {
		int prctScenario = Integer.parseInt(args[1]); // the percentage of the scenario in percent (e.g. 1%-Scenario -> "1")
		Config config = ConfigUtils.createConfig(new DestinationChoiceConfigGroup());
		new ChooseSecondaryFacilitiesConfigCreator().makeConfigIVT(config, prctScenario);

		// Reduce to one replanning phase
		config.setParam("controler", "lastIteration", "1");

		// Set location choice activities
		config.setParam("locationchoice", "flexible_types", "remote_work, leisure, shop, escort_kids, escort_other");
		config.setParam("locationchoice", "epsilonScaleFactors", "0.3, 0.1, 0.1, 0.1, 0.2");

		// Change travel time calculation
		config.setParam("locationchoice", "tt_approximationLevel","2");

		// Add files
		config.setParam("locationchoice", "prefsFile", INBASE_FILES + POPULATION_ATTRIBUTES);

		new ConfigWriter(config).write(args[0]);
	}

	@Override
	protected Map<String, Double> getStrategyDescr() {
		Map<String, Double> strategyDescr = new HashMap<>();
		strategyDescr.put("org.matsim.contrib.locationchoice.BestReplyLocationChoicePlanStrategy", 1.0);
		return strategyDescr;
	}

}
