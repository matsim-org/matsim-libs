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

import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.*;
import org.matsim.contrib.dvrp.extensions.taxi.TaxiUtils;
import org.matsim.contrib.dvrp.passenger.*;
import org.matsim.contrib.dvrp.run.*;
import org.matsim.contrib.dvrp.vrpagent.VrpLegs;
import org.matsim.contrib.dvrp.vrpagent.VrpLegs.LegCreator;
import org.matsim.contrib.dynagent.run.DynAgentLauncherUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.*;

import playground.michalm.demand.taxi.PersonCreatorWithRandomTaxiMode;
import playground.michalm.taxi.*;
import playground.michalm.taxi.data.*;
import playground.michalm.taxi.data.TaxiRequest.TaxiRequestStatus;
import playground.michalm.taxi.data.file.*;
import playground.michalm.taxi.ev.*;
import playground.michalm.taxi.optimizer.*;
import playground.michalm.taxi.optimizer.AbstractTaxiOptimizerParams.TravelTimeSource;
import playground.michalm.taxi.scheduler.TaxiSchedulerParams;
import playground.michalm.taxi.util.stats.*;
import playground.michalm.taxi.util.stats.StatsCollector.StatsCalculator;


class TaxiLauncher
{
    final Configuration config;

    final TaxiLauncherParams launcherParams;
    final Scenario scenario;

    MatsimVrpContext context;
    AbstractTaxiOptimizerParams optimParams;
    TravelTimeCalculator travelTimeCalculator;

    private TravelTime travelTime;
    static final int TIME_INTERVAL = 15 * 60; //15 minutes


    TaxiLauncher(Configuration config)
    {
        this.config = config;
        this.launcherParams = new TaxiLauncherParams(config);

        int hours = 30;//TODO migrate to TaxiParams??
        scenario = VrpLauncherUtils.initScenario(launcherParams.netFile, launcherParams.plansFile,
                launcherParams.changeEventsFile, TIME_INTERVAL, hours * 3600 / TIME_INTERVAL);

        //overwrites the values set by VrpLauncherUtils.initScenario()
        QSimConfigGroup qsc = scenario.getConfig().qsim();
        qsc.setStorageCapFactor(launcherParams.storageCapFactor);
        qsc.setFlowCapFactor(launcherParams.flowCapFactor);

        if (launcherParams.taxiCustomersFile != null) {
            List<String> passengerIds = PersonCreatorWithRandomTaxiMode
                    .readTaxiCustomerIds(launcherParams.taxiCustomersFile);
            VrpPopulationUtils.convertLegModes(passengerIds, TaxiUtils.TAXI_MODE, scenario);
        }

        //TaxiDemandUtils.preprocessPlansBasedOnCoordsOnly(scenario);
    }


    void initTravelTimeAndDisutility()
    {
        if (optimParams.travelTimeSource == TravelTimeSource.FREE_FLOW_SPEED) {
            //works for TimeVariantLinks
            travelTime = new FreeSpeedTravelTime();
        }
        else {// TravelTimeSource.EVENTS
            if (travelTimeCalculator == null) {
                travelTimeCalculator = VrpLauncherUtils.initTravelTimeCalculatorFromEvents(scenario,
                        launcherParams.eventsFile, 15 * 60);
            }

            travelTime = travelTimeCalculator.getLinkTravelTimes();
        }
    }


