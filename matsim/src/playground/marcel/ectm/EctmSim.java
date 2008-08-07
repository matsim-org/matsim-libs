/* *********************************************************************** *
 * project: org.matsim.*
 * EctmSim.java
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

package playground.marcel.ectm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.config.Config;
import org.matsim.config.ConfigWriter;
import org.matsim.config.MatsimConfigReader;
import org.matsim.controler.Controler;
import org.matsim.events.Events;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.ExternalMobsim;
import org.matsim.population.Population;
import org.matsim.utils.misc.ExeRunner;

public class EctmSim extends ExternalMobsim {

	private static final String CONFIG_MODULE = "ectm";
	private static final String CONFIG_CONFIG_TEMPLATE = "configTemplate";

	private String eventsFile = null;

	private static final Logger log = Logger.getLogger(EctmSim.class);

	public EctmSim(final Population population, final Events events) {
		super(population, events);
	}

	@Override
	protected void writeConfig(final String iterationPlansFile, final String iterationEventsFile, final String iterationConfigFile) throws FileNotFoundException, IOException {
		this.eventsFile = iterationEventsFile;

		final Config gblConfig = Gbl.getConfig();
		final String configFile = gblConfig.getParam(CONFIG_MODULE, CONFIG_CONFIG_TEMPLATE);
		Config ectmConfig = new Config();
		new MatsimConfigReader(ectmConfig).readFile(configFile);
		final File networkFile = new File(gblConfig.network().getInputFile());
		ectmConfig.setParam("general", "netfile", networkFile.getAbsolutePath());
		final File plansFile = new File(iterationPlansFile);
		ectmConfig.setParam("MatsimPlansLoader", "plansfile", plansFile.getAbsolutePath());

		new ConfigWriter(ectmConfig, iterationConfigFile).write();
	}

	@Override
	protected void runExe(final String iterationConfigFile) throws FileNotFoundException, IOException {
		String cmd = this.executable + " " + iterationConfigFile + " " + this.eventsFile;// + " " + Controler.getIterationPath() + "/netvis";
		log.info("running command: \"" + cmd);
		Gbl.printMemoryUsage();
		String logfileName = Controler.getIterationFilename("mobsim.log");
		int timeout = Gbl.getConfig().simulation().getExternalTimeOut();
		int exitcode = ExeRunner.run(cmd, logfileName, timeout);
		if (exitcode != 0) {
			Gbl.errorMsg("There was a problem running the external mobsim. exit code: " + exitcode);
		}
	}

}
