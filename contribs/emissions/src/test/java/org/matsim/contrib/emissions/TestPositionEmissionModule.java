package org.matsim.contrib.emissions;

import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestPositionEmissionModule {

    private static final String configFile = "./scenarios/sampleScenario/testv2_Vehv1/config_detailed.xml";
    private static final Logger log = Logger.getLogger(TestPositionEmissionModule.class);

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

        controler.run();
    }

    private VehicleType createVehicleType() {
        VehicleType vehicleType = VehicleUtils.createVehicleType(Id.create("dieselCarFullSpecified", VehicleType.class));
        EngineInformation engineInformation = vehicleType.getEngineInformation();
        VehicleUtils.setHbefaVehicleCategory(engineInformation, "PASSENGER_CAR");
        VehicleUtils.setHbefaTechnology(engineInformation, "diesel");
        VehicleUtils.setHbefaEmissionsConcept(engineInformation, "PC-D-Euro-3");
        VehicleUtils.setHbefaSizeClass(engineInformation, ">1,4L");

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
