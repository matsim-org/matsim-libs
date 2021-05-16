package org.matsim.contrib.emissions;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;
import org.matsim.vis.snapshotwriters.PositionEvent;

import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class PositionEmissionsModule extends AbstractModule {

    private static final Logger log = Logger.getLogger(PositionEmissionsModule.class);

    @Inject
    private Config config;

    private static void checkConsistency(Config config) {

        if (config.qsim().getSnapshotPeriod() > 1) {
            throw new RuntimeException("only snapshot periods of 1s are supported.");
        }
        if (!config.controler().getSnapshotFormat().contains(ControlerConfigGroup.SnapshotFormat.positionevents)) {
            throw new RuntimeException("config.controler.snapshotFormat must be set to 'positionevents'");
        }
        if (!(config.qsim().getSnapshotStyle().equals(QSimConfigGroup.SnapshotStyle.queue) || config.qsim().getSnapshotStyle().equals(QSimConfigGroup.SnapshotStyle.kinematicWaves))) {
            throw new RuntimeException("I think generating emissions only makes sense if config.qsim.snapshotstyle == queue or == kinematicWaves");
        }
    }

    @Override
    public void install() {

        checkConsistency(config);
        bind(EmissionCalculator.class);
        bind(EmissionModule.class);
        addEventHandlerBinding().to(Handler.class);
    }

    static class EmissionCalculator {

        @Inject
        private EmissionsConfigGroup emissionsConfigGroup;
        @Inject
        private EmissionModule emissionModule;

        Map<Pollutant, Double> calculateWarmEmissions(Vehicle vehicle, Link link, double distance, double time, double speed) {

            var vehicleAttributes = getVehicleAttributes(vehicle);
            var roadType = EmissionUtils.getHbefaRoadType(link);
            return emissionModule.getWarmEmissionAnalysisModule().calculateWarmEmissions(time, roadType, link.getFreespeed(), distance, vehicleAttributes);
        }

        Map<Pollutant, Double> calculateColdEmissions(Vehicle vehicle, Id<Link> linkId, double startEngineTime, double parkingDuration, int distance) {
            return emissionModule.getColdEmissionAnalysisModule()
                    .checkVehicleInfoAndCalculateWColdEmissions(vehicle.getType(), vehicle.getId(), linkId, startEngineTime, parkingDuration, distance);
        }

        private Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> getVehicleAttributes(Vehicle vehicle) {
            // the following block fixes the vehicle types's emission information whenusing an  old vehicle type format
            // the unit test I found uses an old format, so have it here.
            {
                String hbefaVehicleTypeDescription = EmissionUtils.getHbefaVehicleDescription(vehicle.getType(), emissionsConfigGroup);
                // (this will, importantly, repair the hbefa description in the vehicle type. kai/kai, jan'20)
                Gbl.assertNotNull(hbefaVehicleTypeDescription);
            }
            return EmissionUtils.convertVehicleDescription2VehicleInformationTuple(vehicle.getType());
        }
    }

    static class Handler implements BasicEventHandler {

        private final Map<Id<Vehicle>, LinkedList<PositionEvent>> trajectories = new HashMap<>();
        private final Map<Id<Vehicle>, VehicleEntersTrafficEvent> vehiclesInTraffic = new HashMap<>();
        private final Map<Id<Vehicle>, VehicleLeavesTrafficEvent> parkedVehicles = new HashMap<>();
        private final Map<Id<Vehicle>, Double> parkingDurations = new HashMap<>();
        private final Set<Id<Vehicle>> vehiclesWaitingForSecondColdEmissionEvent = new HashSet<>();

        @Inject
        private EmissionCalculator emissionCalculator;

        @Inject
        private Vehicles vehicles;

        @Inject
        private Network network;

        @Inject
        private EventsManager eventsManager;

        @Override
        public void handleEvent(Event event) {

            var type = event.getEventType();
            switch (type) {

                case PositionEvent.EVENT_TYPE:
                    handlePositionEvent((PositionEvent) event);
                    break;
                case VehicleEntersTrafficEvent.EVENT_TYPE:
                    handleVehicleEntersTrafficEvent((VehicleEntersTrafficEvent) event);
                    break;
                case VehicleLeavesTrafficEvent.EVENT_TYPE:
                    handleVehicleLeavesTraffic((VehicleLeavesTrafficEvent) event);
                    break;
                default:
                    // we're not interested in anything else
            }
        }

        private void handleVehicleEntersTrafficEvent(VehicleEntersTrafficEvent event) {
            if (!event.getNetworkMode().equals(TransportMode.car)) return;

            vehiclesInTraffic.put(event.getVehicleId(), event);
            vehiclesWaitingForSecondColdEmissionEvent.add(event.getVehicleId());
            var parkingDuration = calculateParkingTime(event.getVehicleId(), event.getTime());
            this.parkingDurations.put(event.getVehicleId(), parkingDuration);
        }

        /**
         * Calculates parking time AND removes vehicle from parking vehicles
         */
        private double calculateParkingTime(Id<Vehicle> vehicleId, double startTime) {

            if (parkedVehicles.containsKey(vehicleId)) {
                var stopTime = parkedVehicles.remove(vehicleId).getTime();
                return startTime - stopTime;
            }
            //parking duration is assumed to be at least 12 hours when parking overnight
            return 43200;
        }

        private void handleVehicleLeavesTraffic(VehicleLeavesTrafficEvent event) {
            if (!event.getNetworkMode().equals(TransportMode.car)) return;

            trajectories.remove(event.getVehicleId());
            parkedVehicles.put(event.getVehicleId(), event);
        }

        private void handlePositionEvent(PositionEvent event) {

            if (!event.getState().equals(AgentSnapshotInfo.AgentState.PERSON_DRIVING_CAR))
                return; // only calculate emissions for cars

            if (!vehiclesInTraffic.containsKey(event.getVehicleId()))
                return; // only collect positions if vehicle has entered traffic (if vehicle is wait2link its position is calculated but it hasn't entered traffic yet.

            if (trajectories.containsKey(event.getVehicleId())) {
                computeWarmEmissionEvent(event);

                if (vehiclesWaitingForSecondColdEmissionEvent.contains(event.getVehicleId()))
                    computeSecondColdEmissionEvent(event);
            } else {
                computeFirstColdEmissionEvent(event);
            }

            // remember all the positions. It is important to do it here, so that the current event is not yet in the
            // queue when emissions events are computed
            // the first few positions seem to be a little weird talk about this with kai
            trajectories.computeIfAbsent(event.getVehicleId(), key -> new LinkedList<>()).add(event);
        }

        private void computeFirstColdEmissionEvent(PositionEvent event) {

            var vehicle = vehicles.getVehicles().get(event.getVehicleId());
            var startEngineTime = vehiclesInTraffic.get(vehicle.getId()).getTime();
            var emissions = emissionCalculator.calculateColdEmissions(vehicle, event.getLinkId(),
                    startEngineTime, parkingDurations.get(vehicle.getId()), 1);

            var emisPosEv = new PositionEmissionEvent(event, emissions, "cold");
            eventsManager.processEvent(emisPosEv);
        }

        private void computeSecondColdEmissionEvent(PositionEvent event) {
            // check for cold emissions which should be computed once if distance is > 1000m
            double distance = calculateTravelledDistance(event);

            if (distance > 1000) {
                var vehicle = vehicles.getVehicles().get(event.getVehicleId());
                var emissions = emissionCalculator.calculateColdEmissions(vehicle, event.getLinkId(),
                        event.getTime(), parkingDurations.get(vehicle.getId()), 2);
                var emisPosEv = new PositionEmissionEvent(event, emissions, "cold");
                eventsManager.processEvent(emisPosEv);

                // this makes sure that this is only computed once
                vehiclesWaitingForSecondColdEmissionEvent.remove(vehicle.getId());
            }
        }

        private double calculateTravelledDistance(PositionEvent event) {
            // check for cold emissions which should be computed once if distance is > 1000m
            double distance = 0;
            Coord previousCoord = null;
            for (var position : trajectories.get(event.getVehicleId())) {

                if (previousCoord != null) {
                    distance += CoordUtils.calcEuclideanDistance(previousCoord, position.getCoord());
                }
                previousCoord = position.getCoord();
            }
            assert previousCoord != null;
            distance += CoordUtils.calcEuclideanDistance(previousCoord, event.getCoord());
            return distance;
        }

        private void computeWarmEmissionEvent(PositionEvent event) {

            var previousPosition = trajectories.get(event.getVehicleId()).getLast();
            var distanceToLastPosition = CoordUtils.calcEuclideanDistance(event.getCoord(), previousPosition.getCoord());

            if (distanceToLastPosition > 0) {

                var link = network.getLinks().get(event.getLinkId());
                var travelTime = event.getTime() - previousPosition.getTime();
                var speed = distanceToLastPosition / travelTime;

                // don't go faster than light (freespeed)
                // add a rounding error to the compared freespeed. The warm emission module has a tolerance of 1km/h
                if (speed <= link.getFreespeed() + 0.01) {
                    var vehicle = vehicles.getVehicles().get(event.getVehicleId());
                    var emissions = emissionCalculator.calculateWarmEmissions(vehicle, link, distanceToLastPosition, travelTime, event.getColorValueBetweenZeroAndOne());
                    eventsManager.processEvent(new PositionEmissionEvent(event, emissions, "warm"));
                }
            }
        }
    }

    public static class PositionEmissionEvent extends Event {

        public static final String EVENT_TYPE = "positionEmission";

        private final PositionEvent position;
        private final Map<Pollutant, Double> emissions;
        private final String emissionType;

        public Map<Pollutant, Double> getEmissions() {
            return emissions;
        }

        public PositionEmissionEvent(PositionEvent positionEvent, Map<Pollutant, Double> emissions, String emissionType) {
            super(positionEvent.getTime());
            this.position = positionEvent;
            this.emissions = emissions;
            this.emissionType = emissionType;
        }

        public Id<Link> getLinkId() {
            return position.getLinkId();
        }

        public Id<Vehicle> getVehicleId() { return position.getVehicleId(); }

        public Id<Person> getPersonId() { return position.getPersonId(); }

        public String getEmissionType() {
            return emissionType;
        }

        public Coord getCoord() { return position.getCoord(); }


        @Override
        public Map<String, String> getAttributes() {

            // call super second, so that the event type get overridden
            var attr = position.getAttributes();
            attr.putAll(super.getAttributes());

            for (var pollutant : emissions.entrySet()) {
                attr.put(pollutant.getKey().toString(), pollutant.getValue().toString());
            }
            return attr;
        }

        @Override
        public String getEventType() {
            return EVENT_TYPE;
        }
    }
}
