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

import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.contrib.dvrp.*;
import org.matsim.contrib.dvrp.extensions.taxi.TaxiUtils;
import org.matsim.contrib.dvrp.passenger.*;
import org.matsim.contrib.dvrp.path.*;
import org.matsim.contrib.dvrp.router.*;
import org.matsim.contrib.dvrp.run.*;
import org.matsim.contrib.dvrp.run.VrpLauncherUtils.TravelTimeSource;
import org.matsim.contrib.dvrp.util.TimeDiscretizer;
import org.matsim.contrib.dvrp.vrpagent.VrpLegs;
import org.matsim.contrib.dvrp.vrpagent.VrpLegs.LegCreator;
import org.matsim.contrib.dynagent.run.DynAgentLauncherUtils;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.util.*;
import org.matsim.core.trafficmonitoring.*;

import playground.michalm.demand.taxi.PersonCreatorWithRandomTaxiMode;
import playground.michalm.taxi.*;
import playground.michalm.taxi.data.*;
import playground.michalm.taxi.data.TaxiRequest.TaxiRequestStatus;
import playground.michalm.taxi.optimizer.*;
import playground.michalm.taxi.optimizer.filter.*;
import playground.michalm.taxi.scheduler.*;
import playground.michalm.taxi.vehreqpath.VehicleRequestPathFinder;
import playground.michalm.zone.*;


class TaxiLauncher
{
    final TaxiLauncherParams params;
    MatsimVrpContext context;
    final Scenario scenario;
    final Map<Id<Zone>, Zone> zones;

    TravelTimeCalculator travelTimeCalculator;
    LeastCostPathCalculatorWithCache routerWithCache;
    VrpPathCalculator pathCalculator;

    private TravelTime travelTime;


    TaxiLauncher(TaxiLauncherParams params)
    {
        this.params = params;

        scenario = VrpLauncherUtils.initScenario(params.netFile, params.plansFile,
                params.changeEventsFile);

        if (params.taxiCustomersFile != null) {
            List<String> passengerIds = PersonCreatorWithRandomTaxiMode
                    .readTaxiCustomerIds(params.taxiCustomersFile);
            VrpPopulationUtils.convertLegModes(passengerIds, TaxiUtils.TAXI_MODE, scenario);
        }

        if (params.zonesXmlFile != null && params.zonesShpFile != null) {
            zones = Zones.readZones(params.zonesXmlFile, params.zonesShpFile);
            System.err.println("No conversion of SRS is done");
        }
        else {
            zones = null;
        }

        //TaxiDemandUtils.preprocessPlansBasedOnCoordsOnly(scenario);
    }


    void initVrpPathCalculator()
    {
        TimeDiscretizer timeDiscretizer = TaxiLauncherUtils.getTimeDiscretizer(scenario,
                params.algorithmConfig.ttimeSource, params.algorithmConfig.tdisSource);

        if (params.algorithmConfig.ttimeSource == TravelTimeSource.FREE_FLOW_SPEED) {
            //works for TimeVariantLinks
            travelTime = new FreeSpeedTravelTime();
        }
        else {// TravelTimeSource.EVENTS
            if (travelTimeCalculator == null) {
                travelTimeCalculator = VrpLauncherUtils.initTravelTimeCalculatorFromEvents(scenario,
                        params.eventsFile, timeDiscretizer.getTimeInterval());
            }

            travelTime = travelTimeCalculator.getLinkTravelTimes();
        }

        TravelDisutility travelDisutility = VrpLauncherUtils
                .initTravelDisutility(params.algorithmConfig.tdisSource, travelTime);

        boolean useTree = !true;//TODO move this switch to TaxiLauncherParams
        if (useTree) {
            routerWithCache = new DijkstraWithDijkstraTreeCache(scenario.getNetwork(),
                    travelDisutility, travelTime, timeDiscretizer);
        }
        else {
            LeastCostPathCalculator router = new DijkstraWithThinPath(scenario.getNetwork(), travelDisutility,
                    travelTime);
            routerWithCache = new DefaultLeastCostPathCalculatorWithCache(router, timeDiscretizer);
        }

        pathCalculator = new VrpPathCalculatorImpl(routerWithCache, new VrpPathFactoryImpl(travelTime, travelDisutility));
    }


