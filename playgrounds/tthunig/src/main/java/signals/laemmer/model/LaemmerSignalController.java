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

import java.util.*;


/**
 * @author dgrether
 * @author tthunig
 */
public class LaemmerSignalController extends AbstractSignalController implements SignalController {

    private static final Logger log = Logger.getLogger(LaemmerSignalController.class);

    public static final String IDENTIFIER = "LaemmerSignalControl";

    private Map<Id<Signal>, Double> avgArrivalRates;
    private Map<Id<Signal>, Double> timeSinceService;

    private Queue<Id<SignalGroup>> regulationQueue = new LinkedList<>();
    private double remainingRegulationTime = 0;

    //TODO: Parametrize periods
    private static final double DESIRED_PERIOD = 90.;
    private static final double MAX_PERIOD = 120.;

    private final double DEFAULT_AMBER = 3;

    private List<Driveway> driveways;
    private Driveway currentSelectedDriveway;


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

    private LinkSensorManager sensorManager;
    private SignalsData signalsData;
    private Network network;


    private LaemmerSignalController(LinkSensorManager sensorManager, SignalsData signalsData, Network network) {
        this.sensorManager = sensorManager;
        this.signalsData = signalsData;
        this.network = network;
    }

    @Override
    public void simulationInitialized(double simStartTimeSeconds) {
        avgArrivalRates = new HashMap<>();
        timeSinceService = new HashMap<>();

        this.initializeSensoring();
        driveways = new ArrayList<>();
        for (SignalGroup group : this.system.getSignalGroups().values()) {

            driveways.add(new Driveway(group));
            this.system.scheduleDropping(simStartTimeSeconds, group.getId());
            //TODO: Zwischenzeiten haengen nicht nur von aktueller Phase ab
            for (Signal signal : group.getSignals().values()) {
                timeSinceService.put(signal.getId(), Double.valueOf(signalsData.getAmberTimesData().getDefaultAmber()));
            }
        }


    }

    @Override
    public void updateState(double timeSeconds) {

        for (Driveway driveway : driveways) {
            driveway.update(timeSeconds);
        }

        Collections.sort(driveways);

        Driveway max = driveways.get(0);
        if (currentSelectedDriveway == null || !max.equals(currentSelectedDriveway)) {
            if (currentSelectedDriveway != null) {
                currentSelectedDriveway.toggleSelection();
                this.system.scheduleDropping(timeSeconds, currentSelectedDriveway.group.getId());
            }
            currentSelectedDriveway = max;
            currentSelectedDriveway.toggleSelection();
            this.system.scheduleOnset(timeSeconds + currentSelectedDriveway.remainingInBetweenTime, currentSelectedDriveway.group.getId());
        }


//        updateRegulationQueue(timeSeconds);
//
//        if (remainingRegulationTime < 1) {
//            Id<SignalGroup> grp;
//            grp = regulationQueue.peek();
//            if (grp == null) {
//                grp = selectSignalGroup(timeSeconds);
//            }
//            processSignalGroup(timeSeconds, grp);
//        }

//        if (remainingInBetweenTime > 0) {
//            remainingInBetweenTime--;
//        }
//        if (remainingRegulationTime > 0) {
//            remainingRegulationTime--;
//        }
    }


//    private void updateRegulationQueue(double timeSeconds) {
//        for (SignalGroup group : this.system.getSignalGroups().values()) {
//            double intergreen = 0;
//            double amberTime = signalsData.getAmberTimesData().getDefaultAmber();
//
//            if (currentGreenTimeGroupId != null) {
//                intergreen = signalsData.getIntergreenTimesData().getIntergreensForSignalSystemDataMap()
//                        .get(this.system.getId()).getIntergreenTime(currentGreenTimeGroupId, group.getId());
//            }
//            double inBetweenTime = amberTime + intergreen;
//
//            for (Signal signal : group.getSignals().values()) {
//                if (checkForRegulation(signal, timeSeconds, inBetweenTime) && !regulationQueue.contains(signal.getId())) {
//                    regulationQueue.add(group.getId());
//                }
//            }
//        }
//    }


