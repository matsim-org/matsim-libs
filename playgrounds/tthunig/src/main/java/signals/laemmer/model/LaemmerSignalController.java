/* *********************************************************************** *
 * project: org.matsim.*
 * DgTaController
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package signals.laemmer.model;

import com.google.inject.Provider;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.signals.model.AbstractSignalController;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalController;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.lanes.data.Lane;
import playground.dgrether.koehlerstrehlersignal.analysis.TtTotalDelay;
import playground.dgrether.signalsystems.LinkSensorManager;
import signals.Analyzable;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;


/**
 * @author dgrether
 * @author tthunig
 * @author nkuehnel
 */
public class LaemmerSignalController extends AbstractSignalController implements SignalController, Analyzable {

    public static final Logger log = Logger.getLogger(LaemmerSignalController.class);
    public static final Logger signalLog = Logger.getLogger(LaemmerSignalController.LaemmerSignal.class);

    public static final String IDENTIFIER = "LaemmerSignalControl";

    private Queue<LaemmerSignal> regulationQueue = new LinkedList<>();

    LaemmerConfig laemmerConfig;

    private final double DESIRED_PERIOD;

    private final double MAX_PERIOD;

    private Map<Id<Signal>, LaemmerSignal> signalId2LaemmerSignal;

    private Request activeRequest = null;
    private LinkSensorManager sensorManager;

    private Network network;

    private final double DEFAULT_INBETWEEN = 5;
    private double tIdle;

    private double flowSum;

    private TtTotalDelay delayCalculator;


    public final static class SignalControlProvider implements Provider<SignalController> {
        private final LaemmerConfig laemmerConfig;
        private final LinkSensorManager sensorManager;
        private final Network network;
        private final TtTotalDelay delayCalculator;

        public SignalControlProvider(LaemmerConfig laemmerConfig, LinkSensorManager sensorManager, Network network, TtTotalDelay delayCalculator) {
            this.laemmerConfig = laemmerConfig;
            this.sensorManager = sensorManager;
            this.network = network;
            this.delayCalculator = delayCalculator;
        }

        @Override
        public SignalController get() {
            return new LaemmerSignalController(laemmerConfig, sensorManager, network, delayCalculator);
        }
    }


    private LaemmerSignalController(LaemmerConfig laemmerConfig, LinkSensorManager sensorManager, Network network, TtTotalDelay delayCalculator) {
        this.laemmerConfig = laemmerConfig;
        this.sensorManager = sensorManager;
        this.network = network;
        this.delayCalculator = delayCalculator;
        DESIRED_PERIOD = laemmerConfig.getDESIRED_PERIOD();
        MAX_PERIOD = laemmerConfig.getMAX_PERIOD();
    }

    @Override
    public void simulationInitialized(double simStartTimeSeconds) {
        signalId2LaemmerSignal = new HashMap<>();
        this.initializeSensoring();
        for (SignalGroup group : this.system.getSignalGroups().values()) {
            this.system.scheduleDropping(simStartTimeSeconds, group.getId());
            for (Signal signal : group.getSignals().values()) {
                flowSum += network.getLinks().get(signal.getLinkId()).getCapacity() / 3600.;
                signalId2LaemmerSignal.put(signal.getId(), new LaemmerSignal(group.getId(), signal));
            }
        }
    }

    @Override
    public boolean analysisEnabled() {
        return this.laemmerConfig.analysisEnabled();
    }


    @Override
    public String getStatFields() {

        StringBuilder builder = new StringBuilder();
        builder.append("t_idle;selected;total delay;");
        for (SignalGroup group : this.system.getSignalGroups().values()) {
            for (Signal signal : group.getSignals().values()) {
                builder.append("index_" + signal.getId() + ";");
                builder.append("load_" + signal.getId() + ";");
                builder.append("a_" + signal.getId() + ";");
                builder.append("abortionPen_" + signal.getId() + ";");
                builder.append("regTime_" + signal.getId() + ";");
                builder.append("n_" + signal.getId() + ";");
            }
        }
        return builder.toString();
    }

