/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.extension.dashboards;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.application.ApplicationUtils;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.simwrapper.SimWrapper;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@CommandLine.Command(
	name = "drt",
	description = "Run DRT analysis and create SimWrapper dashboard for existing run output."
)
final class CreateDrtDashboard implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(CreateDrtDashboard.class);

	@CommandLine.Parameters(arity = "1..*", description = "Path to run output directories for which DRT dashboards are to be generated.")
	private List<Path> inputPaths;

	private CreateDrtDashboard(){
	}

	@Override
	public Integer call() throws Exception {

		for (Path runDirectory : inputPaths) {
			log.info("Running on {}", runDirectory);

			Path configPath = ApplicationUtils.matchInput("config.xml", runDirectory);
			Config config = ConfigUtils.loadConfig(configPath.toString());
			SimWrapper sw = SimWrapper.create(config);

			SimWrapperConfigGroup simwrapperCfg = ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class);

			//skip default dashboards
			simwrapperCfg.defaultDashboards = SimWrapperConfigGroup.Mode.disabled;

			//add drt dashboards
			new DrtDashboardProvider().getDashboards(config, sw).forEach(sw::addDashboard);

			try {
				//append dashboard to existing ones
				boolean append = true;
				sw.generate(runDirectory, append);
				sw.run(runDirectory);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		return 0;
	}

	public static void main(String[] args) {
		new CreateDrtDashboard().execute(args);

	}

}
