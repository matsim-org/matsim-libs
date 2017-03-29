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
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.model.AbstractSignalController;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalController;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.lanes.data.Lane;
import playground.dgrether.signalsystems.LinkSensorManager;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;


/**
 * @author dgrether
 * @author tthunig
 */
public class LaemmerSignalController extends AbstractSignalController implements SignalController {

    public static final Logger log = Logger.getLogger(LaemmerSignalController.class);
    public static final Logger drivewayLog = Logger.getLogger(LaemmerSignalController.LaemmerSignal.class);

    public static final String IDENTIFIER = "LaemmerSignalControl";

    private Queue<LaemmerSignal> regulationQueue = new LinkedList<>();

    //TODO: Parametrize periods
    private static final double DESIRED_PERIOD = 90.;
    private static final double MAX_PERIOD = 120.;

    private List<LaemmerSignal> signals;

    private Request activeRequest = null;

    private LinkSensorManager sensorManager;
    private SignalsData signalsData;
    private Network network;

    private final double DEFAULT_INBETWEEN = 5;

    private double tIdle;
    private double flowSum;


    public final static class SignalControlProvider implements Provider<SignalController> {
        private final LinkSensorManager sensorManager;
        private final SignalsData signalsData;
        private final Network network;

        public SignalControlProvider(LinkSensorManager sensorManager, SignalsData signalsData, Network network) {
            this.sensorManager = sensorManager;
            this.signalsData = signalsData;
            this.network = network;
        }

        @Override
        public SignalController get() {
            return new LaemmerSignalController(sensorManager, signalsData, network);
        }
    }


    private LaemmerSignalController(LinkSensorManager sensorManager, SignalsData signalsData, Network network) {
        this.sensorManager = sensorManager;
        this.signalsData = signalsData;
        this.network = network;
    }

    @Override
    public void simulationInitialized(double simStartTimeSeconds) {
        this.initializeSensoring();
        signals = new ArrayList<>();
        for (SignalGroup group : this.system.getSignalGroups().values()) {
            this.system.scheduleDropping(simStartTimeSeconds, group.getId());
            //TODO: Zwischenzeiten haengen nicht nur von aktueller Phase ab
            for (Signal signal : group.getSignals().values()) {
                flowSum += network.getLinks().get(signal.getLinkId()).getCapacity() / 3600.;
                signals.add(new LaemmerSignal(group.getId(), signal));
            }
        }
    }

    @Override
    public void updateState(double now) {

        log.info("-------------------------------------------------------------");
        log.info("Updating System" + this.system.getId() + ". Time: " + now);

        log.info("Updating signals");

        if (!regulationQueue.isEmpty() && regulationQueue.peek().equals(activeRequest.signal)) {
            LaemmerSignal signal = regulationQueue.peek();
            log.info( "Remaining regulation time for " + signal.signal.getId() + ": " + (activeRequest.time + activeRequest.regulationTime - now) );
            int n = getNumberOfExpectedVehicles(now, signal.link.getId());
            if (activeRequest.regulationTime + activeRequest.time - now <= 0 || n == 0) {
                regulationQueue.poll();
                log.info("Stopping regulation for " + signal.signal.getId() + " remaining vehicles: " + n);
            }
        }

        tIdle = DESIRED_PERIOD;
        for (LaemmerSignal signal : signals) {
            signal.update(now);
            tIdle -= (signal.loadFactor * DESIRED_PERIOD + DEFAULT_INBETWEEN);
        }
        log.info("T_Idle = " + tIdle);


        LaemmerSignal max = regulationQueue.peek();
        if(max==null) {
            log.info("no queue selected.");
        } else {
            log.info("regulating for " + max.signal.getId());
        }
//        if (max == null) {
//            double index = 0;
//            for (LaemmerSignal signal : signals) {
//                if (signal.index > index) {
//                    max = signal;
//                    index = signal.index;
//                    log.info("Max index of " + index + ".Signal " + max.signal.getId() + " of Group " + max.group);
//                }
//            }
//        }

        if (activeRequest != null && activeRequest.signal != max) {
            log.info("Dropping group " + activeRequest.signal.group + " after " + (now - activeRequest.time + "s."));
            this.system.scheduleDropping(now, activeRequest.signal.group);
            activeRequest = null;
        }

        if (activeRequest == null && max != null || activeRequest != null && max != activeRequest.signal && max != null) {
            log.info("Driveway changed. Creating new Request for " + (now + DEFAULT_INBETWEEN) + " granted time: " + max.regulationTime);
            activeRequest = new Request(now + DEFAULT_INBETWEEN, max, max.regulationTime);
        }

        if (activeRequest != null) {
            if (activeRequest.isDue(now)) {
                log.info("Setting group " + activeRequest.signal.group);
                this.system.scheduleOnset(now, activeRequest.signal.group);
            } else if (activeRequest.time > now) {
                log.info("Remaining in-between-time " + (activeRequest.time - now));
            }
        }
    }


    /**
     * @return \hat{n_i} (t)
     */
    private int getNumberOfVehiclesForClearance() {

        return 0;
    }


    /**
     * Zeitreihe der erwarteten Ankuenfte an der Haltelinie
     * <p>
     * N_i^{exp}(t + \hat(g)_i))
     */
    private int getNumberOfExpectedVehicles(double timeSeconds, Id<Link> linkId) {
        Link link = network.getLinks().get(linkId);
        return this.sensorManager.getNumberOfCarsInDistance(linkId, 0., timeSeconds);
    }