    @Override
    public String getStepStats(double now) {
        if (signalId2LaemmerSignal == null) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        String selected = "none";
        if (activeRequest != null) {
            selected = activeRequest.signal.signalId.toString();
        }
        stringBuilder.append(tIdle + ";" + selected + ";" + delayCalculator.getTotalDelay() + ";");
        for (SignalGroup group : this.system.getSignalGroups().values()) {
            for (Signal signal : group.getSignals().values()) {
                LaemmerSignal laemmerSignal = signalId2LaemmerSignal.get(signal.getId());
                if (laemmerSignal != null) {
                    stringBuilder.append(laemmerSignal.index + ";")
                            .append(laemmerSignal.loadFactor + ";")
                            .append(laemmerSignal.a + ";")
                            .append(laemmerSignal.abortionPenalty + ";")
                            .append(laemmerSignal.regulationTime + ";")
                            .append(getNumberOfExpectedVehicles(now, laemmerSignal.link.getId()) + ";");
                }
            }
        }
        return stringBuilder.toString();
    }

    @Override
    public void updateState(double now) {
        log.info("-------------------------------------------------------------");
        log.info("Updating System" + this.system.getId() + ". Time: " + now);
        updateTIdle();
        updateActiveRegulation(now);
        log.debug("T_Idle = " + tIdle);
        log.debug("Updating signals");
        updateSignals(now);
        LaemmerSignal selection = selectSignal();
        processSelection(now, selection);
    }

    private void updateActiveRegulation(double now) {
        if (activeRequest!= null && !regulationQueue.isEmpty() && regulationQueue.peek().equals(activeRequest.signal)) {
            LaemmerSignal signal = regulationQueue.peek();
            log.info("Remaining regulation time for " + signal.signalId + ": " + (activeRequest.time + activeRequest.signal.regulationTime - now));
            int n = getNumberOfExpectedVehicles(now, signal.link.getId());
            if (activeRequest.signal.regulationTime + activeRequest.time - now <= 0 || n == 0) {
                regulationQueue.poll();
                log.info("Stopping regulation for " + signal.signalId + " remaining vehicles: " + n);
            }
        }
    }

    private LaemmerSignal selectSignal() {
        LaemmerSignal max = null;
        if (!laemmerConfig.getActiveRegime().equals(LaemmerConfig.Regime.OPTIMIZING)) {
            max = regulationQueue.peek();
            if (max == null) {
                log.debug("no queue selected.");
            } else {
                log.debug("regulating for " + max.signalId);
            }
        }
        if (!laemmerConfig.getActiveRegime().equals(LaemmerConfig.Regime.STABILIZING)) {
            if (max == null) {
                double index = 0;
                for (LaemmerSignal signal : signalId2LaemmerSignal.values()) {
                    if (signal.index > index) {
                        max = signal;
                        index = signal.index;
                        log.debug("Max index of " + index + ".Signal " + max.signalId + " of Group " + max.group);
                    }
                }
            }
        }
        return max;
    }

    private void processSelection(double now, LaemmerSignal max) {

        if (activeRequest != null && activeRequest.signal != max) {
            log.info("Dropping group " + activeRequest.signal.group + " after " + (now - activeRequest.time + "s."));
            this.system.scheduleDropping(now, activeRequest.signal.group);
            activeRequest = null;
        }

        if (activeRequest == null && max != null || activeRequest != null && max != null && !max.equals(activeRequest.signal)) {
            log.debug("Driveway changed. Creating new Request for " + (now + DEFAULT_INBETWEEN) + " granted time: " + max.regulationTime);
            activeRequest = new Request(now + DEFAULT_INBETWEEN, max);
        }

        if (activeRequest != null) {
            if (activeRequest.isDue(now)) {
                log.info("Setting group " + activeRequest.signal.group);
                this.system.scheduleOnset(now, activeRequest.signal.group);
            } else if (activeRequest.time > now) {
                log.debug("Remaining in-between-time " + (activeRequest.time - now));
            }
        }
    }

