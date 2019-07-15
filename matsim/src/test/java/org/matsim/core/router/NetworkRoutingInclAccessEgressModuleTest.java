package org.matsim.core.router;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleCapacityImpl;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehiclesFactory;

import java.util.*;

public class NetworkRoutingInclAccessEgressModuleTest {

    private static final double SLOW_SPEED = 10 / 3.6;
    private static final double FAST_SPEED = 100 / 3.6;
    private static final Coord HOME = new Coord(0, 0);
    private static final Coord WORK = new Coord(11100, 0);
    private static final String FAST_MODE = "fast-mode";
    private static final String SLOW_MODE = "slow-mode";

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    private static Scenario createScenario() {

        final Set<String> modes = new HashSet<>(Arrays.asList(TransportMode.car, FAST_MODE, SLOW_MODE));
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Network network = scenario.getNetwork();
        NetworkFactory factory = network.getFactory();
        Node n1 = factory.createNode(Id.create("1", Node.class), HOME);
        Node n2 = factory.createNode(Id.create("2", Node.class), new Coord(HOME.getX() + 50, HOME.getY()));
        Node n3 = factory.createNode(Id.create("3", Node.class), new Coord(WORK.getX() - 50, WORK.getY()));
        Node n4 = factory.createNode(Id.create("4", Node.class), WORK);
        network.addNode(n1);
        network.addNode(n2);
        network.addNode(n3);
        network.addNode(n4);
        Link startLink = factory.createLink(Id.createLinkId("start"), n1, n2);
        Link directButSlow = factory.createLink(Id.create("slow-link", Link.class), n2, n3);
        Link longButFast = factory.createLink(Id.create("fast-link", Link.class), n2, n3);
        Link endLInk = factory.createLink(Id.createLinkId("end"), n3, n4);
        startLink.setAllowedModes(modes);
        directButSlow.setAllowedModes(modes);
        longButFast.setAllowedModes(modes);
        endLInk.setAllowedModes(modes);
        startLink.setFreespeed(FAST_SPEED);
        directButSlow.setFreespeed(SLOW_SPEED);
        longButFast.setFreespeed(FAST_SPEED);
        endLInk.setFreespeed(FAST_SPEED);

        startLink.setLength(50);
        directButSlow.setLength(11000);
        longButFast.setLength(100000.0);
        endLInk.setLength(50);
        network.addLink(startLink);
        network.addLink(directButSlow);
        network.addLink(longButFast);
        network.addLink(endLInk);
        return scenario;
    }

    private static Person createPerson(String id, Coord homeCoord, Coord workCoord, String mode, PopulationFactory factory) {

        Person person = factory.createPerson(Id.createPersonId(id));

        Plan plan = factory.createPlan();
        Activity home1 = factory.createActivityFromCoord("home", homeCoord);
        home1.setEndTime(0);
        home1.setLinkId(Id.createLinkId("start"));
        plan.addActivity(home1);

        Leg toWork = factory.createLeg(mode);
        plan.addLeg(toWork);

        Activity work = factory.createActivityFromCoord("work", workCoord);
        work.setEndTime(300);
        work.setLinkId(Id.createLinkId("end"));
        plan.addActivity(work);

        person.addPlan(plan);

        return person;
    }

    private static VehicleType createVehicleType(String name, double maxVelocity, int capacity, VehiclesFactory factory) {

        VehicleType result = factory.createVehicleType(Id.create(name, VehicleType.class));
        VehicleCapacity vehicleCapacity = new VehicleCapacityImpl();
        vehicleCapacity.setSeats(capacity);
        result.setCapacity(vehicleCapacity);
        result.setMaximumVelocity(maxVelocity);
        return result;
    }

    @Test
    public void calcRoute() {

        Scenario scenario = createScenario();

        // add vehicle types
        VehicleType slowType = createVehicleType(SLOW_MODE, SLOW_SPEED, 1, scenario.getVehicles().getFactory());
        VehicleType fastType = createVehicleType(FAST_MODE, FAST_SPEED, 1, scenario.getVehicles().getFactory());

        scenario.getVehicles().addVehicleType(slowType);
        scenario.getVehicles().addVehicleType(fastType);

        // add persons
        Person slowPerson = createPerson("slow-person", HOME, WORK, slowType.getId().toString(), scenario.getPopulation().getFactory());
        Person fastPerson = createPerson("fast-person", HOME, WORK, fastType.getId().toString(), scenario.getPopulation().getFactory());

        scenario.getPopulation().addPerson(slowPerson);
        scenario.getPopulation().addPerson(fastPerson);

        // set up a config
        Collection<String> modes = Arrays.asList(slowType.getId().toString(), fastType.getId().toString());

        scenario.getConfig().qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);
        scenario.getConfig().qsim().setUsePersonIdForMissingVehicleId(true);
        scenario.getConfig().controler().setFirstIteration(0);
        scenario.getConfig().controler().setLastIteration(1);
        scenario.getConfig().controler().setOutputDirectory(utils.getOutputDirectory());
        scenario.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
        scenario.getConfig().qsim().setMainModes(modes);
        scenario.getConfig().plansCalcRoute().setNetworkModes(modes);
        scenario.getConfig().plansCalcRoute().setInsertingAccessEgressWalk(true);

        PlanCalcScoreConfigGroup scoring = scenario.getConfig().planCalcScore();
        PlanCalcScoreConfigGroup.ModeParams slowParams = new PlanCalcScoreConfigGroup.ModeParams(slowType.getId().toString());
        slowParams.setMarginalUtilityOfTraveling(-1);
        scoring.addModeParams(slowParams);

        PlanCalcScoreConfigGroup.ModeParams fastParams = new PlanCalcScoreConfigGroup.ModeParams(fastType.getId().toString());
        fastParams.setMarginalUtilityOfTraveling(-1);
        scoring.addModeParams(fastParams);

        final PlanCalcScoreConfigGroup.ActivityParams homeParams = new PlanCalcScoreConfigGroup.ActivityParams("home");
        homeParams.setTypicalDuration(1);
        scenario.getConfig().planCalcScore().addActivityParams(homeParams);

        final PlanCalcScoreConfigGroup.ActivityParams workParams = new PlanCalcScoreConfigGroup.ActivityParams("work");
        workParams.setTypicalDuration(1);
        scenario.getConfig().planCalcScore().addActivityParams(workParams);

        StrategyConfigGroup.StrategySettings replanning = new StrategyConfigGroup.StrategySettings();
        replanning.setStrategyName("ReRoute");
        replanning.setWeight(1.0);
        scenario.getConfig().strategy().addStrategySettings(replanning);

        Controler controler = new Controler(scenario);

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bind(MainModeIdentifier.class).to(SimpleMainModeIdentifier.class);
            }
        });

        controler.addControlerListener(new ReplanningListener() {
            @Override
            public void notifyReplanning(ReplanningEvent event) {
                System.out.println(event.toString());
            }
        });
        controler.run();

        // get the output here and assert, that the two persons have taken the right path


    }

    private static class SimpleMainModeIdentifier implements MainModeIdentifier {

        @Override
        public String identifyMainMode(List<? extends PlanElement> tripElements) {
            return tripElements.stream()
                    .filter(element -> element instanceof Leg)
                    .map(element -> (Leg) element)
                    .map(Leg::getMode)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("could not find main mode"));
        }
    }
}