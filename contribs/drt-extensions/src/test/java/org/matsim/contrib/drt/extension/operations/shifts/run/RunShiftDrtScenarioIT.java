package org.matsim.contrib.drt.extension.operations.shifts.run;

import org.junit.Test;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystemParams;
import org.matsim.contrib.drt.extension.operations.DrtOperationsControlerCreator;
import org.matsim.contrib.drt.extension.operations.DrtOperationsParams;
import org.matsim.contrib.drt.extension.operations.DrtWithOperationsConfigGroup;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilitiesParams;
import org.matsim.contrib.drt.extension.operations.shifts.config.ShiftsParams;
import org.matsim.contrib.drt.optimizer.insertion.extensive.ExtensiveInsertionSearchParams;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingParams;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingStrategyParams;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.examples.ExamplesUtils;

import java.util.HashSet;
import java.util.Set;

public class RunShiftDrtScenarioIT {

	@Test
	public void test() {

		MultiModeDrtConfigGroup multiModeDrtConfigGroup = new MultiModeDrtConfigGroup(DrtWithOperationsConfigGroup::new);

		String fleetFile = "holzkirchenFleet.xml";
		String plansFile = "holzkirchenPlans.xml.gz";
		String networkFile = "holzkirchenNetwork.xml.gz";
		String opFacilitiesFile = "holzkirchenOperationFacilities.xml";
		String shiftsFile = "holzkirchenShifts.xml";

		DrtWithOperationsConfigGroup drtWithShiftsConfigGroup = (DrtWithOperationsConfigGroup) multiModeDrtConfigGroup.createParameterSet("drt");

		DrtConfigGroup drtConfigGroup = drtWithShiftsConfigGroup;
		drtConfigGroup.mode = TransportMode.drt;
		drtConfigGroup.maxTravelTimeAlpha = 1.5;
		drtConfigGroup.maxTravelTimeBeta = 10. * 60.;
		drtConfigGroup.stopDuration = 30.;
		drtConfigGroup.maxWaitTime = 600.;
		drtConfigGroup.rejectRequestIfMaxWaitOrTravelTimeViolated = true;
		drtConfigGroup.useModeFilteredSubnetwork = false;
		drtConfigGroup.vehiclesFile = fleetFile;
		drtConfigGroup.operationalScheme = DrtConfigGroup.OperationalScheme.door2door;
		drtConfigGroup.plotDetailedCustomerStats = true;
		drtConfigGroup.maxWalkDistance = 1000.;
		drtConfigGroup.idleVehiclesReturnToDepots = false;

		drtConfigGroup.addParameterSet(new ExtensiveInsertionSearchParams());

		ConfigGroup rebalancing = drtConfigGroup.createParameterSet("rebalancing");
		drtConfigGroup.addParameterSet(rebalancing);
		((RebalancingParams) rebalancing).interval = 600;

		MinCostFlowRebalancingStrategyParams strategyParams = new MinCostFlowRebalancingStrategyParams();
		strategyParams.targetAlpha = 0.3;
		strategyParams.targetBeta = 0.3;

		drtConfigGroup.getRebalancingParams().get().addParameterSet(strategyParams);

		DrtZonalSystemParams drtZonalSystemParams = new DrtZonalSystemParams();
		drtZonalSystemParams.zonesGeneration = DrtZonalSystemParams.ZoneGeneration.GridFromNetwork;
		drtZonalSystemParams.cellSize = 500.;
		drtZonalSystemParams.targetLinkSelection = DrtZonalSystemParams.TargetLinkSelection.mostCentral;
		drtConfigGroup.addParameterSet(drtZonalSystemParams);

		multiModeDrtConfigGroup.addParameterSet(drtWithShiftsConfigGroup);

		final Config config = ConfigUtils.createConfig(multiModeDrtConfigGroup,
				new DvrpConfigGroup());
		config.setContext(ExamplesUtils.getTestScenarioURL("holzkirchen"));

		Set<String> modes = new HashSet<>();
		modes.add("drt");
		config.travelTimeCalculator().setAnalyzedModes(modes);

		PlanCalcScoreConfigGroup.ModeParams scoreParams = new PlanCalcScoreConfigGroup.ModeParams("drt");
		config.planCalcScore().addModeParams(scoreParams);
		PlanCalcScoreConfigGroup.ModeParams scoreParams2 = new PlanCalcScoreConfigGroup.ModeParams("walk");
		config.planCalcScore().addModeParams(scoreParams2);

		config.plans().setInputFile(plansFile);
		config.network().setInputFile(networkFile);

		config.qsim().setSimStarttimeInterpretation(QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime);
		config.qsim().setSimEndtimeInterpretation(QSimConfigGroup.EndtimeInterpretation.minOfEndtimeAndMobsimFinished);

		final PlanCalcScoreConfigGroup.ActivityParams home = new PlanCalcScoreConfigGroup.ActivityParams("home");
		home.setTypicalDuration(8 * 3600);
		final PlanCalcScoreConfigGroup.ActivityParams other = new PlanCalcScoreConfigGroup.ActivityParams("other");
		other.setTypicalDuration(4 * 3600);
		final PlanCalcScoreConfigGroup.ActivityParams education = new PlanCalcScoreConfigGroup.ActivityParams("education");
		education.setTypicalDuration(6 * 3600);
		final PlanCalcScoreConfigGroup.ActivityParams shopping = new PlanCalcScoreConfigGroup.ActivityParams("shopping");
		shopping.setTypicalDuration(2 * 3600);
		final PlanCalcScoreConfigGroup.ActivityParams work = new PlanCalcScoreConfigGroup.ActivityParams("work");
		work.setTypicalDuration(2 * 3600);

		config.planCalcScore().addActivityParams(home);
		config.planCalcScore().addActivityParams(other);
		config.planCalcScore().addActivityParams(education);
		config.planCalcScore().addActivityParams(shopping);
		config.planCalcScore().addActivityParams(work);

		final StrategyConfigGroup.StrategySettings stratSets = new StrategyConfigGroup.StrategySettings();
		stratSets.setWeight(1);
		stratSets.setStrategyName("ChangeExpBeta");
		config.strategy().addStrategySettings(stratSets);

		config.controler().setLastIteration(1);
		config.controler().setWriteEventsInterval(1);

		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory("test/output/holzkirchen_shifts");

		DrtOperationsParams operationsParams = (DrtOperationsParams) drtWithShiftsConfigGroup.createParameterSet(DrtOperationsParams.SET_NAME);
		ShiftsParams shiftsParams = (ShiftsParams) operationsParams.createParameterSet(ShiftsParams.SET_NAME);
		OperationFacilitiesParams operationFacilitiesParams = (OperationFacilitiesParams) operationsParams.createParameterSet(OperationFacilitiesParams.SET_NAME);
		operationsParams.addParameterSet(shiftsParams);
		operationsParams.addParameterSet(operationFacilitiesParams);

		operationFacilitiesParams.operationFacilityInputFile = opFacilitiesFile;
		shiftsParams.shiftInputFile = shiftsFile;
		shiftsParams.allowInFieldChangeover = true;
		drtWithShiftsConfigGroup.addParameterSet(operationsParams);

		final Controler run = DrtOperationsControlerCreator.createControler(config, false);
		run.run();
	}
}