    /**
     * Can be called several times (1 call == 1 simulation)
     */
    void simulateIteration()
    {
        MatsimVrpContextImpl contextImpl = new MatsimVrpContextImpl();
        this.context = contextImpl;

        contextImpl.setScenario(scenario);

        ETaxiData taxiData = TaxiLauncherUtils.initTaxiData(scenario, params.taxisFile,
                params.ranksFile);
        contextImpl.setVrpData(taxiData);

        TaxiOptimizerConfiguration optimizerConfig = createOptimizerConfiguration();
        TaxiOptimizer optimizer = params.algorithmConfig.createTaxiOptimizer(optimizerConfig);

        QSim qSim = DynAgentLauncherUtils.initQSim(scenario);
        contextImpl.setMobsimTimer(qSim.getSimTimer());

        qSim.addQueueSimulationListeners(optimizer);

        PassengerEngine passengerEngine = VrpLauncherUtils.initPassengerEngine(TaxiUtils.TAXI_MODE,
                new TaxiRequestCreator(), optimizer, context, qSim);

        if (params.advanceRequestSubmission) {
            // yy to my ears, this is not completely clear.  I don't think that it enables advance request submission
            // for arbitrary times, but rather requests all trips before the simulation starts.  Doesn't it?  kai, jul'14

            //Yes. For a fully-featured advanced request submission process, use TripPrebookingManager, michalm, sept'14
            qSim.addQueueSimulationListeners(new BeforeSimulationTripPrebooker(passengerEngine));
        }

        LegCreator legCreator = params.onlineVehicleTracker ? //
                VrpLegs.createLegWithOnlineTrackerCreator(optimizer, qSim.getSimTimer()) : //
                VrpLegs.createLegWithOfflineTrackerCreator(qSim.getSimTimer());

        TaxiActionCreator actionCreator = new TaxiActionCreator(passengerEngine, legCreator,
                params.pickupDuration);

        VrpLauncherUtils.initAgentSources(qSim, context, optimizer, actionCreator);

        if (params.batteryChargingDischarging) {
            TaxiLauncherUtils.initChargersAndVehicles(taxiData);
            TaxiLauncherUtils.initChargingAndDischargingHandlers(taxiData, scenario.getNetwork(),
                    qSim, travelTime);
            TaxiLauncherUtils.initStatsCollection(taxiData, qSim, null);
        }

        beforeQSim(qSim);
        qSim.run();
        qSim.getEventsManager().finishProcessing();
        validateResults(taxiData);
        afterQSim(qSim);
    }


    TaxiOptimizerConfiguration createOptimizerConfiguration()
    {
        TaxiSchedulerParams schedulerParams = new TaxiSchedulerParams(params.destinationKnown,
                params.vehicleDiversion, params.pickupDuration, params.dropoffDuration);
        TaxiScheduler scheduler = new TaxiScheduler(context, pathCalculator, schedulerParams);
        VehicleRequestPathFinder vrpFinder = new VehicleRequestPathFinder(pathCalculator,
                scheduler);
        FilterFactory filterFactory = new DefaultFilterFactory(scheduler,
                params.nearestRequestsLimit, params.nearestVehiclesLimit);

        return new TaxiOptimizerConfiguration(context, pathCalculator, scheduler, vrpFinder,
                filterFactory, params.algorithmConfig.goal, params.outputDir, zones);
    }


    void beforeQSim(QSim qSim)
    {}


    void afterQSim(QSim qSim)
    {}


    void validateResults(ETaxiData taxiData)
    {
        // check if all reqs have been served
        for (TaxiRequest r : taxiData.getTaxiRequests().values()) {
            if (r.getStatus() != TaxiRequestStatus.PERFORMED) {
                throw new IllegalStateException();
            }
        }
    }
}
