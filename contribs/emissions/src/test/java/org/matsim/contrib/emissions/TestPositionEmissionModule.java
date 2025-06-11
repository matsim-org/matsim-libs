package org.matsim.contrib.emissions;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.EmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.*;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vis.snapshotwriters.PositionEvent;

import jakarta.inject.Singleton;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestPositionEmissionModule {

    private static final String configFile = IOUtils.extendUrl( ExamplesUtils.getTestScenarioURL( "emissions-sampleScenario/testv2_Vehv2" ), "config_detailed.xml" ).toString();
    // (I changed the above from veh v1 to veh v2 since veh v1 is deprecated, especially for emissions.  kai, apr'21)

    @RegisterExtension
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	@Test
	@Disabled
	void simpleTest() {

        var emissionConfig = new EmissionsConfigGroup();
        emissionConfig.setHbefaVehicleDescriptionSource(EmissionsConfigGroup.HbefaVehicleDescriptionSource.fromVehicleTypeDescription);
        emissionConfig.setDetailedVsAverageLookupBehavior(
                EmissionsConfigGroup.DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageThenAverageTable); //This is the previous behaviour
        var config = ConfigUtils.loadConfig(configFile, emissionConfig);
        config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        config.qsim().setSnapshotPeriod(1);
        config.qsim().setSnapshotStyle(QSimConfigGroup.SnapshotStyle.queue);
        config.controller().setWriteSnapshotsInterval(1);
        config.controller().setSnapshotFormat(Set.of(ControllerConfigGroup.SnapshotFormat.positionevents));

        var scenario = ScenarioUtils.loadScenario(config);

        var controler = new Controler(scenario);

        controler.addOverridingModule(new PositionEmissionsModule());
        controler.run();
    }

	@Test
	void compareToOtherModule_singleVehicleSingleLink() {

        var emissionConfig = new EmissionsConfigGroup();
        emissionConfig.setHbefaVehicleDescriptionSource(EmissionsConfigGroup.HbefaVehicleDescriptionSource.fromVehicleTypeDescription);
        emissionConfig.setDetailedVsAverageLookupBehavior(EmissionsConfigGroup.DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageThenAverageTable);

        var config = ConfigUtils.loadConfig(configFile, emissionConfig);
        config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        config.controller().setOutputDirectory(testUtils.getOutputDirectory());
		config.routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);

        emissionConfig.setAverageColdEmissionFactorsFile("../sample_41_EFA_ColdStart_vehcat_2020average.csv");
        emissionConfig.setAverageWarmEmissionFactorsFile( "../sample_41_EFA_HOT_vehcat_2020average.csv" );
        emissionConfig.setHbefaTableConsistencyCheckingLevel( EmissionsConfigGroup.HbefaTableConsistencyCheckingLevel.consistent );

        final ScoringConfigGroup.ActivityParams homeParams = new ScoringConfigGroup.ActivityParams("home")
                .setTypicalDuration(20);
        config.scoring().addActivityParams(homeParams);
        final ScoringConfigGroup.ActivityParams workParams = new ScoringConfigGroup.ActivityParams("work")
                .setTypicalDuration(20);
        config.scoring().addActivityParams(workParams);

        var strategy = new ReplanningConfigGroup.StrategySettings();
        strategy.setStrategyName("ChangeExpBeta");
        strategy.setWeight(1.0);

        config.replanning().addParameterSet(strategy);

        // activate snapshots
        config.qsim().setSnapshotPeriod(1);
        config.qsim().setSnapshotStyle(QSimConfigGroup.SnapshotStyle.queue);
        config.controller().setWriteSnapshotsInterval(1);
        config.controller().setSnapshotFormat(Set.of(ControllerConfigGroup.SnapshotFormat.positionevents));
        config.controller().setFirstIteration(0);
        config.controller().setLastIteration(0);

        config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.fromVehiclesData);

        // create a scenario:
        final MutableScenario scenario = ScenarioUtils.createMutableScenario(config);
        scenario.setNetwork(createSingleLinkNetwork());

        var vehicleType = createVehicleType();
        scenario.getVehicles().addVehicleType(vehicleType);
        Vehicle vehicle = VehicleUtils.createVehicle(Id.createVehicleId("1"), vehicleType);
        scenario.getVehicles().addVehicle(vehicle);
        var person = createPerson(scenario.getPopulation().getFactory());
        scenario.getPopulation().addPerson(person);
        VehicleUtils.insertVehicleIdsIntoAttributes(person, Map.of(vehicle.getType().getNetworkMode(), vehicle.getId()));

        var controler = new Controler(scenario);

        controler.addOverridingModule(new PositionEmissionsModule());
        // exclude last link since both emission calculations work slightly different for the last link of a trip.
        var combinedHandler = new FilterLinkHandler(Set.of(Id.createLinkId("start-link"), Id.createLinkId("link")));

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addEventHandlerBinding().toInstance(combinedHandler);
                addEventHandlerBinding().toInstance((BasicEventHandler) event -> {
                    if (!event.getEventType().equals(PositionEvent.EVENT_TYPE) && !event.getEventType().equals(PositionEmissionsModule.PositionEmissionEvent.EVENT_TYPE)) {
                        System.out.println(event.getEventType());
                    }
                });
                bind(EventsManager.class).to(EventsManagerImpl.class).in(Singleton.class);
            }
        });

        controler.run();

        // check that the overall amount of emissions is equal for the main link
        for (var entry : combinedHandler.getClassicEmissions().entrySet()) {
            assertEquals(entry.getValue(), combinedHandler.getPositionEmissions().get(entry.getKey()), 0.1);
        }
    }

    private static class FilterLinkHandler implements BasicEventHandler {

        private final Set<Id<Link>> filterLinks;
        private final Map<Pollutant, Double> classicEmissions = new HashMap<>();
        private final Map<Pollutant, Double> positionEmissions = new HashMap<>();

        private FilterLinkHandler(Set<Id<Link>> filterLinks) {
            this.filterLinks = filterLinks;
        }

        Map<Pollutant, Double> getClassicEmissions() {
            return classicEmissions;
        }

        Map<Pollutant, Double> getPositionEmissions() {
            return positionEmissions;
        }

        void sumAll(Map<Pollutant, Double> to, Map<Pollutant, Double> from) {

            for (Map.Entry<Pollutant, Double> pollutantDoubleEntry : from.entrySet()) {
                to.merge(pollutantDoubleEntry.getKey(), pollutantDoubleEntry.getValue(), Double::sum);
            }
        }

        @Override
        public void handleEvent(Event event) {
            switch (event.getEventType()) {
                case WarmEmissionEvent.EVENT_TYPE:
                case ColdEmissionEvent.EVENT_TYPE:
                    var emissionEvent = (EmissionEvent) event;
                    if (filterLinks.contains(emissionEvent.getLinkId())) {
                        sumAll(classicEmissions, emissionEvent.getEmissions());
                    }
                    break;
                case PositionEmissionsModule.PositionEmissionEvent.EVENT_TYPE:
                    var positionEvent = (PositionEmissionsModule.PositionEmissionEvent) event;
                    if (positionEvent.getLinkId().equals(Id.createLinkId("link")) && positionEvent.getEmissionType().equals("combined"))
                        sumAll(positionEmissions, positionEvent.getEmissions());
                    break;

            }
        }
    }

    private VehicleType createVehicleType() {
        VehicleType vehicleType = VehicleUtils.createVehicleType(Id.create("dieselCarFullSpecified", VehicleType.class), TransportMode.car);
        EngineInformation engineInformation = vehicleType.getEngineInformation();
        VehicleUtils.setHbefaVehicleCategory(engineInformation, "PASSENGER_CAR");
        VehicleUtils.setHbefaTechnology(engineInformation, "diesel");
        VehicleUtils.setHbefaEmissionsConcept(engineInformation, "PC-D-Euro-3");
        VehicleUtils.setHbefaSizeClass(engineInformation, ">1,4L");
        vehicleType.setMaximumVelocity(10);

        return vehicleType;
    }

    /**
     * Creates network with three links, to make sure the main link is traversed completely
     */
    private Network createSingleLinkNetwork() {

        var network = NetworkUtils.createNetwork(new NetworkConfigGroup());
        addLink(network, "start-link",
                network.getFactory().createNode(Id.createNodeId("1"), new Coord(-100, 0)),
                network.getFactory().createNode(Id.createNodeId("2"), new Coord(0, 0)));
        addLink(network, "link",
                network.getFactory().createNode(Id.createNodeId("2"), new Coord(0, 0)),
                network.getFactory().createNode(Id.createNodeId("3"), new Coord(10000, 0)));
        addLink(network, "end-link",
                network.getFactory().createNode(Id.createNodeId("3"), new Coord(1000, 0)),
                network.getFactory().createNode(Id.createNodeId("4"), new Coord(10100, 0)));
        return network;
    }

    private void addLink(Network network, String id, Node from, Node to) {

        if (!network.getNodes().containsKey(from.getId()))
            network.addNode(from);
        if (!network.getNodes().containsKey(to.getId()))
            network.addNode(to);

        var link = network.getFactory().createLink(Id.createLinkId(id), from, to);
        EmissionUtils.setHbefaRoadType(link, "URB/Local/50");
        link.setFreespeed(10);
        network.addLink(link);
    }

    private Person createPerson(PopulationFactory factory) {

        var plan = factory.createPlan();
        var home = PopulationUtils.createAndAddActivityFromCoord(plan, "home", new Coord(-100, 0));
        home.setEndTime(1);

        var leg = PopulationUtils.createLeg(TransportMode.car);
        leg.setRoute(RouteUtils.createNetworkRoute(List.of(Id.createLinkId("start"), Id.createLinkId("link"), Id.createLinkId("end"))));
        plan.addLeg(leg);

        var work = PopulationUtils.createAndAddActivityFromCoord(plan, "work", new Coord(10100, 0));
        work.setEndTime(3600);

        var person = factory.createPerson(Id.createPersonId("person"));
        person.addPlan(plan);
        person.setSelectedPlan(plan);
        return person;
    }
}
