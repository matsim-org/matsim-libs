/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.michalm.audiAV.flowPaper;

import org.matsim.contrib.av.flow.AvIncreasedCapacityModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.trafficmonitoring.*;
import org.matsim.contrib.taxi.run.*;
import org.matsim.core.config.*;
import org.matsim.core.controler.*;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.name.Names;

public class RunAudiAVFlowPaper {
	public static void run(String configFile, double flowEfficiencyFactor, String inputEvents) {
		Config config = ConfigUtils.loadConfig(configFile, new TaxiConfigGroup(), new DvrpConfigGroup());
		final Controler controler = RunTaxiScenario.createControler(config, false);

		// to speed up computations
		final TravelTime initialTT = TravelTimeUtils.createTravelTimesFromEvents(controler.getScenario(), inputEvents);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(TravelTime.class).annotatedWith(Names.named(DvrpTravelTimeModule.DVRP_INITIAL))
						.toInstance(initialTT);
			}
		});

		controler.addOverridingModule(new AvIncreasedCapacityModule(flowEfficiencyFactor));

		controler.run();
	}

	public static void main(String[] args) {
		double flowEfficiencyFactor = Double.parseDouble(args[1]);
		run(args[0], flowEfficiencyFactor, args[2]);
	}
}