    private double getAverageArrivalRate(double timeSeconds, Id<Link> linkId) {
        Link link = network.getLinks().get(linkId);
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
                    this.sensorManager.registerAverageNumberOfCarsPerSecondMonitoring(signal.getLinkId());
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
        final double time;
        final LaemmerSignal signal;
        final double regulationTime;

        Request(double time, LaemmerSignal laemmerSignal, double regulationTime) {
            this.signal = laemmerSignal;
            this.time = time;
            this.regulationTime = regulationTime;
        }

        public boolean isDue(double timeSeconds) {
            return timeSeconds == this.time;
        }
    }

    class LaemmerSignal {

        Id<SignalGroup> group;
        Signal signal;
        double index = 0;
        private double abortionPenalty = 0;
        private double loadFactor;
        private double a = DEFAULT_INBETWEEN;
        private Link link;
        private double maxFlow;
        private double regulationTime = 0;

        LaemmerSignal(Id<SignalGroup> signalGroup, Signal signal) {
            this.group = signalGroup;
            this.signal = signal;
            link = network.getLinks().get(signal.getLinkId());
            maxFlow = network.getLinks().get(signal.getLinkId()).getCapacity() / 3600.;
            drivewayLog.info("LaemmeSignal for signal " + signal.getId() + " created.");
        }

        public void update(double now) {
            drivewayLog.info("---------------------------------------------------");
            drivewayLog.info("UPDATING " + signal.getId() + " at " + now);

            if (activeRequest != null && this.equals(activeRequest.signal)) {
                calculateAbortionPenalty(now);
                drivewayLog.info("Calculating Abortion Penalty.");
            } else {
                this.abortionPenalty = 0;
            }

            updateArrivalRate(now);
            if (regulate(now)) {
                return;
            }

            if (activeRequest != null && activeRequest.signal == this) {
                calculateActivePriorityIndex(now);
            } else {
                calculateInactivePriorityIndex(now);
            }
        }

        private void calculateInactivePriorityIndex(double now) {
            drivewayLog.info("Non-Active driveway.");
            double nExpected = getNumberOfExpectedVehicles(now + DEFAULT_INBETWEEN, link.getId());
            double reqGreenTime = nExpected / maxFlow;
            double penalty = 0;
            if (activeRequest != null) {
                penalty = activeRequest.signal.abortionPenalty;
            }
            index = nExpected / (penalty + DEFAULT_INBETWEEN + reqGreenTime);
            drivewayLog.info("n=" + nExpected + " at second " + (now + DEFAULT_INBETWEEN) + "resulting in " + reqGreenTime + " required green time. Penalty: " + penalty + ".  Index: " + index);
        }

        private void calculateActivePriorityIndex(double now) {
            drivewayLog.info("Active driveway.");
            double remainingInBetweenTime = Math.max(activeRequest.time - now, 0);
            drivewayLog.info("Remaining in between time " + remainingInBetweenTime);
            for (double i = remainingInBetweenTime; i <= DEFAULT_INBETWEEN; i++) {
                double nExpected = getNumberOfExpectedVehicles(now + i, link.getId());
                double reqGreenTime = nExpected / maxFlow;
                double tempIndex = nExpected / (i + reqGreenTime);
                drivewayLog.info("n=" + nExpected + " at second " + (now + i) + "resulting in " + reqGreenTime + " required green time. Index: " + tempIndex);
                if (tempIndex > index) {
                    index = tempIndex;
                    drivewayLog.info("Index: " + index);
                }
            }
        }

        private void updateArrivalRate(double now) {
            double avgArrivalRate = getAverageArrivalRate(now, link.getId());
            loadFactor = avgArrivalRate / maxFlow;
            drivewayLog.info("Avg Arrival Rate: " + avgArrivalRate + ", load factor: " + loadFactor);
        }

        private void calculateAbortionPenalty(double timeSeconds) {
            this.abortionPenalty = 0;
            double waitingTimeSum = 0;
            double remainingInBetweenTime = Math.max(activeRequest.time - timeSeconds, 0);
            for (double i = remainingInBetweenTime; i < DEFAULT_INBETWEEN; i++) {
                waitingTimeSum += getNumberOfExpectedVehicles(timeSeconds + i, link.getId());
            }
            double n = getNumberOfExpectedVehicles(timeSeconds + DEFAULT_INBETWEEN, link.getId());
            if (n > 0) {
                this.abortionPenalty += waitingTimeSum / n;
            }
            drivewayLog.info("Waiting time sum of " + waitingTimeSum + " in case of abortion. Resulting in " + this.abortionPenalty + "s penalty");
        }

        private boolean regulate(double timeSeconds) {

            drivewayLog.info("Checking for regulation");

            double n = getNumberOfExpectedVehicles(timeSeconds, link.getId());
            double avgArrivalRate = getAverageArrivalRate(timeSeconds, link.getId());

            if (n == 0) {
                a = DEFAULT_INBETWEEN;
                drivewayLog.info("Queue length zero, setting a to inbetween time");
            } else {
                a++;
                drivewayLog.info("a set to " + a);
            }
            if (regulationQueue.contains(this)) {
                drivewayLog.info("Already in regulation queue. Aborting.");
                return false;
            }
            this.regulationTime = 0;
            double nCrit = avgArrivalRate * DESIRED_PERIOD
                    * ((MAX_PERIOD - (a / (1 - avgArrivalRate / maxFlow)))
                    / (MAX_PERIOD - DESIRED_PERIOD));
            drivewayLog.info("n = " + n);
            drivewayLog.info("nCrit = " + nCrit);
            //TODO: keep n>1?
            if (n >= nCrit && n > 1) {
                regulationQueue.add(this);
                this.regulationTime = Math.ceil(this.loadFactor * DESIRED_PERIOD + (maxFlow / flowSum) * tIdle);
                drivewayLog.info("Adding to regulation queue with granted time " + regulationTime );
                return true;
            } else {
                drivewayLog.info("No need for regulation.");
                return false;
            }
        }
    }
}
