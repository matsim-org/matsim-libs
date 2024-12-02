package org.matsim.contrib.drt.extension.operations.shifts.run;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.common.zones.systems.grid.square.SquareGridZoneSystemParams;
import org.matsim.contrib.drt.analysis.zonal.DrtZoneSystemParams;
import org.matsim.contrib.drt.extension.DrtWithExtensionsConfigGroup;
import org.matsim.contrib.drt.extension.operations.DrtOperationsControlerCreator;
import org.matsim.contrib.drt.extension.operations.DrtOperationsParams;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilitiesParams;
import org.matsim.contrib.drt.extension.operations.shifts.config.ShiftsParams;
import org.matsim.contrib.drt.optimizer.constraints.DefaultDrtOptimizationConstraintsSet;
import org.matsim.contrib.drt.fare.DrtFareParams;
import org.matsim.contrib.drt.optimizer.insertion.extensive.ExtensiveInsertionSearchParams;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingParams;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingStrategyParams;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.zone.skims.DvrpTravelTimeMatrixParams;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.examples.ExamplesUtils;

import java.util.HashSet;
import java.util.Set;

public class RunShiftDrtScenarioIT {

    @Test
    void test() {

        MultiModeDrtConfigGroup multiModeDrtConfigGroup = new MultiModeDrtConfigGroup(DrtWithExtensionsConfigGroup::new);

        String fleetFile = "holzkirchenFleet.xml";
        String plansFile = "holzkirchenPlans.xml.gz";
        String networkFile = "holzkirchenNetwork.xml.gz";
        String opFacilitiesFile = "holzkirchenOperationFacilities.xml";
        String shiftsFile = "holzkirchenShifts.xml";

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
        drtWithShiftsConfigGroup.useModeFilteredSubnetwork = false;
        drtWithShiftsConfigGroup.vehiclesFile = fleetFile;
        drtWithShiftsConfigGroup.operationalScheme = DrtConfigGroup.OperationalScheme.door2door;
        drtWithShiftsConfigGroup.plotDetailedCustomerStats = true;
        drtWithShiftsConfigGroup.idleVehiclesReturnToDepots = false;

        drtWithShiftsConfigGroup.addParameterSet(new ExtensiveInsertionSearchParams());

        ConfigGroup rebalancing = drtWithShiftsConfigGroup.createParameterSet("rebalancing");
        drtWithShiftsConfigGroup.addParameterSet(rebalancing);
        ((RebalancingParams) rebalancing).interval = 600;

        MinCostFlowRebalancingStrategyParams strategyParams = new MinCostFlowRebalancingStrategyParams();
        strategyParams.targetAlpha = 0.3;
        strategyParams.targetBeta = 0.3;

        drtWithShiftsConfigGroup.getRebalancingParams().get().addParameterSet(strategyParams);

        DrtZoneSystemParams drtZoneSystemParams = new DrtZoneSystemParams();
        SquareGridZoneSystemParams zoneParams = (SquareGridZoneSystemParams) drtZoneSystemParams.createParameterSet(SquareGridZoneSystemParams.SET_NAME);
        zoneParams.cellSize = 500.;
        drtZoneSystemParams.addParameterSet(zoneParams);
        drtZoneSystemParams.targetLinkSelection = DrtZoneSystemParams.TargetLinkSelection.mostCentral;
        drtWithShiftsConfigGroup.addParameterSet(drtZoneSystemParams);

        multiModeDrtConfigGroup.addParameterSet(drtWithShiftsConfigGroup);

        DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
        DvrpTravelTimeMatrixParams matrixParams = dvrpConfigGroup.getTravelTimeMatrixParams();
        matrixParams.addParameterSet(matrixParams.createParameterSet(SquareGridZoneSystemParams.SET_NAME));

        final Config config = ConfigUtils.createConfig(multiModeDrtConfigGroup,
                dvrpConfigGroup);
        config.setContext(ExamplesUtils.getTestScenarioURL("holzkirchen"));

        Set<String> modes = new HashSet<>();
        modes.add("drt");
        config.travelTimeCalculator().setAnalyzedModes(modes);

        ScoringConfigGroup.ModeParams scoreParams = new ScoringConfigGroup.ModeParams("drt");
        config.scoring().addModeParams(scoreParams);
        ScoringConfigGroup.ModeParams scoreParams2 = new ScoringConfigGroup.ModeParams("walk");
        config.scoring().addModeParams(scoreParams2);

        config.plans().setInputFile(plansFile);
        config.network().setInputFile(networkFile);

        config.qsim().setSimStarttimeInterpretation(QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime);
        config.qsim().setSimEndtimeInterpretation(QSimConfigGroup.EndtimeInterpretation.minOfEndtimeAndMobsimFinished);

        final ScoringConfigGroup.ActivityParams home = new ScoringConfigGroup.ActivityParams("home");
        home.setTypicalDuration(8 * 3600);
        final ScoringConfigGroup.ActivityParams other = new ScoringConfigGroup.ActivityParams("other");
        other.setTypicalDuration(4 * 3600);
        final ScoringConfigGroup.ActivityParams education = new ScoringConfigGroup.ActivityParams("education");
        education.setTypicalDuration(6 * 3600);
        final ScoringConfigGroup.ActivityParams shopping = new ScoringConfigGroup.ActivityParams("shopping");
        shopping.setTypicalDuration(2 * 3600);
        final ScoringConfigGroup.ActivityParams work = new ScoringConfigGroup.ActivityParams("work");
        work.setTypicalDuration(2 * 3600);

        config.scoring().addActivityParams(home);
        config.scoring().addActivityParams(other);
        config.scoring().addActivityParams(education);
        config.scoring().addActivityParams(shopping);
        config.scoring().addActivityParams(work);

        final ReplanningConfigGroup.StrategySettings stratSets = new ReplanningConfigGroup.StrategySettings();
        stratSets.setWeight(1);
        stratSets.setStrategyName("ChangeExpBeta");
        config.replanning().addStrategySettings(stratSets);

        config.controller().setLastIteration(1);
        config.controller().setWriteEventsInterval(1);

        config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        config.controller().setOutputDirectory("test/output/RunShiftDrtScenarioIT");

        DrtOperationsParams operationsParams = (DrtOperationsParams) drtWithShiftsConfigGroup.createParameterSet(DrtOperationsParams.SET_NAME);
        ShiftsParams shiftsParams = (ShiftsParams) operationsParams.createParameterSet(ShiftsParams.SET_NAME);
        OperationFacilitiesParams operationFacilitiesParams = (OperationFacilitiesParams) operationsParams.createParameterSet(OperationFacilitiesParams.SET_NAME);
        operationsParams.addParameterSet(shiftsParams);
        operationsParams.addParameterSet(operationFacilitiesParams);

        operationFacilitiesParams.operationFacilityInputFile = opFacilitiesFile;
        shiftsParams.shiftInputFile = shiftsFile;
        shiftsParams.allowInFieldChangeover = true;
        drtWithShiftsConfigGroup.addParameterSet(operationsParams);

        DrtFareParams drtFareParams = new DrtFareParams();
        drtFareParams.baseFare = 1.;
        drtFareParams.distanceFare_m = 1. / 1000;
        drtWithShiftsConfigGroup.addParameterSet(drtFareParams);

        final Controler run = DrtOperationsControlerCreator.createControler(config, false);
        run.run();
    }
}
