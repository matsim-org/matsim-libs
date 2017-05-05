/* *********************************************************************** *
 * project: org.matsim.*
 * FourWaysVis
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.dgrether.signalsystems.otfvis;

import java.util.Collection;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.controler.SignalsModule;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.ControlerDefaultsModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.controler.NewControlerModule;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.corelisteners.ControlerDefaultCoreListenersModule;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.vis.otfvis.OnTheFlyServer;

import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.util.Types;

public class FourWaysVis {

	public static final String TESTINPUTDIR = "../../contribs/signals/src/test/resources/test/input/org/matsim/contrib/signals/TravelTimeFourWaysTest/";

	public static void main(String[] args) {

		Config config = ConfigUtils.createConfig();
		config.controler().setOutputDirectory("./output/fourWaysVis/");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		config.network().setInputFile(TESTINPUTDIR + "network.xml.gz");
		config.plans().setInputFile(TESTINPUTDIR + "plans.xml.gz");
		config.qsim().setSnapshotStyle(SnapshotStyle.queue);
		config.qsim().setStuckTime(100.0);
		config.qsim().setUsingFastCapacityUpdate(false);

		SignalSystemsConfigGroup signalsConfig = ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
		signalsConfig.setSignalSystemFile(TESTINPUTDIR + "testSignalSystems_v2.0.xml");
		signalsConfig.setSignalGroupsFile(TESTINPUTDIR + "testSignalGroups_v2.0.xml");
		signalsConfig.setSignalControlFile(TESTINPUTDIR + "testSignalControl_v2.0.xml");
		signalsConfig.setUseSignalSystems(true);

		OTFVisConfigGroup otfconfig = ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class);
		otfconfig.setAgentSize(130.0f);

		config.network().setLaneDefinitionsFile(TESTINPUTDIR + "testLaneDefinitions_v2.0.xml");
		config.qsim().setUseLanes(true);

		final Scenario scenario = ScenarioUtils.loadScenario(config);
		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());

		com.google.inject.Injector injector = Injector.createInjector(scenario.getConfig(), new AbstractModule() {
			@Override
			public void install() {
				// defaults
				install(new NewControlerModule());
				install(new ControlerDefaultCoreListenersModule());
				install(new ControlerDefaultsModule());
				install(new ScenarioByInstanceModule(scenario));
				// signal specific module
				install(new SignalsModule());
			}
		});

		EventsManager events = injector.getInstance(EventsManager.class);
		events.initProcessing();

		QSim qSim = (QSim) injector.getInstance(Mobsim.class);
		Collection<Provider<MobsimListener>> mobsimListeners = (Collection<Provider<MobsimListener>>) injector.getInstance(Key.get(Types.collectionOf(Types.providerOf(MobsimListener.class))));
		for (Provider<MobsimListener> provider : mobsimListeners) {
			qSim.addQueueSimulationListeners(provider.get());
		}

		OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(config, scenario, events, qSim);
		OTFClientLive.run(config, server);

		qSim.run();
	}

}
