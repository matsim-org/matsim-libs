package org.matsim.contrib.drt.extension.operations.shifts.run;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.common.zones.systems.grid.square.SquareGridZoneSystemParams;
import org.matsim.contrib.drt.analysis.zonal.DrtZoneSystemParams;
import org.matsim.contrib.drt.extension.DrtWithExtensionsConfigGroup;
import org.matsim.contrib.drt.extension.operations.DrtOperationsControlerCreator;
import org.matsim.contrib.drt.extension.operations.DrtOperationsParams;
import org.matsim.contrib.drt.extension.operations.operationFacilities.*;
import org.matsim.contrib.drt.extension.operations.shifts.config.ShiftsParams;
import org.matsim.contrib.drt.extension.operations.shifts.dispatcher.DefaultShiftScheduler;
import org.matsim.contrib.drt.extension.operations.shifts.dispatcher.ShiftScheduler;
import org.matsim.contrib.drt.extension.operations.shifts.shift.*;
import org.matsim.contrib.drt.optimizer.constraints.DefaultDrtOptimizationConstraintsSet;
import org.matsim.contrib.drt.optimizer.insertion.extensive.ExtensiveInsertionSearchParams;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingParams;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingStrategyParams;
import org.matsim.contrib.drt.prebooking.PrebookingModeQSimModule;
import org.matsim.contrib.drt.prebooking.PrebookingParams;
import org.matsim.contrib.drt.prebooking.logic.AttributeBasedPrebookingLogic;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.fleet.FleetSpecificationImpl;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerRequestSubmittedEventHandler;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.zone.skims.DvrpTravelTimeMatrixParams;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.geometry.CoordUtils;

import java.util.HashSet;
import java.util.Set;



/**
 * @author nkuehnel / MOIA
 */
public class RunPrebookingShiftDrtScenarioIT {


