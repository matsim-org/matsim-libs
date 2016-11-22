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

package playground.agarwalamit.opdyts.patna;

import java.util.HashSet;
import java.util.Set;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.analysis.kai.KaiAnalysisListener;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import playground.agarwalamit.analysis.controlerListner.ModalShareControlerListner;
import playground.agarwalamit.analysis.controlerListner.ModalTravelTimeControlerListner;
import playground.agarwalamit.analysis.modalShare.ModalShareEventHandler;
import playground.agarwalamit.analysis.travelTime.ModalTripTravelTimeHandler;
import playground.agarwalamit.opdyts.ModalStatsControlerListner;
import playground.agarwalamit.opdyts.OpdytsObjectiveFunctionCases;

/**
 * @author amit
 */

public class PatnaPlansRelaxor {

	public void run (String[] args) {

		Config config= ConfigUtils.loadConfig(args[0]);
		config.controler().setOutputDirectory(args[1]);

		config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.abort);

		Scenario scenario = ScenarioUtils.loadScenario(config);
		scenario.getConfig().controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		Set<String> modes2consider = new HashSet<>();
		modes2consider.add("car");
		modes2consider.add("bike");
		modes2consider.add("motorbike");
//		modes2consider.add("pt");
//		modes2consider.add("walk");

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				// add here whatever should be attached to matsim controler

				// some stats
				addControlerListenerBinding().to(KaiAnalysisListener.class);

				this.bind(ModalShareEventHandler.class);
				this.addControlerListenerBinding().to(ModalShareControlerListner.class);

				this.bind(ModalTripTravelTimeHandler.class);
				this.addControlerListenerBinding().to(ModalTravelTimeControlerListner.class);

				this.addControlerListenerBinding().toInstance(new ModalStatsControlerListner(modes2consider, OpdytsObjectiveFunctionCases.PATNA_1Pct));
			}
		});
	controler.run();
	}
}