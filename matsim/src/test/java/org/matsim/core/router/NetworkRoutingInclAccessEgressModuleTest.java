package org.matsim.core.router;

import org.junit.Rule;
import org.junit.Test;
import org.locationtech.jts.util.Assert;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.AccessEgressType;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.*;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class NetworkRoutingInclAccessEgressModuleTest {

    private static final double SLOW_SPEED = 10 / 3.6;
    private static final double FAST_SPEED = 100 / 3.6;
    private static final Coord HOME = new Coord(0, 0);
    private static final Coord NEARHOME = new Coord(10, 10);
    private static final Coord WORK = new Coord(11100, 0);
    private static final String FAST_MODE = "fast-mode";
    private static final String SLOW_MODE = "slow-mode";
    private static final String START_LINK = "start-link";
    private static final String END_LINK = "end-link";
    private static final String SLOW_BUT_DIRECT_LINK = "slow-but-direct-link";
    private static final String FAST_BUT_LONGER_LINK = "fast-but-longer-link";

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    private static Scenario createScenario(Config config) {

        final Set<String> modes = new HashSet<>(Arrays.asList(TransportMode.car, FAST_MODE, SLOW_MODE));
        Scenario scenario = ScenarioUtils.createScenario(config);
        Network network = scenario.getNetwork();
        NetworkFactory factory = network.getFactory();
        Node n1 = factory.createNode(Id.create("1", Node.class), HOME);
        Node n2 = factory.createNode(Id.create("2", Node.class), new Coord(HOME.getX() + 50, HOME.getY()));
        Node n3 = factory.createNode(Id.create("3", Node.class), new Coord((WORK.getX() - HOME.getX()) / 2, HOME.getY() - 1000));
        Node n4 = factory.createNode(Id.create("4", Node.class), new Coord(WORK.getX() - 50, WORK.getY()));
        Node n5 = factory.createNode(Id.create("5", Node.class), WORK);
        network.addNode(n1);
        network.addNode(n2);
        network.addNode(n3);
        network.addNode(n4);
        network.addNode(n5);

        Link startLink = createLink(START_LINK, n1, n2, 50, FAST_SPEED, modes, factory);
        network.addLink(startLink);

        Link directButSlow = createLink(SLOW_BUT_DIRECT_LINK, n2, n4, 11000, SLOW_SPEED, modes, factory);
        network.addLink(directButSlow);

        // split the fast link into two pieces to form a triangle. Thus, one could visually examine the separate links
        Link longButFast1 = createLink(FAST_BUT_LONGER_LINK + "-1", n2, n3, 6000, FAST_SPEED, modes, factory);
        network.addLink(longButFast1);
        Link longButFast2 = createLink(FAST_BUT_LONGER_LINK + "-2", n3, n4, 6000, FAST_SPEED, modes, factory);
        network.addLink(longButFast2);

        Link endLInk = createLink(END_LINK, n4, n5, 50, FAST_SPEED, modes, factory);
        network.addLink(endLInk);

        return scenario;
    }

    private static Controler createControler(Scenario scenario) {

        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bind(MainModeIdentifier.class).to(SimpleMainModeIdentifier.class);
            }
        });
        return controler;
    }

    private Config createConfig() {

        Config config = ConfigUtils.createConfig();
        config.qsim().setUsePersonIdForMissingVehicleId(true);
        config.controler().setFirstIteration(0);
        config.controler().setLastIteration(0);
        config.controler().setOutputDirectory(utils.getOutputDirectory());
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
        config.plansCalcRoute().setAccessEgressType(PlansCalcRouteConfigGroup.AccessEgressType.accessEgressModeToLink);

        final PlanCalcScoreConfigGroup.ActivityParams homeParams = new PlanCalcScoreConfigGroup.ActivityParams("home");
        homeParams.setTypicalDuration(1);
        config.planCalcScore().addActivityParams(homeParams);

        final PlanCalcScoreConfigGroup.ActivityParams workParams = new PlanCalcScoreConfigGroup.ActivityParams("work");
        workParams.setTypicalDuration(1);
        config.planCalcScore().addActivityParams(workParams);

        StrategyConfigGroup.StrategySettings replanning = new StrategyConfigGroup.StrategySettings();
        replanning.setStrategyName("ReRoute");
        replanning.setWeight(1.0);
        config.strategy().addStrategySettings(replanning);

        return config;
    }

    private static Link createLink(String id, Node from, Node to, double length, double freespeed, Set<String> modes, NetworkFactory factory) {

        Link link = factory.createLink(Id.createLinkId(id), from, to);
        link.setLength(length);
        link.setFreespeed(freespeed);
        link.setAllowedModes(modes);
        link.setCapacity(100000); // get capacity constraints out of the way
        return link;
    }

    private static Person createPerson(String id, String mode, PopulationFactory factory) {

        Person person = factory.createPerson(Id.createPersonId(id));

        Plan plan = factory.createPlan();
        Activity home1 = factory.createActivityFromCoord("home", HOME);
        home1.setEndTime(0);
        home1.setLinkId(Id.createLinkId(START_LINK));
        plan.addActivity(home1);

        Leg toWork = factory.createLeg(mode);
        plan.addLeg(toWork);

        Activity work = factory.createActivityFromCoord("work", WORK);
        work.setEndTime(300);
        work.setLinkId(Id.createLinkId(END_LINK));
        plan.addActivity(work);

        person.addPlan(plan);

        return person;
    }

    private static VehicleType createVehicleType(String name, double maxVelocity, VehiclesFactory factory) {

        VehicleType result = factory.createVehicleType(Id.create(name, VehicleType.class));
//        VehicleCapacity vehicleCapacity = new VehicleCapacity();
        result.getCapacity().setSeats(1);
//        result.setCapacity(vehicleCapacity);
        result.setMaximumVelocity(maxVelocity);
        return result;
    }

    private static List<Id<Link>> getNetworkRoute(List<PlanElement> elements) {
        return elements.stream()
                .filter(element -> element instanceof Leg)
                .map(element -> (Leg) element)
                .filter(leg -> !leg.getMode().equals(TransportMode.walk))
                .flatMap(leg -> ((NetworkRoute) leg.getRoute()).getLinkIds().stream())
                .collect(Collectors.toList());
    }

    @Test
    public void calcRoute_modeVehiclesFromVehiclesData_differentTypesTakeDifferentRoutes() {

        Config config = createConfig();

        // set test specific things
        Collection<String> modes = Arrays.asList(SLOW_MODE, FAST_MODE);

        config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);
        config.qsim().setMainModes(modes);
        config.plansCalcRoute().setNetworkModes(modes);
        PlanCalcScoreConfigGroup scoring = config.planCalcScore();

        PlanCalcScoreConfigGroup.ModeParams slowParams = new PlanCalcScoreConfigGroup.ModeParams(SLOW_MODE);
        slowParams.setMarginalUtilityOfTraveling(-1);
        scoring.addModeParams(slowParams);

        PlanCalcScoreConfigGroup.ModeParams fastParams = new PlanCalcScoreConfigGroup.ModeParams(FAST_MODE);
        fastParams.setMarginalUtilityOfTraveling(-1);
        scoring.addModeParams(fastParams);

        Scenario scenario = createScenario(config);

        // create
        VehicleType slowType = createVehicleType(SLOW_MODE, SLOW_SPEED, scenario.getVehicles().getFactory());
        VehicleType fastType = createVehicleType(FAST_MODE, FAST_SPEED, scenario.getVehicles().getFactory());

        scenario.getVehicles().addVehicleType(slowType);
        scenario.getVehicles().addVehicleType(fastType);

        // add persons
        Person slowPerson = createPerson("slow-person", slowType.getId().toString(), scenario.getPopulation().getFactory());
        scenario.getPopulation().addPerson(slowPerson);
        Person fastPerson = createPerson("fast-person", fastType.getId().toString(), scenario.getPopulation().getFactory());
        scenario.getPopulation().addPerson(fastPerson);

        // set up a controler with individual main mode identifier at the point of this writing the default one seems broken
        Controler controler = createControler(scenario);
        controler.run();

        // Assert that the slow agent takes the direct route
        List<Id<Link>> slowLinkIds = getNetworkRoute(slowPerson.getSelectedPlan().getPlanElements());
        assertEquals(1, slowLinkIds.size());
        assertEquals(Id.createLinkId(SLOW_BUT_DIRECT_LINK), slowLinkIds.get(0));

        // assert that the fast agent takes the longer route with higher speed
        List<Id<Link>> fastLinkIds = getNetworkRoute(fastPerson.getSelectedPlan().getPlanElements());
        assertEquals(2, fastLinkIds.size());
        assertEquals(Id.createLinkId(FAST_BUT_LONGER_LINK + "-1"), fastLinkIds.get(0));
        assertEquals(Id.createLinkId(FAST_BUT_LONGER_LINK + "-2"), fastLinkIds.get(1));
    }




    @Test
    public void calcRoute_defaultVehicle_defaultVehicleIsAssigned() {

        Config config = createConfig();
        config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.defaultVehicle);

        Scenario scenario = createScenario(config);

        // add persons
        Person person = createPerson("slow-person", "car", scenario.getPopulation().getFactory());
        scenario.getPopulation().addPerson(person);

        // set up a controler with individual main mode identifier at the point of this writing the default one seems broken
        Controler controler = createControler(scenario);
        controler.run();

        Map<String, Id<Vehicle>> modeId = VehicleUtils.getVehicleIds(person);

        // should be only one, but however
        for (Id<Vehicle> vehicleId : modeId.values()) {
            assertTrue(scenario.getVehicles().getVehicles().containsKey(vehicleId));
            assertEquals(VehicleUtils.getDefaultVehicleType(), scenario.getVehicles().getVehicles().get(vehicleId).getType());
        }
    }


    @Test
    public void useAccessEgressTimeFromLinkAttributes() {

        Config config = createConfig();
        config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.defaultVehicle);
        config.plansCalcRoute().setAccessEgressType(PlansCalcRouteConfigGroup.AccessEgressType.walkConstantTimeToLink);
        Scenario scenario = createScenario(config);
        NetworkUtils.setLinkAccessTime(scenario.getNetwork().getLinks().get(Id.createLinkId(START_LINK)),TransportMode.car,75);
        NetworkUtils.setLinkEgressTime(scenario.getNetwork().getLinks().get(Id.createLinkId(END_LINK)),TransportMode.car,180);
        // add persons
        Person person = createPerson("slow-person", TransportMode.car, scenario.getPopulation().getFactory());
        scenario.getPopulation().addPerson(person);

        Controler controler = createControler(scenario);
        controler.run();
        var legs = TripStructureUtils.getLegs(person.getSelectedPlan());
        Assert.equals(3,legs.size());
        Assert.equals(75.0,legs.get(0).getTravelTime().seconds());
        Assert.equals(180.0,legs.get(2).getTravelTime().seconds());
    }

    @Test
    public void useAccessEgressTimeFromConstantAndWalkTime() {

        Config config = createConfig();
        config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.defaultVehicle);
        config.plansCalcRoute().setAccessEgressType(PlansCalcRouteConfigGroup.AccessEgressType.accessEgressModeToLinkPlusTimeConstant);
        Scenario scenario = createScenario(config);
        NetworkUtils.setLinkAccessTime(scenario.getNetwork().getLinks().get(Id.createLinkId(START_LINK)),TransportMode.car,75);
        NetworkUtils.setLinkEgressTime(scenario.getNetwork().getLinks().get(Id.createLinkId(END_LINK)),TransportMode.car,180);
        // add persons
        Person person = createPerson("slow-person", TransportMode.car, scenario.getPopulation().getFactory());
        Activity h = (Activity) person.getSelectedPlan().getPlanElements().get(0);
        h.setCoord(NEARHOME);
        scenario.getPopulation().addPerson(person);

        Controler controler = createControler(scenario);
        controler.run();
        var legs = TripStructureUtils.getLegs(person.getSelectedPlan());
        Assert.equals(3,legs.size());
        Assert.equals(90.0,legs.get(0).getTravelTime().seconds());
        Assert.equals(180.0,legs.get(2).getTravelTime().seconds());
    }
    
    @Test
    public void routingModeInEvents() {

        Config config = createConfig();
        config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.defaultVehicle);
        config.plansCalcRoute().setAccessEgressType(PlansCalcRouteConfigGroup.AccessEgressType.accessEgressModeToLinkPlusTimeConstant);
        Scenario scenario = createScenario(config);
        NetworkUtils.setLinkAccessTime(scenario.getNetwork().getLinks().get(Id.createLinkId(START_LINK)),TransportMode.car,75);
        NetworkUtils.setLinkEgressTime(scenario.getNetwork().getLinks().get(Id.createLinkId(END_LINK)),TransportMode.car,180);
        // add persons
        Person person = createPerson("slow-person", TransportMode.car, scenario.getPopulation().getFactory());
        Activity h = (Activity) person.getSelectedPlan().getPlanElements().get(0);
        h.setCoord(NEARHOME);
        scenario.getPopulation().addPerson(person);

        Controler controler = createControler(scenario);
        
        Set<String> legModes = new HashSet<>();
        Set<String> routingModes = new HashSet<>();
        
        controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().toInstance(new PersonDepartureEventHandler() {
					@Override
					public void handleEvent(PersonDepartureEvent event) {
						legModes.add(event.getLegMode());
						routingModes.add(event.getRoutingMode());
					}
				});
			}
		});
        
        controler.run();
        
        Assert.equals(2, legModes.size());
        Assert.equals(1, routingModes.size());
        Assert.isTrue(legModes.contains("walk"));
        Assert.isTrue(legModes.contains("car"));
        Assert.isTrue(routingModes.contains("car"));
    }

    @Test(expected = RuntimeException.class)
    public void failifNoAccessTimeSet() {

        Config config = createConfig();
        config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.defaultVehicle);
        config.plansCalcRoute().setAccessEgressType(PlansCalcRouteConfigGroup.AccessEgressType.walkConstantTimeToLink);
        Scenario scenario = createScenario(config);
        NetworkUtils.setLinkAccessTime(scenario.getNetwork().getLinks().get(Id.createLinkId(START_LINK)),TransportMode.car,75);
        Person person = createPerson("slow-person", TransportMode.car, scenario.getPopulation().getFactory());
        scenario.getPopulation().addPerson(person);
        Controler controler = createControler(scenario);
        controler.run();
    }


    @Test(expected = RuntimeException.class)
    public void failifNoEgressTimeSet() {

        Config config = createConfig();
        config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.defaultVehicle);
        config.plansCalcRoute().setAccessEgressType(AccessEgressType.walkConstantTimeToLink);
        Scenario scenario = createScenario(config);
        NetworkUtils.setLinkEgressTime(scenario.getNetwork().getLinks().get(Id.createLinkId(END_LINK)),TransportMode.car,180);
        Person person = createPerson("slow-person", TransportMode.car, scenario.getPopulation().getFactory());
        scenario.getPopulation().addPerson(person);
        Controler controler = createControler(scenario);
        controler.run();
    }

    @Test
    public void calcAccessTimeFromDistanceToLink() {

        Config config = createConfig();
        config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.defaultVehicle);
        config.plansCalcRoute().setAccessEgressType(PlansCalcRouteConfigGroup.AccessEgressType.accessEgressModeToLink);
        Scenario scenario = createScenario(config);
        NetworkUtils.setLinkAccessTime(scenario.getNetwork().getLinks().get(Id.createLinkId(START_LINK)),TransportMode.car,75);
        NetworkUtils.setLinkAccessTime(scenario.getNetwork().getLinks().get(Id.createLinkId(END_LINK)),TransportMode.car,180);
        // add persons
        Person person = createPerson("slow-person", TransportMode.car, scenario.getPopulation().getFactory());
        Activity h = (Activity) person.getSelectedPlan().getPlanElements().get(0);
        h.setCoord(NEARHOME);
        scenario.getPopulation().addPerson(person);

        Controler controler = createControler(scenario);
        controler.run();
        var legs = TripStructureUtils.getLegs(person.getSelectedPlan());
        Assert.equals(3,legs.size());
        //the agent starts at the fromNode
        Assert.equals(15.0,legs.get(0).getTravelTime().seconds());
        //the agent is lucky: work location is at the to-node
        Assert.equals(0.0,legs.get(2).getTravelTime().seconds());
    }

    @Test
    public void noBushwackingLegs() {

        Config config = createConfig();
        config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.defaultVehicle);
        config.plansCalcRoute().setAccessEgressType(AccessEgressType.none);
        Scenario scenario = createScenario(config);
        NetworkUtils.setLinkAccessTime(scenario.getNetwork().getLinks().get(Id.createLinkId(START_LINK)),TransportMode.car,75);
        NetworkUtils.setLinkAccessTime(scenario.getNetwork().getLinks().get(Id.createLinkId(END_LINK)),TransportMode.car,180);
        // add persons
        Person person = createPerson("slow-person", TransportMode.car, scenario.getPopulation().getFactory());
        scenario.getPopulation().addPerson(person);

        Controler controler = createControler(scenario);
        controler.run();
        var legs = TripStructureUtils.getLegs(person.getSelectedPlan());
        Assert.equals(1,legs.size());
        Assert.equals(TransportMode.car,legs.get(0).getMode());

    }

    /**
     * returns the mode of the first leg in a plan, which is not a walk (walk is now used as access/egress mode to network modes,
     * except if walk is routed on the network as network mode).
     */
    private static class SimpleMainModeIdentifier implements MainModeIdentifier {

        @Override
        public String identifyMainMode(List<? extends PlanElement> tripElements) {
            return tripElements.stream()
                    .filter(element -> element instanceof Leg)
                    .map(element -> (Leg) element)
                    .filter(leg -> !leg.getMode().equals(TransportMode.walk))
                    .map(Leg::getMode)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("could not find main mode"));
        }
    }
}
