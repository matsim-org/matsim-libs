package org.matsim.contrib.emissions;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;
import org.matsim.vis.snapshotwriters.PositionEvent;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URL;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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

    @Override
    public void install() {

        checkConsistency(emissionsConfigGroup);

        var averageWarmEmissions =
                new AverageWarmEmissionsLoader().load(emissionsConfigGroup.getAverageWarmEmissionFactorsFileURL(config.getContext()));

        var detailedWarmEmissions =
                new DetailedWarmEmissionsLoader().load(emissionsConfigGroup.getDetailedWarmEmissionFactorsFileURL(config.getContext()));

      /*  var averageColdEmissions =
                new AverageColdEmissionsLoader().load(emissionsConfigGroup.getAverageColdEmissionFactorsFileURL(config.getContext()));

       */

        Map<HbefaRoadVehicleCategoryKey, Map<HbefaTrafficSituation, Double>> roadTrafficSpeeds = new HashMap<>(); // load this as well

        // hard code this for now, but use something other later
        var pollutants = Set.of(Pollutant.NO2, Pollutant.NOx, Pollutant.CO2_TOTAL, Pollutant.PM, Pollutant.PM2_5);

        var eventsManager = EventsUtils.createEventsManager(); // TODO this is a nasty hack
        var analyser = new WarmEmissionAnalysisModule(
                averageWarmEmissions, detailedWarmEmissions, roadTrafficSpeeds, pollutants, eventsManager, emissionsConfigGroup);

        var calculator = new EmissionCalculator(emissionsConfigGroup, averageWarmEmissions, detailedWarmEmissions);

        bind(EmissionCalculator.class).toInstance(calculator);
        addEventHandlerBinding().to(Handler.class);


        log.info("Installed Handler and WarmEmissionAnalysis Module.");

    }

    static class EmissionCalculator {

        private final EmissionsConfigGroup emissionsConfigGroup;
        private final Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> avgHbefaWarmTable;
        private final Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> detailedHbefaWarmTable;

        public EmissionCalculator(EmissionsConfigGroup emissionsConfigGroup, Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> avgHbefaWarmTable, Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> detailedHbefaWarmTable) {
            this.emissionsConfigGroup = emissionsConfigGroup;
            this.avgHbefaWarmTable = avgHbefaWarmTable;
            this.detailedHbefaWarmTable = detailedHbefaWarmTable;
        }

        Map<Pollutant, Double> calculateWarmEmissions(Vehicle vehicle, Link link, double distance, double speed) {

            Map<Pollutant, Double> result = new EnumMap<>(Pollutant.class);
            var vehicleAttributes = getVehicleAttributes(vehicle);
            var key = new HbefaWarmEmissionFactorKey();
            key.setHbefaVehicleCategory(vehicleAttributes.getFirst());
            key.setHbefaRoadCategory(EmissionUtils.getHbefaRoadType(link));

            // the original code has handling for detailed warm emissions here.

            for (var pollutant : Pollutant.values()) {

                key.setHbefaComponent(pollutant);
                if (speed >= 0.5)
                    key.setHbefaTrafficSituation(HbefaTrafficSituation.FREEFLOW);
                else
                    key.setHbefaTrafficSituation(HbefaTrafficSituation.STOPANDGO);

                // next:
                // 1. look up emission value for pollutant. This will be in g/km
                // 2. multiply g/km with distance since last emission event. Originally, since emissions were calculated
                //    when agents left links this would be the length of the link. Now, we have to provide the distance to
                //        a. The last emission event
                //        b. The beginning of the link

                var emissionPerKilometer = 0.0; // here should be a look up
                var emissionValue = distance * emissionPerKilometer / 1000;

                result.put(pollutant, emissionValue);

            }
            return result;
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
                case PositionEvent.EVENT_TYPE:
                    handlePositionEvent((PositionEvent) event);
                    break;
                case VehicleEntersTrafficEvent.EVENT_TYPE:
                    handleVehicleEntersTraffic((VehicleEntersTrafficEvent) event);
                default:
                    // we're not interested in anything else
            }
        }

        private void handleLinkEnterEvent(LinkEnterEvent event) {

        }

        private void handleVehicleEntersTraffic(VehicleEntersTrafficEvent event) {
            vehiclesInTraffic.put(event.getVehicleId(), event);
        }

        private void handleVehicleLeavesTraffic(VehicleLeavesTrafficEvent event) {
            vehiclesInTraffic.remove(event.getVehicleId());
        }

        private void handlePositionEvent(PositionEvent event) {

            if (!event.getState().equals(AgentSnapshotInfo.AgentState.PERSON_DRIVING_CAR))
                return; // only calculate emissions for cars

            var vehicle = vehicles.getVehicles().get(event.getVehicleId());
            var link = network.getLinks().get(event.getLinkId());
            var travelTime = event.getTime() - vehiclesInTraffic.get(event.getVehicleId()).getTime();

            //var emissions = analysisModule.checkVehicleInfoAndCalculateWarmEmissions(vehicle, link, travelTime);
            var emissions = emissionCalculator.calculateWarmEmissions(vehicle, link, 0, event.speed());

            log.info("calculated emissions: " + emissions);

            // maybe refactor emission position event, so that it takes position event and emission map
            var emissionEvent = new WarmEmissionEvent(event.getTime(), event.getLinkId(), event.getVehicleId(), emissions);
            eventsManager.processEvent(new EmissionPositionEvent(event, emissionEvent));
        }
    }

    static class EmissionPositionEvent extends Event {

        private final PositionEvent position;
        private final WarmEmissionEvent emission;

        public EmissionPositionEvent(PositionEvent positionEvent, WarmEmissionEvent emissionEvent) {
            super(positionEvent.getTime());
            this.position = positionEvent;
            this.emission = emissionEvent;
        }

        @Override
        public Map<String, String> getAttributes() {
            var attr = super.getAttributes();
            // there are multiple duplicated keys in both events which should also have the same values
            // the map should sort this out.
            attr.putAll(position.getAttributes());
            attr.putAll(emission.getAttributes());
            return attr;
        }

        @Override
        public String getEventType() {
            return "emissionPositionEvent";
        }
    }

    static class AverageWarmEmissionsLoader extends HbefaTableLoader {

        @Override
        protected void setSpecificThings(HbefaWarmEmissionFactorKey key, HbefaWarmEmissionFactor value, CSVRecord record) {

            key.setHbefaVehicleAttributes(new HbefaVehicleAttributes());
            value.setSpeed(Double.parseDouble(record.get("V_weighted")));
            value.setWarmEmissionFactor(Double.parseDouble(record.get("EFA_weighted")));
        }
    }

    static class DetailedWarmEmissionsLoader extends HbefaTableLoader {

        @Override
        protected void setSpecificThings(HbefaWarmEmissionFactorKey key, HbefaWarmEmissionFactor value, CSVRecord record) {

            var vehicleAttributes = new HbefaVehicleAttributes();
            vehicleAttributes.setHbefaTechnology(record.get("Technology"));
            vehicleAttributes.setHbefaSizeClass(record.get("SizeClasse"));
            vehicleAttributes.setHbefaEmConcept("EmConcept");

            value.setSpeed(Double.parseDouble(record.get("V")));
            value.setWarmEmissionFactor(Double.parseDouble(record.get("EFA")));
        }
    }

    static class AverageColdEmissionsLoader extends HbefaTableLoader {

        @Override
        protected void setSpecificThings(HbefaWarmEmissionFactorKey key, HbefaWarmEmissionFactor value, CSVRecord record) {

        }
    }

    static abstract class HbefaTableLoader {

        private static HbefaTrafficSituation mapString2HbefaTrafficSituation(String string) {

            if (string.endsWith("Freeflow")) return HbefaTrafficSituation.FREEFLOW;
            else if (string.endsWith("Heavy")) return HbefaTrafficSituation.HEAVY;
            else if (string.endsWith("Satur.")) return HbefaTrafficSituation.SATURATED;
            else if (string.endsWith("St+Go")) return HbefaTrafficSituation.STOPANDGO;
            else if (string.endsWith("St+Go2")) return HbefaTrafficSituation.STOPANDGO_HEAVY;
            else {
                log.warn("Could not map String " + string + " to any HbefaTrafficSituation; please check syntax in hbefa input file.");
                throw new RuntimeException();
            }
        }

        Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> load(URL file) {

            Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> avgWarmTable = new HashMap<>();

            try (var reader = IOUtils.getBufferedReader(file);
                 var parser = CSVParser.parse(reader, CSVFormat.newFormat(';').withFirstRecordAsHeader())) {

                for (var record : parser) {
                    var key = new HbefaWarmEmissionFactorKey();

                    var vehicleCategory = EmissionUtils.mapString2HbefaVehicleCategory(record.get("VehCat"));
                    var pollutant = EmissionUtils.getPollutant(record.get("Component"));
                    var trafficSit = record.get("TrafficSit");
                    var roadCategory = trafficSit.substring(0, trafficSit.lastIndexOf('/'));
                    var trafficSituation = mapString2HbefaTrafficSituation(trafficSit);

                    key.setHbefaVehicleCategory(vehicleCategory);
                    key.setHbefaComponent(pollutant);
                    key.setHbefaRoadCategory(roadCategory);
                    key.setHbefaTrafficSituation(trafficSituation);


                    var value = new HbefaWarmEmissionFactor();

                    setSpecificThings(key, value, record);
                    avgWarmTable.put(key, value);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return avgWarmTable;
        }

        protected abstract void setSpecificThings(HbefaWarmEmissionFactorKey key, HbefaWarmEmissionFactor value, CSVRecord record);
    }

    static class HbefaColdFactorKey extends HbefaFactorKey {

        private final int parkingTime;
        private final int distance;

        public HbefaColdFactorKey(HbefaVehicleCategory vehicleCategory, HbefaVehicleAttributes vehicleAttributes, Pollutant component, int parkingTime, int distance) {
            super(vehicleCategory, vehicleAttributes, component);
            this.parkingTime = parkingTime;
            this.distance = distance;
        }

        public int getParkingTime() {
            return parkingTime;
        }

        public int getDistance() {
            return distance;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;

            HbefaColdFactorKey that = (HbefaColdFactorKey) o;

            if (parkingTime != that.parkingTime) return false;
            return distance == that.distance;
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + parkingTime;
            result = 31 * result + distance;
            return result;
        }
    }

    static class HbefaWarmFactorKey extends HbefaFactorKey {

        private final HbefaTrafficSituation trafficSituation;
        private final String roadCategory;

        public HbefaWarmFactorKey(HbefaVehicleCategory vehicleCategory, HbefaVehicleAttributes vehicleAttributes, Pollutant component, HbefaTrafficSituation trafficSituation, String roadCategory) {
            super(vehicleCategory, vehicleAttributes, component);
            this.trafficSituation = trafficSituation;
            this.roadCategory = roadCategory;
        }

        public HbefaTrafficSituation getTrafficSituation() {
            return trafficSituation;
        }

        public String getRoadCategory() {
            return roadCategory;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;

            HbefaWarmFactorKey that = (HbefaWarmFactorKey) o;

            if (trafficSituation != that.trafficSituation) return false;
            return roadCategory.equals(that.roadCategory);
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + trafficSituation.hashCode();
            result = 31 * result + roadCategory.hashCode();
            return result;
        }
    }

    static abstract class HbefaFactorKey {

        private final HbefaVehicleCategory vehicleCategory;
        private final HbefaVehicleAttributes vehicleAttributes;

        private final Pollutant component;

        public HbefaFactorKey(HbefaVehicleCategory vehicleCategory, HbefaVehicleAttributes vehicleAttributes, Pollutant component) {
            this.vehicleCategory = vehicleCategory;
            this.vehicleAttributes = vehicleAttributes;
            this.component = component;
        }

        public HbefaVehicleCategory getVehicleCategory() {
            return vehicleCategory;
        }

        public HbefaVehicleAttributes getVehicleAttributes() {
            return vehicleAttributes;
        }

        public Pollutant getComponent() {
            return component;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof HbefaFactorKey)) return false;

            HbefaFactorKey that = (HbefaFactorKey) o;

            if (vehicleCategory != that.vehicleCategory) return false;
            if (!vehicleAttributes.equals(that.vehicleAttributes)) return false;
            return component == that.component;
        }

        @Override
        public int hashCode() {
            int result = vehicleCategory.hashCode();
            result = 31 * result + vehicleAttributes.hashCode();
            result = 31 * result + component.hashCode();
            return result;
        }
    }
}
