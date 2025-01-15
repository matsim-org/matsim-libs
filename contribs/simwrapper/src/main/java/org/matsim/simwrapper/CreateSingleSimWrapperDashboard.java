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

package org.matsim.simwrapper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.application.ApplicationUtils;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.simwrapper.dashboard.*;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;

@CommandLine.Command(
	name = "dashboard",
	description = "Run analysis and create SimWrapper dashboard for existing run output."
)

/**
 * This class creates single SimWrapper dashboards for multiple output directories. It is meant to be run as a post-process,
 * e.g. when a specific dashboard was missing after the initial run (for whatever reason).
 * It will create the dashboard with all standard settings.
 * Depending on the dashboard type, it might be required to provide ShpOptions for data filtering.
 * TODO: test whether this works for the non-noise DashboardTypes.
 */
final class CreateSingleSimWrapperDashboard implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(CreateSingleSimWrapperDashboard.class);
	@CommandLine.Mixin
	private final ShpOptions shp = new ShpOptions();
	@CommandLine.Option(names = "--type", required = true, description = "Provide the dashboard type to be generated. See DashboardType enum within this class.")
	private DashboardType dashboardType;
	@CommandLine.Parameters(arity = "1..*", description = "Path to run output directories for which the dashboards is to be generated.")
	private List<Path> inputPaths;

	private CreateSingleSimWrapperDashboard() {
	}

	public static void main(String[] args) {
		new CreateSingleSimWrapperDashboard().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		for (Path runDirectory : inputPaths) {
			log.info("Creating " + dashboardType + " for {}", runDirectory);

			Path configPath = ApplicationUtils.matchInput("config.xml", runDirectory);
			Config config = ConfigUtils.loadConfig(configPath.toString());
			SimWrapper sw = SimWrapper.create(config);

			SimWrapperConfigGroup simwrapperCfg = ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class);

			if (shp.isDefined()) {
				//not sure if this is the best way to go, might be that the shape file would be automatically read by providing the --shp command line option
				simwrapperCfg.defaultParams().shp = shp.getShapeFile();
			}

			//skip default dashboards
			simwrapperCfg.defaultDashboards = SimWrapperConfigGroup.Mode.disabled;

			//add dashboard
			switch (dashboardType) {
				case noise -> {
					sw.addDashboard(new NoiseDashboard(config.global().getCoordinateSystem()));
				}
				case emissions -> {
					sw.addDashboard(new EmissionsDashboard(config.global().getCoordinateSystem()));
				}
				case traffic -> {
					sw.addDashboard(new TrafficDashboard());
				}
				case overview -> {
					sw.addDashboard(new OverviewDashboard());
				}
				case stuckAgent -> {
					sw.addDashboard(new StuckAgentDashboard());
				}
				case populationAttribute -> {
					sw.addDashboard(new PopulationAttributeDashboard());
				}
				case ODTrip -> {
					throw new RuntimeException("ODTripDashboard needs additional information. Single creation is currently not implemented");
//					sw.addDashboard(new ODTripDashboard());
				}
				case trip -> {
					sw.addDashboard(new TripDashboard());
				}
				case publicTransit -> {
					sw.addDashboard(new PublicTransitDashboard());
				}
				case impactAnalysis -> {
					HashSet<String> modes = new HashSet<>();
					modes.add("car");
					modes.add("freight");

					sw.addDashboard(new ImpactAnalysisDashboard(modes));
				}
				default -> throw new IllegalArgumentException("unkown dashboard type: " + dashboardType);
			}

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

	enum DashboardType {
		noise,
		emissions,
		traffic,
		overview,
		stuckAgent,
		populationAttribute,
		ODTrip,
		trip,
		publicTransit,
		impactAnalysis
	}

}

