package playground.dgrether;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.controler.SignalsModule;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.contrib.signals.data.SignalsScenarioWriter;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerDefaultsModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.controler.NewControlerModule;
import org.matsim.core.controler.corelisteners.ControlerDefaultCoreListenersModule;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OnTheFlyServer;

import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.util.Types;

/* *********************************************************************** *
 * project: org.matsim.*
 * DgOTFVisReplayLastIteration
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
 * @author dgrether
 * 
 */
public class DgOTFVisReplayLastIteration {

	private static final Logger log = Logger.getLogger(DgOTFVisReplayLastIteration.class);

	private void playOutputConfig(String configfile) throws FileNotFoundException, IOException {
		String currentDirectory = configfile.substring(0, configfile.lastIndexOf("/") + 1);
		if (currentDirectory == null) {
			currentDirectory = configfile.substring(0, configfile.lastIndexOf("\\") + 1);
		}
		log.info("using " + currentDirectory + " as base directory...");
		Config config = ConfigUtils.loadConfig(configfile);
		// disable snapshot writing as the snapshot should not be overwritten
		config.qsim().setSnapshotPeriod(0.0);
		
		String inputPreFix = "";
		if (config.controler().getRunId() != null) {
			inputPreFix = config.controler().getRunId() + ".";
		} 
		config.network().setInputFile(inputPreFix + Controler.FILENAME_NETWORK);
		config.plans().setInputFile(inputPreFix + Controler.FILENAME_POPULATION);
		if (config.network().getLaneDefinitionsFile() != null || config.qsim().isUseLanes()) {
			config.network().setLaneDefinitionsFile(inputPreFix + Controler.FILENAME_LANES);
		}
		SignalSystemsConfigGroup signalsConfig = ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
		if (signalsConfig.isUseSignalSystems()) {
			signalsConfig.setSignalSystemFile(inputPreFix + SignalsScenarioWriter.FILENAME_SIGNAL_SYSTEMS);
			signalsConfig.setSignalGroupsFile(inputPreFix + SignalsScenarioWriter.FILENAME_SIGNAL_GROUPS);
			signalsConfig.setSignalControlFile(inputPreFix + SignalsScenarioWriter.FILENAME_SIGNAL_CONTROL);
			if (signalsConfig.isUseAmbertimes()){
				signalsConfig.setAmberTimesFile(inputPreFix + SignalsScenarioWriter.FILENAME_AMBER_TIMES);
			}
		}

		log.info("Write config file");
		new ConfigWriter(config).write(currentDirectory + "lastItLiveConfig.xml");

		final Scenario sc = ScenarioUtils.loadScenario(config);
		sc.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());

		com.google.inject.Injector injector = Injector.createInjector(config, new AbstractModule() {
			@Override
			public void install() {
				// defaults
				install(new NewControlerModule());
				install(new ControlerDefaultCoreListenersModule());
				install(new ControlerDefaultsModule());
				install(new ScenarioByInstanceModule(sc));
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

		OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(sc.getConfig(), sc, events, qSim);
		OTFClientLive.run(sc.getConfig(), server);
		qSim.run();
	}

	public static final String chooseFile() {
		JFileChooser fc = new JFileChooser();

		fc.setFileFilter(new FileFilter() {

			@Override
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().toLowerCase().endsWith(".xml");
			}

			@Override
			public String getDescription() {
				return "MATSim config file (*.xml)";
			}
		});

		fc.setFileFilter(new FileFilter() {

			@Override
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().toLowerCase().endsWith(".xml.gz");
			}

			@Override
			public String getDescription() {
				return "MATSim zipped config file (*.xml.gz)";
			}
		});

		int state = fc.showOpenDialog(null);
		if (state == JFileChooser.APPROVE_OPTION) {
			String args_new = fc.getSelectedFile().getAbsolutePath();
			return args_new;
		}
		System.out.println("No file selected.");
		return null;
	}

	/**
	 * @param args
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		String configfile = null;
		if (args.length == 0) {
			 configfile = chooseFile();
		} else if (args.length == 1) {
			configfile = args[0];
		} else {
			log.error("not the correct arguments");
		}
		if (configfile != null) {
			new DgOTFVisReplayLastIteration().playOutputConfig(configfile);
		}
	}

}