    private void updateSignals(double now) {
        for (LaemmerSignal signal : signalId2LaemmerSignal.values()) {
            signal.update(now);
            if(signal.stabilize  && !regulationQueue.contains(signal)) {
                regulationQueue.add(signal);
            }
        }
    }

    private void updateTIdle() {
        tIdle = DESIRED_PERIOD;
        for (LaemmerSignal signal : signalId2LaemmerSignal.values()) {
            tIdle -= (signal.loadFactor * DESIRED_PERIOD + DEFAULT_INBETWEEN);
        }
    }

    private int getNumberOfExpectedVehicles(double timeSeconds, Id<Link> linkId) {
        return this.sensorManager.getNumberOfCarsInDistance(linkId, 0., timeSeconds);
    }

    private double getAverageArrivalRate(double timeSeconds, Id<Link> linkId) {
        return this.sensorManager.getAverageArrivalRate(linkId, timeSeconds);
    }


    @Override
    public void reset(Integer iterationNumber) {
    }

    private void initializeSensoring() {
        for (SignalGroup group : this.system.getSignalGroups().values()) {
            for (Signal signal : group.getSignals().values()) {
                if (signal.getLaneIds() == null || signal.getLaneIds().isEmpty()) {
                    //					log.error("system: " + this.system.getId() + " signal: " + signal.getId() + " has no lanes...");
                    this.sensorManager.registerNumberOfCarsInDistanceMonitoring(signal.getLinkId(), 0.);
                } else {
                    for (Id<Lane> laneId : signal.getLaneIds()) {
                        //TODO check this part again concerning implementation of CarsOnLaneHandler
                        this.sensorManager.registerNumberOfCarsMonitoringOnLane(signal.getLinkId(), laneId);
                    }
                }
            }
        }
    }

    class Request {
        private final double time;
        private final LaemmerSignal signal;

        Request(double time, LaemmerSignal laemmerSignal) {
            this.signal = laemmerSignal;
            this.time = time;
        }

        public boolean isDue(double timeSeconds) {
            return timeSeconds == this.time;
        }
    }

    class LaemmerSignal {

        private boolean stabilize = false;
        private boolean liveArrivalRate;
        Id<SignalGroup> group;
        Id<Signal> signalId;
        double index = 0;
        private double abortionPenalty = 0;
        private double loadFactor;
        private double a = DEFAULT_INBETWEEN;
        private Link link;
        private double maxFlow;
        private double regulationTime = 0;
        private double avgArrivalRate;

        LaemmerSignal(Id<SignalGroup> signalGroup, Signal signal) {

            this.group = signalGroup;
            this.signalId = signal.getId();
            link = network.getLinks().get(signal.getLinkId());
            maxFlow = network.getLinks().get(signal.getLinkId()).getCapacity() / 3600.;


            if (laemmerConfig.getArrivalRateForSignal(signal.getId()) == null) {
                this.liveArrivalRate = true;
                sensorManager.registerAverageNumberOfCarsPerSecondMonitoring(signal.getLinkId());
            } else {
                this.avgArrivalRate = laemmerConfig.getArrivalRateForSignal(signal.getId());
                this.loadFactor = this.avgArrivalRate / this.maxFlow;
            }

            signalLog.debug("LaemmeSignal for signal " + signal.getId() + " created.");

        }

        public void update(double now) {
            signalLog.info("---------------------------------------------------");
            signalLog.info("UPDATING " + signalId + " at " + now);

            updateAbortionPenalty(now);

            if (liveArrivalRate) {
                updateArrivalRate(now);
            }
            updateStabilization(now);
            calculatePriorityIndex(now);
        }