    @Test
    void testWithReattempts() {

        MultiModeDrtConfigGroup multiModeDrtConfigGroup = new MultiModeDrtConfigGroup(DrtWithExtensionsConfigGroup::new);
        DrtWithExtensionsConfigGroup drtWithShiftsConfigGroup = (DrtWithExtensionsConfigGroup) multiModeDrtConfigGroup.createParameterSet("drt");
        final Controler run = prepare(drtWithShiftsConfigGroup, multiModeDrtConfigGroup, "_testWithReattempts");

        Multiset<Id<Person>> submittedPersons = HashMultiset.create();
        Multiset<Id<Person>> scheduledPersons = HashMultiset.create();
        Multiset<Id<Person>> rejectedPersons = HashMultiset.create();

        run.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addEventHandlerBinding().toInstance((PassengerRequestSubmittedEventHandler) event -> submittedPersons.addAll(event.getPersonIds()));
                addEventHandlerBinding().toInstance((PassengerRequestScheduledEventHandler) event -> scheduledPersons.addAll(event.getPersonIds()));
                addEventHandlerBinding().toInstance((PassengerRequestRejectedEventHandler) event -> rejectedPersons.addAll(event.getPersonIds()));
            }
        });

        PrebookingParams prebookingParams = new PrebookingParams();
        prebookingParams.maximumPassengerDelay = 600;
        prebookingParams.unschedulingMode = PrebookingParams.UnschedulingMode.Routing;
        prebookingParams.scheduleWaitBeforeDrive = true;
        prebookingParams.abortRejectedPrebookings = false;
        drtWithShiftsConfigGroup.addParameterSet(prebookingParams);

        run.addOverridingQSimModule(new PrebookingModeQSimModule(drtWithShiftsConfigGroup.getMode(),
                prebookingParams));

        run.run();

        Assertions.assertEquals(1, submittedPersons.count(Id.createPersonId(1)));
        Assertions.assertEquals(2, submittedPersons.count(Id.createPersonId(2)));
        Assertions.assertEquals(1, submittedPersons.count(Id.createPersonId(3)));
        Assertions.assertEquals(2, submittedPersons.count(Id.createPersonId(4)));
        Assertions.assertEquals(1, submittedPersons.count(Id.createPersonId(5)));
        Assertions.assertEquals(2, submittedPersons.count(Id.createPersonId(6)));

        Assertions.assertEquals(1, scheduledPersons.count(Id.createPersonId(1)));
        Assertions.assertEquals(1, scheduledPersons.count(Id.createPersonId(2)));
        Assertions.assertEquals(1, scheduledPersons.count(Id.createPersonId(3)));
        Assertions.assertEquals(1, scheduledPersons.count(Id.createPersonId(4)));
        Assertions.assertEquals(1, scheduledPersons.count(Id.createPersonId(5)));
        Assertions.assertEquals(0, scheduledPersons.count(Id.createPersonId(6)));

        Assertions.assertEquals(0, rejectedPersons.count(Id.createPersonId(1)));
        Assertions.assertEquals(1, rejectedPersons.count(Id.createPersonId(2)));
        Assertions.assertEquals(0, rejectedPersons.count(Id.createPersonId(3)));
        Assertions.assertEquals(1, rejectedPersons.count(Id.createPersonId(4)));
        Assertions.assertEquals(0, rejectedPersons.count(Id.createPersonId(5)));
        Assertions.assertEquals(2, rejectedPersons.count(Id.createPersonId(6)));
    }

    @Test
    void testWithoutReattempts() {

        MultiModeDrtConfigGroup multiModeDrtConfigGroup = new MultiModeDrtConfigGroup(DrtWithExtensionsConfigGroup::new);
        DrtWithExtensionsConfigGroup drtWithShiftsConfigGroup = (DrtWithExtensionsConfigGroup) multiModeDrtConfigGroup.createParameterSet("drt");
        final Controler run = prepare(drtWithShiftsConfigGroup, multiModeDrtConfigGroup, "_testWithoutReattempts");

        Multiset<Id<Person>> submittedPersons = HashMultiset.create();
        Multiset<Id<Person>> scheduledPersons = HashMultiset.create();
        Multiset<Id<Person>> rejectedPersons = HashMultiset.create();

        run.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addEventHandlerBinding().toInstance((PassengerRequestSubmittedEventHandler) event -> submittedPersons.addAll(event.getPersonIds()));
                addEventHandlerBinding().toInstance((PassengerRequestScheduledEventHandler) event -> scheduledPersons.addAll(event.getPersonIds()));
                addEventHandlerBinding().toInstance((PassengerRequestRejectedEventHandler) event -> rejectedPersons.addAll(event.getPersonIds()));
            }
        });

        PrebookingParams prebookingParams = new PrebookingParams();
        prebookingParams.maximumPassengerDelay = 600;
        prebookingParams.unschedulingMode = PrebookingParams.UnschedulingMode.Routing;
        prebookingParams.scheduleWaitBeforeDrive = true;
        prebookingParams.abortRejectedPrebookings = true;
        drtWithShiftsConfigGroup.addParameterSet(prebookingParams);

        run.addOverridingQSimModule(new PrebookingModeQSimModule(drtWithShiftsConfigGroup.getMode(),
                prebookingParams));

        run.run();

        Assertions.assertEquals(1, submittedPersons.count(Id.createPersonId(1)));
        Assertions.assertEquals(1, submittedPersons.count(Id.createPersonId(2)));
        Assertions.assertEquals(1, submittedPersons.count(Id.createPersonId(3)));
        Assertions.assertEquals(1, submittedPersons.count(Id.createPersonId(4)));
        Assertions.assertEquals(1, submittedPersons.count(Id.createPersonId(5)));
        Assertions.assertEquals(1, submittedPersons.count(Id.createPersonId(6)));

        Assertions.assertEquals(1, scheduledPersons.count(Id.createPersonId(1)));
        Assertions.assertEquals(0, scheduledPersons.count(Id.createPersonId(2)));
        Assertions.assertEquals(1, scheduledPersons.count(Id.createPersonId(3)));
        Assertions.assertEquals(0, scheduledPersons.count(Id.createPersonId(4)));
        Assertions.assertEquals(1, scheduledPersons.count(Id.createPersonId(5)));
        Assertions.assertEquals(0, scheduledPersons.count(Id.createPersonId(6)));

        Assertions.assertEquals(0, rejectedPersons.count(Id.createPersonId(1)));
        Assertions.assertEquals(1, rejectedPersons.count(Id.createPersonId(2)));
        Assertions.assertEquals(0, rejectedPersons.count(Id.createPersonId(3)));
        Assertions.assertEquals(1, rejectedPersons.count(Id.createPersonId(4)));
        Assertions.assertEquals(0, rejectedPersons.count(Id.createPersonId(5)));
        Assertions.assertEquals(1, rejectedPersons.count(Id.createPersonId(6)));
    }

    @NotNull
    private Controler prepare(DrtWithExtensionsConfigGroup drtWithShiftsConfigGroup, MultiModeDrtConfigGroup multiModeDrtConfigGroup, String outputSuffix) {
        drtWithShiftsConfigGroup.mode = TransportMode.drt;
        DefaultDrtOptimizationConstraintsSet defaultConstraintsSet =
                (DefaultDrtOptimizationConstraintsSet) drtWithShiftsConfigGroup.addOrGetDrtOptimizationConstraintsParams()
                        .addOrGetDefaultDrtOptimizationConstraintsSet();
        drtWithShiftsConfigGroup.stopDuration = 30.;
        defaultConstraintsSet.maxTravelTimeAlpha = 1.5;
        defaultConstraintsSet.maxTravelTimeBeta = 10. * 60.;
        defaultConstraintsSet.maxWaitTime = 600.;
        defaultConstraintsSet.rejectRequestIfMaxWaitOrTravelTimeViolated = true;
        defaultConstraintsSet.maxWalkDistance = 1000.;
        drtWithShiftsConfigGroup.operationalScheme = DrtConfigGroup.OperationalScheme.door2door;

        drtWithShiftsConfigGroup.addParameterSet(new ExtensiveInsertionSearchParams());

        ConfigGroup rebalancing = drtWithShiftsConfigGroup.createParameterSet("rebalancing");
        drtWithShiftsConfigGroup.addParameterSet(rebalancing);
        ((RebalancingParams) rebalancing).interval = 600;

        MinCostFlowRebalancingStrategyParams strategyParams = new MinCostFlowRebalancingStrategyParams();
        strategyParams.targetAlpha = 0.3;
        strategyParams.targetBeta = 0.3;

        drtWithShiftsConfigGroup.getRebalancingParams().get().addParameterSet(strategyParams);

        SquareGridZoneSystemParams zoneParams = new SquareGridZoneSystemParams();
        zoneParams.cellSize = 500.;

        DrtZoneSystemParams drtZoneSystemParams = new DrtZoneSystemParams();
        drtZoneSystemParams.addParameterSet(zoneParams);
        drtWithShiftsConfigGroup.addParameterSet(drtZoneSystemParams);

        DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
        DvrpTravelTimeMatrixParams matrixParams = dvrpConfigGroup.getTravelTimeMatrixParams();
        matrixParams.addParameterSet(zoneParams);

        multiModeDrtConfigGroup.addParameterSet(drtWithShiftsConfigGroup);

        final Config config = ConfigUtils.createConfig(multiModeDrtConfigGroup,
                dvrpConfigGroup);

        Set<String> modes = new HashSet<>();
        modes.add("drt");
        config.travelTimeCalculator().setAnalyzedModes(modes);

        ScoringConfigGroup.ModeParams scoreParams = new ScoringConfigGroup.ModeParams("drt");
        config.scoring().addModeParams(scoreParams);
        ScoringConfigGroup.ModeParams scoreParams2 = new ScoringConfigGroup.ModeParams("walk");
        config.scoring().addModeParams(scoreParams2);

        final ScoringConfigGroup.ActivityParams start = new ScoringConfigGroup.ActivityParams("start");
        start.setScoringThisActivityAtAll(false);
        final ScoringConfigGroup.ActivityParams end = new ScoringConfigGroup.ActivityParams("end");
        end.setScoringThisActivityAtAll(false);

        config.scoring().addActivityParams(start);
        config.scoring().addActivityParams(end);

        config.qsim().setSimStarttimeInterpretation(QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime);
        config.qsim().setSimEndtimeInterpretation(QSimConfigGroup.EndtimeInterpretation.minOfEndtimeAndMobsimFinished);

        final ReplanningConfigGroup.StrategySettings stratSets = new ReplanningConfigGroup.StrategySettings();
        stratSets.setWeight(1);
        stratSets.setStrategyName("ChangeExpBeta");
        config.replanning().addStrategySettings(stratSets);

        config.controller().setLastIteration(0);
        config.controller().setWriteEventsInterval(1);

        config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        config.controller().setOutputDirectory("test/output/prebooking_shifts"+outputSuffix);

        DrtOperationsParams operationsParams = (DrtOperationsParams) drtWithShiftsConfigGroup.createParameterSet(DrtOperationsParams.SET_NAME);
        ShiftsParams shiftsParams = (ShiftsParams) operationsParams.createParameterSet(ShiftsParams.SET_NAME);
        OperationFacilitiesParams operationFacilitiesParams = (OperationFacilitiesParams) operationsParams.createParameterSet(OperationFacilitiesParams.SET_NAME);
        operationsParams.addParameterSet(shiftsParams);
        operationsParams.addParameterSet(operationFacilitiesParams);

        shiftsParams.considerUpcomingShiftsForInsertion = true;
        shiftsParams.shiftEndLookAhead = 900.;
        drtWithShiftsConfigGroup.addParameterSet(operationsParams);


        Scenario scenario = DrtControlerCreator.createScenarioWithDrtRouteFactory(config);
        prepareNetwork(scenario);
        preparePopulation(scenario);

        final Controler run = DrtOperationsControlerCreator.createControler(config, scenario, false);
        prepareOperations(run, drtWithShiftsConfigGroup);


        AttributeBasedPrebookingLogic.install(run, drtWithShiftsConfigGroup);
        return run;
    }

    private void preparePopulation(Scenario scenario) {
        Population population = scenario.getPopulation();
        PopulationFactory factory = PopulationUtils.getFactory();

        //person 1 - prebooking submitted once shift is assigned (but not started) for time when shift should be active - ok
        {
            Person person = factory.createPerson(Id.createPersonId(1));
            Plan plan = factory.createPlan();
            Activity start = factory.createActivityFromLinkId("start", Id.createLinkId(1));
            start.setEndTime(5000);
            AttributeBasedPrebookingLogic.setSubmissionTime("drt", start, 1800.0);
            AttributeBasedPrebookingLogic.setPlannedDepartureTime("drt", start, 5000.0);
            plan.addActivity(start);
            plan.addLeg(factory.createLeg("drt"));
            plan.addActivity(factory.createActivityFromLinkId("end", Id.createLinkId(2)));
            person.addPlan(plan);
            population.addPerson(person);
        }

        //person 2 - prebooking submitted before shift is assigned for time when shift should be active - rejected
        {
            Person person = factory.createPerson(Id.createPersonId(2));
            Plan plan = factory.createPlan();
            Activity start = factory.createActivityFromLinkId("start", Id.createLinkId(1));
            start.setEndTime(5000);
            AttributeBasedPrebookingLogic.setSubmissionTime("drt", start, 900.0);
            AttributeBasedPrebookingLogic.setPlannedDepartureTime("drt", start, 5005.0);
            plan.addActivity(start);
            plan.addLeg(factory.createLeg("drt"));
            plan.addActivity(factory.createActivityFromLinkId("end", Id.createLinkId(2)));
            person.addPlan(plan);
            population.addPerson(person);
        }

        //person 3 - prebooking submitted during shift for time when shift should be active - ok
        {
            Person person = factory.createPerson(Id.createPersonId(3));
            Plan plan = factory.createPlan();
            Activity start = factory.createActivityFromLinkId("start", Id.createLinkId(1));
            start.setEndTime(5000);
            AttributeBasedPrebookingLogic.setSubmissionTime("drt", start, 4000.0);
            AttributeBasedPrebookingLogic.setPlannedDepartureTime("drt", start, 5000.0);
            plan.addActivity(start);
            plan.addLeg(factory.createLeg("drt"));
            plan.addActivity(factory.createActivityFromLinkId("end", Id.createLinkId(2)));
            person.addPlan(plan);
            population.addPerson(person);
        }

        //person 4 - prebooking submitted during shift for time when shift should be ended - rejected
        {
            Person person = factory.createPerson(Id.createPersonId(4));
            Plan plan = factory.createPlan();
            Activity start = factory.createActivityFromLinkId("start", Id.createLinkId(1));
            start.setEndTime(8000);
            AttributeBasedPrebookingLogic.setSubmissionTime("drt", start, 4000.0);
            AttributeBasedPrebookingLogic.setPlannedDepartureTime("drt", start, 11000.0);
            plan.addActivity(start);
            plan.addLeg(factory.createLeg("drt"));
            plan.addActivity(factory.createActivityFromLinkId("end", Id.createLinkId(2)));
            person.addPlan(plan);
            population.addPerson(person);
        }

        //person 5 - prebooking submitted during shift for time which falls into break beginning of break corridor with enough remaining time - ok
        {
            Person person = factory.createPerson(Id.createPersonId(5));
            Plan plan = factory.createPlan();
            Activity start = factory.createActivityFromLinkId("start", Id.createLinkId(1));
            start.setEndTime(6000.);
            AttributeBasedPrebookingLogic.setSubmissionTime("drt", start, 4000.0);
            AttributeBasedPrebookingLogic.setPlannedDepartureTime("drt", start, 6000.0);
            plan.addActivity(start);
            plan.addLeg(factory.createLeg("drt"));
            plan.addActivity(factory.createActivityFromLinkId("end", Id.createLinkId(2)));
            person.addPlan(plan);
            population.addPerson(person);
        }

        //person 6 - prebooking submitted during shift for time which would preclude meaningful break - rejected
        {
            Person person = factory.createPerson(Id.createPersonId(6));
            Plan plan = factory.createPlan();
            Activity start = factory.createActivityFromLinkId("start", Id.createLinkId(1));
            start.setEndTime(6500.);
            AttributeBasedPrebookingLogic.setSubmissionTime("drt", start, 4000.0);
            AttributeBasedPrebookingLogic.setPlannedDepartureTime("drt", start, 6500.0);
            plan.addActivity(start);
            plan.addLeg(factory.createLeg("drt"));
            plan.addActivity(factory.createActivityFromLinkId("end", Id.createLinkId(2)));
            person.addPlan(plan);
            population.addPerson(person);
        }
    }

    private static void prepareOperations(Controler run, DrtWithExtensionsConfigGroup drtWithShiftsConfigGroup) {
        FleetSpecification fleetSpecification = new FleetSpecificationImpl();
        fleetSpecification.addVehicleSpecification(ImmutableDvrpVehicleSpecification.newBuilder() //
                .id(Id.create("v1", DvrpVehicle.class)) //
                .capacity(1) //
                .serviceBeginTime(0.0) //
                .serviceEndTime(24 * 3600) //
                .startLinkId(Id.createLinkId(1)) //
                .build());

        OperationFacilitiesSpecification opFasSpecification = new OperationFacilitiesSpecificationImpl();
        opFasSpecification.addOperationFacilitySpecification(OperationFacilitySpecificationImpl
                .newBuilder()
                .capacity(1)
                .type(OperationFacilityType.hub)
                .coord(new Coord(1000, 1000))
                .linkId(Id.createLinkId(1))
                .id(Id.create(1, OperationFacility.class))
                .build());

        DrtShiftsSpecification shiftsSpecification = new DrtShiftsSpecificationImpl();
        shiftsSpecification.addShiftSpecification(DrtShiftSpecificationImpl.newBuilder()
                .start(3600)
                .end(10800)
                .id(Id.create(1, DrtShift.class))
                .shiftBreak(DrtShiftBreakSpecificationImpl.newBuilder().duration(600.).earliestStart(6000.).latestEnd(7000.).build())
                .operationFacility(Id.create(1, OperationFacility.class))
                .build()
        );
        run.addOverridingModule(new AbstractDvrpModeModule(drtWithShiftsConfigGroup.getMode()) {
            @Override
            public void install() {
                bindModal(FleetSpecification.class).toInstance(fleetSpecification);
                bindModal(OperationFacilitiesSpecification.class).toInstance(opFasSpecification);
                bindModal(ShiftScheduler.class).toProvider(modalProvider(getter -> new DefaultShiftScheduler(shiftsSpecification)));
            }
        });

    }

    private void prepareNetwork(Scenario scenario) {
        Network network = scenario.getNetwork();
        Node node1 = NetworkUtils.createAndAddNode(network, Id.createNodeId(1), new Coord(0, 0));
        Node node2 = NetworkUtils.createAndAddNode(network, Id.createNodeId(2), new Coord(1000, 1000));

        NetworkUtils.createAndAddLink(network, Id.createLinkId(1), node1, node2, CoordUtils.length(node2.getCoord()), 50 / 3.6, 100, 1, null, null);
        NetworkUtils.createAndAddLink(network, Id.createLinkId(2), node2, node1, CoordUtils.length(node2.getCoord()), 50 / 3.6, 100, 1, null, null);
    }

}