    /**
     * Can be called several times (1 call == 1 simulation)
     */
    void simulateIteration(String simId)
    {
        MatsimVrpContextImpl contextImpl = new MatsimVrpContextImpl();
        this.context = contextImpl;
        contextImpl.setScenario(scenario);

        ETaxiData taxiData = new ETaxiData();
        new ETaxiReader(scenario, taxiData).parse(launcherParams.taxisFile);
        if (launcherParams.ranksFile != null) {
            new TaxiRankReader(scenario, taxiData).parse(launcherParams.ranksFile);
        }
        contextImpl.setVrpData(taxiData);

        TaxiSchedulerParams schedulerParams = new TaxiSchedulerParams(
                TaxiConfigUtils.getSchedulerConfig(config));
        if (schedulerParams.vehicleDiversion && !launcherParams.onlineVehicleTracker) {
            throw new IllegalStateException("Diversion requires online tracking");
        }
        TaxiOptimizer optimizer = TaxiOptimizers.createOptimizer(contextImpl, travelTime,
                optimParams, schedulerParams);

        QSim qSim = DynAgentLauncherUtils.initQSim(scenario);
        contextImpl.setMobsimTimer(qSim.getSimTimer());
        qSim.addQueueSimulationListeners(optimizer);

        PassengerEngine passengerEngine = VrpLauncherUtils.initPassengerEngine(TaxiUtils.TAXI_MODE,
                new TaxiRequestCreator(), optimizer, context, qSim);
        if (launcherParams.prebookTripsBeforeSimulation) {
            qSim.addQueueSimulationListeners(new BeforeSimulationTripPrebooker(passengerEngine));
        }

        LegCreator legCreator = launcherParams.onlineVehicleTracker ? //
                VrpLegs.createLegWithOnlineTrackerCreator(optimizer, qSim.getSimTimer()) : //
                VrpLegs.createLegWithOfflineTrackerCreator(qSim.getSimTimer());
        TaxiActionCreator actionCreator = new TaxiActionCreator(passengerEngine, legCreator,
                schedulerParams.pickupDuration);
        VrpLauncherUtils.initAgentSources(qSim, context, optimizer, actionCreator);

        Configuration eTaxiConfig = TaxiConfigUtils.getETaxiConfig(config);
        if (!eTaxiConfig.isEmpty()) {
            ETaxiParams eTaxiParams = new ETaxiParams(eTaxiConfig);
            ETaxiUtils.initChargersAndVehicles(taxiData);
            qSim.getEventsManager().addHandler(ETaxiUtils.createDriveDischargingHandler(taxiData,
                    scenario.getNetwork(), travelTime, eTaxiParams));
            qSim.addQueueSimulationListeners(ETaxiUtils.createChargingAuxDischargingHandler(
                    taxiData, scenario.getNetwork(), travelTime, eTaxiParams));

            if (launcherParams.eTaxiStatsFile != null) {
                StatsCalculator<String> socStatsCalc = ETaxiUtils.createStatsCollection(taxiData);
                qSim.addQueueSimulationListeners(new StatsCollector<>(socStatsCalc, 300,
                        "mean [kWh]\tdischarged", launcherParams.eTaxiStatsFile + simId));
            }
        }

        if (launcherParams.taxiStatsFile != null) {
            StatsCalculator<String> dispatchStatsCalc = StatsCalculators.combineStatsCalculators(
                    StatsCalculators.createCurrentTaxiTaskOfTypeCounter(taxiData), //
                    StatsCalculators.createRequestsWithStatusCounter(taxiData,
                            TaxiRequestStatus.UNPLANNED));
            qSim.addQueueSimulationListeners(new StatsCollector<>(dispatchStatsCalc, 300,
                    StatsCalculators.TAXI_TASK_TYPES_HEADER + //
                    TaxiRequestStatus.UNPLANNED, //
                    launcherParams.taxiStatsFile + simId));
        }

        beforeQSim(qSim);
        qSim.run();
        qSim.getEventsManager().finishProcessing();
        validateResults(taxiData);
        afterQSim(qSim);
    }


    void beforeQSim(QSim qSim)
    {}


    void afterQSim(QSim qSim)
    {}


    private void validateResults(ETaxiData taxiData)
    {
        // check if all reqs have been served
        for (TaxiRequest r : taxiData.getTaxiRequests().values()) {
            if (r.getStatus() != TaxiRequestStatus.PERFORMED) {
                throw new IllegalStateException();
            }
        }
        
        //TODO check if all vehicles are done... (i.e. schedule.status==PERFORMED)
    }
}
