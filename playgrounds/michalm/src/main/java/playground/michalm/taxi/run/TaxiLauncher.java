/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.michalm.taxi.run;

import java.io.PrintWriter;
import java.util.List;

import org.matsim.analysis.LegHistogram;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.*;
import org.matsim.contrib.dvrp.extensions.taxi.TaxiUtils;
import org.matsim.contrib.dvrp.passenger.*;
import org.matsim.contrib.dvrp.router.*;
import org.matsim.contrib.dvrp.run.*;
import org.matsim.contrib.dvrp.run.VrpLauncherUtils.TravelDisutilitySource;
import org.matsim.contrib.dvrp.run.VrpLauncherUtils.TravelTimeSource;
import org.matsim.contrib.dvrp.util.gis.Schedules2GIS;
import org.matsim.contrib.dvrp.util.time.TimeDiscretizer;
import org.matsim.contrib.dvrp.vrpagent.*;
import org.matsim.contrib.dvrp.vrpagent.VrpLegs.LegCreator;
import org.matsim.contrib.dynagent.run.DynAgentLauncherUtils;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.algorithms.*;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.*;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.vis.otfvis.OTFVisConfigGroup.ColoringScheme;

import pl.poznan.put.util.ChartUtils;
import playground.michalm.demand.taxi.PersonCreatorWithRandomTaxiMode;
import playground.michalm.taxi.*;
import playground.michalm.taxi.data.*;
import playground.michalm.taxi.data.TaxiRequest.TaxiRequestStatus;
import playground.michalm.taxi.optimizer.*;
import playground.michalm.taxi.optimizer.filter.*;
import playground.michalm.taxi.scheduler.*;
import playground.michalm.taxi.util.chart.TaxiScheduleChartUtils;
import playground.michalm.taxi.util.stats.*;
import playground.michalm.taxi.util.stats.TaxiStatsCalculator.TaxiStats;
import playground.michalm.taxi.vehreqpath.VehicleRequestPathFinder;
import playground.michalm.util.MovingAgentsRegister;


class TaxiLauncher
{
    final TaxiLauncherParams params;
    MatsimVrpContext context;
    final Scenario scenario;

    private TravelTimeCalculator travelTimeCalculator;
    private LeastCostPathCalculatorWithCache routerWithCache;
    private VrpPathCalculator pathCalculator;

    LegHistogram legHistogram;
    TaxiDelaySpeedupStats delaySpeedupStats;
    LeastCostPathCalculatorCacheStats cacheStats;


    TaxiLauncher(TaxiLauncherParams params)
    {
        this.params = params;

        if (params.changeEventsFile != null && params.onlineVehicleTracker) {
            System.err.println("Online vehicle tracking may not be useful -- "
                    + "travel times should be (almost?) deterministic for a time variant network");
        }

        scenario = VrpLauncherUtils.initScenario(params.netFile, params.plansFile,
                params.changeEventsFile);

        if (params.taxiCustomersFile != null) {
            List<String> passengerIds = PersonCreatorWithRandomTaxiMode
                    .readTaxiCustomerIds(params.taxiCustomersFile);
            VrpPopulationUtils.convertLegModes(passengerIds, TaxiUtils.TAXI_MODE, scenario);
        }

        //TaxiDemandUtils.preprocessPlansBasedOnCoordsOnly(scenario);
    }


    void initVrpPathCalculator()
    {
        TravelTime travelTime = travelTimeCalculator == null ? //
                VrpLauncherUtils.initTravelTime(scenario, params.algorithmConfig.ttimeSource,
                        params.eventsFile) : //
                travelTimeCalculator.getLinkTravelTimes();

        TravelDisutility travelDisutility = VrpLauncherUtils.initTravelDisutility(
                params.algorithmConfig.tdisSource, travelTime);

        LeastCostPathCalculator router = new Dijkstra(scenario.getNetwork(), travelDisutility,
                travelTime);

        TimeDiscretizer timeDiscretizer = (params.algorithmConfig.tdisSource == TravelDisutilitySource.DISTANCE)
                || //
                (params.algorithmConfig.ttimeSource == TravelTimeSource.FREE_FLOW_SPEED && //
                !scenario.getConfig().network().isTimeVariantNetwork()) ? //
                TimeDiscretizer.CYCLIC_24_HOURS : //
                TimeDiscretizer.CYCLIC_15_MIN;

        routerWithCache = new LeastCostPathCalculatorWithCache(router, timeDiscretizer);
        pathCalculator = new VrpPathCalculatorImpl(routerWithCache, travelTime, travelDisutility);
    }


    void clearVrpPathCalculator()
    {
        travelTimeCalculator = null;
        routerWithCache = null;
        pathCalculator = null;
    }


