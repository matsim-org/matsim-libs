/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2018.
 */

package ch.sbb.matsim.routing.pt.raptor;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.config.SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.events.ControlerEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.PtConstants;
import org.matsim.pt.router.TransitScheduleChangedEvent;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author mrieser / SBB
 */
public class SwissRailRaptorModuleTest {

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Before
    public void setUp() {
        System.setProperty("matsim.preferLocalDtds", "true");
    }

    @Test
    public void testInitialization() {
        Config config = ConfigUtils.createConfig();
        config.controler().setLastIteration(0);
        config.controler().setOutputDirectory(this.utils.getOutputDirectory());
        config.controler().setCreateGraphs(false);
        config.controler().setDumpDataAtEnd(false);
        config.transit().setUseTransit(true);
        Scenario scenario = ScenarioUtils.createScenario(config);
        Controler controler = new Controler(scenario);

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                install(new SwissRailRaptorModule());
            }
        });
        controler.run();

        TripRouter tripRouter = controler.getInjector().getInstance(TripRouter.class);

        // this test mostly checks that no exception occurred

        RoutingModule module = tripRouter.getRoutingModule(TransportMode.pt);
        Assert.assertTrue(module instanceof SwissRailRaptorRoutingModule);
    }

    @Test
    public void testIntermodalIntegration() {
        IntermodalFixture f = new IntermodalFixture();

        // add a single agent traveling with (intermodal) pt from A to B

        Population pop = f.scenario.getPopulation();
        PopulationFactory pf = pop.getFactory();
        Person p1 = pf.createPerson(Id.create(1, Person.class));
        pop.addPerson(p1);
        Plan plan = pf.createPlan();
        p1.addPlan(plan);
        Activity homeAct = pf.createActivityFromCoord("home", new Coord(10000, 10500));
        homeAct.setEndTime(7*3600);
        plan.addActivity(homeAct);
        plan.addLeg(pf.createLeg(TransportMode.pt));
        plan.addActivity(pf.createActivityFromCoord("work", new Coord(50000, 10500)));

        // prepare intermodal swissrailraptor

        f.srrConfig.setUseIntermodalAccessEgress(true);
        IntermodalAccessEgressParameterSet walkAccess = new IntermodalAccessEgressParameterSet();
        walkAccess.setMode(TransportMode.walk);
        walkAccess.setMaxRadius(1000);
        walkAccess.setInitialSearchRadius(1000);
        f.srrConfig.addIntermodalAccessEgress(walkAccess);
        IntermodalAccessEgressParameterSet bikeAccess = new IntermodalAccessEgressParameterSet();
        bikeAccess.setMode(TransportMode.bike);
        bikeAccess.setMaxRadius(1500);
        bikeAccess.setInitialSearchRadius(1500);
        bikeAccess.setStopFilterAttribute("bikeAccessible");
        bikeAccess.setLinkIdAttribute("accessLinkId_bike");
        bikeAccess.setStopFilterValue("true");
        f.srrConfig.addIntermodalAccessEgress(bikeAccess);

        // prepare scoring
        Config config = f.config;
        PlanCalcScoreConfigGroup.ActivityParams homeScoring = new PlanCalcScoreConfigGroup.ActivityParams("home");
        homeScoring.setTypicalDuration(16*3600);
        f.config.planCalcScore().addActivityParams(homeScoring);
        PlanCalcScoreConfigGroup.ActivityParams workScoring = new PlanCalcScoreConfigGroup.ActivityParams("work");
        workScoring.setTypicalDuration(8*3600);
        f.config.planCalcScore().addActivityParams(workScoring);

        PlanCalcScoreConfigGroup.ModeParams accessWalk = new PlanCalcScoreConfigGroup.ModeParams("non_network_walk");
        accessWalk.setMarginalUtilityOfTraveling(0.0);
        f.config.planCalcScore().addModeParams(accessWalk);
        PlanCalcScoreConfigGroup.ModeParams transitWalk = new PlanCalcScoreConfigGroup.ModeParams("transit_walk");
        transitWalk.setMarginalUtilityOfTraveling(0.0);
        f.config.planCalcScore().addModeParams(transitWalk);
        PlanCalcScoreConfigGroup.ModeParams egressWalk = new PlanCalcScoreConfigGroup.ModeParams("non_network_walk");
        egressWalk.setMarginalUtilityOfTraveling(0.0);
        f.config.planCalcScore().addModeParams(egressWalk);

        // prepare rest of config

        config.controler().setLastIteration(0);
        config.controler().setOutputDirectory(this.utils.getOutputDirectory());
        config.controler().setCreateGraphs(false);
        config.controler().setDumpDataAtEnd(false);
        config.qsim().setEndTime(10*3600);
        config.transit().setUseTransit(true);
        Controler controler = new Controler(f.scenario);

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                install(new SwissRailRaptorModule());
            }
        });

        // run a single iteration
        controler.run();

        // test that swiss rail raptor was used
        TripRouter tripRouter = controler.getInjector().getInstance(TripRouter.class);
        RoutingModule module = tripRouter.getRoutingModule(TransportMode.pt);
        Assert.assertTrue(module instanceof SwissRailRaptorRoutingModule);

        // also test that our one agent got correctly routed with intermodal access
        List<PlanElement> planElements = plan.getPlanElements();
        for (PlanElement pe : planElements) {
            System.out.println(pe);
        }

        Assert.assertEquals("wrong number of PlanElements.", 11, planElements.size());
        Assert.assertTrue(planElements.get(0) instanceof Activity);
        Assert.assertTrue(planElements.get(1) instanceof Leg);
        Assert.assertTrue(planElements.get(2) instanceof Activity);
        Assert.assertTrue(planElements.get(3) instanceof Leg);
        Assert.assertTrue(planElements.get(4) instanceof Activity);
        Assert.assertTrue(planElements.get(5) instanceof Leg);
        Assert.assertTrue(planElements.get(6) instanceof Activity);
        Assert.assertTrue(planElements.get(7) instanceof Leg);
        Assert.assertTrue(planElements.get(8) instanceof Activity);
        Assert.assertTrue(planElements.get(9) instanceof Leg);
        Assert.assertTrue(planElements.get(10) instanceof Activity);

        Assert.assertEquals("home", ((Activity) planElements.get(0)).getType());
        Assert.assertEquals(PtConstants.TRANSIT_ACTIVITY_TYPE, ((Activity) planElements.get(2)).getType());
        Assert.assertEquals(PtConstants.TRANSIT_ACTIVITY_TYPE, ((Activity) planElements.get(4)).getType());
        Assert.assertEquals(PtConstants.TRANSIT_ACTIVITY_TYPE, ((Activity) planElements.get(6)).getType());
        Assert.assertEquals(PtConstants.TRANSIT_ACTIVITY_TYPE, ((Activity) planElements.get(8)).getType());
        Assert.assertEquals("work", ((Activity) planElements.get(10)).getType());

        Assert.assertEquals(TransportMode.bike, ((Leg) planElements.get(1)).getMode());
        Assert.assertEquals(TransportMode.non_network_walk, ((Leg) planElements.get(3)).getMode());
        Assert.assertEquals(TransportMode.pt, ((Leg) planElements.get(5)).getMode());
        Assert.assertEquals(TransportMode.non_network_walk, ((Leg) planElements.get(7)).getMode());
        Assert.assertEquals(TransportMode.bike, ((Leg) planElements.get(9)).getMode());

        Assert.assertEquals(0.0, ((Activity) planElements.get(2)).getMaximumDuration(), 0.0);
        Assert.assertEquals(0.0, ((Activity) planElements.get(4)).getMaximumDuration(), 0.0);
        Assert.assertEquals(0.0, ((Activity) planElements.get(6)).getMaximumDuration(), 0.0);
        Assert.assertEquals(0.0, ((Activity) planElements.get(8)).getMaximumDuration(), 0.0);
    }
    
    /**
     * Test update of SwissRailRaptorData after TransitScheduleChangedEvent
     */
    @Test
    public void testTransitScheduleUpdate() {
        Fixture f = new Fixture();
        f.init();
        f.addVehicles();

        Population pop = f.scenario.getPopulation();
        PopulationFactory pf = pop.getFactory();
        Person p1 = pf.createPerson(Id.create(1, Person.class));
        pop.addPerson(p1);
        Plan plan = pf.createPlan();
        p1.addPlan(plan);
        Activity homeAct = pf.createActivityFromCoord("home", new Coord(12000, 5000));
        homeAct.setEndTime(7*3600);
        plan.addActivity(homeAct);
        plan.addLeg(pf.createLeg(TransportMode.pt));
        plan.addActivity(pf.createActivityFromCoord("work", new Coord(24010, 10000)));
        
        // prepare scoring
        Config config = f.config;
        PlanCalcScoreConfigGroup.ActivityParams homeScoring = new PlanCalcScoreConfigGroup.ActivityParams("home");
        homeScoring.setTypicalDuration(16*3600);
        f.config.planCalcScore().addActivityParams(homeScoring);
        PlanCalcScoreConfigGroup.ActivityParams workScoring = new PlanCalcScoreConfigGroup.ActivityParams("work");
        workScoring.setTypicalDuration(8*3600);
        f.config.planCalcScore().addActivityParams(workScoring);

        PlanCalcScoreConfigGroup.ModeParams accessWalk = new PlanCalcScoreConfigGroup.ModeParams("non_network_walk");
        accessWalk.setMarginalUtilityOfTraveling(0.0);
        f.config.planCalcScore().addModeParams(accessWalk);
        PlanCalcScoreConfigGroup.ModeParams transitWalk = new PlanCalcScoreConfigGroup.ModeParams("transit_walk");
        transitWalk.setMarginalUtilityOfTraveling(0.0);
        f.config.planCalcScore().addModeParams(transitWalk);
        PlanCalcScoreConfigGroup.ModeParams egressWalk = new PlanCalcScoreConfigGroup.ModeParams("non_network_walk");
        egressWalk.setMarginalUtilityOfTraveling(0.0);
        f.config.planCalcScore().addModeParams(egressWalk);

        StrategySettings reRoute = new StrategySettings();
        reRoute.setStrategyName("ReRoute");
        reRoute.setWeight(1.0);
        config.strategy().addStrategySettings(reRoute);
        config.strategy().setMaxAgentPlanMemorySize(1);

        // prepare rest of config
        config.controler().setLastIteration(1);
        config.controler().setOutputDirectory(this.utils.getOutputDirectory());
        config.controler().setCreateGraphs(false);
        config.controler().setDumpDataAtEnd(false);
        config.qsim().setEndTime(10*3600);
        config.transit().setUseTransit(true);
        Controler controler = new Controler(f.scenario);

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                install(new SwissRailRaptorModule());
                addControlerListenerBinding().to(ScheduleModifierControlerListener.class);
            }
        });

        controler.run();
        
        // test that swiss rail raptor was used
        TripRouter tripRouter = controler.getInjector().getInstance(TripRouter.class);
        RoutingModule module = tripRouter.getRoutingModule(TransportMode.pt);
        Assert.assertTrue(module instanceof SwissRailRaptorRoutingModule);
        
        // Check routed plan
        List<PlanElement> planElements = p1.getSelectedPlan().getPlanElements();
        for (PlanElement pe : planElements) {
            System.out.println(pe);
        }

        Assert.assertEquals("wrong number of PlanElements.", 7, planElements.size());
        Assert.assertTrue(planElements.get(0) instanceof Activity);
        Assert.assertTrue(planElements.get(1) instanceof Leg);
        Assert.assertTrue(planElements.get(2) instanceof Activity);
        Assert.assertTrue(planElements.get(3) instanceof Leg);
        Assert.assertTrue(planElements.get(4) instanceof Activity);
        Assert.assertTrue(planElements.get(5) instanceof Leg);
        Assert.assertTrue(planElements.get(6) instanceof Activity);

        Assert.assertEquals("home", ((Activity) planElements.get(0)).getType());
        Assert.assertEquals(PtConstants.TRANSIT_ACTIVITY_TYPE, ((Activity) planElements.get(2)).getType());
        Assert.assertEquals(PtConstants.TRANSIT_ACTIVITY_TYPE, ((Activity) planElements.get(4)).getType());
        Assert.assertEquals("work", ((Activity) planElements.get(6)).getType());

        Assert.assertEquals(TransportMode.non_network_walk, ((Leg) planElements.get(1)).getMode());
        Assert.assertEquals(TransportMode.pt, ((Leg) planElements.get(3)).getMode());
        Assert.assertEquals(TransportMode.non_network_walk, ((Leg) planElements.get(5)).getMode());
        
        // Check route: should return one of the added lines although the removed green line would be faster
        Leg ptLeg = (Leg) planElements.get(3);
        ExperimentalTransitRoute ptRoute = (ExperimentalTransitRoute) ptLeg.getRoute();
        Assert.assertEquals(Id.create("AddedLine" + 1, TransitLine.class), ptRoute.getLineId());        
    }
    
    /**
     * Test individual scoring parameters for agents
     */
    @Test
    public void testRaptorParametersForPerson() {
        SwissRailRaptorConfigGroup srrConfig = new SwissRailRaptorConfigGroup();
        srrConfig.setScoringParameters(ch.sbb.matsim.config.SwissRailRaptorConfigGroup.ScoringParameters.Individual);

        Config config = ConfigUtils.createConfig(srrConfig);
        config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
        config.controler().setLastIteration(0);
        config.controler().setOutputDirectory(this.utils.getOutputDirectory());
        config.transit().setUseTransit(true);

        config.planCalcScore().getOrCreateScoringParameters("default").setPerforming_utils_hr(3600.0 * 50.0);
        config.planCalcScore().getOrCreateScoringParameters("sub").setPerforming_utils_hr(3600.0 * 50.0);

        for (String mode : Arrays.asList("car", "walk", "pt")) {
            config.planCalcScore().getOrCreateScoringParameters("default").getOrCreateModeParams(mode);
            config.planCalcScore().getOrCreateScoringParameters("sub").getOrCreateModeParams(mode);
        }

        config.planCalcScore().getOrCreateScoringParameters("default").setMarginalUtlOfWaitingPt_utils_hr(-3600.0 * 30.0);
        config.planCalcScore().getOrCreateScoringParameters("sub").setMarginalUtlOfWaitingPt_utils_hr(-3600.0 * 10.0);

        Scenario scenario = ScenarioUtils.createScenario(config);

        Person personA = scenario.getPopulation().getFactory().createPerson(Id.createPersonId("A"));
        PopulationUtils.putPersonAttribute( personA, "subpopulation", "default" );

        Person personB = scenario.getPopulation().getFactory().createPerson(Id.createPersonId("B"));
        PopulationUtils.putPersonAttribute( personB, "subpopulation" , "sub" );

        Controler controller = new Controler(scenario);
        controller.addOverridingModule(new SwissRailRaptorModule());
        controller.run();

        RaptorParametersForPerson parameters = controller.getInjector().getInstance(RaptorParametersForPerson.class);

        Assert.assertEquals(-80.0, parameters.getRaptorParameters(personA).getMarginalUtilityOfWaitingPt_utl_s(), 1e-3);
        Assert.assertEquals(-60.0, parameters.getRaptorParameters(personB).getMarginalUtilityOfWaitingPt_utl_s(), 1e-3);
    }

    private static class ScheduleModifierControlerListener implements StartupListener, IterationStartsListener {

        @Override
        public void notifyIterationStarts(IterationStartsEvent event) {
            int iteration = event.getIteration();
            addLineAndStop(event, iteration);
            event.getServices().getEvents().processEvent(new TransitScheduleChangedEvent(0.0));
        }

        @Override
        public void notifyStartup(StartupEvent event) {
            removeGreenLineAndStop(event);
            event.getServices().getEvents().processEvent(new TransitScheduleChangedEvent(0.0));
        }

        private void addLineAndStop(ControlerEvent event, int iteration) {
            TransitSchedule schedule = event.getServices().getScenario().getTransitSchedule();
            TransitScheduleFactory scheduleFactory = schedule.getFactory();
            Vehicles transitVehicles = event.getServices().getScenario().getTransitVehicles();
            VehiclesFactory vf = event.getServices().getScenario().getTransitVehicles().getFactory();

            // Add a stop near the stop to be removed from the green line and add a line serving that stop
            TransitStopFacility addedStop = scheduleFactory.createTransitStopFacility(
                Id.create("AddedStop" + iteration, TransitStopFacility.class),
                CoordUtils.createCoord(24000 + iteration * 10, 10000), false);
            addedStop.setLinkId(Id.create( "3", Link.class));
            TransitLine addedLine = scheduleFactory.createTransitLine(Id.create(
                "AddedLine" + iteration, TransitLine.class));

            // Set some arbitrary NetworkRoute to obtain a valid TransitRoute
            NetworkRoute netRoute = RouteUtils.createLinkNetworkRouteImpl(Id.create(
                "15", Link.class), Id.create( "3", Link.class));
            List<TransitRouteStop> stops = new ArrayList<>();
            // Same travel time in vehicle as green line from stop 18 to stop 20
            stops.add(scheduleFactory.createTransitRouteStop(schedule.getFacilities().get(Id.create(
                "5", TransitStopFacility.class)), 0.0, 60.0));
            stops.add(scheduleFactory.createTransitRouteStop(addedStop, 20*60.0, 21*60));
            TransitRoute addedRoute = scheduleFactory.createTransitRoute(Id.create(
                "AddedRoute" + iteration, TransitRoute.class), netRoute, stops, TransportMode.pt);

            // new line departs 5 mins after green line in Fixture -> router would return green line if it was not deleted
            Departure addedDep = scheduleFactory.createDeparture(Id.create(
                "AddedDeparture" + iteration, Departure.class), 7*60*60 + 5*60);
            Id<Vehicle> vehicleId = Id.createVehicleId("AddedVehicle" + iteration);
                transitVehicles.addVehicle(vf.createVehicle(vehicleId, transitVehicles.getVehicleTypes().get(Id.create("train", VehicleType.class))));
            addedDep.setVehicleId(vehicleId);

            addedRoute.addDeparture(addedDep);
            addedLine.addRoute(addedRoute);

            schedule.addStopFacility(addedStop);
            schedule.addTransitLine(addedLine);
        }

        private void removeGreenLineAndStop(ControlerEvent event) {
            TransitSchedule schedule = event.getServices().getScenario().getTransitSchedule();

            // Remove a line and a stop only served by that line
            TransitLine greenLine = schedule.getTransitLines().get(Id.create("green", TransitLine.class));
            schedule.removeTransitLine(greenLine);
            schedule.removeStopFacility(schedule.getFacilities().get(Id.create("13", TransitStopFacility.class)));
        }

    }
    
}
