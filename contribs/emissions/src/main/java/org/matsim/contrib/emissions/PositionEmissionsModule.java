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
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControlerConfigGroup;
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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

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
    }

    @Override
    public void install() {

        checkConsistency(config);
        bind(EmissionCalculator.class);
        bind(EmissionModule.class);
        addEventHandlerBinding().to(Handler.class);
        log.info("Installed Handler and WarmEmissionAnalysis Module.");
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

            if (!trajectories.containsKey(event.getVehicleId())) {
                computeFirstColdEmissionEvent(event);
            } else {
                computeEmissionEvents(event);
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

            eventsManager.processEvent(new EmissionPositionEvent(event, emissions));
        }

        private void computeEmissionEvents(PositionEvent event) {

            var previousPosition = trajectories.get(event.getVehicleId()).getLast();
            var distanceToLastPosition = CoordUtils.calcEuclideanDistance(event.getCoord(), previousPosition.getCoord());

            // do matsim cars come to a halt in a traffic jam, or do they move very slowly????
            if (distanceToLastPosition > 0) {
                var vehicle = vehicles.getVehicles().get(event.getVehicleId());
                computeWarmEmissionEvent(event, vehicle, distanceToLastPosition);
                computeSecondColdEmissionEvent(event, vehicle);
            }
        }

        private void computeSecondColdEmissionEvent(PositionEvent event, Vehicle vehicle) {
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

            if (distance > 1000) {
                var startEngineTime = vehiclesInTraffic.get(vehicle.getId()).getTime();
                var emissions = emissionCalculator.calculateColdEmissions(vehicle, event.getLinkId(),
                        startEngineTime, parkingDurations.get(vehicle.getId()), 2);
                eventsManager.processEvent(new EmissionPositionEvent(event, emissions));
            }
        }

        private void computeWarmEmissionEvent(PositionEvent event, Vehicle vehicle, double distanceToLastPosition) {
            var link = network.getLinks().get(event.getLinkId());
            var travelTime = event.getTime() - trajectories.get(vehicle.getId()).getLast().getTime();

            // don't go faster than light (freespeed)
            if (distanceToLastPosition / travelTime <= link.getFreespeed()) {
                //var emissions = analysisModule.checkVehicleInfoAndCalculateWarmEmissions(vehicle, link, travelTime);
                var emissions = emissionCalculator.calculateWarmEmissions(vehicle, link, distanceToLastPosition, travelTime, event.speed());

                log.info("calculated emissions: " + emissions);
                eventsManager.processEvent(new EmissionPositionEvent(event, emissions));
            }
        }
    }

    static class EmissionPositionEvent extends Event {

        private final PositionEvent position;
        private final Map<Pollutant, Double> emissions;

        public EmissionPositionEvent(PositionEvent positionEvent, Map<Pollutant, Double> emissions) {
            super(positionEvent.getTime() + 1);
            this.position = positionEvent;
            this.emissions = emissions;
        }

        @Override
        public Map<String, String> getAttributes() {
            var attr = super.getAttributes();
            attr.putAll(position.getAttributes());
            for (var pollutant : emissions.entrySet()) {
                attr.put(pollutant.getKey().toString(), pollutant.getValue().toString());
            }
            return attr;
        }

        @Override
        public String getEventType() {
            return "emissionPositionEvent";
        }
    }
}
