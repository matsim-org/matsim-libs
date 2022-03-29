package org.matsim.contrib.drt.extension.eshifts.run;

import org.junit.Test;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystemParams;
import org.matsim.contrib.drt.extension.eshifts.optimizer.ShiftEDrtVehicleDataEntryFactory;
import org.matsim.contrib.drt.extension.shifts.config.ShiftDrtConfigGroup;
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

		MultiModeDrtConfigGroup multiModeDrtConfigGroup = new MultiModeDrtConfigGroup();

		String fleetFile =  "holzkirchenFleet.xml";
		String plansFile =  "holzkirchenPlans.xml.gz";
		String networkFile =  "holzkirchenNetwork.xml.gz";
		String opFacilitiesFile =  "holzkirchenOperationFacilities.xml";
		String shiftsFile =  "holzkirchenShifts.xml";
		String chargersFile =  "holzkirchenChargers.xml";
		String evsFile =  "holzkirchenElectricFleet.xml";

		DrtConfigGroup drtConfigGroup = new DrtConfigGroup().setMode(TransportMode.drt)
				.setMaxTravelTimeAlpha(1.5)
				.setMaxTravelTimeBeta(10. * 60.)
				.setStopDuration(30.)
				.setMaxWaitTime(600.)
				.setRejectRequestIfMaxWaitOrTravelTimeViolated(true)
				.setUseModeFilteredSubnetwork(false)
				.setVehiclesFile(fleetFile)
				.setOperationalScheme(DrtConfigGroup.OperationalScheme.door2door)
				.setPlotDetailedCustomerStats(true)
				.setMaxWalkDistance(1000.)
				.setIdleVehiclesReturnToDepots(false);

		drtConfigGroup.addParameterSet(new ExtensiveInsertionSearchParams());

		ConfigGroup rebalancing = drtConfigGroup.createParameterSet("rebalancing");
		drtConfigGroup.addParameterSet(rebalancing);
		((RebalancingParams) rebalancing).setInterval(600);

		MinCostFlowRebalancingStrategyParams strategyParams = new MinCostFlowRebalancingStrategyParams();
		strategyParams.setTargetAlpha(0.3);
		strategyParams.setTargetBeta(0.3);

		drtConfigGroup.getRebalancingParams().get().addParameterSet((ConfigGroup) strategyParams);

		DrtZonalSystemParams drtZonalSystemParams = new DrtZonalSystemParams();
		drtZonalSystemParams.setZonesGeneration(DrtZonalSystemParams.ZoneGeneration.GridFromNetwork);
		drtZonalSystemParams.setCellSize(500.);
		drtZonalSystemParams.setTargetLinkSelection(DrtZonalSystemParams.TargetLinkSelection.mostCentral);
		drtConfigGroup.addParameterSet(drtZonalSystemParams);

		multiModeDrtConfigGroup.addParameterSet(drtConfigGroup);

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

		ShiftDrtConfigGroup shiftDrtConfigGroup = ConfigUtils.addOrGetModule(config, ShiftDrtConfigGroup.class);
		shiftDrtConfigGroup.setOperationFacilityInputFile(opFacilitiesFile);
		shiftDrtConfigGroup.setShiftInputFile(shiftsFile);
		shiftDrtConfigGroup.setAllowInFieldChangeover(true);

		//e shifts
		shiftDrtConfigGroup.setShiftAssignmentBatteryThreshold(0.6);
		shiftDrtConfigGroup.setChargeAtHubThreshold(0.8);
		shiftDrtConfigGroup.setOutOfShiftChargerType("slow");
		shiftDrtConfigGroup.setBreakChargerType("fast");

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
