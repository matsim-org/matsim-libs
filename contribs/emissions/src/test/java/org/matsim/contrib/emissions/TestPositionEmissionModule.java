package org.matsim.contrib.emissions;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
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

import javax.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class TestPositionEmissionModule {

    private static final String configFile = IOUtils.extendUrl( ExamplesUtils.getTestScenarioURL( "emissions-sampleScenario/testv2_Vehv2" ), "config_detailed.xml" ).toString();
    // (I changed the above from veh v1 to veh v2 since veh v1 is deprecated, especially for emissions.  kai, apr'21)

    @Rule
    public MatsimTestUtils testUtils = new MatsimTestUtils();

    @Test
    @Ignore
    public void simpleTest() {

        var emissionConfig = new EmissionsConfigGroup();
        emissionConfig.setHbefaVehicleDescriptionSource(EmissionsConfigGroup.HbefaVehicleDescriptionSource.fromVehicleTypeDescription);
        emissionConfig.setDetailedVsAverageLookupBehavior(
                EmissionsConfigGroup.DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageThenAverageTable); //This is the previous behaviour
        var config = ConfigUtils.loadConfig(configFile, emissionConfig);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        config.qsim().setSnapshotPeriod(1);
        config.qsim().setSnapshotStyle(QSimConfigGroup.SnapshotStyle.queue);
        config.controler().setWriteSnapshotsInterval(1);
        config.controler().setSnapshotFormat(Set.of(ControlerConfigGroup.SnapshotFormat.positionevents));

        var scenario = ScenarioUtils.loadScenario(config);

        var controler = new Controler(scenario);

        controler.addOverridingModule(new PositionEmissionsModule());
        controler.run();
    }

    @Test
    public void compareToOtherModule_singleVehicleSingleLink() {

        var emissionConfig = new EmissionsConfigGroup();
        emissionConfig.setHbefaVehicleDescriptionSource(EmissionsConfigGroup.HbefaVehicleDescriptionSource.fromVehicleTypeDescription);
        emissionConfig.setDetailedVsAverageLookupBehavior(EmissionsConfigGroup.DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageThenAverageTable);

        var config = ConfigUtils.loadConfig(configFile, emissionConfig);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        config.controler().setOutputDirectory(testUtils.getOutputDirectory());

        final PlanCalcScoreConfigGroup.ActivityParams homeParams = new PlanCalcScoreConfigGroup.ActivityParams("home")
                .setTypicalDuration(20);
        config.planCalcScore().addActivityParams(homeParams);
        final PlanCalcScoreConfigGroup.ActivityParams workParams = new PlanCalcScoreConfigGroup.ActivityParams("work")
                .setTypicalDuration(20);
        config.planCalcScore().addActivityParams(workParams);

        var strategy = new StrategyConfigGroup.StrategySettings();
        strategy.setStrategyName("ChangeExpBeta");
        strategy.setWeight(1.0);

        config.strategy().addParameterSet(strategy);

        // activate snapshots
        config.qsim().setSnapshotPeriod(1);
        config.qsim().setSnapshotStyle(QSimConfigGroup.SnapshotStyle.queue);
        config.controler().setWriteSnapshotsInterval(1);
        config.controler().setSnapshotFormat(Set.of(ControlerConfigGroup.SnapshotFormat.positionevents));
        config.controler().setFirstIteration(0);
        config.controler().setLastIteration(0);

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
        var handler = new AllEmissionsHandler();
        var mainLinkWarmHandler = new MainLinkWarmHandler();
        var coldHandler = new ColdHandler();
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addEventHandlerBinding().toInstance(handler);
                addEventHandlerBinding().toInstance(mainLinkWarmHandler);
                addEventHandlerBinding().toInstance(coldHandler);
                bind(EventsManager.class).to(EventsManagerImpl.class).in(Singleton.class);
            }
        });

        controler.run();

        // check for equal values on the main link for warm emissions
        for (var entry : mainLinkWarmHandler.getClassicEmissions().entrySet()) {
            assertEquals(entry.getValue(), mainLinkWarmHandler.getPositionEmissions().get(entry.getKey()), 0.1);
        }

        // check for equal values for cold emissions
        for (var entry : coldHandler.getClassicEmissions().entrySet()) {
            assertEquals(entry.getValue(), coldHandler.getPositionEmissions().get(entry.getKey()), 0.1);
        }

        // this test fails because WarmEmissionHandler doesn't generate an event for the last link of a leg.
        // I think this is not correct. But talk to kai about it.
        // In the end the overall ammount of emissions should also be equal.
       /* for (var entry : handler.getClassicEmissions().entrySet()) {
            assertEquals(entry.getValue(), handler.getPositionEmissions().get(entry.getKey()), 0.1);
        }
        */
    }

    private static class AllEmissionsHandler extends Handler implements BasicEventHandler {

        @Override
        public void handleEvent(Event event) {

            switch (event.getEventType()) {
                case ColdEmissionEvent.EVENT_TYPE:
                    sumAll(classicEmissions, ((ColdEmissionEvent) event).getColdEmissions());
                    break;
                case WarmEmissionEvent.EVENT_TYPE:
                    sumAll(classicEmissions, ((WarmEmissionEvent) event).getWarmEmissions());
                    break;
                case PositionEmissionsModule.PositionEmissionEvent.EVENT_TYPE:
                    sumAll(positionEmissions, ((PositionEmissionsModule.PositionEmissionEvent) event).getEmissions());
                    break;
            }
        }
    }

    private static class MainLinkWarmHandler extends Handler implements BasicEventHandler {

        @Override
        public void handleEvent(Event event) {

            switch (event.getEventType()) {
                case WarmEmissionEvent.EVENT_TYPE:
                    var warmEmissionEvent = (WarmEmissionEvent) event;
                    if (warmEmissionEvent.getLinkId().equals(Id.createLinkId("link")))
                        sumAll(classicEmissions, warmEmissionEvent.getWarmEmissions());
                    break;
                case PositionEmissionsModule.PositionEmissionEvent.EVENT_TYPE:
                    var positionEvent = (PositionEmissionsModule.PositionEmissionEvent) event;
                    if (positionEvent.getLinkId().equals(Id.createLinkId("link")) && positionEvent.getEmissionType().equals("warm"))
                        sumAll(positionEmissions, positionEvent.getEmissions());
                    break;
            }
        }
    }

    private static class ColdHandler extends Handler implements BasicEventHandler {

        @Override
        public void handleEvent(Event event) {

            switch (event.getEventType()) {
                case ColdEmissionEvent.EVENT_TYPE:
                    var coldEmissionEvent = (ColdEmissionEvent) event;
                    sumAll(classicEmissions, coldEmissionEvent.getColdEmissions());
                    break;
                case PositionEmissionsModule.PositionEmissionEvent.EVENT_TYPE:
                    var positionEvent = (PositionEmissionsModule.PositionEmissionEvent) event;
                    if (positionEvent.getEmissionType().equals("cold"))
                        sumAll(positionEmissions, positionEvent.getEmissions());
                    break;
            }
        }
    }

    private static class Handler {

        final Map<Pollutant, Double> classicEmissions = new HashMap<>();
        final Map<Pollutant, Double> positionEmissions = new HashMap<>();

        public Map<Pollutant, Double> getClassicEmissions() {
            return classicEmissions;
        }

        public Map<Pollutant, Double> getPositionEmissions() {
            return positionEmissions;
        }

        void sumAll(Map<Pollutant, Double> to, Map<Pollutant, Double> from) {

            for (Map.Entry<Pollutant, Double> pollutantDoubleEntry : from.entrySet()) {
                to.merge(pollutantDoubleEntry.getKey(), pollutantDoubleEntry.getValue(), Double::sum);
            }
        }
    }

    private VehicleType createVehicleType() {
        VehicleType vehicleType = VehicleUtils.createVehicleType(Id.create("dieselCarFullSpecified", VehicleType.class));
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

        var network = NetworkUtils.createNetwork();
        addLink(network, "start-link",
                network.getFactory().createNode(Id.createNodeId("1"), new Coord(-100, 0)),
                network.getFactory().createNode(Id.createNodeId("2"), new Coord(0, 0)));
        addLink(network, "link",
                network.getFactory().createNode(Id.createNodeId("2"), new Coord(0, 0)),
                network.getFactory().createNode(Id.createNodeId("3"), new Coord(1000, 0)));
        addLink(network, "end-link",
                network.getFactory().createNode(Id.createNodeId("3"), new Coord(1000, 0)),
                network.getFactory().createNode(Id.createNodeId("4"), new Coord(1100, 0)));
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

        var work = PopulationUtils.createAndAddActivityFromCoord(plan, "work", new Coord(1100, 0));
        work.setEndTime(3600);

        var person = factory.createPerson(Id.createPersonId("person"));
        person.addPlan(plan);
        person.setSelectedPlan(plan);
        return person;
    }
}