    private boolean checkForRegulation(Signal signal, double timeSeconds, double inBetweenTime) {
        Id<Link> linkId = signal.getLinkId();
        double n = this.getNumberOfExpectedVehicles(timeSeconds, linkId);
        double a;
        if (n == 0) {
            a = inBetweenTime;
        } else {
            a = timeSinceService.get(signal.getId()) + 1;
        }
        timeSinceService.put(signal.getId(), a);

        double avgArrivalRate = avgArrivalRates.get(signal.getId());
        double maxFlow = this.network.getLinks().get(linkId).getCapacity() / 3600.;
        double nCrit = avgArrivalRate * DESIRED_PERIOD
                * (MAX_PERIOD - a / (1 - avgArrivalRate / maxFlow)
                / (MAX_PERIOD - DESIRED_PERIOD));
        return n >= nCrit;
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


    @Override
    public void reset(Integer iterationNumber) {
    }

    private void initializeSensoring() {
        for (SignalGroup group : this.system.getSignalGroups().values()) {
            for (Signal signal : group.getSignals().values()) {
                if (signal.getLaneIds() == null || signal.getLaneIds().isEmpty()) {
                    //					log.error("system: " + this.system.getId() + " signal: " + signal.getId() + " has no lanes...");
                    this.sensorManager.registerNumberOfCarsInDistanceMonitoring(signal.getLinkId(), 0.);
                    //TODO: register for arrival rate
                    avgArrivalRates.put(signal.getId(), 0.25);
                } else {
                    for (Id<Lane> laneId : signal.getLaneIds()) {
                        //TODO check this part again concerning implementation of CarsOnLaneHandler
                        this.sensorManager.registerNumberOfCarsMonitoringOnLane(signal.getLinkId(), laneId);
                    }
                }
            }
        }
    }

    class Driveway implements Comparable<Driveway> {

        SignalGroup group;

        boolean selected = false;
        double groupIndex = 0;
        double remainingInBetweenTime;
        double intergreen = 0;
        private double abortionPenalty = 0;


        Driveway(SignalGroup signalGroup) {
            this.group = signalGroup;
        }

        public void update(double timeSeconds) {

            if (selected) {
                calculateAbortionPenalty(timeSeconds);
            }
            updateIntergreen();

            //update avgarrival
            //update a

            groupIndex = 0;

            for (Signal signal : group.getSignals().values()) {
                double signalIndex = 0;
                Id<Link> linkId = signal.getLinkId();
                double flow = network.getLinks().get(linkId).getCapacity() / 3600.;
                if (selected) {
                    //TODO: currentInBetween time has to exclude intergreens
                    for (double i = remainingInBetweenTime; i <= (intergreen + DEFAULT_AMBER); i++) {
                        double nExpected = getNumberOfExpectedVehicles(timeSeconds + i, linkId);
                        double reqGreenTime = nExpected / flow;
                        double tempIndex = nExpected / (i + reqGreenTime);
                        if (tempIndex > signalIndex) {
                            signalIndex = tempIndex;
                        }
                    }
                } else {
                    double nExpected = getNumberOfExpectedVehicles(timeSeconds + (intergreen + DEFAULT_AMBER), linkId);
                    double reqGreenTime = nExpected / flow;
                    double penalty = 0;
                    if (currentSelectedDriveway != null) {
                        penalty = currentSelectedDriveway.abortionPenalty;
                    }
                    signalIndex = nExpected / (penalty + (intergreen + DEFAULT_AMBER) + reqGreenTime);
                }
                groupIndex += signalIndex;
            }

            if (selected && remainingInBetweenTime > 0) {
                remainingInBetweenTime--;
            }
        }

        private void calculateAbortionPenalty(double timeSeconds) {

            if (!selected) {
                this.abortionPenalty = 0;
                return;
            }
            double penaltySum = 0;

            for (Signal signal : group.getSignals().values()) {
                Id<Link> linkId = signal.getLinkId();
                double waitingTimeSum = 0;
                //TODO: reduce inbetweentime for selected driveway to amber times only after start of service
                //TODO: check integration implementation
                for (double i = remainingInBetweenTime; i <= (intergreen + DEFAULT_AMBER); i++) {
                    waitingTimeSum += (waitingTimeSum + getNumberOfExpectedVehicles(timeSeconds + i, linkId));
                }
                double n = getNumberOfExpectedVehicles(timeSeconds + (intergreen + DEFAULT_AMBER), linkId);
                if (n > 0) {
                    penaltySum += waitingTimeSum / n;
                }
            }
            this.abortionPenalty = penaltySum;
        }

        private void updateIntergreen() {
            intergreen = 0;
            if (!selected && currentSelectedDriveway != null) {
                Id<SignalGroup> currentGroupId = currentSelectedDriveway.group.getId();
                intergreen = signalsData.getIntergreenTimesData().getIntergreensForSignalSystemDataMap()
                        .get(system.getId()).getIntergreenTime(currentGroupId, this.group.getId());
            } else if (selected && remainingInBetweenTime <= DEFAULT_AMBER) {
                intergreen = 0;
            }
        }


        private void toggleSelection() {
            this.selected = !this.selected;
            this.remainingInBetweenTime = intergreen + DEFAULT_AMBER;
        }

        @Override
        public int compareTo(Driveway o) {
            if (o.groupIndex > this.groupIndex) {
                return -1;
            } else if (o.groupIndex < this.groupIndex) {
                return 1;
            } else {
                return 0;
            }
        }
    }
}
