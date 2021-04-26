/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.application.analysis.emissions;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.analysis.AnalysisSummary;
import org.matsim.application.options.CrsOptions;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.HbefaVehicleCategory;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup.DetailedVsAverageLookupBehavior;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup.HbefaRoadTypeSource;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup.NonScenarioVehicles;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.CipherUtils;
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.Arrays;


@CommandLine.Command(
        name = "air-pollution-by-vehicle",
        description = "Run offline air pollution analysis assuming default vehicles"
)
public class AirPollutionByVehicleCategory implements MATSimAppCommand {

    private static final Logger log = LogManager.getLogger(AirPollutionByVehicleCategory.class);

    @CommandLine.Parameters(paramLabel = "INPUT", arity = "1", description = "Path to run directory")
    private Path runDirectory;

    @CommandLine.Option(names = "--runId", description = "Pattern to match runId.", defaultValue = "*")
    private String runId;

    @CommandLine.Option(names = "--hbefa-warm", required = true)
    private Path hbefaWarmFile;

    @CommandLine.Option(names = "--hbefa-cold", required = true)
    private Path hbefaColdFile;

    @CommandLine.Option(names = "-p", description = "Password for encrypted hbefa files", interactive = true, required = false)
    private char[] password;

    @CommandLine.Option(names = "--output", description = "Output events file", required = true)
    private Path output;

    @CommandLine.Option(names = "--use-default-road-types", description = "Add default hbefa_road_type link attributes to the network", defaultValue = "false")
    private boolean useDefaultRoadTypes;

    @CommandLine.Mixin
    private CrsOptions crs = new CrsOptions();



    public AirPollutionByVehicleCategory(Path runDirectory, String runId, Path hbefaFileWarm, Path hbefaFileCold, Path output) {
        this.runDirectory = runDirectory;
        this.runId = runId;
        this.hbefaWarmFile = hbefaFileWarm;
        this.hbefaColdFile = hbefaFileCold;
        this.output = output;
    }

    @Override
    public Integer call() throws Exception {

        if (password != null) {
            System.setProperty(CipherUtils.ENVIRONMENT_VARIABLE, new String(password));
            // null out the arrays when done
            Arrays.fill(password, ' ');
        }

        Config config = ConfigUtils.createConfig();
        config.vehicles().setVehiclesFile(AnalysisSummary.globFile(runDirectory, runId, "vehicles"));
        config.network().setInputFile(AnalysisSummary.globFile(runDirectory, runId, "network"));
        config.transit().setTransitScheduleFile(AnalysisSummary.globFile(runDirectory, runId, "transitSchedule"));
        config.transit().setVehiclesFile(AnalysisSummary.globFile(runDirectory, runId, "transitVehicles"));
        config.global().setCoordinateSystem(crs.getInputCRS());
        config.plans().setInputFile(null);
        config.parallelEventHandling().setNumberOfThreads(null);
        config.parallelEventHandling().setEstimatedNumberOfEvents(null);
        config.global().setNumberOfThreads(1);

        EmissionsConfigGroup eConfig = ConfigUtils.addOrGetModule(config, EmissionsConfigGroup.class);
        eConfig.setDetailedVsAverageLookupBehavior(DetailedVsAverageLookupBehavior.directlyTryAverageTable);
        eConfig.setAverageColdEmissionFactorsFile(this.hbefaColdFile.toString());
        eConfig.setAverageWarmEmissionFactorsFile(this.hbefaWarmFile.toString());
        eConfig.setHbefaRoadTypeSource(HbefaRoadTypeSource.fromLinkAttributes);
        eConfig.setNonScenarioVehicles(NonScenarioVehicles.ignore);

        final String eventsFile = AnalysisSummary.globFile(runDirectory, runId, "events");

        Scenario scenario = ScenarioUtils.loadScenario(config);

        if (useDefaultRoadTypes) {
            log.info("Using integrated road types");
            // 1 / 0.9, default free speed factor
            addDefaultRoadTypes(scenario.getNetwork(), 1.11);
        }

        // vehicles

        Id<VehicleType> carVehicleTypeId = Id.create("car", VehicleType.class);
        Id<VehicleType> freightVehicleTypeId = Id.create("freight", VehicleType.class);

        VehicleType carVehicleType = scenario.getVehicles().getVehicleTypes().get(carVehicleTypeId);
        VehicleType freightVehicleType = scenario.getVehicles().getVehicleTypes().get(freightVehicleTypeId);

        EngineInformation carEngineInformation = carVehicleType.getEngineInformation();
        VehicleUtils.setHbefaVehicleCategory(carEngineInformation, HbefaVehicleCategory.PASSENGER_CAR.toString());
        VehicleUtils.setHbefaTechnology(carEngineInformation, "average");
        VehicleUtils.setHbefaSizeClass(carEngineInformation, "average");
        VehicleUtils.setHbefaEmissionsConcept(carEngineInformation, "average");

        EngineInformation freightEngineInformation = freightVehicleType.getEngineInformation();
        VehicleUtils.setHbefaVehicleCategory(freightEngineInformation, HbefaVehicleCategory.HEAVY_GOODS_VEHICLE.toString());
        VehicleUtils.setHbefaTechnology(freightEngineInformation, "average");
        VehicleUtils.setHbefaSizeClass(freightEngineInformation, "average");
        VehicleUtils.setHbefaEmissionsConcept(freightEngineInformation, "average");

        // public transit vehicles should be considered as non-hbefa vehicles
        for (VehicleType type : scenario.getTransitVehicles().getVehicleTypes().values()) {
            EngineInformation engineInformation = type.getEngineInformation();
            // TODO: Check! Is this a zero emission vehicle?!
            VehicleUtils.setHbefaVehicleCategory(engineInformation, HbefaVehicleCategory.NON_HBEFA_VEHICLE.toString());
            VehicleUtils.setHbefaTechnology(engineInformation, "average");
            VehicleUtils.setHbefaSizeClass(engineInformation, "average");
            VehicleUtils.setHbefaEmissionsConcept(engineInformation, "average");
        }

        // the following is copy paste from the example...

        EventsManager eventsManager = EventsUtils.createEventsManager();

        AbstractModule module = new AbstractModule() {
            @Override
            public void install() {
                bind(Scenario.class).toInstance(scenario);
                bind(EventsManager.class).toInstance(eventsManager);
                bind(EmissionModule.class);
            }
        };

        com.google.inject.Injector injector = Injector.createInjector(config, module);

        EmissionModule emissionModule = injector.getInstance(EmissionModule.class);

        EventWriterXML emissionEventWriter = new EventWriterXML(output.toString());
        emissionModule.getEmissionEventsManager().addHandler(emissionEventWriter);

        eventsManager.initProcessing();
        MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);
        matsimEventsReader.readFile(eventsFile);
        eventsManager.finishProcessing();

