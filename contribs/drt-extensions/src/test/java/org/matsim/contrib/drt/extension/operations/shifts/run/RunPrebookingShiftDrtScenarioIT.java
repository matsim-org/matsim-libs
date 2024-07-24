package org.matsim.contrib.drt.extension.operations.shifts.run;

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
    void test() {

        MultiModeDrtConfigGroup multiModeDrtConfigGroup = new MultiModeDrtConfigGroup(DrtWithExtensionsConfigGroup::new);

        DrtWithExtensionsConfigGroup drtWithShiftsConfigGroup = (DrtWithExtensionsConfigGroup) multiModeDrtConfigGroup.createParameterSet("drt");
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
        config.controller().setOutputDirectory("test/output/prebooking_shifts");

        DrtOperationsParams operationsParams = (DrtOperationsParams) drtWithShiftsConfigGroup.createParameterSet(DrtOperationsParams.SET_NAME);
        ShiftsParams shiftsParams = (ShiftsParams) operationsParams.createParameterSet(ShiftsParams.SET_NAME);
        OperationFacilitiesParams operationFacilitiesParams = (OperationFacilitiesParams) operationsParams.createParameterSet(OperationFacilitiesParams.SET_NAME);
        operationsParams.addParameterSet(shiftsParams);
        operationsParams.addParameterSet(operationFacilitiesParams);

        shiftsParams.considerUpcomingShiftsForInsertion = true;
        shiftsParams.shiftEndLookAhead = 900.;
        drtWithShiftsConfigGroup.addParameterSet(operationsParams);

        PrebookingParams prebookingParams = new PrebookingParams();
        prebookingParams.maximumPassengerDelay = 600;
        prebookingParams.unschedulingMode = PrebookingParams.UnschedulingMode.Routing;
        prebookingParams.scheduleWaitBeforeDrive = true;
        drtWithShiftsConfigGroup.addParameterSet(prebookingParams);

        Scenario scenario = DrtControlerCreator.createScenarioWithDrtRouteFactory(config);
        prepareNetwork(scenario);
        preparePopulation(scenario);

        final Controler run = DrtOperationsControlerCreator.createControler(config, scenario, false);
        prepareOperations(run, drtWithShiftsConfigGroup);

        run.addOverridingQSimModule(new PrebookingModeQSimModule(drtWithShiftsConfigGroup.getMode(),
                prebookingParams));
        AttributeBasedPrebookingLogic.install(run, drtWithShiftsConfigGroup);

        Set<Id<Person>> rejectedPersons = new HashSet<>();
        run.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addEventHandlerBinding().toInstance((PassengerRequestRejectedEventHandler) event -> rejectedPersons.addAll(event.getPersonIds()));
            }
        });

        run.run();


        Assertions.assertFalse(rejectedPersons.contains(Id.createPersonId(1)));
        Assertions.assertTrue(rejectedPersons.contains(Id.createPersonId(2)));
        Assertions.assertFalse(rejectedPersons.contains(Id.createPersonId(3)));
        Assertions.assertTrue(rejectedPersons.contains(Id.createPersonId(4)));
        Assertions.assertFalse(rejectedPersons.contains(Id.createPersonId(5)));
        Assertions.assertTrue(rejectedPersons.contains(Id.createPersonId(6)));
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
            start.getAttributes().putAttribute("prebooking:submissionTime" + "drt", 1800.);
            start.getAttributes().putAttribute("prebooking:plannedDepartureTime" + "drt", 5000.);
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
            start.getAttributes().putAttribute("prebooking:submissionTime" + "drt", 900.);
            start.getAttributes().putAttribute("prebooking:plannedDepartureTime" + "drt", 5000.);
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
            start.getAttributes().putAttribute("prebooking:submissionTime" + "drt", 4000.);
            start.getAttributes().putAttribute("prebooking:plannedDepartureTime" + "drt", 5000.);
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
            start.getAttributes().putAttribute("prebooking:submissionTime" + "drt", 4000.);
            start.getAttributes().putAttribute("prebooking:plannedDepartureTime" + "drt", 11000.);
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
            start.getAttributes().putAttribute("prebooking:submissionTime" + "drt", 4000.);
            start.getAttributes().putAttribute("prebooking:plannedDepartureTime" + "drt", 6000.);
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
            start.getAttributes().putAttribute("prebooking:submissionTime" + "drt", 4000.);
            start.getAttributes().putAttribute("prebooking:plannedDepartureTime" + "drt", 6500.);
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
                bindModal(DrtShiftsSpecification.class).toInstance(shiftsSpecification);
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