        private void updateAbortionPenalty(double now) {
            if (activeRequest != null && this.equals(activeRequest.signal)) {
                this.abortionPenalty = 0;
                double waitingTimeSum = 0;
                double remainingInBetweenTime = Math.max(activeRequest.time - now, 0);
                for (double i = remainingInBetweenTime; i < DEFAULT_INBETWEEN; i++) {
                    waitingTimeSum += getNumberOfExpectedVehicles(now + i, link.getId());
                }
                double n = getNumberOfExpectedVehicles(now + DEFAULT_INBETWEEN, link.getId());
                if (n > 0) {
                    this.abortionPenalty += waitingTimeSum / n;
                }
                signalLog.debug("Waiting time sum of " + waitingTimeSum + " in case of abortion. Resulting in " + this.abortionPenalty + "s penalty");
                signalLog.debug("Calculating Abortion Penalty.");
            } else {
                this.abortionPenalty = 0;
            }
        }

        private void calculatePriorityIndex(double now) {
            this.index = 0;
            if (activeRequest != null && activeRequest.signal == this) {
                signalLog.debug("Selected signal.");
                double remainingInBetweenTime = Math.max(activeRequest.time - now, 0);
                signalLog.debug("Remaining in between time " + remainingInBetweenTime);
                for (double i = remainingInBetweenTime; i <= DEFAULT_INBETWEEN; i++) {
                    double nExpected = getNumberOfExpectedVehicles(now + i, link.getId());
                    double reqGreenTime = nExpected / maxFlow;
                    double tempIndex = nExpected / (i + reqGreenTime);
                    signalLog.debug("n=" + nExpected + " at second " + (now + i) + "resulting in " + reqGreenTime + " required green time. Index: " + tempIndex);
                    if (tempIndex > index) {
                        index = tempIndex;
                        signalLog.info("Index: " + index);
                    }
                }
            } else {
                signalLog.debug("Non-Active signal.");
                double nExpected = getNumberOfExpectedVehicles(now + DEFAULT_INBETWEEN, link.getId());
                double reqGreenTime = nExpected / maxFlow;
                double penalty = 0;
                if (activeRequest != null) {
                    penalty = activeRequest.signal.abortionPenalty;
                }
                index = nExpected / (penalty + DEFAULT_INBETWEEN + reqGreenTime);
                signalLog.info("n=" + nExpected + " at second " + (now + DEFAULT_INBETWEEN) + "resulting in " + reqGreenTime + " required green time. Penalty: " + penalty + ".  Index: " + index);
            }
        }

        private void updateArrivalRate(double now) {
            avgArrivalRate = getAverageArrivalRate(now, link.getId());
            loadFactor = avgArrivalRate / maxFlow;
            signalLog.debug("Avg Arrival Rate: " + avgArrivalRate + ", load factor: " + loadFactor);
        }

        private void updateStabilization(double timeSeconds) {

            if(avgArrivalRate == 0) {
                return;
            }

            signalLog.debug("Checking for regulation");

            double n = getNumberOfExpectedVehicles(timeSeconds, link.getId());

            if(n == 0) {
               a = DEFAULT_INBETWEEN;
            } else {
                a++;
            }

            if (regulationQueue.contains(this)) {
                signalLog.debug("Already in regulation queue. Aborting.");
                return;
            }

            this.regulationTime = 0;
            this.stabilize = false;

            double nCrit = avgArrivalRate * DESIRED_PERIOD
                    * ((MAX_PERIOD - (a / (1 - avgArrivalRate / maxFlow)))
                    / (MAX_PERIOD - DESIRED_PERIOD));
            signalLog.debug("n = " + n);
            signalLog.debug("nCrit = " + nCrit);
            if (n >= nCrit) {
                regulationQueue.add(this);
                signalLog.debug("Regulation time parameters: lambda: " + this.loadFactor + " | T: " + DESIRED_PERIOD + " | qmax: " + maxFlow + " | qsum: " + flowSum + " | T_idle:" + tIdle);
                this.regulationTime = Math.rint(this.loadFactor * DESIRED_PERIOD + (maxFlow / flowSum) * tIdle);
                signalLog.info("Granted time " + regulationTime);
                this.stabilize = true;
            }
        }
    }
}
