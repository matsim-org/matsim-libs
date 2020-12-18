package org.matsim.contrib.emissions;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
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
import java.util.Map;

public class PositionEmissionsModule extends AbstractModule {

    private static final Logger log = Logger.getLogger(PositionEmissionsModule.class);

    @Inject
    private EmissionsConfigGroup emissionsConfigGroup;

    @Inject
    private Config config;

 /*   @Inject
    private EventsManager eventsManager;
*/

    private static void checkConsistency(EmissionsConfigGroup configGroup) {

        if (configGroup.getDetailedVsAverageLookupBehavior().equals(EmissionsConfigGroup.DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageElseAbort)) {
            //Check if value was loaded
            if (StringUtils.isBlank(configGroup.getDetailedColdEmissionFactorsFile())) {
                throw new RuntimeException("You have requested " + configGroup.getDetailedVsAverageLookupBehavior() + " but are not providing a corresponding" +
                        " cold emissions file.");
            }
            if (StringUtils.isBlank(configGroup.getDetailedWarmEmissionFactorsFile())) {
                throw new RuntimeException("You have requested " + configGroup.getDetailedVsAverageLookupBehavior() + " but are not providing a corresponding" +
                        " warm emissions file.");
            }
        } else {
            if (StringUtils.isBlank(configGroup.getAverageColdEmissionFactorsFile())) {
                throw new RuntimeException("You have requested " + configGroup.getDetailedVsAverageLookupBehavior() + " but are not providing a corresponding" +
                        " cold emissions file.");
            }
            if (StringUtils.isBlank(configGroup.getAverageWarmEmissionFactorsFile())) {
                throw new RuntimeException("You have requested " + configGroup.getDetailedVsAverageLookupBehavior() + " but are not providing a corresponding" +
                        " warm emissions file.");
            }
        }
    }

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

        private final Map<Id<Vehicle>, VehicleEntersTrafficEvent> vehiclesInTraffic = new HashMap<>();
        private final Map<Id<Vehicle>, LinkEnterEvent> vehiclesOnLinks = new HashMap<>();
        private final Map<Id<Vehicle>, PositionEvent> previousPositions = new HashMap<>();

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
                case LinkEnterEvent.EVENT_TYPE:
                    handleLinkEnterEvent((LinkEnterEvent) event);
                    break;
                case LinkLeaveEvent.EVENT_TYPE:
                    handleLinkLeaveEvent((LinkLeaveEvent) event);
                    break;
                case PositionEvent.EVENT_TYPE:
                    handlePositionEvent((PositionEvent) event);
                    break;
                case VehicleEntersTrafficEvent.EVENT_TYPE:
                    handleVehicleEntersTraffic((VehicleEntersTrafficEvent) event);
                    break;
                case VehicleLeavesTrafficEvent.EVENT_TYPE:
                    handleVehicleLeavesTraffic((VehicleLeavesTrafficEvent) event);
                    break;
                default:
                    // we're not interested in anything else
            }
        }

        private void handleLinkEnterEvent(LinkEnterEvent event) {
            vehiclesOnLinks.put(event.getVehicleId(), event);
        }

        private void handleLinkLeaveEvent(LinkLeaveEvent event) {
            vehiclesOnLinks.remove(event.getVehicleId());
        }

        private void handleVehicleEntersTraffic(VehicleEntersTrafficEvent event) {
            vehiclesInTraffic.put(event.getVehicleId(), event);
        }

        private void handleVehicleLeavesTraffic(VehicleLeavesTrafficEvent event) {
            vehiclesInTraffic.remove(event.getVehicleId());
            previousPositions.remove(event.getVehicleId());
        }

        private void handlePositionEvent(PositionEvent event) {

            if (!event.getState().equals(AgentSnapshotInfo.AgentState.PERSON_DRIVING_CAR))
                return; // only calculate emissions for cars

            var previousPosition = previousPositions.get(event.getVehicleId());
            previousPositions.put(event.getVehicleId(), event);

            // we start calculating warm emissions once we have the second position
            if (previousPosition != null) {

                var travelledDistance = CoordUtils.calcEuclideanDistance(event.getCoord(), previousPosition.getCoord());

                // do matsim cars come to a halt in a traffic jam, or do they move very slowly????
                if (travelledDistance > 0) {
                    var vehicle = vehicles.getVehicles().get(event.getVehicleId());
                    var link = network.getLinks().get(event.getLinkId());
                    var travelTime = event.getTime() - previousPosition.getTime();

                    // don't go faster than light (freespeed)
                    if (travelledDistance / travelTime <= link.getFreespeed()) {
                        //var emissions = analysisModule.checkVehicleInfoAndCalculateWarmEmissions(vehicle, link, travelTime);
                        var emissions = emissionCalculator.calculateWarmEmissions(vehicle, link, travelledDistance, travelTime, event.speed());

                        log.info("calculated emissions: " + emissions);
                        eventsManager.processEvent(new EmissionPositionEvent(event, emissions));
                    }
                }
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
