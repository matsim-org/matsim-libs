package org.matsim.contrib.drt.extension.eshifts.run;

import org.junit.Test;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystemParams;
import org.matsim.contrib.drt.extension.eshifts.optimizer.ShiftEDrtVehicleDataEntryFactory;
import org.matsim.contrib.drt.extension.shifts.config.DrtWithShiftsConfigGroup;
import org.matsim.contrib.drt.extension.shifts.config.DrtShiftParams;
import org.matsim.contrib.drt.optimizer.insertion.extensive.ExtensiveInsertionSearchParams;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingParams;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingStrategyParams;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.charging.*;
import org.matsim.contrib.ev.temperature.TemperatureService;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.examples.ExamplesUtils;

import java.util.HashSet;
import java.util.Set;

public class RunEShiftDrtScenarioIT {

	private static final double MAX_RELATIVE_SOC = 0.9;// charge up to 80% SOC
	private static final double MIN_RELATIVE_SOC = 0.15;// send to chargers vehicles below 20% SOC
	private static final double TEMPERATURE = 20;// oC

	@Test
	public void test() {

		MultiModeDrtConfigGroup multiModeDrtConfigGroup = new MultiModeDrtConfigGroup(DrtWithShiftsConfigGroup::new);

		String fleetFile =  "holzkirchenFleet.xml";
		String plansFile =  "holzkirchenPlans.xml.gz";
		String networkFile =  "holzkirchenNetwork.xml.gz";
		String opFacilitiesFile =  "holzkirchenOperationFacilities.xml";
		String shiftsFile =  "holzkirchenShifts.xml";
		String chargersFile =  "holzkirchenChargers.xml";
		String evsFile =  "holzkirchenElectricFleet.xml";

		DrtWithShiftsConfigGroup drtWithShiftsConfigGroup = (DrtWithShiftsConfigGroup) multiModeDrtConfigGroup.createParameterSet("drt");

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
		config.controler().setOutputDirectory("test/output/holzkirchen_eshifts");

		DrtShiftParams drtShiftParams = (DrtShiftParams) drtWithShiftsConfigGroup.createParameterSet(DrtShiftParams.SET_NAME);
		drtShiftParams.setOperationFacilityInputFile(opFacilitiesFile);
		drtShiftParams.setShiftInputFile(shiftsFile);
		drtShiftParams.setAllowInFieldChangeover(true);

		//e shifts
		drtShiftParams.setShiftAssignmentBatteryThreshold(0.6);
		drtShiftParams.setChargeAtHubThreshold(0.8);
		drtShiftParams.setOutOfShiftChargerType("slow");
		drtShiftParams.setBreakChargerType("fast");

		drtWithShiftsConfigGroup.addParameterSet(drtShiftParams);

		final EvConfigGroup evConfigGroup = new EvConfigGroup();
		evConfigGroup.setChargersFile(chargersFile);
		evConfigGroup.setMinimumChargeTime(0);
		evConfigGroup.setVehiclesFile(evsFile);
		evConfigGroup.setTimeProfiles(true);
		config.addModule(evConfigGroup);


		final Controler run = EvShiftDrtControlerCreator.createControler(config, false);

		for (DrtConfigGroup drtCfg : MultiModeDrtConfigGroup.get(config).getModalElements()) {
			run.addOverridingModule(new AbstractDvrpModeModule(drtCfg.getMode()) {
				@Override
				public void install() {
					bind(ShiftEDrtVehicleDataEntryFactory.ShiftEDrtVehicleDataEntryFactoryProvider.class).toInstance(
							new ShiftEDrtVehicleDataEntryFactory.ShiftEDrtVehicleDataEntryFactoryProvider(drtCfg, MIN_RELATIVE_SOC));
				}
			});
		}

		run.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(ChargingLogic.Factory.class).toProvider(new ChargingWithQueueingAndAssignmentLogic.FactoryProvider(
						charger -> new ChargeUpToMaxSocStrategy(charger, MAX_RELATIVE_SOC)));
				bind(ChargingPower.Factory.class).toInstance(FastThenSlowCharging::new);
				bind(TemperatureService.class).toInstance(linkId -> TEMPERATURE);
			}
		});
		run.run();
	}
}
