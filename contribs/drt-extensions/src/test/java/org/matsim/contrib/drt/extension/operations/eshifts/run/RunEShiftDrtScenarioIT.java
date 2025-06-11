package org.matsim.contrib.drt.extension.operations.eshifts.run;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.common.zones.systems.grid.square.SquareGridZoneSystemParams;
import org.matsim.contrib.drt.extension.DrtWithExtensionsConfigGroup;
import org.matsim.contrib.drt.extension.operations.DrtOperationsParams;

import org.matsim.contrib.drt.extension.operations.EDrtOperationsControlerCreator;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilitiesParams;
import org.matsim.contrib.drt.extension.operations.shifts.config.ShiftsParams;
import org.matsim.contrib.drt.optimizer.constraints.DrtOptimizationConstraintsSetImpl;
import org.matsim.contrib.drt.optimizer.insertion.extensive.ExtensiveInsertionSearchParams;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingParams;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingStrategyParams;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.EvConfigGroup.EvAnalysisOutput;
import org.matsim.contrib.ev.charging.*;
import org.matsim.contrib.ev.temperature.TemperatureService;
import org.matsim.contrib.zone.skims.DvrpTravelTimeMatrixParams;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.examples.ExamplesUtils;

import com.google.inject.Key;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class RunEShiftDrtScenarioIT {

	private static final double MAX_RELATIVE_SOC = 0.9;// charge up to 80% SOC
	private static final double TEMPERATURE = 20;// oC

	@Test
	void test() {

		MultiModeDrtConfigGroup multiModeDrtConfigGroup = new MultiModeDrtConfigGroup(DrtWithExtensionsConfigGroup::new);

		String fleetFile =  "holzkirchenFleet.xml";
		String plansFile =  "holzkirchenPlans.xml.gz";
		String networkFile =  "holzkirchenNetwork.xml.gz";
		String opFacilitiesFile =  "holzkirchenOperationFacilities.xml";
		String shiftsFile =  "holzkirchenShifts.xml";
		String chargersFile =  "holzkirchenChargers.xml";
		String evsFile =  "holzkirchenElectricFleet.xml";

		DrtWithExtensionsConfigGroup drtWithShiftsConfigGroup = (DrtWithExtensionsConfigGroup) multiModeDrtConfigGroup.createParameterSet("drt");

		DrtConfigGroup drtConfigGroup = drtWithShiftsConfigGroup;
		drtConfigGroup.setMode(TransportMode.drt);
		DrtOptimizationConstraintsSetImpl constraintsSet =
                drtConfigGroup.addOrGetDrtOptimizationConstraintsParams()
                        .addOrGetDefaultDrtOptimizationConstraintsSet();
		constraintsSet.setMaxTravelTimeAlpha(1.5);
        constraintsSet.setMaxTravelTimeBeta(10. * 60.);
		drtConfigGroup.setStopDuration(30.);
        constraintsSet.setMaxWaitTime(600.);
        constraintsSet.setRejectRequestIfMaxWaitOrTravelTimeViolated(true);
		drtConfigGroup.setUseModeFilteredSubnetwork(false);
		drtConfigGroup.setVehiclesFile(fleetFile);
		drtConfigGroup.setOperationalScheme(DrtConfigGroup.OperationalScheme.door2door);
		drtConfigGroup.setPlotDetailedCustomerStats(true);
        constraintsSet.setMaxWalkDistance(1000.);
		drtConfigGroup.setIdleVehiclesReturnToDepots(false);

		drtConfigGroup.addParameterSet(new ExtensiveInsertionSearchParams());

		ConfigGroup rebalancing = drtConfigGroup.createParameterSet("rebalancing");
		drtConfigGroup.addParameterSet(rebalancing);
		((RebalancingParams) rebalancing).setInterval(600);

		MinCostFlowRebalancingStrategyParams strategyParams = new MinCostFlowRebalancingStrategyParams();
		strategyParams.setTargetAlpha(0.3);
		strategyParams.setTargetBeta(0.3);

		Optional<RebalancingParams> rebalancingParams = drtConfigGroup.getRebalancingParams();
		rebalancingParams.get().addParameterSet(strategyParams);

		ConfigGroup parameterSet = rebalancing.createParameterSet(SquareGridZoneSystemParams.SET_NAME);
		((SquareGridZoneSystemParams) parameterSet).setCellSize(500.);
		rebalancing.addParameterSet(parameterSet);
		rebalancingParams.get().setTargetLinkSelection(RebalancingParams.TargetLinkSelection.mostCentral);
		drtWithShiftsConfigGroup.addParameterSet(parameterSet);


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
		config.controller().setOutputDirectory("test/output/holzkirchen_eshifts");

		DrtOperationsParams operationsParams = (DrtOperationsParams) drtWithShiftsConfigGroup.createParameterSet(DrtOperationsParams.SET_NAME);
		ShiftsParams shiftsParams = (ShiftsParams) operationsParams.createParameterSet(ShiftsParams.SET_NAME);
		OperationFacilitiesParams operationFacilitiesParams = (OperationFacilitiesParams) operationsParams.createParameterSet(OperationFacilitiesParams.SET_NAME);
		operationsParams.addParameterSet(shiftsParams);
		operationsParams.addParameterSet(operationFacilitiesParams);

		operationFacilitiesParams.setOperationFacilityInputFile(opFacilitiesFile);
		shiftsParams.setShiftInputFile(shiftsFile);
		shiftsParams.setAllowInFieldChangeover(true);

		//e shifts
		shiftsParams.setShiftAssignmentBatteryThreshold(0.6);
		shiftsParams.setChargeAtHubThreshold(0.8);
		shiftsParams.setOutOfShiftChargerType("slow");
		shiftsParams.setBreakChargerType("fast");

		drtWithShiftsConfigGroup.addParameterSet(operationsParams);

		final EvConfigGroup evConfigGroup = new EvConfigGroup();
		evConfigGroup.setChargersFile(chargersFile);
		evConfigGroup.setMinimumChargeTime(0);
		evConfigGroup.setAnalysisOutputs(Set.of(EvAnalysisOutput.TimeProfiles));
		config.addModule(evConfigGroup);

		config.vehicles().setVehiclesFile(evsFile);

		final Controler controler = EDrtOperationsControlerCreator.createControler(config, false);

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(ChargingLogic.Factory.class).to(ChargingWithQueueingAndAssignmentLogic.Factory.class);
				bind(Key.get(ChargingStrategy.Factory.class, DvrpModes.mode(drtConfigGroup.getMode()))).toInstance(new ChargeUpToMaxSocStrategy.Factory(MAX_RELATIVE_SOC));
				bind(ChargingPower.Factory.class).toInstance(FastThenSlowCharging::new);
				bind(TemperatureService.class).toInstance(linkId -> TEMPERATURE);
			}
		});
		controler.run();
	}
}