    /**
     * Can be called several times (1 call == 1 simulation)
     */
    void go(boolean warmup)
    {
        MatsimVrpContextImpl contextImpl = new MatsimVrpContextImpl();
        this.context = contextImpl;

        contextImpl.setScenario(scenario);

        TaxiData taxiData = TaxiLauncherUtils.initTaxiData(scenario, params.taxisFile,
                params.ranksFile);
        contextImpl.setVrpData(taxiData);

        TaxiOptimizerConfiguration optimizerConfig = createOptimizerConfiguration();
        TaxiOptimizer optimizer = params.algorithmConfig.createTaxiOptimizer(optimizerConfig);

        QSim qSim = DynAgentLauncherUtils.initQSim(scenario);
        contextImpl.setMobsimTimer(qSim.getSimTimer());

        qSim.addQueueSimulationListeners(optimizer);

        PassengerEngine passengerEngine = VrpLauncherUtils.initPassengerEngine(
                TaxiUtils.TAXI_MODE, new TaxiRequestCreator(), optimizer, context, qSim);

        if (params.advanceRequestSubmission) {
            // yy to my ears, this is not completely clear.  I don't think that it enables advance request submission
            // for arbitrary times, but rather requests all trips before the simulation starts.  Doesn't it?  kai, jul'14

            //Yes. For a fully-featured advanced request submission process, use TripPrebookingManager, michalm, sept'14
            qSim.addQueueSimulationListeners(new BeforeSimulationTripPrebooker(passengerEngine));
        }

        LegCreator legCreator = params.onlineVehicleTracker ? VrpLegs
                .createLegWithOnlineTrackerCreator(optimizer, qSim.getSimTimer())
                : VrpLegs.LEG_WITH_OFFLINE_TRACKER_CREATOR;

        TaxiActionCreator actionCreator = new TaxiActionCreator(passengerEngine, legCreator,
                params.pickupDuration);

        VrpLauncherUtils.initAgentSources(qSim, context, optimizer, actionCreator);

        EventsManager events = qSim.getEventsManager();

        EventWriter eventWriter = null;
        if (params.eventsOutFile != null) {
            eventWriter = new EventWriterXML(params.eventsOutFile);
            events.addHandler(eventWriter);
        }

        if (warmup) {
            if (travelTimeCalculator == null) {
                travelTimeCalculator = TravelTimeCalculators.createTravelTimeCalculator(scenario);
            }

            events.addHandler(travelTimeCalculator);
        }
        else {
            optimizerConfig.scheduler.setDelaySpeedupStats(delaySpeedupStats);
        }

        MovingAgentsRegister mar = new MovingAgentsRegister();
        events.addHandler(mar);

        if (params.otfVis) { // OFTVis visualization
            DynAgentLauncherUtils.runOTFVis(qSim, false, ColoringScheme.taxicab);
        }

        if (params.histogramOutDir != null) {
            events.addHandler(legHistogram = new LegHistogram(300));
        }

        qSim.run();

        events.finishProcessing();

        if (params.eventsOutFile != null) {
            eventWriter.closeFile();
        }

        // check if all reqs have been served
        for (TaxiRequest r : taxiData.getTaxiRequests()) {
            if (r.getStatus() != TaxiRequestStatus.PERFORMED) {
                throw new IllegalStateException();
            }
        }

        if (cacheStats != null) {
            cacheStats.updateStats(routerWithCache);
        }
    }


    private TaxiOptimizerConfiguration createOptimizerConfiguration()
    {
        TaxiSchedulerParams schedulerParams = new TaxiSchedulerParams(params.destinationKnown,
                params.pickupDuration, params.dropoffDuration);
        TaxiScheduler scheduler = new TaxiScheduler(context, pathCalculator, schedulerParams);
        VehicleRequestPathFinder vrpFinder = new VehicleRequestPathFinder(pathCalculator, scheduler);
        FilterFactory filterFactory = new DefaultFilterFactory(scheduler,
                params.nearestRequestsLimit, params.nearestVehiclesLimit);

        return new TaxiOptimizerConfiguration(context, pathCalculator, scheduler, vrpFinder,
                filterFactory, params.algorithmConfig.goal, params.outputDir);
    }


    void generateOutput()
    {
        PrintWriter pw = new PrintWriter(System.out);
        pw.println(params.algorithmConfig.name());
        pw.println("m\t" + context.getVrpData().getVehicles().size());
        pw.println("n\t" + context.getVrpData().getRequests().size());
        pw.println(TaxiStats.HEADER);
        TaxiStats stats = new TaxiStatsCalculator().calculateStats(context.getVrpData()
                .getVehicles());
        pw.println(stats);
        pw.flush();

        if (params.vrpOutDir != null) {
            new Schedules2GIS(context.getVrpData().getVehicles(),
                    TransformationFactory.WGS84_UTM33N).write(params.vrpOutDir);
        }

        // ChartUtils.showFrame(RouteChartUtils.chartRoutesByStatus(data.getVrpData()));
        ChartUtils.showFrame(TaxiScheduleChartUtils.chartSchedule(context.getVrpData()
                .getVehicles()));

        if (params.histogramOutDir != null) {
            VrpLauncherUtils.writeHistograms(legHistogram, params.histogramOutDir);
        }
    }


    public static void main(String... args)
    {
        TaxiLauncherParams params = TaxiLauncherParams.readParams(args[0]);
        TaxiLauncher launcher = new TaxiLauncher(params);
        launcher.initVrpPathCalculator();
        launcher.go(false);
        launcher.generateOutput();
    }
}
