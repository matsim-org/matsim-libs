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
import org.matsim.lanes.data.Lanes;
import playground.dgrether.koehlerstrehlersignal.analysis.TtTotalDelay;
import playground.dgrether.signalsystems.LinkSensorManager;
import signals.Analyzable;

import java.util.*;


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

    private final LaemmerConfig laemmerConfig;

    private final double DESIRED_PERIOD;

    private final double MAX_PERIOD;

    private Map<Id<Signal>, LaemmerSignal> signalId2LaemmerSignal;

    private Request activeRequest = null;
    private LinkSensorManager sensorManager;

    private Network network;
    private Lanes lanes;

    private final double DEFAULT_INTERGREEN;
    private double tIdle;

    private double flowSum;

    private TtTotalDelay delayCalculator;


    public final static class SignalControlProvider implements Provider<SignalController> {
        private final LaemmerConfig laemmerConfig;
        private final LinkSensorManager sensorManager;
        private final Network network;
        private final TtTotalDelay delayCalculator;
        private final Lanes lanes;

        public SignalControlProvider(LaemmerConfig laemmerConfig, LinkSensorManager sensorManager, Network network, Lanes lanes, TtTotalDelay delayCalculator) {
            this.laemmerConfig = laemmerConfig;
            this.sensorManager = sensorManager;
            this.network = network;
            this.lanes = lanes;
            this.delayCalculator = delayCalculator;
        }

        @Override
        public SignalController get() {
            return new LaemmerSignalController(laemmerConfig, sensorManager, network, lanes, delayCalculator);
        }
    }


    private LaemmerSignalController(LaemmerConfig laemmerConfig, LinkSensorManager sensorManager, Network network, Lanes lanes, TtTotalDelay delayCalculator) {
        this.laemmerConfig = laemmerConfig;
        this.sensorManager = sensorManager;
        this.network = network;
        this.lanes = lanes;
        this.delayCalculator = delayCalculator;
        DESIRED_PERIOD = laemmerConfig.getDESIRED_PERIOD();
        MAX_PERIOD = laemmerConfig.getMAX_PERIOD();
        DEFAULT_INTERGREEN = laemmerConfig.getDEFAULT_INTERGREEN();

    }

    @Override
    public void simulationInitialized(double simStartTimeSeconds) {
        signalId2LaemmerSignal = new HashMap<>();
        this.initializeSensoring();
        for (SignalGroup group : this.system.getSignalGroups().values()) {
            this.system.scheduleDropping(simStartTimeSeconds, group.getId());
            for (Signal signal : group.getSignals().values()) {
                LaemmerSignal laemmerSignal = new LaemmerSignal(group.getId(), signal);
                signalId2LaemmerSignal.put(signal.getId(), laemmerSignal);
                flowSum += laemmerSignal.determiningOutflow;
            }
        }
    }

    @Override
    public boolean analysisEnabled() {
        return this.laemmerConfig.analysisEnabled();
    }


    @Override
    public void updateState(double now) {
        log.info("-------------------------------------------------------------");
        log.info("Updating System" + this.system.getId() + ". Time: " + now);
        updateTIdle();
        if (!laemmerConfig.getActiveRegime().equals(LaemmerConfig.Regime.OPTIMIZING)) {
            updateActiveRegulation(now);
        }
        log.debug("T_Idle = " + tIdle);
        log.debug("Updating signals");
        updateSignals(now);
        LaemmerSignal selection = selectSignal();
        processSelection(now, selection);
    }

    private void updateActiveRegulation(double now) {
        if (activeRequest != null && !regulationQueue.isEmpty() && regulationQueue.peek().equals(activeRequest.signal)) {
            LaemmerSignal signal = regulationQueue.peek();
            log.info("Remaining regulation time for " + signal.signalId + ": " + (activeRequest.time + activeRequest.signal.regulationTime - now));
            int n;
            if(signal.determiningLane != null) {
                n = getNumberOfExpectedVehiclesOnLane(now, signal.link.getId(), signal.determiningLane);
            } else {
                n = getNumberOfExpectedVehiclesOnLink(now, signal.link.getId());
            }
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
            log.debug("Driveway changed. Creating new Request for " + (now + DEFAULT_INTERGREEN) + " granted time: " + max.regulationTime);
            activeRequest = new Request(now + DEFAULT_INTERGREEN, max);
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
            if (signal.stabilize && !regulationQueue.contains(signal)) {
                regulationQueue.add(signal);
            }
        }
    }

    private void updateTIdle() {
        tIdle = DESIRED_PERIOD;
        for (LaemmerSignal signal : signalId2LaemmerSignal.values()) {
            tIdle -= (signal.determiningLoad * DESIRED_PERIOD + DEFAULT_INTERGREEN);
        }
    }

    private int getNumberOfExpectedVehiclesOnLink(double now, Id<Link> linkId) {
        return this.sensorManager.getNumberOfCarsInDistance(linkId, 0., now);
    }

    private int getNumberOfExpectedVehiclesOnLane(double now, Id<Link> linkId, Id<Lane> laneId) {
        if(lanes.getLanesToLinkAssignments().get(linkId).getLanes().size()==1) {
            return getNumberOfExpectedVehiclesOnLink(now, linkId);
        } else {
            return this.sensorManager.getNumberOfCarsInDistanceOnLane(linkId, laneId, 0., now);
        }
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
                if (signal.getLaneIds() != null && !(signal.getLaneIds().isEmpty())) {
                    for (Id<Lane> laneId : signal.getLaneIds()) {
                        this.sensorManager.registerNumberOfCarsOnLaneInDistanceMonitoring(signal.getLinkId(), laneId, 0.);
                    }
                }
                //always register link in case only one lane is specified (-> no LaneEnter/Leave-Events?)
                this.sensorManager.registerNumberOfCarsInDistanceMonitoring(signal.getLinkId(), 0.);
            }
        }
    }

    @Override
    public String getStatFields() {

        StringBuilder builder = new StringBuilder();
        builder.append("t_idle;selected;total delay;");
        for (SignalGroup group : this.system.getSignalGroups().values()) {
            for (Signal signal : group.getSignals().values()) {
                LaemmerSignal laemmerSignal = signalId2LaemmerSignal.get(signal.getId());
                if (laemmerSignal != null) {
                    laemmerSignal.getStatFields(builder);
                }
            }
        }
        return builder.toString();
    }

    @Override
    public String getStepStats(double now) {
        if (signalId2LaemmerSignal == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        String selected = "none";
        if (activeRequest != null) {
            selected = activeRequest.signal.signalId.toString();
        }
        builder.append(tIdle + ";" + selected + ";" + delayCalculator.getTotalDelay() + ";");
        for (SignalGroup group : this.system.getSignalGroups().values()) {
            for (Signal signal : group.getSignals().values()) {
                LaemmerSignal laemmerSignal = signalId2LaemmerSignal.get(signal.getId());
                if (laemmerSignal != null) {
                    laemmerSignal.getStepStats(builder, now);
                }
            }
        }
        return builder.toString();
    }

    class Request {
        private final double time;
        private final LaemmerSignal signal;

        Request(double time, LaemmerSignal laemmerSignal) {
            this.signal = laemmerSignal;
            this.time = time;
        }

        private boolean isDue(double timeSeconds) {
            return timeSeconds == this.time;
        }
    }

    class LaemmerSignal {

        private Link link;
        Id<SignalGroup> group;
        Id<Signal> signalId;
        private Collection<Id<Lane>> signalLanes = null;

        double index = 0;
        private double abortionPenalty = 0;

        private boolean liveArrivalRate;
        private boolean stabilize = false;

        private double a = DEFAULT_INTERGREEN;
        private double regulationTime = 0;

        private Id<Lane> determiningLane;
        private double determiningArrivalRate;
        private double determiningOutflow;
        private double determiningLoad;

        LaemmerSignal(Id<SignalGroup> signalGroup, Signal signal) {

            this.group = signalGroup;
            this.signalId = signal.getId();
            link = network.getLinks().get(signal.getLinkId());

            if (signal.getLaneIds() != null && !signal.getLaneIds().isEmpty()) {
                this.signalLanes = signal.getLaneIds();
                if (laemmerConfig.getLaneArrivalRates(signal.getId()) == null || laemmerConfig.getLaneArrivalRates(signal.getId()).isEmpty()) {
                    this.liveArrivalRate = true;
                    //TODO: register method for lanes
                } else {
                    this.determiningLoad = 0;
                    Map<Id<Lane>, Double> laneArrivalRates = laemmerConfig.getLaneArrivalRates(signal.getId());
                    for (Id<Lane> laneId : signalLanes) {
                        double arrivalRate = laneArrivalRates.get(laneId);
                        double outflow = lanes.getLanesToLinkAssignments().get(link.getId()).getLanes().get(laneId).getCapacityVehiclesPerHour() / 3600;
                        double tempLoad = arrivalRate / outflow;
                        if (tempLoad > this.determiningLoad) {
                            this.determiningOutflow = outflow;
                            this.determiningLoad = tempLoad;
                            this.determiningArrivalRate = arrivalRate;
                            this.determiningLane = laneId;
                        }
                    }
                }

            } else {
                if (laemmerConfig.getSignalArrivalRate(signal.getId()) == null) {
                    this.liveArrivalRate = true;
                    sensorManager.registerAverageNumberOfCarsPerSecondMonitoring(signal.getLinkId());
                } else {
                    this.determiningOutflow = link.getCapacity() / 3600;
                    this.determiningArrivalRate = laemmerConfig.getSignalArrivalRate(signal.getId());
                    this.determiningLoad = this.determiningArrivalRate / this.determiningOutflow;
                }
            }
            signalLog.debug("LaemmeSignal for signal " + signal.getId() + " created.");
        }

        private void update(double now) {
            signalLog.info("---------------------------------------------------");
            signalLog.info("UPDATING " + signalId + " at " + now);

            updateAbortionPenalty(now);

            if (liveArrivalRate) {
                updateArrivalRate(now);
            }
            if (!laemmerConfig.getActiveRegime().equals(LaemmerConfig.Regime.OPTIMIZING)) {
                updateStabilization(now);
            }
            calculatePriorityIndex(now);
        }

        private void updateAbortionPenalty(double now) {
            this.abortionPenalty = 0;
            if (activeRequest != null && this.equals(activeRequest.signal)) {
                double waitingTimeSum = 0;
                double remainingInBetweenTime = Math.max(activeRequest.time - now, 0);
                for (double i = remainingInBetweenTime; i < DEFAULT_INTERGREEN; i++) {
                    if (signalLanes != null && !signalLanes.isEmpty()) {
                        for (Id<Lane> laneId : signalLanes) {
                            waitingTimeSum += getNumberOfExpectedVehiclesOnLane(now + i, link.getId(), laneId);
                        }
                    } else {
                        waitingTimeSum += getNumberOfExpectedVehiclesOnLink(now + i, link.getId());
                    }
                }
                double n = 0;
                if (signalLanes != null && !signalLanes.isEmpty()) {
                    for (Id<Lane> laneId : signalLanes) {
                        n += getNumberOfExpectedVehiclesOnLane(now + DEFAULT_INTERGREEN, link.getId(), laneId);
                    }
                } else {
                    n = getNumberOfExpectedVehiclesOnLink(now + DEFAULT_INTERGREEN, link.getId());
                }
                if (n > 0) {
                    this.abortionPenalty += waitingTimeSum / n;
                }
                signalLog.debug("Waiting time sum of " + waitingTimeSum + " in case of abortion. Resulting in " + this.abortionPenalty + "s penalty");
                signalLog.debug("Calculating Abortion Penalty.");
            }
        }

        private void calculatePriorityIndex(double now) {
            this.index = 0;
            if (activeRequest != null && activeRequest.signal == this) {
                signalLog.debug("Selected signal.");
                double remainingInBetweenTime = Math.max(activeRequest.time - now, 0);
                signalLog.debug("Remaining in between time " + remainingInBetweenTime);
                for (double i = remainingInBetweenTime; i <= DEFAULT_INTERGREEN; i++) {
                    double nExpected = 0;
                    double reqGreenTime = 0;
                    if (signalLanes != null && !signalLanes.isEmpty()) {
                        for (Id<Lane> laneId : signalLanes) {
                            double nTemp = getNumberOfExpectedVehiclesOnLane(now + i, link.getId(), laneId);
                            nExpected += nTemp;
                            double laneFlow = lanes.getLanesToLinkAssignments().get(link.getId()).getLanes().get(laneId).getCapacityVehiclesPerHour() / 3600;
                            double tempGreenTime = nTemp / laneFlow;
                            if (tempGreenTime > reqGreenTime) {
                                reqGreenTime = tempGreenTime;
                            }
                        }
                    } else {
                        nExpected = getNumberOfExpectedVehiclesOnLink(now + i, link.getId());
                        reqGreenTime = nExpected / this.determiningOutflow;
                    }
                    double tempIndex = nExpected / (i + reqGreenTime);
                    signalLog.debug("n=" + nExpected + " at second " + (now + i) + "resulting in " + reqGreenTime + " required green time. Index: " + tempIndex);
                    if (tempIndex > index) {
                        index = tempIndex;
                        signalLog.info("Index: " + index);
                    }
                }
            } else {
                signalLog.debug("Non-Active signal.");
                double nExpected = 0;
                double reqGreenTime = 0;
                if (signalLanes != null && !signalLanes.isEmpty()) {
                    for (Id<Lane> laneId : signalLanes) {
                        double nTemp = getNumberOfExpectedVehiclesOnLane(now + DEFAULT_INTERGREEN, link.getId(), laneId);
                        nExpected += nTemp;
                        double laneFlow = lanes.getLanesToLinkAssignments().get(link.getId()).getLanes().get(laneId).getCapacityVehiclesPerHour() / 3600;
                        double tempGreenTime = nTemp / laneFlow;
                        if (tempGreenTime > reqGreenTime) {
                            reqGreenTime = tempGreenTime;
                        }
                    }
                } else {
                    nExpected = getNumberOfExpectedVehiclesOnLink(now + DEFAULT_INTERGREEN, link.getId());
                    reqGreenTime = nExpected / this.determiningOutflow;
                }
                double penalty = 0;
                if (activeRequest != null) {
                    penalty = activeRequest.signal.abortionPenalty;
                }
                index = nExpected / (penalty + DEFAULT_INTERGREEN + reqGreenTime);
                signalLog.info("n=" + nExpected + " at second " + (now + DEFAULT_INTERGREEN) + "resulting in " + reqGreenTime + " required green time. Penalty: " + penalty + ".  Index: " + index);
            }
        }

        private void updateArrivalRate(double now) {
            //TODO: update for lanes, determining lane loadfactor
            determiningArrivalRate = getAverageArrivalRate(now, link.getId());
            determiningLoad = determiningArrivalRate / determiningOutflow;
            signalLog.debug("Avg Arrival Rate: " + determiningArrivalRate + ", load factor: " + determiningLoad);
        }

        private void updateStabilization(double now) {

            if (determiningArrivalRate == 0) {
                return;
            }

            signalLog.debug("Checking for regulation");

            double n = 0;
            if (signalLanes != null && !signalLanes.isEmpty()) {
                n = getNumberOfExpectedVehiclesOnLane(now, link.getId(), determiningLane);
            } else {
                n = getNumberOfExpectedVehiclesOnLink(now, link.getId());
            }

            if (n == 0) {
                a = DEFAULT_INTERGREEN;
            } else {
                a++;
            }

            if (regulationQueue.contains(this)) {
                signalLog.debug("Already in regulation queue. Aborting.");
                return;
            }

            this.regulationTime = 0;
            this.stabilize = false;
            double nCrit = determiningArrivalRate * DESIRED_PERIOD
                    * ((MAX_PERIOD - (a / (1 - determiningLoad)))
                    / (MAX_PERIOD - DESIRED_PERIOD));
            signalLog.debug("n = " + n);
            signalLog.debug("nCrit = " + nCrit);
            if (n >= nCrit) {
                regulationQueue.add(this);
                signalLog.debug("Regulation time parameters: lambda: " + determiningLoad + " | T: " + DESIRED_PERIOD + " | qmax: " + determiningOutflow + " | qsum: " + flowSum + " | T_idle:" + tIdle);
                this.regulationTime = Math.rint(determiningLoad * DESIRED_PERIOD + (determiningOutflow / flowSum) * tIdle);
                signalLog.info("Granted time " + regulationTime);
                this.stabilize = true;
            }
        }

        public void getStatFields(StringBuilder builder) {
            builder.append("index_" + this.signalId + ";");
            builder.append("load_" + this.signalId + ";");
            builder.append("a_" + this.signalId + ";");
            builder.append("abortionPen_" + this.signalId + ";");
            builder.append("regTime_" + this.signalId + ";");
            builder.append("nTotal_" + this.signalId + ";");
        }

        public void getStepStats(StringBuilder builder, double now) {
            int totalN = 0;
            if (signalLanes != null && !signalLanes.isEmpty()) {
                for (Id<Lane> laneId : signalLanes) {
                    totalN += getNumberOfExpectedVehiclesOnLane(now, link.getId(), laneId);
                }
            } else {
                totalN = getNumberOfExpectedVehiclesOnLink(now, link.getId());
            }
            builder.append(this.index + ";")
                    .append(this.determiningLoad + ";")
                    .append(this.a + ";")
                    .append(this.abortionPenalty + ";")
                    .append(this.regulationTime + ";")
                    .append(totalN+ ";");
        }
    }
}