        emissionEventWriter.closeFile();

        return 0;
    }

    /**
     * Default logic to add hbefa road types.
     */
    private void addDefaultRoadTypes(Network network, double speedFactor) {
        // network
        for (Link link : network.getLinks().values()) {

            double freespeed = Double.NaN;

            if (link.getFreespeed() <= 13.888889) {
                freespeed = link.getFreespeed() * speedFactor;
                // for non motorway roads, the free speed level was reduced
            } else {
                freespeed = link.getFreespeed();
                // for motorways, the original speed levels seems ok.
            }

            if (freespeed <= 8.333333333) { //30kmh
                link.getAttributes().putAttribute("hbefa_road_type", "URB/Access/30");
            } else if (freespeed <= 11.111111111) { //40kmh
                link.getAttributes().putAttribute("hbefa_road_type", "URB/Access/40");
            } else if (freespeed <= 13.888888889) { //50kmh
                double lanes = link.getNumberOfLanes();
                if (lanes <= 1.0) {
                    link.getAttributes().putAttribute("hbefa_road_type", "URB/Local/50");
                } else if (lanes <= 2.0) {
                    link.getAttributes().putAttribute("hbefa_road_type", "URB/Distr/50");
                } else if (lanes > 2.0) {
                    link.getAttributes().putAttribute("hbefa_road_type", "URB/Trunk-City/50");
                } else {
                    throw new RuntimeException("NoOfLanes not properly defined");
                }
            } else if (freespeed <= 16.666666667) { //60kmh
                double lanes = link.getNumberOfLanes();
                if (lanes <= 1.0) {
                    link.getAttributes().putAttribute("hbefa_road_type", "URB/Local/60");
                } else if (lanes <= 2.0) {
                    link.getAttributes().putAttribute("hbefa_road_type", "URB/Trunk-City/60");
                } else if (lanes > 2.0) {
                    link.getAttributes().putAttribute("hbefa_road_type", "URB/MW-City/60");
                } else {
                    throw new RuntimeException("NoOfLanes not properly defined");
                }
            } else if (freespeed <= 19.444444444) { //70kmh
                link.getAttributes().putAttribute("hbefa_road_type", "URB/MW-City/70");
            } else if (freespeed <= 22.222222222) { //80kmh
                link.getAttributes().putAttribute("hbefa_road_type", "URB/MW-Nat./80");
            } else if (freespeed > 22.222222222) { //faster
                link.getAttributes().putAttribute("hbefa_road_type", "RUR/MW/>130");
            } else {
                throw new RuntimeException("Link not considered...");
            }
        }

    }

}

